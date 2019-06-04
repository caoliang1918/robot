package com.zhongweixian.socket.tcp;

import com.zhongweixian.socket.tcp.channel.NettyChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 *
 */
@Component
public class TcpServerStart {
    private Logger logger = LoggerFactory.getLogger(TcpServerStart.class);


    private EventLoopGroup boss = new NioEventLoopGroup();
    private EventLoopGroup work = new NioEventLoopGroup(10);

    @Value("${tcp.port}")
    private int port;


    @PostConstruct
    public void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new NettyChannelInitializer(new TcpMessageService() {
                    @Override
                    public void readMessgae(String message) {
                        logger.info("接受到客户端消息:{}" , message);
                    }
                }));
        ChannelFuture channelFuture = serverBootstrap.bind().sync();
        if (channelFuture.isSuccess()) {
            logger.info("netty server started on port :{} (tcp)", port);
        }
    }

    @PreDestroy
    public void destory() {
        boss.shutdownGracefully().syncUninterruptibly();
        work.shutdownGracefully().syncUninterruptibly();
        logger.info("netty stop");
    }


    public void sendMesage(String clientId, String messageReq) {
        NioSocketChannel nioSocketChannel = ChannelRepository.getChannel(clientId);
        if (nioSocketChannel == null || !nioSocketChannel.isOpen()) {
            logger.error("client : {} not exist or is closed!", clientId);
            return;
        }
        ChannelFuture future = nioSocketChannel.writeAndFlush(Unpooled.copiedBuffer(messageReq, CharsetUtil.UTF_8));
        future.addListener((ChannelFutureListener) channelFuture ->
                logger.info("send to clientId:{} message success:{}", clientId, messageReq));
    }
}
