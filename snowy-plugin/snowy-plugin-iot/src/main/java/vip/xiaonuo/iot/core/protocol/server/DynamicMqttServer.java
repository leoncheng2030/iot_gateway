/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy閲囩敤APACHE LICENSE 2.0寮€婧愬崗璁紝鎮ㄥ湪浣跨敤杩囩▼涓紝闇€瑕佹敞鎰忎互涓嬪嚑鐐癸細
 *
 * 1.璇蜂笉瑕佸垹闄ゅ拰淇敼鏍圭洰褰曚笅鐨凩ICENSE鏂囦欢銆?
 * 2.璇蜂笉瑕佸垹闄ゅ拰淇敼Snowy婧愮爜澶撮儴鐨勭増鏉冨０鏄庛€?
 * 3.鏈」鐩唬鐮佸彲鍏嶈垂鍟嗕笟浣跨敤锛屽晢涓氫娇鐢ㄨ淇濈暀婧愮爜鍜岀浉鍏虫弿杩版枃浠剁殑椤圭洰鍑哄锛屼綔鑰呭０鏄庣瓑銆?
 * 4.鍒嗗彂婧愮爜鏃跺€欙紝璇锋敞鏄庤蒋浠跺嚭澶?https://www.xiaonuo.vip
 * 5.涓嶅彲浜屾鍒嗗彂寮€婧愬弬涓庡悓绫荤珵鍝侊紝濡傛湁鎯虫硶鍙仈绯诲洟闃焫iaonuobase@qq.com鍟嗚鍚堜綔銆?
 * 6.鑻ユ偍鐨勯」鐩棤娉曟弧瓒充互涓婂嚑鐐癸紝闇€瑕佹洿澶氬姛鑳戒唬鐮侊紝鑾峰彇Snowy鍟嗕笟鎺堟潈璁稿彲锛岃鍦ㄥ畼缃戣喘涔版巿鏉冿紝鍦板潃涓?https://www.xiaonuo.vip
 */
package vip.xiaonuo.iot.core.protocol.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.xiaonuo.iot.core.config.NettyThreadPoolConfig;
import vip.xiaonuo.iot.core.mqtt.MqttServerHandler;
import vip.xiaonuo.iot.core.mqtt.MqttWebSocketFrameHandler;
import vip.xiaonuo.iot.core.protocol.ProtocolServer;
import vip.xiaonuo.iot.core.protocol.annotation.Protocol;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 鍔ㄦ€丮QTT鏈嶅姟鍣?
 *
 * @author jetox
 * @date 2025/12/11 10:40
 **/
@Slf4j
@Protocol(type = "MQTT", name = "MQTT鍗忚", description = "鍔ㄦ€丮QTT鍗忚鏈嶅姟鍣紝鏀寔MQTT 3.1/3.1.1/5.0")
public class DynamicMqttServer implements ProtocolServer {

    private final MqttServerHandler mqttServerHandler;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Integer port;

    public DynamicMqttServer(MqttServerHandler mqttServerHandler) {
        this.mqttServerHandler = mqttServerHandler;
    }

    @Override
    public void start(Integer port, Map<String, Object> config) {
        this.port = port;
        
        // 浠庨厤缃腑鑾峰彇鍙傛暟锛屽鏋滄病鏈夊垯浣跨敤榛樿鍊?
        int keepAlive = getConfigInt(config, "keepAlive", 60);
        int maxMessageSize = getConfigInt(config, "maxMessageSize", 8192);
        boolean enableWebSocket = getConfigBoolean(config, "enableWebSocket", false);
        String wsPath = getConfigString(config, "wsPath", "/mqtt");

        try {
            bossGroup = new NioEventLoopGroup(NettyThreadPoolConfig.getBossThreads());
            workerGroup = new NioEventLoopGroup(NettyThreadPoolConfig.getWorkerThreads());

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 蹇冭烦妫€娴?
                            pipeline.addLast(new IdleStateHandler(
                                    keepAlive * 2,
                                    0,
                                    0,
                                    TimeUnit.SECONDS
                            ));
                            
                            // MQTT缂栬В鐮佸櫒锛堢函TCP妯″紡锛?
                            pipeline.addLast("decoder", new MqttDecoder(maxMessageSize));
                            pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                            
                            // MQTT涓氬姟澶勭悊鍣?
                            pipeline.addLast("handler", mqttServerHandler);
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            log.info(">>> 鍔ㄦ€丮QTT鏈嶅姟鍣ㄥ惎鍔ㄦ垚鍔燂紝鐩戝惉绔彛: {}", port);

        } catch (Exception e) {
            stop();
            throw new RuntimeException("MQTT鏈嶅姟鍣ㄥ惎鍔ㄥけ璐? " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(
                NettyThreadPoolConfig.getShutdownQuietPeriod(),
                NettyThreadPoolConfig.getShutdownTimeout(),
                TimeUnit.SECONDS
            );
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(
                NettyThreadPoolConfig.getShutdownQuietPeriod(),
                NettyThreadPoolConfig.getShutdownTimeout(),
                TimeUnit.SECONDS
            );
        }
        log.info(">>> 鍔ㄦ€丮QTT鏈嶅姟鍣ㄥ凡鍏抽棴锛岀鍙? {}", port);
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getProtocolType() {
        return "MQTT";
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getConfigBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private String getConfigString(Map<String, Object> config, String key, String defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 宸ュ巶绫伙紝鐢ㄤ簬鍒涘缓DynamicMqttServer瀹炰緥
     */
    @Component
    public static class Factory {
        
        @Resource
        private MqttServerHandler mqttServerHandler;

        public DynamicMqttServer create() {
            return new DynamicMqttServer(mqttServerHandler);
        }
    }
}
