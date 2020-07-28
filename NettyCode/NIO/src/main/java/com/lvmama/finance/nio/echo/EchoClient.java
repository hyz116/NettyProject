package com.lvmama.finance.nio.echo;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class EchoClient {
    private static final int PORT = 8899;
    private SocketChannel socketChannel;
    // 接收控制台输入的数据，发送到服务端
    private ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
    // 接收服务端发送来的数据
    private ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);

    private Selector selector;

    public EchoClient() throws IOException {
        this.socketChannel = SocketChannel.open();
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, PORT);
        // 采用阻塞模式连接服务器
        this.socketChannel.connect(inetSocketAddress);
        // 设置为非阻塞模式
        this.socketChannel.configureBlocking(false);
        log.info("与服务器连接成功");
        this.selector = Selector.open();
    }

    public static void main(String[] args) throws IOException {
        final EchoClient echoClient = new EchoClient();

        Thread receive = new Thread(() -> {
            echoClient.receiveFromUser();   // 获取用户向控制台输入的数据
        });
        receive.start(); // 启动receive线程

        echoClient.talk();
    }

    /**
     * 接收用户从控制台输入的数据，将数据放置到sendBuffer中
     */
    private void receiveFromUser() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String msg = null;
            while ((msg = bufferedReader.readLine()) != null) {
                // 主线程在执行sendDataToServer方法时会对sendBuffer进行操作
                synchronized (sendBuffer) {
                    sendBuffer.put(CodeUtil.encode(msg + "\r\n"));
                }
                if ("bye".equals(msg)) {
                    break;
                }
            }
        } catch (IOException ex) {}
    }


    /**
     * 接收和发送数据
     * 向Selector注册读就绪和写就绪事件，然后轮询已经发生的事件，并做相应的处理。
     * @throws IOException
     */
    private void talk() throws IOException {
        this.socketChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        while (this.selector.select() > 0) {
            Set<SelectionKey> readyKeys = this.selector.selectedKeys();
            Iterator<SelectionKey> ite = readyKeys.iterator();
            while (ite.hasNext()) {
                SelectionKey selectionKey = null;
                try {
                    selectionKey = ite.next();
                    ite.remove();

                    if (selectionKey.isReadable()) {
                        this.receiveDataFromServer(selectionKey);
                    } else if (selectionKey.isWritable()) {
                        this.sendDataToServer(selectionKey);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    selectionKey.cancel();
                    selectionKey.channel().close();
                }
            }
        }
    }

    // 发送数据到服务端
    private void sendDataToServer(SelectionKey selectionKey) throws IOException {
        // 将sendbuffer中的数据发送到服务端
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        synchronized (sendBuffer) {
            sendBuffer.flip();
            socketChannel.write(sendBuffer);
            // 删除已经发送的数据
            sendBuffer.compact();
        }
    }
    // 从服务端接收数据，把数据放到receiveBuffer中
    // 如果receiveBuffer有一行数据，就打印这行数据，之后删除
    private void receiveDataFromServer(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        socketChannel.read(receiveBuffer);
        receiveBuffer.flip();
        String receiveData = CodeUtil.decode(receiveBuffer);
        if (receiveData.indexOf("\n") == -1) {
            return;
        }
        String outputData = receiveData.substring(0, receiveData.indexOf("\n") + 1);
        System.out.println(outputData);

        if ("echo:bye\r\n".equals(outputData)) {
            selectionKey.channel();
            socketChannel.close();
            log.info("关闭与服务器的连接");
            selector.close();
            // 结束程序
            System.exit(0);
        }

        // 删除已经打印的数据
        ByteBuffer temp = CodeUtil.encode(outputData);
        receiveBuffer.position(temp.limit());
        receiveBuffer.compact();
    }

}
