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
package vip.xiaonuo.iot.modular.northboundlog.service.impl;

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
import vip.xiaonuo.iot.modular.northboundlog.entity.IotNorthboundLog;
import vip.xiaonuo.iot.modular.northboundlog.mapper.IotNorthboundLogMapper;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogAddParam;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogEditParam;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogIdParam;
import vip.xiaonuo.iot.modular.northboundlog.param.IotNorthboundLogPageParam;
import vip.xiaonuo.iot.modular.northboundlog.service.IotNorthboundLogService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送日志表Service接口实现类
 *
 * @author jetox
 * @date  2026/01/08 10:25
 **/
@Service
public class IotNorthboundLogServiceImpl extends ServiceImpl<IotNorthboundLogMapper, IotNorthboundLog> implements IotNorthboundLogService {

    @Override
    public Page<IotNorthboundLog> page(IotNorthboundLogPageParam iotNorthboundLogPageParam) {
        QueryWrapper<IotNorthboundLog> queryWrapper = new QueryWrapper<IotNorthboundLog>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotNorthboundLogPageParam.getConfigId())) {
            queryWrapper.lambda().like(IotNorthboundLog::getConfigId, iotNorthboundLogPageParam.getConfigId());
        }
        if(ObjectUtil.isNotEmpty(iotNorthboundLogPageParam.getConfigName())) {
            queryWrapper.lambda().like(IotNorthboundLog::getConfigName, iotNorthboundLogPageParam.getConfigName());
        }
        if(ObjectUtil.isAllNotEmpty(iotNorthboundLogPageParam.getSortField(), iotNorthboundLogPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotNorthboundLogPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotNorthboundLogPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotNorthboundLogPageParam.getSortField()));
        } else {
            // 默认按推送时间倒序排列(最新的在前)
            queryWrapper.lambda().orderByDesc(IotNorthboundLog::getPushTime);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotNorthboundLogAddParam iotNorthboundLogAddParam) {
        IotNorthboundLog iotNorthboundLog = BeanUtil.toBean(iotNorthboundLogAddParam, IotNorthboundLog.class);
        this.save(iotNorthboundLog);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotNorthboundLogEditParam iotNorthboundLogEditParam) {
        IotNorthboundLog iotNorthboundLog = this.queryEntity(iotNorthboundLogEditParam.getId());
        BeanUtil.copyProperties(iotNorthboundLogEditParam, iotNorthboundLog);
        this.updateById(iotNorthboundLog);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotNorthboundLogIdParam> iotNorthboundLogIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotNorthboundLogIdParamList, IotNorthboundLogIdParam::getId));
    }

    @Override
    public IotNorthboundLog detail(IotNorthboundLogIdParam iotNorthboundLogIdParam) {
        return this.queryEntity(iotNorthboundLogIdParam.getId());
    }

    @Override
    public IotNorthboundLog queryEntity(String id) {
        IotNorthboundLog iotNorthboundLog = this.getById(id);
        if(ObjectUtil.isEmpty(iotNorthboundLog)) {
            throw new CommonException("北向推送日志表不存在，id值为：{}", id);
        }
        return iotNorthboundLog;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundLogEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "北向推送日志表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundLogEditParam.class).sheet("北向推送日志表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送日志表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "北向推送日志表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotNorthboundLogImportTemplate.xlsx"));
            // 读取excel
            List<IotNorthboundLogEditParam> iotNorthboundLogEditParamList =  EasyExcel.read(tempFile).head(IotNorthboundLogEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotNorthboundLog> allDataList = this.list();
            for (int i = 0; i < iotNorthboundLogEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotNorthboundLogEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotNorthboundLogEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 北向推送日志表导入失败：", e);
            throw new CommonException("北向推送日志表导入失败");
        }
    }

    public JSONObject doImport(List<IotNorthboundLog> allDataList, IotNorthboundLogEditParam iotNorthboundLogEditParam, int i) {
        String id = iotNorthboundLogEditParam.getId();
        if(ObjectUtil.hasEmpty(id)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotNorthboundLog::getId).indexOf(iotNorthboundLogEditParam.getId());
                IotNorthboundLog iotNorthboundLog;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotNorthboundLog = new IotNorthboundLog();
                } else {
                    iotNorthboundLog = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotNorthboundLogEditParam, iotNorthboundLog);
                if(isAdd) {
                    allDataList.add(iotNorthboundLog);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotNorthboundLog);
                }
                this.saveOrUpdate(iotNorthboundLog);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotNorthboundLogIdParam> iotNorthboundLogIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundLogEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotNorthboundLogIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotNorthboundLogIdParamList, IotNorthboundLogIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotNorthboundLogEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotNorthboundLogEditParam.class);
         }
         String fileName = "北向推送日志表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundLogEditParam.class).sheet("北向推送日志表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送日志表导出失败：", e);
         CommonResponseUtil.renderError(response, "北向推送日志表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
