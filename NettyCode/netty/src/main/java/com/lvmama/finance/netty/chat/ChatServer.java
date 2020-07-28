package com.lvmama.finance.netty.chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 描述：
 * 多个客户端与服务端进行连接互相发送数据。假如有a、b、c三个客户端，s为服务端
 * 当客户端与服务端建立连接，通知其他客户端，该客户端上线。
 * 当客户端发送数据到服务端s，服务端s广播数据到所有的客户端
 *
 * <p>
 *     对于即时通信，例如微信。很多客户端与服务端通过长连接建立连接。
 *     当服务端向某个客户端推送数据，此时该客户端离线，则会将消息存储起来。下一次客户端与服务端建立连接后，会去取出离线消息，再做发送操作。
 *
 */
public class ChatServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler())
                    .childHandler(new ChatServerInitializer());

            ChannelFuture future = serverBootstrap.bind(8855).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
