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
package vip.xiaonuo.iot.modular.devicedriver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import java.math.BigDecimal;
import java.util.Date;
import vip.xiaonuo.iot.modular.devicedriver.entity.IotDeviceDriver;
import vip.xiaonuo.iot.modular.devicedriver.mapper.IotDeviceDriverMapper;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverAddParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverEditParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverIdParam;
import vip.xiaonuo.iot.modular.devicedriver.param.IotDeviceDriverPageParam;
import vip.xiaonuo.iot.modular.devicedriver.service.IotDeviceDriverService;
import vip.xiaonuo.iot.core.driver.DriverManager;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 设备驱动配置表Service接口实现类
 *
 * @author jetox
 * @date  2025/12/13 09:45
 **/
@Service
public class IotDeviceDriverServiceImpl extends ServiceImpl<IotDeviceDriverMapper, IotDeviceDriver> implements IotDeviceDriverService {

    @Resource
    private DriverManager driverManager;

    @Override
    public Page<IotDeviceDriver> page(IotDeviceDriverPageParam iotDeviceDriverPageParam) {
        QueryWrapper<IotDeviceDriver> queryWrapper = new QueryWrapper<IotDeviceDriver>().checkSqlInjection();
        if(ObjectUtil.isAllNotEmpty(iotDeviceDriverPageParam.getSortField(), iotDeviceDriverPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDeviceDriverPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDeviceDriverPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDeviceDriverPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotDeviceDriver::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotDeviceDriverAddParam iotDeviceDriverAddParam) {
        IotDeviceDriver iotDeviceDriver = BeanUtil.toBean(iotDeviceDriverAddParam, IotDeviceDriver.class);
        // 根据驱动类型自动设置实现类
        if (StrUtil.isBlank(iotDeviceDriver.getDriverClass())) {
            iotDeviceDriver.setDriverClass(getDriverClassByType(iotDeviceDriver.getDriverType()));
        }
        this.save(iotDeviceDriver);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDeviceDriverEditParam iotDeviceDriverEditParam) {
        IotDeviceDriver iotDeviceDriver = this.queryEntity(iotDeviceDriverEditParam.getId());
        BeanUtil.copyProperties(iotDeviceDriverEditParam, iotDeviceDriver);
        // 根据驱动类型自动设置实现类
        if (StrUtil.isBlank(iotDeviceDriver.getDriverClass())) {
            iotDeviceDriver.setDriverClass(getDriverClassByType(iotDeviceDriver.getDriverType()));
        }
        this.updateById(iotDeviceDriver);
    }

    /**
     * 根据驱动类型获取实现类
     */
    private String getDriverClassByType(String driverType) {
        return switch (driverType) {
            case "DTU_GATEWAY" -> "vip.xiaonuo.iot.core.driver.impl.DtuGatewayDriver";
            case "TCP_DIRECT" -> "vip.xiaonuo.iot.core.driver.impl.TcpDirectDriver";
            case "UDP_DIRECT" -> "vip.xiaonuo.iot.core.driver.impl.UdpDirectDriver";
            case "MODBUS_TCP" -> "vip.xiaonuo.iot.core.driver.impl.ModbusTcpDriver";
            case "MQTT" -> "vip.xiaonuo.iot.core.driver.impl.MqttDriver";
            case "HTTP" -> "vip.xiaonuo.iot.core.driver.impl.HttpDriver";
            case "LORA_GATEWAY" -> "vip.xiaonuo.iot.core.driver.impl.LoraGatewayDriver";
            case "ZIGBEE_GATEWAY" -> "vip.xiaonuo.iot.core.driver.impl.ZigbeeGatewayDriver";
            case "OPCUA" -> "vip.xiaonuo.iot.core.driver.impl.OpcUaDriver";
            case "CUSTOM" -> "vip.xiaonuo.iot.core.driver.impl.CustomDriver";
            default -> throw new CommonException("不支持的驱动类型: {}", driverType);
        };
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDeviceDriverIdParam> iotDeviceDriverIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotDeviceDriverIdParamList, IotDeviceDriverIdParam::getId));
    }

    @Override
    public IotDeviceDriver detail(IotDeviceDriverIdParam iotDeviceDriverIdParam) {
        return this.queryEntity(iotDeviceDriverIdParam.getId());
    }

    @Override
    public IotDeviceDriver queryEntity(String id) {
        IotDeviceDriver iotDeviceDriver = this.getById(id);
        if(ObjectUtil.isEmpty(iotDeviceDriver)) {
            throw new CommonException("设备驱动配置表不存在，id值为：{}", id);
        }
        return iotDeviceDriver;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceDriverEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "设备驱动配置表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceDriverEditParam.class).sheet("设备驱动配置表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备驱动配置表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "设备驱动配置表导入模板下载失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public JSONObject importData(MultipartFile file) {
        try {
            int successCount = 0;
            int errorCount = 0;
            JSONArray errorDetail = JSONUtil.createArray();
            // 创建临时文件
            File tempFile = FileUtil.writeBytes(file.getBytes(), FileUtil.file(FileUtil.getTmpDir() +
                    FileUtil.FILE_SEPARATOR + "iotDeviceDriverImportTemplate.xlsx"));
            // 读取excel
            List<IotDeviceDriverEditParam> iotDeviceDriverEditParamList =  EasyExcel.read(tempFile).head(IotDeviceDriverEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDeviceDriver> allDataList = this.list();
            for (int i = 0; i < iotDeviceDriverEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDeviceDriverEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDeviceDriverEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 设备驱动配置表导入失败：", e);
            throw new CommonException("设备驱动配置表导入失败");
        }
    }

    public JSONObject doImport(List<IotDeviceDriver> allDataList, IotDeviceDriverEditParam iotDeviceDriverEditParam, int i) {
        String id = iotDeviceDriverEditParam.getId();
        String driverName = iotDeviceDriverEditParam.getDriverName();
        String driverType = iotDeviceDriverEditParam.getDriverType();
        String driverClass = iotDeviceDriverEditParam.getDriverClass();
        if(ObjectUtil.hasEmpty(id, driverName, driverType, driverClass)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDeviceDriver::getId).indexOf(iotDeviceDriverEditParam.getId());
                IotDeviceDriver iotDeviceDriver;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDeviceDriver = new IotDeviceDriver();
                } else {
                    iotDeviceDriver = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDeviceDriverEditParam, iotDeviceDriver);
                if(isAdd) {
                    allDataList.add(iotDeviceDriver);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDeviceDriver);
                }
                this.saveOrUpdate(iotDeviceDriver);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDeviceDriverIdParam> iotDeviceDriverIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceDriverEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDeviceDriverIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDeviceDriverIdParamList, IotDeviceDriverIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDeviceDriverEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDeviceDriverEditParam.class);
         }
         String fileName = "设备驱动配置表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceDriverEditParam.class).sheet("设备驱动配置表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备驱动配置表导出失败：", e);
         CommonResponseUtil.renderError(response, "设备驱动配置表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Override
    public void startDriver(String driverId) {
        try {
            driverManager.startDriver(driverId);
        } catch (Exception e) {
            log.error("启动驱动失败", e);
            throw new CommonException(e.getMessage());
        }
    }

    @Override
    public void stopDriver(String driverId) {
        try {
            driverManager.stopDriver(driverId);
        } catch (Exception e) {
            log.error("停止驱动失败", e);
            throw new CommonException(e.getMessage());
        }
    }

    @Override
    public void restartDriver(String driverId) {
        try {
            driverManager.restartDriver(driverId);
        } catch (Exception e) {
            log.error("重启驱动失败", e);
            throw new CommonException(e.getMessage());
        }
    }

    @Override
    public boolean isDriverRunning(String driverId) {
        return driverManager.isDriverRunning(driverId);
    }
    
    @Override
    public JSONObject getDriverStatus(String driverId) {
        JSONObject status = new JSONObject();
        boolean isRunning = driverManager.isDriverRunning(driverId);
        status.set("running", isRunning);
        
        if (isRunning) {
            long uptime = driverManager.getDriverUptime(driverId);
            status.set("uptime", uptime);
        }
        
        return status;
    }
}
