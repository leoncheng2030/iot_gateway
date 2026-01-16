package vip.xiaonuo.iot.core.security;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 设备认证器
 * 提供设备身份验证和Token管理
 * 
 * 安全特性：
 * - 设备密钥验证
 * - Token生成与验证
 * - Token自动过期
 * - 防重放攻击（Nonce检查）
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class DeviceAuthenticator {
    
    @Value("${iot.security.token-expire-hours:24}")
    private int tokenExpireHours;
    
    @Value("${iot.security.hmac-secret:wqs-iot-gateway-secret}")
    private String hmacSecret;
    
    /** 设备Token缓存 <deviceKey, TokenInfo> */
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    /** Nonce缓存（防重放攻击）<nonce, expireTime> */
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();
    
    /** 设备密钥缓存 <deviceKey, deviceSecret> */
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();
    
    /** 定时清理任务 */
    private ScheduledExecutorService scheduler;
    
    /** HMAC实例 */
    private HMac hmac;
    
    @PostConstruct
    public void init() {
        hmac = new HMac(HmacAlgorithm.HmacSHA256, hmacSecret.getBytes(StandardCharsets.UTF_8));
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "device-auth-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        
        // 每小时清理过期数据
        scheduler.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.HOURS);
        
        log.info("设备认证器初始化完成，Token有效期: {}小时", tokenExpireHours);
    }
    
    /**
     * 注册设备密钥（应用启动时从数据库加载）
     * 
     * @param deviceKey 设备Key
     * @param deviceSecret 设备密钥
     */
    public void registerDeviceSecret(String deviceKey, String deviceSecret) {
        secretCache.put(deviceKey, deviceSecret);
        log.debug("注册设备密钥: {}", deviceKey);
    }
    
    /**
     * 生成设备Token
     * 
     * @param deviceKey 设备Key
     * @param deviceSecret 设备密钥
     * @param nonce 随机数（防重放）
     * @return 认证结果
     */
    public AuthResult authenticate(String deviceKey, String deviceSecret, String nonce) {
        // 1. 检查Nonce（防重放攻击）
        if (nonce != null && !validateNonce(nonce)) {
            return AuthResult.fail("NONCE_REUSED", "Nonce已使用，请重新生成");
        }
        
        // 2. 验证设备密钥
        if (!validateDeviceSecret(deviceKey, deviceSecret)) {
            log.warn("设备密钥验证失败: {}", deviceKey);
            return AuthResult.fail("INVALID_SECRET", "设备密钥验证失败");
        }
        
        // 3. 生成Token
        String token = generateToken(deviceKey);
        long expireTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(tokenExpireHours);
        
        // 4. 缓存Token
        TokenInfo tokenInfo = new TokenInfo(token, expireTime, deviceKey, System.currentTimeMillis());
        tokenCache.put(deviceKey, tokenInfo);
        
        // 5. 记录Nonce
        if (nonce != null) {
            nonceCache.put(nonce, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        }
        
        log.info("设备认证成功: deviceKey={}", deviceKey);
        
        return AuthResult.success(token, expireTime);
    }
    
    /**
     * 验证设备Token
     * 
     * @param deviceKey 设备Key
     * @param token 设备Token
     * @return 验证结果
     */
    public TokenValidationResult validateToken(String deviceKey, String token) {
        TokenInfo tokenInfo = tokenCache.get(deviceKey);
        
        if (tokenInfo == null) {
            return TokenValidationResult.invalid("TOKEN_NOT_FOUND", "Token不存在");
        }
        
        // 检查Token是否过期
        if (System.currentTimeMillis() > tokenInfo.getExpireTime()) {
            tokenCache.remove(deviceKey);
            return TokenValidationResult.invalid("TOKEN_EXPIRED", "Token已过期");
        }
        
        // 验证Token
        if (!tokenInfo.getToken().equals(token)) {
            return TokenValidationResult.invalid("TOKEN_MISMATCH", "Token不匹配");
        }
        
        return TokenValidationResult.valid(tokenInfo);
    }
    
    /**
     * 刷新Token
     * 
     * @param deviceKey 设备Key
     * @param oldToken 旧Token
     * @return 新的认证结果
     */
    public AuthResult refreshToken(String deviceKey, String oldToken) {
        TokenValidationResult validation = validateToken(deviceKey, oldToken);
        
        if (!validation.isValid()) {
            return AuthResult.fail(validation.getErrorCode(), validation.getErrorMessage());
        }
        
        // 生成新Token
        String newToken = generateToken(deviceKey);
        long expireTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(tokenExpireHours);
        
        // 更新缓存
        TokenInfo tokenInfo = new TokenInfo(newToken, expireTime, deviceKey, System.currentTimeMillis());
        tokenCache.put(deviceKey, tokenInfo);
        
        log.info("设备Token刷新成功: deviceKey={}", deviceKey);
        
        return AuthResult.success(newToken, expireTime);
    }
    
    /**
     * 撤销设备Token
     * 
     * @param deviceKey 设备Key
     */
    public void revokeToken(String deviceKey) {
        TokenInfo removed = tokenCache.remove(deviceKey);
        if (removed != null) {
            log.info("设备Token已撤销: deviceKey={}", deviceKey);
        }
    }
    
    /**
     * 生成签名（用于设备请求验证）
     * 
     * @param deviceKey 设备Key
     * @param timestamp 时间戳
     * @param payload 请求负载
     * @return 签名
     */
    public String generateSignature(String deviceKey, long timestamp, String payload) {
        String signContent = deviceKey + timestamp + payload;
        return hmac.digestHex(signContent);
    }
    
    /**
     * 验证签名
     * 
     * @param deviceKey 设备Key
     * @param timestamp 时间戳
     * @param payload 请求负载
     * @param signature 签名
     * @return 验证结果
     */
    public boolean verifySignature(String deviceKey, long timestamp, String payload, String signature) {
        // 检查时间戳（5分钟有效期）
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > TimeUnit.MINUTES.toMillis(5)) {
            log.warn("签名时间戳过期: deviceKey={}, timestamp={}", deviceKey, timestamp);
            return false;
        }
        
        String expectedSignature = generateSignature(deviceKey, timestamp, payload);
        return expectedSignature.equals(signature);
    }
    
    /**
     * 验证设备密钥
     */
    private boolean validateDeviceSecret(String deviceKey, String deviceSecret) {
        String cachedSecret = secretCache.get(deviceKey);
        
        if (cachedSecret == null) {
            // TODO: 从数据库查询设备密钥
            // IotDevice device = deviceService.getByDeviceKey(deviceKey);
            // cachedSecret = device.getDeviceSecret();
            return false;
        }
        
        // 密钥可能是明文或MD5加密
        return cachedSecret.equals(deviceSecret) 
            || cachedSecret.equals(SecureUtil.md5(deviceSecret));
    }
    
    /**
     * 验证Nonce（防重放攻击）
     */
    private boolean validateNonce(String nonce) {
        Long expireTime = nonceCache.get(nonce);
        
        if (expireTime != null) {
            // Nonce已使用
            return false;
        }
        
        return true;
    }
    
    /**
     * 生成Token
     */
    private String generateToken(String deviceKey) {
        String raw = deviceKey + System.currentTimeMillis() + IdUtil.fastSimpleUUID();
        return SecureUtil.sha256(raw);
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupExpired() {
        try {
            long now = System.currentTimeMillis();
            
            // 清理过期Token
            int tokenRemoved = 0;
            var tokenIterator = tokenCache.entrySet().iterator();
            while (tokenIterator.hasNext()) {
                var entry = tokenIterator.next();
                if (entry.getValue().getExpireTime() < now) {
                    tokenIterator.remove();
                    tokenRemoved++;
                }
            }
            
            // 清理过期Nonce
            int nonceRemoved = 0;
            var nonceIterator = nonceCache.entrySet().iterator();
            while (nonceIterator.hasNext()) {
                var entry = nonceIterator.next();
                if (entry.getValue() < now) {
                    nonceIterator.remove();
                    nonceRemoved++;
                }
            }
            
            if (tokenRemoved > 0 || nonceRemoved > 0) {
                log.info("清理过期认证数据: tokens={}, nonces={}", tokenRemoved, nonceRemoved);
            }
            
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
    
    /**
     * 获取在线设备数量
     */
    public int getAuthenticatedDeviceCount() {
        return tokenCache.size();
    }
    
    /**
     * 检查设备是否已认证
     */
    public boolean isDeviceAuthenticated(String deviceKey) {
        TokenInfo tokenInfo = tokenCache.get(deviceKey);
        return tokenInfo != null && tokenInfo.getExpireTime() > System.currentTimeMillis();
    }
    
    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        tokenCache.clear();
        nonceCache.clear();
        log.info("设备认证器已关闭");
    }
    
    /**
     * Token信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenInfo {
        private String token;
        private long expireTime;
        private String deviceKey;
        private long createTime;
    }
    
    /**
     * 认证结果
     */
    @Data
    @AllArgsConstructor
    public static class AuthResult {
        private boolean success;
        private String token;
        private long expireTime;
        private String errorCode;
        private String errorMessage;
        
        public static AuthResult success(String token, long expireTime) {
            return new AuthResult(true, token, expireTime, null, null);
        }
        
        public static AuthResult fail(String errorCode, String errorMessage) {
            return new AuthResult(false, null, 0, errorCode, errorMessage);
        }
    }
    
    /**
     * Token验证结果
     */
    @Data
    @AllArgsConstructor
    public static class TokenValidationResult {
        private boolean valid;
        private TokenInfo tokenInfo;
        private String errorCode;
        private String errorMessage;
        
        public static TokenValidationResult valid(TokenInfo tokenInfo) {
            return new TokenValidationResult(true, tokenInfo, null, null);
        }
        
        public static TokenValidationResult invalid(String errorCode, String errorMessage) {
            return new TokenValidationResult(false, null, errorCode, errorMessage);
        }
    }
}
