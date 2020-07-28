package com.lvmama.finance.netty.chat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * 在该类中处理客户端上线，下线等
 */
@Slf4j
public class ChatServerHandler extends SimpleChannelInboundHandler<String> {
    // 该类需要管理所有客户端的所有连接
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 处理数据
     * 如果客户端a发送数据到服务端，则服务端广播数据到所有的客户端。其中a的格式为：自己 + 数据，其他客户端：a客户端地址+数据
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel channel = ctx.channel();
        // 向所有的客户端广播数据
        channelGroup.forEach(ch -> {
            if (ch == channel) {
                // 向数据来源客户端广播数据
                ch.writeAndFlush("自己发送消息：" + msg + "\n");
            } else {
                // 向其他客户端广播数据
                ch.writeAndFlush(channel.remoteAddress() + " 发送消息：" + msg + "\n");
            }
        });
    }

    /**
     * 新的连接加入（客户端加入）
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        // 通知除了此客户端之外的所有客户端
        channelGroup.writeAndFlush("【服务器】- " + channel.remoteAddress() + " 加入\n");
        // 将该客户端加入到客户端组中
        channelGroup.add(channel);
    }

    /**
     * 连接断开（客户端断开）
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();

        // 告诉其他客户端该客户端离开了
        channelGroup.writeAndFlush("【服务器】- " + channel.remoteAddress() + " 离开\n");
        // 此时不需要 remove方法删除，netty会自行处理，至于netty怎么获取我们自定定义的channelGrop，先不关心
//        channelGroup.remove(channel);
        log.info("当前在线客户端数量：{}", channelGroup.size());
    }

    // 在服务器端打印客户端上线，下线
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("客户端{}上线", channel.remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("客户端{}下线", channel.remoteAddress());
    }

    // 发生异常，关闭连接
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
