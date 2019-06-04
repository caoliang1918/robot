package com.zhongweixian.socket.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongweixian.socket.tcp.ChannelRepository;
import com.zhongweixian.socket.tcp.TcpMessageService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 服务器监听处理器
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    /**
     * 消息接口
     */
    private TcpMessageService tcpMessageService;

    public NettyServerHandler(TcpMessageService tcpMessageService){
        this.tcpMessageService = tcpMessageService;
    }
    /**
     * 绑定
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        logger.info("client connect :{}", byteBuf.toString(CharsetUtil.UTF_8));
         /*
        if (StringUtils.isEmpty(messageProtocol.getStation())) {
            channelHandlerContext.close().channel();
            return;
        }
        ChannelRepository.put(messageProtocol.getStation(), (NioSocketChannel) channelHandlerContext.channel());*/
    }


    /**
     * 取消绑定
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelInactive");
        NioSocketChannel nioSocketChannel = (NioSocketChannel) ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        if (msg != null) {
            tcpMessageService.readMessgae(msg.toString());
        }
    }

    /**
     * 心跳机制  用户事件触发
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                logger.debug("10秒没有收到客户端信息,关闭连接！");
                //向客户端发送心跳消息
                JSONObject messageProtocol = new JSONObject();
                messageProtocol.put("id", 1L);
                messageProtocol.put("cmd", "logout");
                messageProtocol.put("message", "10秒没有收到客户端信息,关闭连接");
                messageProtocol.put("cts", new Date());
                ByteBuf byteBuf = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(messageProtocol.toString(), CharsetUtil.UTF_8));
                ctx.writeAndFlush(byteBuf).addListener(ChannelFutureListener.CLOSE);
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("客户端连接  Netty 出错...");
        cause.printStackTrace();
        ChannelRepository.remove((NioSocketChannel) ctx.channel());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("register success");
        ctx.fireChannelRegistered();
        NioSocketChannel channel = (NioSocketChannel) ctx.channel();
        ChannelRepository.put("XXXXXX", channel);
    }
}
