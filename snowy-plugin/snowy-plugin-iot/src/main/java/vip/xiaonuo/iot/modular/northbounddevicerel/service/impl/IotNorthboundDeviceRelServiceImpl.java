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
package vip.xiaonuo.iot.modular.northbounddevicerel.service.impl;

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
import vip.xiaonuo.iot.modular.northbounddevicerel.entity.IotNorthboundDeviceRel;
import vip.xiaonuo.iot.modular.northbounddevicerel.mapper.IotNorthboundDeviceRelMapper;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelAddParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelEditParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelIdParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.param.IotNorthboundDeviceRelPageParam;
import vip.xiaonuo.iot.modular.northbounddevicerel.service.IotNorthboundDeviceRelService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 北向推送设备关联表Service接口实现类
 *
 * @author jetox
 * @date  2026/01/08 10:25
 **/
@Service
public class IotNorthboundDeviceRelServiceImpl extends ServiceImpl<IotNorthboundDeviceRelMapper, IotNorthboundDeviceRel> implements IotNorthboundDeviceRelService {

    @Override
    public Page<IotNorthboundDeviceRel> page(IotNorthboundDeviceRelPageParam iotNorthboundDeviceRelPageParam) {
        QueryWrapper<IotNorthboundDeviceRel> queryWrapper = new QueryWrapper<IotNorthboundDeviceRel>().checkSqlInjection();
        if(ObjectUtil.isAllNotEmpty(iotNorthboundDeviceRelPageParam.getSortField(), iotNorthboundDeviceRelPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotNorthboundDeviceRelPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotNorthboundDeviceRelPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotNorthboundDeviceRelPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotNorthboundDeviceRel::getId);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotNorthboundDeviceRelAddParam iotNorthboundDeviceRelAddParam) {
        IotNorthboundDeviceRel iotNorthboundDeviceRel = BeanUtil.toBean(iotNorthboundDeviceRelAddParam, IotNorthboundDeviceRel.class);
        this.save(iotNorthboundDeviceRel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotNorthboundDeviceRelEditParam iotNorthboundDeviceRelEditParam) {
        IotNorthboundDeviceRel iotNorthboundDeviceRel = this.queryEntity(iotNorthboundDeviceRelEditParam.getId());
        BeanUtil.copyProperties(iotNorthboundDeviceRelEditParam, iotNorthboundDeviceRel);
        this.updateById(iotNorthboundDeviceRel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotNorthboundDeviceRelIdParam> iotNorthboundDeviceRelIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotNorthboundDeviceRelIdParamList, IotNorthboundDeviceRelIdParam::getId));
    }

    @Override
    public IotNorthboundDeviceRel detail(IotNorthboundDeviceRelIdParam iotNorthboundDeviceRelIdParam) {
        return this.queryEntity(iotNorthboundDeviceRelIdParam.getId());
    }

    @Override
    public IotNorthboundDeviceRel queryEntity(String id) {
        IotNorthboundDeviceRel iotNorthboundDeviceRel = this.getById(id);
        if(ObjectUtil.isEmpty(iotNorthboundDeviceRel)) {
            throw new CommonException("北向推送设备关联表不存在，id值为：{}", id);
        }
        return iotNorthboundDeviceRel;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundDeviceRelEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "北向推送设备关联表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundDeviceRelEditParam.class).sheet("北向推送设备关联表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送设备关联表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "北向推送设备关联表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotNorthboundDeviceRelImportTemplate.xlsx"));
            // 读取excel
            List<IotNorthboundDeviceRelEditParam> iotNorthboundDeviceRelEditParamList =  EasyExcel.read(tempFile).head(IotNorthboundDeviceRelEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotNorthboundDeviceRel> allDataList = this.list();
            for (int i = 0; i < iotNorthboundDeviceRelEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotNorthboundDeviceRelEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotNorthboundDeviceRelEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 北向推送设备关联表导入失败：", e);
            throw new CommonException("北向推送设备关联表导入失败");
        }
    }

    public JSONObject doImport(List<IotNorthboundDeviceRel> allDataList, IotNorthboundDeviceRelEditParam iotNorthboundDeviceRelEditParam, int i) {
        String id = iotNorthboundDeviceRelEditParam.getId();
        String configId = iotNorthboundDeviceRelEditParam.getConfigId();
        String deviceId = iotNorthboundDeviceRelEditParam.getDeviceId();
        String productId = iotNorthboundDeviceRelEditParam.getProductId();
        String deviceGroupId = iotNorthboundDeviceRelEditParam.getDeviceGroupId();
        if(ObjectUtil.hasEmpty(id, configId, deviceId, productId, deviceGroupId)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotNorthboundDeviceRel::getId).indexOf(iotNorthboundDeviceRelEditParam.getId());
                IotNorthboundDeviceRel iotNorthboundDeviceRel;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotNorthboundDeviceRel = new IotNorthboundDeviceRel();
                } else {
                    iotNorthboundDeviceRel = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotNorthboundDeviceRelEditParam, iotNorthboundDeviceRel);
                if(isAdd) {
                    allDataList.add(iotNorthboundDeviceRel);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotNorthboundDeviceRel);
                }
                this.saveOrUpdate(iotNorthboundDeviceRel);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotNorthboundDeviceRelIdParam> iotNorthboundDeviceRelIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundDeviceRelEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotNorthboundDeviceRelIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotNorthboundDeviceRelIdParamList, IotNorthboundDeviceRelIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotNorthboundDeviceRelEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotNorthboundDeviceRelEditParam.class);
         }
         String fileName = "北向推送设备关联表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundDeviceRelEditParam.class).sheet("北向推送设备关联表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送设备关联表导出失败：", e);
         CommonResponseUtil.renderError(response, "北向推送设备关联表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bind(JSONObject params) {
        List<String> deviceIds = params.getBeanList("deviceIds", String.class);
        List<String> configIds = params.getBeanList("configIds", String.class);

        if (CollectionUtil.isEmpty(deviceIds) || CollectionUtil.isEmpty(configIds)) {
            throw new CommonException("设备ID或配置ID不能为空");
        }

        // 先删除这些设备的所有绑定
        LambdaQueryWrapper<IotNorthboundDeviceRel> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(IotNorthboundDeviceRel::getDeviceId, deviceIds);
        this.remove(deleteWrapper);

        // 重新绑定
        List<IotNorthboundDeviceRel> relList = new ArrayList<>();
        for (String deviceId : deviceIds) {
            for (String configId : configIds) {
                IotNorthboundDeviceRel rel = new IotNorthboundDeviceRel();
                rel.setDeviceId(deviceId);
                rel.setConfigId(configId);
                relList.add(rel);
            }
        }

        if (CollectionUtil.isNotEmpty(relList)) {
            this.saveBatch(relList);
        }
    }
}
