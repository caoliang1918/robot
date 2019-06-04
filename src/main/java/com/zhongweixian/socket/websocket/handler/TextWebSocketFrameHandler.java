package com.zhongweixian.socket.websocket.handler;

import com.zhongweixian.socket.websocket.WsMessgaeService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private WsMessgaeService wsMessgaeService;

    public TextWebSocketFrameHandler(WsMessgaeService wsMessgaeService) {
        this.wsMessgaeService = wsMessgaeService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) {
        if (StringUtils.isNoneEmpty(textWebSocketFrame.text())){
            wsMessgaeService.readMessage(textWebSocketFrame.text());
        }
        //读取收到的信息写回到客户端
        channelHandlerContext.channel().writeAndFlush(new TextWebSocketFrame(new Date().toString()));
    }
}
