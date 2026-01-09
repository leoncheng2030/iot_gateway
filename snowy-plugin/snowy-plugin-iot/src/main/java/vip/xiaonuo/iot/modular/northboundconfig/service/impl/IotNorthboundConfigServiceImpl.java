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
package vip.xiaonuo.iot.modular.northboundconfig.service.impl;

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
import vip.xiaonuo.iot.modular.northboundconfig.entity.IotNorthboundConfig;
import vip.xiaonuo.iot.modular.northboundconfig.mapper.IotNorthboundConfigMapper;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigAddParam;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigEditParam;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigIdParam;
import vip.xiaonuo.iot.modular.northboundconfig.param.IotNorthboundConfigPageParam;
import vip.xiaonuo.iot.modular.northboundconfig.service.IotNorthboundConfigService;
import vip.xiaonuo.iot.modular.northbound.handler.MqttPushHandler;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送配置表Service接口实现类
 *
 * @author jetox
 * @date  2026/01/08 10:20
 **/
@Service
public class IotNorthboundConfigServiceImpl extends ServiceImpl<IotNorthboundConfigMapper, IotNorthboundConfig> implements IotNorthboundConfigService {

    @Resource
    private MqttPushHandler mqttPushHandler;

    @Override
    public Page<IotNorthboundConfig> page(IotNorthboundConfigPageParam iotNorthboundConfigPageParam) {
        QueryWrapper<IotNorthboundConfig> queryWrapper = new QueryWrapper<IotNorthboundConfig>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotNorthboundConfigPageParam.getName())) {
            queryWrapper.lambda().like(IotNorthboundConfig::getName, iotNorthboundConfigPageParam.getName());
        }
        if(ObjectUtil.isAllNotEmpty(iotNorthboundConfigPageParam.getSortField(), iotNorthboundConfigPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotNorthboundConfigPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotNorthboundConfigPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotNorthboundConfigPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotNorthboundConfig::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotNorthboundConfigAddParam iotNorthboundConfigAddParam) {
        IotNorthboundConfig iotNorthboundConfig = BeanUtil.toBean(iotNorthboundConfigAddParam, IotNorthboundConfig.class);
        this.save(iotNorthboundConfig);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotNorthboundConfigEditParam iotNorthboundConfigEditParam) {
        IotNorthboundConfig iotNorthboundConfig = this.queryEntity(iotNorthboundConfigEditParam.getId());
        
        // 如果是MQTT类型且配置发生变化，断开旧连接
        if ("MQTT".equals(iotNorthboundConfig.getPushType())) {
            mqttPushHandler.disconnect(iotNorthboundConfig.getId());
        }
        
        BeanUtil.copyProperties(iotNorthboundConfigEditParam, iotNorthboundConfig);
        this.updateById(iotNorthboundConfig);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotNorthboundConfigIdParam> iotNorthboundConfigIdParamList) {
        // 删除前清理MQTT连接
        for (IotNorthboundConfigIdParam param : iotNorthboundConfigIdParamList) {
            IotNorthboundConfig config = this.getById(param.getId());
            if (config != null && "MQTT".equals(config.getPushType())) {
                mqttPushHandler.disconnect(config.getId());
            }
        }
        
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotNorthboundConfigIdParamList, IotNorthboundConfigIdParam::getId));
    }

    @Override
    public IotNorthboundConfig detail(IotNorthboundConfigIdParam iotNorthboundConfigIdParam) {
        return this.queryEntity(iotNorthboundConfigIdParam.getId());
    }

    @Override
    public IotNorthboundConfig queryEntity(String id) {
        IotNorthboundConfig iotNorthboundConfig = this.getById(id);
        if(ObjectUtil.isEmpty(iotNorthboundConfig)) {
            throw new CommonException("北向推送配置表不存在，id值为：{}", id);
        }
        return iotNorthboundConfig;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundConfigEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "北向推送配置表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundConfigEditParam.class).sheet("北向推送配置表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送配置表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "北向推送配置表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotNorthboundConfigImportTemplate.xlsx"));
            // 读取excel
            List<IotNorthboundConfigEditParam> iotNorthboundConfigEditParamList =  EasyExcel.read(tempFile).head(IotNorthboundConfigEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotNorthboundConfig> allDataList = this.list();
            for (int i = 0; i < iotNorthboundConfigEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotNorthboundConfigEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotNorthboundConfigEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 北向推送配置表导入失败：", e);
            throw new CommonException("北向推送配置表导入失败");
        }
    }

    public JSONObject doImport(List<IotNorthboundConfig> allDataList, IotNorthboundConfigEditParam iotNorthboundConfigEditParam, int i) {
        String id = iotNorthboundConfigEditParam.getId();
        if(ObjectUtil.hasEmpty(id)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotNorthboundConfig::getId).indexOf(iotNorthboundConfigEditParam.getId());
                IotNorthboundConfig iotNorthboundConfig;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotNorthboundConfig = new IotNorthboundConfig();
                } else {
                    iotNorthboundConfig = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotNorthboundConfigEditParam, iotNorthboundConfig);
                if(isAdd) {
                    allDataList.add(iotNorthboundConfig);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotNorthboundConfig);
                }
                this.saveOrUpdate(iotNorthboundConfig);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotNorthboundConfigIdParam> iotNorthboundConfigIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundConfigEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotNorthboundConfigIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotNorthboundConfigIdParamList, IotNorthboundConfigIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotNorthboundConfigEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotNorthboundConfigEditParam.class);
         }
         String fileName = "北向推送配置表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundConfigEditParam.class).sheet("北向推送配置表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送配置表导出失败：", e);
         CommonResponseUtil.renderError(response, "北向推送配置表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
