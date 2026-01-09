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
package vip.xiaonuo.iot.modular.deviceshadow.service.impl;

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
import vip.xiaonuo.iot.modular.deviceshadow.entity.IotDeviceShadow;
import vip.xiaonuo.iot.modular.deviceshadow.mapper.IotDeviceShadowMapper;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowAddParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowEditParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowIdParam;
import vip.xiaonuo.iot.modular.deviceshadow.param.IotDeviceShadowPageParam;
import vip.xiaonuo.iot.modular.deviceshadow.service.IotDeviceShadowService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 设备影子表Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:28
 **/
@Service
public class IotDeviceShadowServiceImpl extends ServiceImpl<IotDeviceShadowMapper, IotDeviceShadow> implements IotDeviceShadowService {

    @Override
    public Page<IotDeviceShadow> page(IotDeviceShadowPageParam iotDeviceShadowPageParam) {
        QueryWrapper<IotDeviceShadow> queryWrapper = new QueryWrapper<IotDeviceShadow>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotDeviceShadowPageParam.getDeviceId())) {
            queryWrapper.lambda().like(IotDeviceShadow::getDeviceId, iotDeviceShadowPageParam.getDeviceId());
        }
        if(ObjectUtil.isAllNotEmpty(iotDeviceShadowPageParam.getSortField(), iotDeviceShadowPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDeviceShadowPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDeviceShadowPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDeviceShadowPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotDeviceShadow::getId);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotDeviceShadowAddParam iotDeviceShadowAddParam) {
        IotDeviceShadow iotDeviceShadow = BeanUtil.toBean(iotDeviceShadowAddParam, IotDeviceShadow.class);
        this.save(iotDeviceShadow);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDeviceShadowEditParam iotDeviceShadowEditParam) {
        IotDeviceShadow iotDeviceShadow = this.queryEntity(iotDeviceShadowEditParam.getId());
        BeanUtil.copyProperties(iotDeviceShadowEditParam, iotDeviceShadow);
        this.updateById(iotDeviceShadow);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDeviceShadowIdParam> iotDeviceShadowIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotDeviceShadowIdParamList, IotDeviceShadowIdParam::getId));
    }

    @Override
    public IotDeviceShadow detail(IotDeviceShadowIdParam iotDeviceShadowIdParam) {
        return this.queryEntity(iotDeviceShadowIdParam.getId());
    }

    @Override
    public IotDeviceShadow queryEntity(String id) {
        IotDeviceShadow iotDeviceShadow = this.getById(id);
        if(ObjectUtil.isEmpty(iotDeviceShadow)) {
            throw new CommonException("设备影子表不存在，id值为：{}", id);
        }
        return iotDeviceShadow;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceShadowEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "设备影子表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceShadowEditParam.class).sheet("设备影子表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备影子表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "设备影子表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotDeviceShadowImportTemplate.xlsx"));
            // 读取excel
            List<IotDeviceShadowEditParam> iotDeviceShadowEditParamList =  EasyExcel.read(tempFile).head(IotDeviceShadowEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDeviceShadow> allDataList = this.list();
            for (int i = 0; i < iotDeviceShadowEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDeviceShadowEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDeviceShadowEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 设备影子表导入失败：", e);
            throw new CommonException("设备影子表导入失败");
        }
    }

    public JSONObject doImport(List<IotDeviceShadow> allDataList, IotDeviceShadowEditParam iotDeviceShadowEditParam, int i) {
        String id = iotDeviceShadowEditParam.getId();
        String deviceId = iotDeviceShadowEditParam.getDeviceId();
        if(ObjectUtil.hasEmpty(id, deviceId)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDeviceShadow::getId).indexOf(iotDeviceShadowEditParam.getId());
                IotDeviceShadow iotDeviceShadow;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDeviceShadow = new IotDeviceShadow();
                } else {
                    iotDeviceShadow = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDeviceShadowEditParam, iotDeviceShadow);
                if(isAdd) {
                    allDataList.add(iotDeviceShadow);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDeviceShadow);
                }
                this.saveOrUpdate(iotDeviceShadow);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDeviceShadowIdParam> iotDeviceShadowIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceShadowEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDeviceShadowIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDeviceShadowIdParamList, IotDeviceShadowIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDeviceShadowEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDeviceShadowEditParam.class);
         }
         String fileName = "设备影子表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceShadowEditParam.class).sheet("设备影子表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备影子表导出失败：", e);
         CommonResponseUtil.renderError(response, "设备影子表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Override
    public IotDeviceShadow getByDeviceId(String id) {
        return this.getOne(new QueryWrapper<IotDeviceShadow>().eq("device_id", id));
    }

}
