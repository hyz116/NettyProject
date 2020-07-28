package com.lvmama.finance.netty.echo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

@Slf4j
public class EchoServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) {
        // 如果业务处理耗时，不应该阻塞 reactor 线程

        log.info("接收到客户端数据：{}", s);

        // 将数据写回客户端，加一个随机数
        String msg = s + "-" +  new SecureRandom().nextInt(1000);
        channelHandlerContext.writeAndFlush(msg);
    }
}
