package com.zhongweixian.socket.tcp.channel;

import com.zhongweixian.socket.tcp.TcpMessageService;
import com.zhongweixian.socket.tcp.decoder.MessageDecoder;
import com.zhongweixian.socket.tcp.handler.NettyServerHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * <p>
 * 服务端
 */
public class NettyChannelInitializer extends ChannelInitializer<Channel> {

    private TcpMessageService tcpMessageService;

    public NettyChannelInitializer(TcpMessageService tcpMessageService){
        this.tcpMessageService = tcpMessageService;
    }
    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline().addLast("timeout", new IdleStateHandler(10, 0, 0))
                .addLast(new MessageDecoder())
                .addLast(new NettyServerHandler(tcpMessageService));

    }
}
