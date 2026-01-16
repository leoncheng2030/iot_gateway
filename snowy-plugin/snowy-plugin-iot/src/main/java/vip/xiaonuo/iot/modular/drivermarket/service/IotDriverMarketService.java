package vip.xiaonuo.iot.modular.drivermarket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.iot.modular.drivermarket.entity.IotDriverMarket;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketIdParam;
import vip.xiaonuo.iot.modular.drivermarket.param.IotDriverMarketPageParam;

import java.util.List;

/**
 * 驱动市场服务接口
 *
 * @author wqs
 * @date 2026/01/15
 */
public interface IotDriverMarketService extends IService<IotDriverMarket> {

    /**
     * 分页查询
     *
     * @param param 查询参数
     * @return 分页结果
     */
    Page<IotDriverMarket> page(IotDriverMarketPageParam param);

    /**
     * 获取详情
     *
     * @param param ID参数
     * @return 详情
     */
    IotDriverMarket detail(IotDriverMarketIdParam param);

    /**
     * 根据驱动类型获取
     *
     * @param driverType 驱动类型
     * @return 驱动市场记录
     */
    IotDriverMarket getByDriverType(String driverType);

    /**
     * 安装驱动
     *
     * @param driverType 驱动类型
     */
    void installDriver(String driverType);

    /**
     * 卸载驱动
     *
     * @param driverType 驱动类型
     */
    void uninstallDriver(String driverType);

    /**
     * 启用驱动
     *
     * @param driverType 驱动类型
     */
    void enableDriver(String driverType);

    /**
     * 禁用驱动
     *
     * @param driverType 驱动类型
     */
    void disableDriver(String driverType);

    /**
     * 上传驱动JAR包
     *
     * @param jarFile JAR文件
     */
    void uploadDriver(MultipartFile jarFile);

    /**
     * 同步内置驱动列表
     */
    void syncBuiltinDrivers();

    /**
     * 获取已启用的驱动列表
     *
     * @return 已启用驱动列表
     */
    List<IotDriverMarket> getEnabledDrivers();
}
