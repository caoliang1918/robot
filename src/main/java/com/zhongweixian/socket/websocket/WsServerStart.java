package com.zhongweixian.socket.websocket;

import com.zhongweixian.socket.websocket.channel.WebSocketChannelInitaializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

@Component
public class WsServerStart {
    private Logger logger = LoggerFactory.getLogger(WsServerStart.class);

    @Value("${ws.port}")
    private int port;

    private EventLoopGroup boosGrop = new NioEventLoopGroup();
    private EventLoopGroup workerGrop = new NioEventLoopGroup();

    @PostConstruct
    private void start() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            //使用服务端初始化自定义类WebSocketChannelInitaializer
            serverBootstrap.group(boosGrop, workerGrop).channel(NioServerSocketChannel.class).
                    localAddress(new InetSocketAddress(port)).
                    childHandler(new WebSocketChannelInitaializer(new WsMessgaeService() {
                        @Override
                        public void readMessage(String message) {
                            logger.info("接受到客户端消息:{}", message);
                        }
                    }));

            //使用了不同的端口绑定方式
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            if (channelFuture.isSuccess()) {
                logger.info("websocket started on port :{} (websocket)", port);
            }
            //阻塞关闭连接，需要使用子线程
            //channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @PreDestroy
    public void destory() {
        boosGrop.shutdownGracefully().syncUninterruptibly();
        workerGrop.shutdownGracefully().syncUninterruptibly();
        logger.info("websocket stop");
    }

    /**
     * 发送消息
     *
     * @param message
     */
    public void sendMessage(String message) {

    }

}
