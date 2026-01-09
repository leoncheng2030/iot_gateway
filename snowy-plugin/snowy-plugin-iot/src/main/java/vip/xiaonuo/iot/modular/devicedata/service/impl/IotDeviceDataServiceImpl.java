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
package vip.xiaonuo.iot.modular.devicedata.service.impl;

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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import java.math.BigDecimal;
import java.util.Date;
import vip.xiaonuo.iot.modular.devicedata.entity.IotDeviceData;
import vip.xiaonuo.iot.modular.devicedata.mapper.IotDeviceDataMapper;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataAddParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataEditParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataIdParam;
import vip.xiaonuo.iot.modular.devicedata.param.IotDeviceDataPageParam;
import vip.xiaonuo.iot.modular.devicedata.service.IotDeviceDataService;
import vip.xiaonuo.iot.core.storage.InfluxDBService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备数据Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:27
 **/
@Service
public class IotDeviceDataServiceImpl extends ServiceImpl<IotDeviceDataMapper, IotDeviceData> implements IotDeviceDataService {

    @Resource
    private InfluxDBService influxDBService;

    @Override
    public Page<IotDeviceData> page(IotDeviceDataPageParam iotDeviceDataPageParam) {
        // 只从 MySQL 查询事件和指令数据（PROPERTY 类型的数据已在 InfluxDB 中）
        QueryWrapper<IotDeviceData> queryWrapper = new QueryWrapper<IotDeviceData>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotDeviceDataPageParam.getDeviceId())) {
            queryWrapper.lambda().eq(IotDeviceData::getDeviceId, iotDeviceDataPageParam.getDeviceId());
        }
        // 只查询事件和指令响应数据
        queryWrapper.lambda().in(IotDeviceData::getDataType, "EVENT", "COMMAND_RESPONSE");
        
        if(ObjectUtil.isAllNotEmpty(iotDeviceDataPageParam.getSortField(), iotDeviceDataPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDeviceDataPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDeviceDataPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDeviceDataPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByDesc(IotDeviceData::getDataTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotDeviceDataAddParam iotDeviceDataAddParam) {
        IotDeviceData iotDeviceData = BeanUtil.toBean(iotDeviceDataAddParam, IotDeviceData.class);
        this.save(iotDeviceData);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDeviceDataEditParam iotDeviceDataEditParam) {
        IotDeviceData iotDeviceData = this.queryEntity(iotDeviceDataEditParam.getId());
        BeanUtil.copyProperties(iotDeviceDataEditParam, iotDeviceData);
        this.updateById(iotDeviceData);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDeviceDataIdParam> iotDeviceDataIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotDeviceDataIdParamList, IotDeviceDataIdParam::getId));
    }

    @Override
    public IotDeviceData detail(IotDeviceDataIdParam iotDeviceDataIdParam) {
        return this.queryEntity(iotDeviceDataIdParam.getId());
    }

    @Override
    public IotDeviceData queryEntity(String id) {
        IotDeviceData iotDeviceData = this.getById(id);
        if(ObjectUtil.isEmpty(iotDeviceData)) {
            throw new CommonException("设备数据不存在，id值为：{}", id);
        }
        return iotDeviceData;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceDataEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "设备数据导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceDataEditParam.class).sheet("设备数据").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备数据导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "设备数据导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotDeviceDataImportTemplate.xlsx"));
            // 读取excel
            List<IotDeviceDataEditParam> iotDeviceDataEditParamList =  EasyExcel.read(tempFile).head(IotDeviceDataEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDeviceData> allDataList = this.list();
            for (int i = 0; i < iotDeviceDataEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDeviceDataEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDeviceDataEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 设备数据导入失败：", e);
            throw new CommonException("设备数据导入失败");
        }
    }

    public JSONObject doImport(List<IotDeviceData> allDataList, IotDeviceDataEditParam iotDeviceDataEditParam, int i) {
        String id = iotDeviceDataEditParam.getId();
        String deviceId = iotDeviceDataEditParam.getDeviceId();
        String dataType = iotDeviceDataEditParam.getDataType();
        String dataKey = iotDeviceDataEditParam.getDataKey();
        String dataValue = iotDeviceDataEditParam.getDataValue();
        if(ObjectUtil.hasEmpty(id, deviceId, dataType, dataKey, dataValue)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDeviceData::getId).indexOf(iotDeviceDataEditParam.getId());
                IotDeviceData iotDeviceData;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDeviceData = new IotDeviceData();
                } else {
                    iotDeviceData = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDeviceDataEditParam, iotDeviceData);
                if(isAdd) {
                    allDataList.add(iotDeviceData);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDeviceData);
                }
                this.saveOrUpdate(iotDeviceData);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDeviceDataIdParam> iotDeviceDataIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceDataEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDeviceDataIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDeviceDataIdParamList, IotDeviceDataIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDeviceDataEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDeviceDataEditParam.class);
         }
         String fileName = "设备数据_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceDataEditParam.class).sheet("设备数据").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备数据导出失败：", e);
         CommonResponseUtil.renderError(response, "设备数据导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Override
    public List<JSONObject> getChartData(String deviceId) {
        // 从InfluxDB查询最近24小时的数据
        return influxDBService.getChartData(deviceId, 24);
    }
}
