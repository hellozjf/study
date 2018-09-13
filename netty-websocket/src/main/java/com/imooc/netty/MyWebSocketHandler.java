package com.imooc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 接收/处理/响应客户端websocket请求的核心业务处理类
 * @author hellozjf
 */
@Slf4j
public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;
    private static final String WEB_SOCKET_URL = "ws://localhost:8888/websocket";

    /**
     * 客户端与服务端创建连接的时候调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        log.info("客户端与服务端连接开启, channel={}", ctx.channel());
    }

    /**
     * 客户端与服务端断开连接的时候调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.remove(ctx.channel());
        log.info("客户端与服务端连接关闭, channel={}", ctx.channel());
    }

    /**
     * 服务端接收客户端发送过来的数据之后调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 功能出现异常的时候调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端处理客户端发websocket请求的核心方法
     * @param context
     * @param msg
     * @throws Exception
     */
    @Override
    protected void messageReceived(ChannelHandlerContext context, Object msg) throws Exception {

        if (msg instanceof FullHttpRequest) {
            // 处理客户端向服务端发送http握手请求的业务
            handHttpRequest(context, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            // 处理websocket连接业务
            handWebsocketFrame(context, (WebSocketFrame) msg);
        }
    }

    /**
     * 处理客户端与服务端之间的websocket业务
     * @param ctx
     * @param frame
     */
    private void handWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            // 判断是否是关闭websocket的指令
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
        } else if (frame instanceof PingWebSocketFrame) {
            // 判断是否是ping消息
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        } else if (! (frame instanceof TextWebSocketFrame)) {
            // 判断是否是二进制消息，如果是二进制消息，则抛出异常
            log.error("目前我们不支持二进制消息");
            throw new WebSocketException(WebSocketResultEnum.UNSUPPORT_MESSAGE);
        } else {
            // 说明是文本消息，返回应答消息
            // 获取客户端向服务端发送的消息
            String request = ((TextWebSocketFrame) frame).text();
            log.debug("服务端收到客户端的消息 ===>>> {}", request);
            TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() +
                    ctx.channel().id() + " ===>>> " + request);
            // 群发，服务端向每个连接上来的客户端群发消息
            NettyConfig.group.write(tws);
        }
    }

    /**
     * 处理客户端向服务端发起http握手请求的业务
     * @param ctx
     * @param req
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (! req.getDecoderResult().isSuccess() ||
                ! "websocket".equals(req.headers().get("Upgrade"))) {
            sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(WEB_SOCKET_URL,null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    /**
     * 服务端向客户端响应消息
     * @param ctx
     * @param req
     * @param res
     */
    private void sendHttpResponse(ChannelHandlerContext ctx,
                                  FullHttpRequest req,
                                  FullHttpResponse res) {
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 服务端向客户端发送数据
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
