package com.zhongweixian.socket.websocket;

public interface WsMessgaeService {

    /**
     * 接受消息
     * @param message
     */
    void readMessage(String message);
}
