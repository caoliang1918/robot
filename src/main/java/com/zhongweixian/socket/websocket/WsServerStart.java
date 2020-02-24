package com.zhongweixian.socket.websocket;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zhongweixian.listener.ConnectionListener;
import org.zhongweixian.server.websocket.WebSocketServer;

import javax.annotation.PostConstruct;

@Component
public class WsServerStart {
    private Logger logger = LoggerFactory.getLogger(WsServerStart.class);

    @Value("${ws.port}")
    private int port;

    private EventLoopGroup boosGrop = new NioEventLoopGroup();
    private EventLoopGroup workerGrop = new NioEventLoopGroup();

    @PostConstruct
    private void start() {
        WebSocketServer webSocketServer = new WebSocketServer(port, 60, "ws", new ConnectionListener() {
            @Override
            public void onClose(Channel channel, int i, String s) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onFail(int i, String s) {

            }

            @Override
            public void onMessage(Channel channel, String s) throws Exception {
                logger.info("received client:{}  message:{}", channel.id(), s);
                JSONObject json = JSONObject.parseObject(s);
                if (json == null || !json.containsKey("cmd")) {
                    return;
                }
                String cmd = json.getString("cmd");
                switch (cmd) {
                    case "logon":
                        channel.writeAndFlush(new TextWebSocketFrame("{\"reason\":\"上班成功\",\"type\":\"logon\",\"systemtime\":\"2020-02-06 10:04:41\",\"status\":0}"));
                        logger.info("send chient:{}  login success", channel.id());
                        break;

                    case "agentidle":
                        channel.writeAndFlush(new TextWebSocketFrame("{\"reason\":\"示闲成功\",\"type\":\"agentidle\",\"status\":0}"));
                        break;

                    case "agentbusy":
                        channel.writeAndFlush(new TextWebSocketFrame("{\"reason\":\"示忙成功\",\"agentkey\":\"1001@May1\",\"type\":\"agentbusy\",\"status\":0}"));
                        break;

                }
            }

            @Override
            public void onMessage(Channel channel, ByteBuf byteBuf) throws Exception {

            }

            @Override
            public void connect(Channel channel) throws Exception {
                logger.info("has client connected:{}", channel);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            //channel.writeAndFlush(new TextWebSocketFrame("{\"type\":\"pong\",\"systemtime\":\"2020-02-06 10:04:41\",\"status\":0}"));
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                logger.error("{}", e);
                            }
                        }
                    }
                }).start();
            }
        });
        webSocketServer.start();
    }


    /**
     * 发送消息
     *
     * @param message
     */
    public void sendMessage(String message) {

    }

}
