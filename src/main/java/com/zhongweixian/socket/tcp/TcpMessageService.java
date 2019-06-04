package com.zhongweixian.socket.tcp;

public interface TcpMessageService {

    /**
     * 接受tcp消息
     *
     * @param message
     */
    void readMessgae(String message);
}
