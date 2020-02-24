package com.zhongweixian.socket.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zhongweixian.listener.ConnectionListener;

/**
 * Created by caoliang on 2020-02-24
 */
@Component
public class TcpServerHandler implements ConnectionListener {
    private Logger logger = LoggerFactory.getLogger(TcpServerHandler.class);

    @Override
    public void onClose(Channel channel, int i, String s) {
        logger.info("onClose:{}", channel);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onFail(int i, String s) {

    }

    @Override
    public void onMessage(Channel channel, String s) throws Exception {
        logger.info("received:{}", s);
    }

    @Override
    public void onMessage(Channel channel, ByteBuf byteBuf) throws Exception {
        logger.info("received:{}", byteBuf);
    }

    @Override
    public void connect(Channel channel) throws Exception {
        logger.info("connected:{}", channel);
    }
}
