package vip.xiaonuo.iot.modular.drivermarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.iot.core.driver.spi.DriverProvider;
import vip.xiaonuo.iot.core.driver.spi.DriverSpiLoader;
import vip.xiaonuo.iot.core.driver.spi.ExternalDriverLoader;
import vip.xiaonuo.iot.modular.devicedriver.service.IotDeviceDriverService;
import vip.xiaonuo.iot.modular.drivermarket.entity.IotDriverMarket;
import vip.xiaonuo.iot.modular.drivermarket.enums.DriverInstallStatusEnum;
import vip.xiaonuo.iot.modular.drivermarket.enums.DriverSourceTypeEnum;
import vip.xiaonuo.iot.modular.drivermarket.mapper.IotDriverMarketMapper;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketIdParam;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketPageParam;
import vip.xiaonuo.iot.modular.drivermarket.service.IotDriverMarketService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 驱动市场服务实现类
 *
 * @author wqs
 * @date 2026/01/15
 */
@Slf4j
@Service
public class IotDriverMarketServiceImpl extends ServiceImpl<IotDriverMarketMapper, IotDriverMarket>
        implements IotDriverMarketService {

    @Resource
    private DriverSpiLoader driverSpiLoader;

    @Resource
    private ExternalDriverLoader externalDriverLoader;

    @Resource
    private IotDeviceDriverService iotDeviceDriverService;

    private static final String DRIVERS_DIR = "drivers";

    @PostConstruct
    public void init() {
        // 延迟同步，等待 DriverSpiLoader 加载完成
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                syncBuiltinDrivers();
            } catch (Exception e) {
                log.error("同步内置驱动失败", e);
            }
        }, "driver-market-sync").start();
    }

    @Override
    public Page<IotDriverMarket> page(IotDriverMarketPageParam param) {
        QueryWrapper<IotDriverMarket> queryWrapper = new QueryWrapper<IotDriverMarket>()
                .like(StrUtil.isNotBlank(param.getSearchKey()), "driver_name", param.getSearchKey())
                .eq(StrUtil.isNotBlank(param.getSourceType()), "source_type", param.getSourceType())
                .eq(StrUtil.isNotBlank(param.getInstallStatus()), "install_status", param.getInstallStatus())
                .eq(StrUtil.isNotBlank(param.getCategory()), "category", param.getCategory())
                .orderByAsc("sort_code");

        return this.page(new Page<>(param.getCurrent(), param.getSize()), queryWrapper);
    }

    @Override
    public IotDriverMarket detail(IotDriverMarketIdParam param) {
        return this.queryEntity(param.getId());
    }

    @Override
    public IotDriverMarket getByDriverType(String driverType) {
        return this.lambdaQuery()
                .eq(IotDriverMarket::getDriverType, driverType)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void installDriver(String driverType) {
        IotDriverMarket driver = getByDriverType(driverType);
        if (driver == null) {
            throw new CommonException("驱动不存在: {}", driverType);
        }

        String currentStatus = driver.getInstallStatus();
        if (!DriverInstallStatusEnum.UNINSTALLED.getValue().equals(currentStatus)) {
            throw new CommonException("驱动已安装或状态异常");
        }

        // 验证驱动可加载性
        if (DriverSourceTypeEnum.BUILTIN.getValue().equals(driver.getSourceType())) {
            DriverProvider provider = driverSpiLoader.getProvider(driverType);
            if (provider == null) {
                throw new CommonException("驱动提供者未找到: {}", driverType);
            }
        } else {
            // 上传驱动验证 JAR 文件
            if (StrUtil.isBlank(driver.getJarFilePath())) {
                throw new CommonException("JAR 文件路径为空");
            }
            File jarFile = new File(driver.getJarFilePath());
            if (!jarFile.exists()) {
                throw new CommonException("JAR 文件不存在: {}", driver.getJarFilePath());
            }
        }

        // 更新状态
        driver.setInstallStatus(DriverInstallStatusEnum.INSTALLED.getValue());
        driver.setInstalledTime(new Date());
        this.updateById(driver);

        log.info("驱动安装成功: {}", driverType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uninstallDriver(String driverType) {
        IotDriverMarket driver = getByDriverType(driverType);
        if (driver == null) {
            throw new CommonException("驱动不存在: {}", driverType);
        }

        // 检查是否被使用
        long usageCount = iotDeviceDriverService.lambdaQuery()
                .eq(vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver::getDriverType, driverType)
                .count();

        if (usageCount > 0) {
            throw new CommonException("该驱动正在被 {} 个驱动实例使用，无法卸载", usageCount);
        }

        // 从 SpiLoader 卸载
        driverSpiLoader.unregisterDriver(driverType);

        if (DriverSourceTypeEnum.UPLOADED.getValue().equals(driver.getSourceType())) {
            // 上传驱动：卸载 JAR 并删除记录
            if (StrUtil.isNotBlank(driver.getJarFilePath())) {
                externalDriverLoader.unloadJar(driver.getJarFilePath());
                File jarFile = new File(driver.getJarFilePath());
                if (jarFile.exists()) {
                    jarFile.delete();
                }
            }
            this.removeById(driver.getId());
            log.info("上传驱动已卸载并删除: {}", driverType);
        } else {
            // 内置驱动：重置状态
            driver.setInstallStatus(DriverInstallStatusEnum.UNINSTALLED.getValue());
            driver.setInstalledTime(null);
            driver.setEnabledTime(null);
            this.updateById(driver);
            log.info("内置驱动已卸载: {}", driverType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableDriver(String driverType) {
        IotDriverMarket driver = getByDriverType(driverType);
        if (driver == null) {
            throw new CommonException("驱动不存在: {}", driverType);
        }

        String currentStatus = driver.getInstallStatus();
        if (!DriverInstallStatusEnum.INSTALLED.getValue().equals(currentStatus) &&
                !DriverInstallStatusEnum.DISABLED.getValue().equals(currentStatus)) {
            throw new CommonException("驱动状态异常，无法启用");
        }

        // 确保驱动已注册
        DriverProvider provider = driverSpiLoader.getProvider(driverType);
        if (provider == null) {
            // 尝试重新加载
            if (DriverSourceTypeEnum.UPLOADED.getValue().equals(driver.getSourceType())) {
                if (StrUtil.isNotBlank(driver.getJarFilePath())) {
                    File jarFile = new File(driver.getJarFilePath());
                    if (jarFile.exists()) {
                        try {
                            externalDriverLoader.loadFromJar(jarFile);
                        } catch (IOException e) {
                            throw new CommonException("加载驱动JAR失败: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // 更新状态
        driver.setInstallStatus(DriverInstallStatusEnum.ENABLED.getValue());
        driver.setEnabledTime(new Date());
        driver.setDownloadCount(ObjectUtil.defaultIfNull(driver.getDownloadCount(), 0) + 1);
        this.updateById(driver);

        log.info("驱动已启用: {}", driverType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableDriver(String driverType) {
        IotDriverMarket driver = getByDriverType(driverType);
        if (driver == null) {
            throw new CommonException("驱动不存在: {}", driverType);
        }

        if (!DriverInstallStatusEnum.ENABLED.getValue().equals(driver.getInstallStatus())) {
            throw new CommonException("驱动未启用，无法禁用");
        }

        // 检查是否被使用
        long usageCount = iotDeviceDriverService.lambdaQuery()
                .eq(vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver::getDriverType, driverType)
                .count();

        if (usageCount > 0) {
            throw new CommonException("该驱动正在被 {} 个驱动实例使用，无法禁用", usageCount);
        }

        driver.setInstallStatus(DriverInstallStatusEnum.DISABLED.getValue());
        this.updateById(driver);

        log.info("驱动已禁用: {}", driverType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadDriver(MultipartFile jarFile) {
        if (jarFile == null || jarFile.isEmpty()) {
            throw new CommonException("请选择文件");
        }

        String originalFilename = jarFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".jar")) {
            throw new CommonException("只支持 JAR 格式文件");
        }

        // 确保目录存在
        File driversDir = new File(DRIVERS_DIR);
        if (!driversDir.exists()) {
            driversDir.mkdirs();
        }

        // 生成唯一文件名
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + ".jar";
        File targetFile = new File(driversDir, uniqueFileName);

        try {
            // 保存文件
            Files.copy(jarFile.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 加载驱动
            List<String> loadedTypes = externalDriverLoader.loadFromJar(targetFile);
            if (loadedTypes.isEmpty()) {
                targetFile.delete();
                throw new CommonException("JAR 中未找到有效的驱动提供者");
            }

            // 遍历加载的驱动类型
            for (String driverType : loadedTypes) {
                // 检查是否已存在
                IotDriverMarket existing = getByDriverType(driverType);
                if (existing != null) {
                    targetFile.delete();
                    externalDriverLoader.unloadJar(targetFile.getAbsolutePath());
                    throw new CommonException("驱动类型 {} 已存在", driverType);
                }

                // 获取驱动提供者信息
                DriverProvider provider = driverSpiLoader.getProvider(driverType);
                if (provider == null) {
                    continue;
                }

                // 创建记录
                IotDriverMarket newDriver = new IotDriverMarket();
                newDriver.setDriverType(driverType);
                newDriver.setDriverName(provider.getDriverName());
                newDriver.setDriverVersion(provider.getProtocolVersion());
                newDriver.setDescription(provider.getDriverDescription());
                newDriver.setSourceType(DriverSourceTypeEnum.UPLOADED.getValue());
                newDriver.setInstallStatus(DriverInstallStatusEnum.INSTALLED.getValue());
                newDriver.setJarFilePath(targetFile.getAbsolutePath());
                newDriver.setProviderClass(provider.getClass().getName());
                newDriver.setSupportsHotUpdate(provider.supportsHotUpdate());
                newDriver.setInstalledTime(new Date());
                newDriver.setSortCode(100);
                newDriver.setDownloadCount(0);

                if (provider.getConfigFields() != null) {
                    newDriver.setConfigFields(JSONUtil.toJsonStr(provider.getConfigFields()));
                }

                if (provider.getProviderInfo() != null) {
                    newDriver.setVendor(provider.getProviderInfo().getVendor());
                    newDriver.setProtocolVersion(provider.getProviderInfo().getVersion());
                }

                this.save(newDriver);
                log.info("上传驱动成功: {}", driverType);
            }

        } catch (IOException e) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw new CommonException("文件上传失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBuiltinDrivers() {
        log.info("开始同步内置驱动列表...");

        Collection<DriverProvider> providers = driverSpiLoader.getAllProviders();

        for (DriverProvider provider : providers) {
            String driverType = provider.getDriverType();

            IotDriverMarket existing = getByDriverType(driverType);
            if (existing == null) {
                // 新增记录
                IotDriverMarket newDriver = new IotDriverMarket();
                newDriver.setDriverType(driverType);
                newDriver.setDriverName(provider.getDriverName());
                newDriver.setDriverVersion(provider.getProtocolVersion());
                newDriver.setDescription(provider.getDriverDescription());
                newDriver.setSourceType(DriverSourceTypeEnum.BUILTIN.getValue());
                newDriver.setInstallStatus(DriverInstallStatusEnum.UNINSTALLED.getValue());
                newDriver.setProviderClass(provider.getClass().getName());
                newDriver.setSupportsHotUpdate(provider.supportsHotUpdate());
                newDriver.setSortCode(provider.getPriority());
                newDriver.setDownloadCount(0);

                if (provider.getConfigFields() != null) {
                    newDriver.setConfigFields(JSONUtil.toJsonStr(provider.getConfigFields()));
                }

                if (provider.getProviderInfo() != null) {
                    newDriver.setVendor(provider.getProviderInfo().getVendor());
                    newDriver.setProtocolVersion(provider.getProviderInfo().getVersion());
                }

                // 设置分类
                newDriver.setCategory(guessCategory(driverType));

                this.save(newDriver);
                log.info("同步内置驱动: {}", driverType);
            } else {
                // 更新元数据（保持安装状态不变）
                existing.setDriverName(provider.getDriverName());
                existing.setDescription(provider.getDriverDescription());
                existing.setProviderClass(provider.getClass().getName());
                existing.setSupportsHotUpdate(provider.supportsHotUpdate());

                if (provider.getConfigFields() != null) {
                    existing.setConfigFields(JSONUtil.toJsonStr(provider.getConfigFields()));
                }

                if (provider.getProviderInfo() != null) {
                    existing.setVendor(provider.getProviderInfo().getVendor());
                    existing.setProtocolVersion(provider.getProviderInfo().getVersion());
                }

                this.updateById(existing);
            }
        }

        log.info("内置驱动同步完成，共 {} 个", providers.size());
    }

    @Override
    public List<IotDriverMarket> getEnabledDrivers() {
        return this.lambdaQuery()
                .eq(IotDriverMarket::getInstallStatus, DriverInstallStatusEnum.ENABLED.getValue())
                .list();
    }

    /**
     * 查询实体
     */
    private IotDriverMarket queryEntity(String id) {
        IotDriverMarket entity = this.getById(id);
        if (entity == null) {
            throw new CommonException("驱动不存在");
        }
        return entity;
    }

    /**
     * 根据驱动类型猜测分类
     */
    private String guessCategory(String driverType) {
        if (driverType == null) {
            return "OTHER";
        }
        String type = driverType.toUpperCase();
        if (type.contains("MODBUS") || type.contains("S7") || type.contains("OPC")) {
            return "INDUSTRIAL";
        }
        if (type.contains("MQTT") || type.contains("HTTP") || type.contains("TCP") || type.contains("UDP")) {
            return "NETWORK";
        }
        if (type.contains("GATEWAY") || type.contains("DTU") || type.contains("LORA") || type.contains("ZIGBEE")) {
            return "GATEWAY";
        }
        return "OTHER";
    }
}
