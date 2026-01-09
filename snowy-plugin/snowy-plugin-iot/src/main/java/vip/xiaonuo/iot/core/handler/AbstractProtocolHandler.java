package vip.xiaonuo.iot.core.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议处理器抽象基类
 * 
 * 提供通用的连接管理、异常处理、心跳检测等功能
 * 子类只需实现具体的业务逻辑
 *
 * @author jetox
 * @date 2025/12/11
 */
@Slf4j
public abstract class AbstractProtocolHandler extends ChannelInboundHandlerAdapter {

    /**
     * 连接建立时调用
     *
     * @param ctx 通道上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.info(">>> {} 连接建立 - 远程地址: {}", getProtocolName(), remoteAddress);
        
        // 子类处理连接建立逻辑
        onConnectionEstablished(ctx);
        
        super.channelActive(ctx);
    }

    /**
     * 连接断开时调用
     *
     * @param ctx 通道上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.info(">>> {} 连接断开 - 远程地址: {}", getProtocolName(), remoteAddress);
        
        // 子类处理连接断开逻辑
        onConnectionClosed(ctx);
        
        super.channelInactive(ctx);
    }

    /**
     * 读取数据时调用
     *
     * @param ctx 通道上下文
     * @param msg 消息对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            // 子类处理具体的业务逻辑
            handleData(ctx, msg);
        } catch (Exception e) {
            log.error(">>> {} 数据处理异常", getProtocolName(), e);
            handleBusinessException(ctx, msg, e);
        }
    }

    /**
     * 异常捕获
     *
     * @param ctx 通道上下文
     * @param cause 异常原因
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.error(">>> {} 发生异常 - 远程地址: {}", getProtocolName(), remoteAddress, cause);
        
        // 子类处理异常
        onExceptionOccurred(ctx, cause);
        
        // 关闭连接
        ctx.close();
    }

    /**
     * 用户事件触发（如心跳超时）
     *
     * @param ctx 通道上下文
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn(">>> {} 读超时，关闭连接 - 远程地址: {}", 
                    getProtocolName(), ctx.channel().remoteAddress());
                onHeartbeatTimeout(ctx);
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.warn(">>> {} 写超时 - 远程地址: {}", 
                    getProtocolName(), ctx.channel().remoteAddress());
                onWriteTimeout(ctx);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    // ==================== 抽象方法（子类必须实现） ====================

    /**
     * 获取协议名称（用于日志输出）
     *
     * @return 协议名称，如 "MQTT", "Modbus TCP" 等
     */
    protected abstract String getProtocolName();

    /**
     * 处理业务数据
     *
     * @param ctx 通道上下文
     * @param msg 消息对象
     */
    protected abstract void handleData(ChannelHandlerContext ctx, Object msg) throws Exception;

    // ==================== 钩子方法（子类可选择性覆盖） ====================

    /**
     * 连接建立时的回调
     *
     * @param ctx 通道上下文
     */
    protected void onConnectionEstablished(ChannelHandlerContext ctx) {
        // 默认空实现，子类按需覆盖
    }

    /**
     * 连接关闭时的回调
     *
     * @param ctx 通道上下文
     */
    protected void onConnectionClosed(ChannelHandlerContext ctx) {
        // 默认空实现，子类按需覆盖
    }

    /**
     * 业务异常处理
     *
     * @param ctx 通道上下文
     * @param msg 消息对象
     * @param e 异常
     */
    protected void handleBusinessException(ChannelHandlerContext ctx, Object msg, Exception e) {
        // 默认空实现，子类按需覆盖
    }

    /**
     * 异常发生时的回调
     *
     * @param ctx 通道上下文
     * @param cause 异常原因
     */
    protected void onExceptionOccurred(ChannelHandlerContext ctx, Throwable cause) {
        // 默认空实现，子类按需覆盖
    }

    /**
     * 心跳超时回调
     *
     * @param ctx 通道上下文
     */
    protected void onHeartbeatTimeout(ChannelHandlerContext ctx) {
        // 默认空实现，子类按需覆盖
    }

    /**
     * 写超时回调
     *
     * @param ctx 通道上下文
     */
    protected void onWriteTimeout(ChannelHandlerContext ctx) {
        // 默认空实现，子类按需覆盖
    }
}
