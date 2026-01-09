/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.dev.modular.sse.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.pojo.CommonResult;
import vip.xiaonuo.dev.modular.sse.enums.DevSseEmitterParameterEnum;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;

/**
 * SseEmitter工具类
 *
 * @author diantu
 * @date 2023/7/3
 **/
@Slf4j
public class DevSseCacheUtil {

    /**
     * 创建一个容器来存储所有的 SseEmitter(使用ConcurrentHashMap是因为它是线程安全的)。
     */
    public static Map<String, Map<String, Object>> sseCache = new ConcurrentHashMap<>();

    /**
     * 最大连接数限制
     */
    private static final int MAX_CONNECTIONS = 5000;

    /**
     * 连接过期时间（毫秒）- 30分钟无活动则视为过期
     */
    private static final long CONNECTION_EXPIRE_TIME = 30 * 60 * 1000;

    /**
     * 定时清理过期连接的线程池
     */
    private static final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r, "SSE-Connection-Cleanup");
        thread.setDaemon(true);
        return thread;
    });

    static {
        // 每5分钟清理一次过期连接
        cleanupExecutor.scheduleWithFixedDelay(() -> {
            try {
                cleanupExpiredConnections();
            } catch (Exception e) {
                log.error("清理过期SSE连接失败", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * 清理过期连接
     */
    private static void cleanupExpiredConnections() {
        if (!existSseCache()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        List<String> expiredClients = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : sseCache.entrySet()) {
            Map<String, Object> map = entry.getValue();
            Long lastActiveTime = (Long) map.get(DevSseEmitterParameterEnum.LAST_ACTIVE_TIME.getValue());
            if (lastActiveTime != null && (currentTime - lastActiveTime) > CONNECTION_EXPIRE_TIME) {
                expiredClients.add(entry.getKey());
            }
        }
        
        // 批量移除过期连接
        for (String clientId : expiredClients) {
            log.info("移除过期连接: {}", clientId);
            removeConnection(clientId);
        }
        
        if (!expiredClients.isEmpty()) {
            log.info("清理了 {} 个过期SSE连接，当前连接数: {}", expiredClients.size(), sseCache.size());
        }
    }

    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return sseCache.size();
    }

    /**
     * 更新连接的最后活跃时间
     */
    private static void updateLastActiveTime(String clientId) {
        Map<String, Object> map = sseCache.get(clientId);
        if (map != null) {
            map.put(DevSseEmitterParameterEnum.LAST_ACTIVE_TIME.getValue(), System.currentTimeMillis());
        }
    }


    /**
     * 根据客户端id获取连接对象
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static SseEmitter getSseEmitterByClientId(String clientId) {
        Map<String, Object> map = sseCache.get(clientId);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return (SseEmitter) map.get(DevSseEmitterParameterEnum.EMITTER.getValue());
    }

    /**
     * 根据客户端id获取心跳
     *
     * @author diantu
     * @date 2023/7/18
     **/
    public static ScheduledFuture<?> getSseFutureByClientId(String clientId) {
        Map<String, Object> map = sseCache.get(clientId);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return (ScheduledFuture<?>) map.get(DevSseEmitterParameterEnum.FUTURE.getValue());
    }

    /**
     * 根据客户端id获取用户id
     *
     * @author diantu
     * @date 2023/7/18
     **/
    public static ScheduledFuture<?> getLoginIdByClientId(String clientId) {
        Map<String, Object> map = sseCache.get(clientId);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return (ScheduledFuture<?>) map.get(DevSseEmitterParameterEnum.LOGINID.getValue());
    }

    /**
     * 根据用户id获取客户端id
     *
     * @author diantu
     * @date 2023/7/18
     **/
    public static String getClientIdByLoginId(String loginId) {
        if (existSseCache()) {
            for (Map.Entry<String, Map<String, Object>> entry : sseCache.entrySet()) {
                Map<String, Object> map = sseCache.get(entry.getKey());
                String lId = (String) map.get(DevSseEmitterParameterEnum.LOGINID.getValue());
                if (loginId.equals(lId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 判断容器是否存在连接
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static boolean existSseCache() {
        return !sseCache.isEmpty();
    }

    /**
     * 判断连接是否有效
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static boolean connectionValidity(String clientId, String loginId) {
        if (sseCache.get(clientId) == null) {
            return false;
        }
        return Objects.equals(loginId, sseCache.get(clientId).get(DevSseEmitterParameterEnum.LOGINID.getValue()));
    }

    /**
     * 增加连接
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static void addConnection(String clientId, String loginId, SseEmitter emitter, ScheduledFuture<?> future) {
        final SseEmitter oldEmitter = getSseEmitterByClientId(clientId);
        if (oldEmitter != null) {
            throw new CommonException("连接已存在:{}", clientId);
        }
        
        // 检查连接数限制
        if (sseCache.size() >= MAX_CONNECTIONS) {
            log.warn("SSE连接数已达上限: {}，拒绝新连接: {}", MAX_CONNECTIONS, clientId);
            throw new CommonException("服务器连接数已达上限，请稍后重试");
        }
        
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put(DevSseEmitterParameterEnum.EMITTER.getValue(), emitter);
        if (future != null) {
            map.put(DevSseEmitterParameterEnum.FUTURE.getValue(), future);
        }
        map.put(DevSseEmitterParameterEnum.LOGINID.getValue(), loginId);
        map.put(DevSseEmitterParameterEnum.LAST_ACTIVE_TIME.getValue(), System.currentTimeMillis());
        sseCache.put(clientId, map);
        
        log.info("新增SSE连接: {}，当前连接数: {}", clientId, sseCache.size());
    }

    /**
     * 移除连接
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static void removeConnection(String clientId) {
        SseEmitter emitter = getSseEmitterByClientId(clientId);
        if (emitter != null) {
            cancelScheduledFuture(clientId);
        }
        sseCache.remove(clientId);
        log.info("移除连接:{}", clientId);
    }

    /**
     * 中断心跳发送任务
     *
     * @author diantu
     * @date 2023/7/3
     */
    public static void cancelScheduledFuture(String clientId) {
        ScheduledFuture<?> future = getSseFutureByClientId(clientId);
        if (future != null) {
            future.cancel(true);
        }
    }


    /**
     * 长链接完成后回调
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static Runnable completionCallBack(String clientId) {
        return () -> {
            log.info("结束连接:{}", clientId);
            removeConnection(clientId);
            cancelScheduledFuture(clientId);
        };
    }

    /**
     * 连接超时回调
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static Runnable timeoutCallBack(String clientId) {
        return () -> {
            log.info("连接超时:{}", clientId);
            removeConnection(clientId);
            cancelScheduledFuture(clientId);
        };
    }

    /**
     * 推送消息异常时回调
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static Consumer<Throwable> errorCallBack(String clientId) {
        return throwable -> {
            log.info("推送消息异常:{}", clientId);
            removeConnection(clientId);
            cancelScheduledFuture(clientId);
        };
    }

    /**
     * 推送消息到所有客户端
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static void sendMessageToAllClient(String msg) {
        if (!existSseCache()) {
            return;
        }
        // 判断发送的消息是否为空
        if (StrUtil.isEmpty(msg)) {
            log.info("群发消息为空");
            return;
        }
        CommonResult<String> message = new CommonResult<>(CommonResult.CODE_SUCCESS, "", msg);
        for (Map.Entry<String, Map<String, Object>> entry : sseCache.entrySet()) {
            String clientId = entry.getKey();
            sendMessageToClientByClientId(clientId, message);
            updateLastActiveTime(clientId);
        }
    }

    /**
     * 根据clientId发送消息给某一客户端
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static void sendMessageToOneClient(String clientId, String msg) {
        if (StrUtil.isEmpty(clientId)) {
            log.info("客户端ID为空");
            return;
        }
        if (StrUtil.isEmpty(msg)) {
            log.info("向客户端{}推送消息为空", clientId);
            return;
        }
        CommonResult<String> message = new CommonResult<>(CommonResult.CODE_SUCCESS, "", msg);
        sendMessageToClientByClientId(clientId, message);
        updateLastActiveTime(clientId);
    }

    /**
     * 推送消息到客户端
     *
     * @author diantu
     * @date 2023/7/3
     **/
    public static void sendMessageToClientByClientId(String clientId, CommonResult<String> message) {
        Map<String, Object> map = sseCache.get(clientId);
        if (map == null || map.isEmpty()) {
            log.debug("推送消息失败:客户端{}未创建长链接,失败消息:{}", clientId, message.toString());
            return;
        }
        SseEmitter.SseEventBuilder sendData = SseEmitter.event().data(message, MediaType.APPLICATION_JSON);
        SseEmitter sseEmitter = getSseEmitterByClientId(clientId);
        try {
            Objects.requireNonNull(sseEmitter).send(sendData);
        } catch (Exception e) {
            // 判断是否为客户端主动断开连接的异常
            String exceptionName = e.getClass().getName();
            if (exceptionName.contains("ClientAbortException") || 
                e.getMessage() != null && (e.getMessage().contains("你的主机中的软件中止了一个已建立的连接") ||
                                          e.getMessage().contains("Connection reset") ||
                                          e.getMessage().contains("Broken pipe"))) {
                // 客户端正常断开,使用DEBUG级别
                log.debug("客户端{}断开SSE连接: {}", clientId, e.getMessage());
            } else {
                // 其他异常使用ERROR级别
                log.error("推送消息到客户端{}失败,异常:", clientId, e);
            }
            removeConnection(clientId);
        }
    }

}
