package vip.xiaonuo.iot.core.protocol.modbus;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modbus连接池
 * 基于Apache Commons Pool2实现
 * 
 * 特点：
 * - 连接复用，减少TCP握手开销
 * - 自动健康检查
 * - 支持连接预热
 * - 空闲连接自动回收
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
public class ModbusConnectionPool {
    
    /** 连接池 */
    private final GenericObjectPool<ModbusMaster> pool;
    
    /** 目标主机 */
    private final String host;
    
    /** 目标端口 */
    private final int port;
    
    /** 连接池配置 */
    private final PoolConfig config;
    
    /**
     * 创建Modbus连接池
     * 
     * @param host 主机地址
     * @param port 端口号
     */
    public ModbusConnectionPool(String host, int port) {
        this(host, port, new PoolConfig());
    }
    
    /**
     * 创建Modbus连接池（自定义配置）
     * 
     * @param host 主机地址
     * @param port 端口号
     * @param config 连接池配置
     */
    public ModbusConnectionPool(String host, int port, PoolConfig config) {
        this.host = host;
        this.port = port;
        this.config = config;
        
        // 配置连接池
        GenericObjectPoolConfig<ModbusMaster> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(config.getMaxTotal());
        poolConfig.setMaxIdle(config.getMaxIdle());
        poolConfig.setMinIdle(config.getMinIdle());
        poolConfig.setTestOnBorrow(config.isTestOnBorrow());
        poolConfig.setTestOnReturn(config.isTestOnReturn());
        poolConfig.setTestWhileIdle(config.isTestWhileIdle());
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(config.getTimeBetweenEvictionRunsMillis()));
        poolConfig.setMinEvictableIdleTime(Duration.ofMillis(config.getMinEvictableIdleTimeMillis()));
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofMillis(config.getMaxWaitMillis()));
        
        // 创建连接池
        pool = new GenericObjectPool<>(
            new ModbusMasterFactory(host, port, config.getTimeout(), config.getRetries()),
            poolConfig
        );
        
        log.info("Modbus连接池初始化: host={}, port={}, maxTotal={}", host, port, config.getMaxTotal());
    }
    
    /**
     * 获取连接
     * 
     * @return ModbusMaster连接
     * @throws Exception 获取失败
     */
    public ModbusMaster borrowConnection() throws Exception {
        ModbusMaster master = pool.borrowObject();
        log.debug("借用Modbus连接: {}:{}, active={}, idle={}", 
            host, port, pool.getNumActive(), pool.getNumIdle());
        return master;
    }
    
    /**
     * 归还连接
     * 
     * @param master ModbusMaster连接
     */
    public void returnConnection(ModbusMaster master) {
        if (master != null) {
            pool.returnObject(master);
            log.debug("归还Modbus连接: {}:{}, active={}, idle={}", 
                host, port, pool.getNumActive(), pool.getNumIdle());
        }
    }
    
    /**
     * 使连接失效（连接出现问题时调用）
     * 
     * @param master ModbusMaster连接
     */
    public void invalidateConnection(ModbusMaster master) {
        if (master != null) {
            try {
                pool.invalidateObject(master);
                log.warn("使Modbus连接失效: {}:{}", host, port);
            } catch (Exception e) {
                log.error("使连接失效时出错", e);
            }
        }
    }
    
    /**
     * 预热连接池
     * 
     * @param count 预热连接数
     */
    public void warmUp(int count) {
        log.info("预热Modbus连接池: {}:{}, count={}", host, port, count);
        
        ModbusMaster[] masters = new ModbusMaster[Math.min(count, config.getMaxTotal())];
        
        try {
            // 借用连接
            for (int i = 0; i < masters.length; i++) {
                masters[i] = pool.borrowObject();
            }
            
            // 归还连接
            for (ModbusMaster master : masters) {
                if (master != null) {
                    pool.returnObject(master);
                }
            }
            
            log.info("连接池预热完成: {}:{}, warmed={}", host, port, masters.length);
            
        } catch (Exception e) {
            log.error("连接池预热失败: {}:{}", host, port, e);
        }
    }
    
    /**
     * 获取连接池状态
     * 
     * @return 连接池状态
     */
    public PoolStatus getStatus() {
        return new PoolStatus(
            pool.getNumActive(),
            pool.getNumIdle(),
            pool.getMaxTotal(),
            pool.getBorrowedCount(),
            pool.getReturnedCount(),
            pool.getCreatedCount(),
            pool.getDestroyedCount()
        );
    }
    
    /**
     * 关闭连接池
     */
    public void close() {
        pool.close();
        log.info("Modbus连接池已关闭: {}:{}", host, port);
    }
    
    /**
     * 获取目标主机
     */
    public String getHost() {
        return host;
    }
    
    /**
     * 获取目标端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Modbus连接工厂
     */
    private static class ModbusMasterFactory extends BasePooledObjectFactory<ModbusMaster> {
        private final String host;
        private final int port;
        private final int timeout;
        private final int retries;
        private final ModbusFactory modbusFactory = new ModbusFactory();
        
        public ModbusMasterFactory(String host, int port, int timeout, int retries) {
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            this.retries = retries;
        }
        
        @Override
        public ModbusMaster create() throws Exception {
            IpParameters params = new IpParameters();
            params.setHost(host);
            params.setPort(port);
            params.setEncapsulated(false);
            
            ModbusMaster master = modbusFactory.createTcpMaster(params, true);
            master.setTimeout(timeout);
            master.setRetries(retries);
            master.init();
            
            log.debug("创建Modbus连接: {}:{}", host, port);
            return master;
        }
        
        @Override
        public PooledObject<ModbusMaster> wrap(ModbusMaster master) {
            return new DefaultPooledObject<>(master);
        }
        
        @Override
        public void destroyObject(PooledObject<ModbusMaster> p) {
            ModbusMaster master = p.getObject();
            if (master != null) {
                master.destroy();
                log.debug("销毁Modbus连接: {}:{}", host, port);
            }
        }
        
        @Override
        public boolean validateObject(PooledObject<ModbusMaster> p) {
            ModbusMaster master = p.getObject();
            try {
                // 简单验证：检查是否已初始化
                return master != null && master.isInitialized();
            } catch (Exception e) {
                log.warn("验证Modbus连接失败: {}:{}", host, port, e);
                return false;
            }
        }
        
        @Override
        public void activateObject(PooledObject<ModbusMaster> p) throws Exception {
            // 激活对象时可以执行一些操作
        }
        
        @Override
        public void passivateObject(PooledObject<ModbusMaster> p) throws Exception {
            // 钝化对象时可以执行一些清理操作
        }
    }
    
    /**
     * 连接池配置
     */
    @Data
    public static class PoolConfig {
        /** 最大连接数 */
        private int maxTotal = 50;
        
        /** 最大空闲连接数 */
        private int maxIdle = 20;
        
        /** 最小空闲连接数 */
        private int minIdle = 5;
        
        /** 借用时测试 */
        private boolean testOnBorrow = true;
        
        /** 归还时测试 */
        private boolean testOnReturn = false;
        
        /** 空闲时测试 */
        private boolean testWhileIdle = true;
        
        /** 空闲检测周期（毫秒） */
        private long timeBetweenEvictionRunsMillis = 60000;
        
        /** 最小空闲时间（毫秒） */
        private long minEvictableIdleTimeMillis = 300000;
        
        /** 最大等待时间（毫秒） */
        private long maxWaitMillis = 5000;
        
        /** 连接超时（毫秒） */
        private int timeout = 3000;
        
        /** 重试次数 */
        private int retries = 2;
    }
    
    /**
     * 连接池状态
     */
    @Data
    @AllArgsConstructor
    public static class PoolStatus {
        /** 活跃连接数 */
        private int activeConnections;
        
        /** 空闲连接数 */
        private int idleConnections;
        
        /** 最大连接数 */
        private int maxConnections;
        
        /** 借用次数 */
        private long borrowedCount;
        
        /** 归还次数 */
        private long returnedCount;
        
        /** 创建次数 */
        private long createdCount;
        
        /** 销毁次数 */
        private long destroyedCount;
    }
    
    /**
     * 连接池管理器（管理多个目标的连接池）
     */
    @Slf4j
    public static class PoolManager {
        
        /** 连接池映射 <host:port, ModbusConnectionPool> */
        private final Map<String, ModbusConnectionPool> pools = new ConcurrentHashMap<>();
        
        /** 默认配置 */
        private final PoolConfig defaultConfig;
        
        public PoolManager() {
            this(new PoolConfig());
        }
        
        public PoolManager(PoolConfig defaultConfig) {
            this.defaultConfig = defaultConfig;
        }
        
        /**
         * 获取连接池（不存在则创建）
         */
        public ModbusConnectionPool getPool(String host, int port) {
            String key = host + ":" + port;
            return pools.computeIfAbsent(key, k -> {
                log.info("创建新的Modbus连接池: {}", key);
                return new ModbusConnectionPool(host, port, defaultConfig);
            });
        }
        
        /**
         * 移除连接池
         */
        public void removePool(String host, int port) {
            String key = host + ":" + port;
            ModbusConnectionPool pool = pools.remove(key);
            if (pool != null) {
                pool.close();
                log.info("移除Modbus连接池: {}", key);
            }
        }
        
        /**
         * 获取所有连接池状态
         */
        public Map<String, PoolStatus> getAllStatus() {
            Map<String, PoolStatus> status = new ConcurrentHashMap<>();
            pools.forEach((key, pool) -> status.put(key, pool.getStatus()));
            return status;
        }
        
        /**
         * 关闭所有连接池
         */
        public void closeAll() {
            pools.forEach((key, pool) -> {
                try {
                    pool.close();
                } catch (Exception e) {
                    log.error("关闭连接池失败: {}", key, e);
                }
            });
            pools.clear();
            log.info("所有Modbus连接池已关闭");
        }
    }
}
