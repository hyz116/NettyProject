package com.lvmama.finance.nio.echo;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class EchoServer {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private int port = 8899;

    public EchoServer() throws IOException {
        // 创建Selector对象
        this.selector = Selector.open();
        // 创建一个ServerSocketChannel对象
        this.serverSocketChannel = ServerSocketChannel.open();
        // 使得在同一主机上关闭了服务器程序，紧接着再启动服务器程序时，可以顺利地绑定到相同的端口
        this.serverSocketChannel.socket().setReuseAddress(true);
        // 设置ServerSocketChannel以非阻塞方式工作
        this.serverSocketChannel.configureBlocking(false);
        // 将服务器进程与一个本地端口绑定
        this.serverSocketChannel.socket().bind(new InetSocketAddress(this.port));
        log.info("服务器启动，监听端口：{}", this.port);
    }

    public static void main(String[] args) throws IOException {
        new EchoServer().service();
    }

    // 主方法
    public void service() throws IOException {
        // 向Selector注册 接收连接就绪事件
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (this.selector.select() > 0) { // Selector#select()方法返回当前相关事件已经发生的SelectionKey的个数，如果没有，就会阻塞下去
            Set<SelectionKey> readyKeys = selector.selectedKeys();  // 获取Selector的selected-keys集合，该集合存放了相关事件已经发生的SelectionKey对象
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = null;
                try {
                    selectionKey = iterator.next();
                    iterator.remove();  // 把SelectionKey对象从selected-keys集合中移除

                    if (selectionKey.isAcceptable()) { // 处理接收连接就绪事件
                        this.doAcceptable(selectionKey);
                    }

                    if (selectionKey.isReadable()) { // 处理读就绪事件
                        this.receive(selectionKey);
                    }

                    if (selectionKey.isWritable()) { // 处理写就绪事件
                        this.send(selectionKey);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // 如果发生异常使SelectionKey失效
                    if (selectionKey != null) {
                        // 使得Selector不在监控这个SelectionKey对象感兴趣的事件
                        selectionKey.cancel();
                        // 关闭与这个SelectionKey关联的SocketChannel
                        selectionKey.channel().close();
                    }
                }
            }
        }
    }



    private void doAcceptable(SelectionKey selectionKey) throws IOException {

        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        log.info("接收到客户连接，来自：{}", socketChannel.socket().getInetAddress() + ":" + socketChannel.socket().getPort());
        socketChannel.configureBlocking(false);   // 设置SocketChannel为非阻塞模式
        ByteBuffer buffer = ByteBuffer.allocate(1024); // 创建一个用于存放用户发来数据的缓冲区，作为附件，作为读和写的共享数据
        socketChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, buffer); // 注册读就绪和写就绪事件，关联一个附件
    }

    // 发送数据
    private void send(SelectionKey selectionKey) throws IOException {
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment(); // 获取与SelectionKey对象关联的附件
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        // 把极限值设置为位置值，再把位置值设置为0
        buffer.flip();
        // 按照指定的编码，把buffer中的字节序列解码为字符串
        String data = CodeUtil.decode(buffer);
        if (data.indexOf("\r\n") == -1) {   // 还没有读到一行数据，返回
            return;
        }

        String outputData = data.substring(0, data.indexOf("\n") + 1);  // 截取一行数据
        log.info("待发送数据：{}", outputData);

        ByteBuffer outputBuffer = CodeUtil.encode("echo:" + outputData); // 将待发送数据放到outputBuffer中
        // 发送一行字符串，判断是否还有未发送完的字节
        while (outputBuffer.hasRemaining()) {
            socketChannel.write(outputBuffer);
        }

        ByteBuffer tempBuffer = CodeUtil.encode(outputData);
        // 把buffer的位置设置为tempBuffer的极限值
        buffer.position(tempBuffer.limit());
        // 删除已经处理的字符串
        buffer.compact();

        if (outputData.equals("Bye\r\n")) { // 如果已经输出了“Byte\r\n”，就使SelectinKey实现，并关闭SocketChannel
            selectionKey.cancel();
            socketChannel.close();
            log.info("关闭与客户的连接");
        }
    }

    // 处理接收到的数据
    private void receive(SelectionKey selectionKey) throws IOException {
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();  // 获取与SelectionKey对象关联的附件

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer readBuff = ByteBuffer.allocate(32);  // 创建一个Bytebuffer,用于存放读取到的数据
        socketChannel.read(readBuff);
        readBuff.flip();

        buffer.limit(buffer.capacity());  // 把buffer的极限值改为容量值
        buffer.put(readBuff);   // 把读到的数据放到buffer中去，此处假设buffer的容量足够大，不会出现缓冲区溢出异常，后期可使用ChannelIO类提供的容量可增长的缓冲区
    }
}
