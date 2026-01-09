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
package vip.xiaonuo.iot.modular.devicedriverrel.service.impl;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import java.math.BigDecimal;
import java.util.Date;
import vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel;
import vip.xiaonuo.iot.modular.devicedriverrel.mapper.IotDeviceDriverRelMapper;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelAddParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelEditParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelIdParam;
import vip.xiaonuo.iot.modular.devicedriverrel.param.IotDeviceDriverRelPageParam;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;
import cn.hutool.core.util.IdUtil;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 设备驱动Service接口实现类
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
@Slf4j
@Service
public class IotDeviceDriverRelServiceImpl extends ServiceImpl<IotDeviceDriverRelMapper, IotDeviceDriverRel> implements IotDeviceDriverRelService {

    @Override
    public Page<IotDeviceDriverRel> page(IotDeviceDriverRelPageParam iotDeviceDriverRelPageParam) {
        QueryWrapper<IotDeviceDriverRel> queryWrapper = new QueryWrapper<IotDeviceDriverRel>().checkSqlInjection();
        if(ObjectUtil.isAllNotEmpty(iotDeviceDriverRelPageParam.getSortField(), iotDeviceDriverRelPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDeviceDriverRelPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDeviceDriverRelPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDeviceDriverRelPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotDeviceDriverRel::getId);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotDeviceDriverRelAddParam iotDeviceDriverRelAddParam) {
        IotDeviceDriverRel iotDeviceDriverRel = BeanUtil.toBean(iotDeviceDriverRelAddParam, IotDeviceDriverRel.class);
        this.save(iotDeviceDriverRel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDeviceDriverRelEditParam iotDeviceDriverRelEditParam) {
        IotDeviceDriverRel iotDeviceDriverRel = this.queryEntity(iotDeviceDriverRelEditParam.getId());
        BeanUtil.copyProperties(iotDeviceDriverRelEditParam, iotDeviceDriverRel);
        this.updateById(iotDeviceDriverRel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDeviceDriverRelIdParam> iotDeviceDriverRelIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotDeviceDriverRelIdParamList, IotDeviceDriverRelIdParam::getId));
    }

    @Override
    public IotDeviceDriverRel detail(IotDeviceDriverRelIdParam iotDeviceDriverRelIdParam) {
        return this.queryEntity(iotDeviceDriverRelIdParam.getId());
    }

    @Override
    public IotDeviceDriverRel queryEntity(String id) {
        IotDeviceDriverRel iotDeviceDriverRel = this.getById(id);
        if(ObjectUtil.isEmpty(iotDeviceDriverRel)) {
            throw new CommonException("设备驱动不存在，id值为：{}", id);
        }
        return iotDeviceDriverRel;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceDriverRelEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "设备驱动导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceDriverRelEditParam.class).sheet("设备驱动").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备驱动导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "设备驱动导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotDeviceDriverRelImportTemplate.xlsx"));
            // 读取excel
            List<IotDeviceDriverRelEditParam> iotDeviceDriverRelEditParamList =  EasyExcel.read(tempFile).head(IotDeviceDriverRelEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDeviceDriverRel> allDataList = this.list();
            for (int i = 0; i < iotDeviceDriverRelEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDeviceDriverRelEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDeviceDriverRelEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 设备驱动导入失败：", e);
            throw new CommonException("设备驱动导入失败");
        }
    }

    public JSONObject doImport(List<IotDeviceDriverRel> allDataList, IotDeviceDriverRelEditParam iotDeviceDriverRelEditParam, int i) {
        String id = iotDeviceDriverRelEditParam.getId();
        String deviceId = iotDeviceDriverRelEditParam.getDeviceId();
        String driverId = iotDeviceDriverRelEditParam.getDriverId();
        String deviceConfig = iotDeviceDriverRelEditParam.getDeviceConfig();
        if(ObjectUtil.hasEmpty(id, deviceId, driverId, deviceConfig)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDeviceDriverRel::getId).indexOf(iotDeviceDriverRelEditParam.getId());
                IotDeviceDriverRel iotDeviceDriverRel;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDeviceDriverRel = new IotDeviceDriverRel();
                } else {
                    iotDeviceDriverRel = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDeviceDriverRelEditParam, iotDeviceDriverRel);
                if(isAdd) {
                    allDataList.add(iotDeviceDriverRel);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDeviceDriverRel);
                }
                this.saveOrUpdate(iotDeviceDriverRel);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDeviceDriverRelIdParam> iotDeviceDriverRelIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceDriverRelEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDeviceDriverRelIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDeviceDriverRelIdParamList, IotDeviceDriverRelIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDeviceDriverRelEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDeviceDriverRelEditParam.class);
         }
         String fileName = "设备驱动_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceDriverRelEditParam.class).sheet("设备驱动").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备驱动导出失败：", e);
         CommonResponseUtil.renderError(response, "设备驱动导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Override
    public List<IotDeviceDriverRel> listByDeviceId(String deviceId) {
        return this.lambdaQuery()
                .eq(IotDeviceDriverRel::getDeviceId, deviceId)
                .list();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bindDriver(String deviceId, String driverId, String deviceConfig) {
        // 1. 检查设备是否已经绑定了其他驱动（一个设备只能绑定一个驱动）
        List<IotDeviceDriverRel> existingRels = this.lambdaQuery()
                .eq(IotDeviceDriverRel::getDeviceId, deviceId)
                .list();
        
        if (!existingRels.isEmpty()) {
            // 检查是否是同一个驱动
            IotDeviceDriverRel existingRel = existingRels.get(0);
            if (!existingRel.getDriverId().equals(driverId)) {
                // 已绑定其他驱动，先删除旧绑定
                this.removeByIds(CollStreamUtil.toList(existingRels, IotDeviceDriverRel::getId));
                log.info("设备 {} 已解除与驱动 {} 的绑定，准备绑定新驱动 {}", deviceId, existingRel.getDriverId(), driverId);
            } else {
                // 同一个驱动，只更新配置
                existingRel.setDeviceConfig(deviceConfig);
                this.updateById(existingRel);
                log.info("更新设备 {} 与驱动 {} 的配置", deviceId, driverId);
                return;
            }
        }
        
        // 2. 创建新绑定
        IotDeviceDriverRel rel = new IotDeviceDriverRel();
        rel.setDeviceId(deviceId);
        rel.setDriverId(driverId);
        rel.setDeviceConfig(deviceConfig);
        this.save(rel);
        log.info("设备 {} 成功绑定驱动 {}", deviceId, driverId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unbindDriver(String deviceId, String driverId) {
        this.lambdaUpdate()
                .eq(IotDeviceDriverRel::getDeviceId, deviceId)
                .eq(IotDeviceDriverRel::getDriverId, driverId)
                .remove();
    }
}
