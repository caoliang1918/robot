package com.zhongweixian.socket.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zhongweixian.server.tcp.NettyServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 *
 */
@Component
public class TcpServerStart {
    private Logger logger = LoggerFactory.getLogger(TcpServerStart.class);


    @Value("${tcp.port}")
    private int port;

    @Autowired
    private TcpServerHandler tcpServerHandler;

    private NettyServer nettyServer;


    @PostConstruct
    public void start() {
        nettyServer = new NettyServer(port, tcpServerHandler);
        nettyServer.start();
    }

    @PreDestroy
    public void destory() {
        if (nettyServer != null) {
            nettyServer.close();
            logger.info("netty stop");
        }
    }


    public void sendMesage(String clientId, String messageReq) {

    }
}
