package vip.xiaonuo.iot.core.driver.spi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部驱动加载器
 * 支持从JAR文件动态加载驱动
 * 
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Component
public class ExternalDriverLoader {
    
    @Resource
    private DriverSpiLoader driverSpiLoader;
    
    /** 已加载的外部驱动 <jarPath, List<driverType>> */
    private final Map<String, List<String>> loadedDrivers = new ConcurrentHashMap<>();
    
    /** 类加载器缓存 <jarPath, ClassLoader> */
    private final Map<String, URLClassLoader> classLoaderCache = new ConcurrentHashMap<>();
    
    /** 默认驱动目录 */
    private static final String DEFAULT_DRIVER_DIR = "drivers";
    
    /**
     * 从JAR文件加载驱动
     * 
     * @param jarFile 驱动JAR文件
     * @return 加载的驱动类型列表
     * @throws IOException 加载失败
     */
    public List<String> loadFromJar(File jarFile) throws IOException {
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("JAR文件不存在: " + jarFile.getAbsolutePath());
        }
        
        if (!jarFile.getName().toLowerCase().endsWith(".jar")) {
            throw new IllegalArgumentException("无效的JAR文件: " + jarFile.getName());
        }
        
        String jarPath = jarFile.getAbsolutePath();
        
        // 检查是否已加载
        if (loadedDrivers.containsKey(jarPath)) {
            log.warn("JAR已加载，跳过: {}", jarPath);
            return loadedDrivers.get(jarPath);
        }
        
        List<String> loadedTypes = new ArrayList<>();
        
        try {
            // 创建独立的类加载器
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                getClass().getClassLoader()
            );
            
            // 使用ServiceLoader加载驱动
            ServiceLoader<DriverProvider> loader = ServiceLoader.load(
                DriverProvider.class, 
                classLoader
            );
            
            for (DriverProvider provider : loader) {
                try {
                    String type = provider.getDriverType();
                    driverSpiLoader.registerDriver(provider);
                    loadedTypes.add(type);
                    
                    log.info("从JAR加载驱动: jar={}, type={}, name={}", 
                        jarFile.getName(), type, provider.getDriverName());
                        
                } catch (Exception e) {
                    log.error("注册外部驱动失败: {}", provider.getClass().getName(), e);
                }
            }
            
            if (loadedTypes.isEmpty()) {
                classLoader.close();
                log.warn("JAR中未发现驱动: {}", jarPath);
            } else {
                classLoaderCache.put(jarPath, classLoader);
                loadedDrivers.put(jarPath, loadedTypes);
            }
            
        } catch (Exception e) {
            log.error("加载外部驱动失败: {}", jarFile.getName(), e);
            throw new IOException("加载驱动JAR失败: " + e.getMessage(), e);
        }
        
        return loadedTypes;
    }
    
    /**
     * 扫描目录加载所有驱动JAR
     * 
     * @param directory 驱动目录
     * @return 加载结果 <jarName, List<driverType>>
     */
    public Map<String, List<String>> loadFromDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            log.info("驱动目录不存在，尝试创建: {}", directory);
            if (!dir.mkdirs()) {
                log.warn("创建驱动目录失败: {}", directory);
                return Collections.emptyMap();
            }
        }
        
        if (!dir.isDirectory()) {
            log.warn("路径不是目录: {}", directory);
            return Collections.emptyMap();
        }
        
        File[] jarFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("驱动目录为空: {}", directory);
            return Collections.emptyMap();
        }
        
        Map<String, List<String>> results = new HashMap<>();
        
        for (File jarFile : jarFiles) {
            try {
                List<String> types = loadFromJar(jarFile);
                if (!types.isEmpty()) {
                    results.put(jarFile.getName(), types);
                }
            } catch (Exception e) {
                log.error("加载驱动JAR失败: {}", jarFile.getName(), e);
            }
        }
        
        log.info("从目录加载驱动完成: dir={}, 加载{}个JAR", directory, results.size());
        return results;
    }
    
    /**
     * 加载默认驱动目录
     * 
     * @return 加载结果
     */
    public Map<String, List<String>> loadDefaultDirectory() {
        return loadFromDirectory(DEFAULT_DRIVER_DIR);
    }
    
    /**
     * 卸载JAR中的驱动
     * 
     * @param jarPath JAR文件路径
     * @return true表示卸载成功
     */
    public boolean unloadJar(String jarPath) {
        List<String> types = loadedDrivers.remove(jarPath);
        if (types == null) {
            log.warn("JAR未加载: {}", jarPath);
            return false;
        }
        
        // 卸载驱动
        for (String type : types) {
            driverSpiLoader.unregisterDriver(type);
        }
        
        // 关闭类加载器
        URLClassLoader classLoader = classLoaderCache.remove(jarPath);
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("关闭类加载器失败: {}", jarPath, e);
            }
        }
        
        log.info("卸载JAR驱动: path={}, types={}", jarPath, types);
        return true;
    }
    
    /**
     * 获取已加载的外部驱动信息
     * 
     * @return 已加载的驱动信息
     */
    public Map<String, List<String>> getLoadedDrivers() {
        return Collections.unmodifiableMap(loadedDrivers);
    }
    
    /**
     * 监控驱动目录变化（可选功能）
     * 
     * @param directory 监控目录
     * @throws IOException 监控失败
     */
    public void watchDirectory(String directory) throws IOException {
        Path path = Paths.get(directory);
        
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, 
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE);
        
        Thread watchThread = new Thread(() -> {
            log.info("开始监控驱动目录: {}", directory);
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path fileName = (Path) event.context();
                        
                        if (fileName.toString().toLowerCase().endsWith(".jar")) {
                            File jarFile = path.resolve(fileName).toFile();
                            
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                log.info("检测到新驱动JAR: {}", fileName);
                                // 延迟加载，确保文件写入完成
                                Thread.sleep(1000);
                                try {
                                    loadFromJar(jarFile);
                                } catch (Exception e) {
                                    log.error("自动加载驱动失败: {}", fileName, e);
                                }
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                log.info("检测到驱动JAR删除: {}", fileName);
                                unloadJar(jarFile.getAbsolutePath());
                            }
                        }
                    }
                    
                    key.reset();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("监控驱动目录异常", e);
                }
            }
            
            log.info("停止监控驱动目录");
        }, "driver-watcher");
        
        watchThread.setDaemon(true);
        watchThread.start();
    }
    
    /**
     * 重新加载所有外部驱动
     */
    public void reloadAll() {
        log.info("重新加载所有外部驱动...");
        
        // 卸载所有
        new ArrayList<>(loadedDrivers.keySet()).forEach(this::unloadJar);
        
        // 重新加载默认目录
        loadDefaultDirectory();
    }
}
