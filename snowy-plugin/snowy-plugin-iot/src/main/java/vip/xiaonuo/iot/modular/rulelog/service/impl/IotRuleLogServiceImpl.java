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
package vip.xiaonuo.iot.modular.rulelog.service.impl;

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
import vip.xiaonuo.iot.modular.rulelog.entity.IotRuleLog;
import vip.xiaonuo.iot.modular.rulelog.mapper.IotRuleLogMapper;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogAddParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogEditParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogIdParam;
import vip.xiaonuo.iot.modular.rulelog.param.IotRuleLogPageParam;
import vip.xiaonuo.iot.modular.rulelog.service.IotRuleLogService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 规则执行日志表Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:39
 **/
@Service
public class IotRuleLogServiceImpl extends ServiceImpl<IotRuleLogMapper, IotRuleLog> implements IotRuleLogService {

    @Override
    public Page<IotRuleLog> page(IotRuleLogPageParam iotRuleLogPageParam) {
        QueryWrapper<IotRuleLog> queryWrapper = new QueryWrapper<IotRuleLog>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotRuleLogPageParam.getRuleId())) {
            queryWrapper.lambda().like(IotRuleLog::getRuleId, iotRuleLogPageParam.getRuleId());
        }
        if(ObjectUtil.isNotEmpty(iotRuleLogPageParam.getRuleName())) {
            queryWrapper.lambda().like(IotRuleLog::getRuleName, iotRuleLogPageParam.getRuleName());
        }
        if(ObjectUtil.isNotEmpty(iotRuleLogPageParam.getExecuteResult())) {
            queryWrapper.lambda().eq(IotRuleLog::getExecuteResult, iotRuleLogPageParam.getExecuteResult());
        }
        if(ObjectUtil.isAllNotEmpty(iotRuleLogPageParam.getSortField(), iotRuleLogPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotRuleLogPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotRuleLogPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotRuleLogPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotRuleLog::getId);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotRuleLogAddParam iotRuleLogAddParam) {
        IotRuleLog iotRuleLog = BeanUtil.toBean(iotRuleLogAddParam, IotRuleLog.class);
        this.save(iotRuleLog);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotRuleLogEditParam iotRuleLogEditParam) {
        IotRuleLog iotRuleLog = this.queryEntity(iotRuleLogEditParam.getId());
        BeanUtil.copyProperties(iotRuleLogEditParam, iotRuleLog);
        this.updateById(iotRuleLog);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotRuleLogIdParam> iotRuleLogIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotRuleLogIdParamList, IotRuleLogIdParam::getId));
    }

    @Override
    public IotRuleLog detail(IotRuleLogIdParam iotRuleLogIdParam) {
        return this.queryEntity(iotRuleLogIdParam.getId());
    }

    @Override
    public IotRuleLog queryEntity(String id) {
        IotRuleLog iotRuleLog = this.getById(id);
        if(ObjectUtil.isEmpty(iotRuleLog)) {
            throw new CommonException("规则执行日志表不存在，id值为：{}", id);
        }
        return iotRuleLog;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotRuleLogEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "规则执行日志表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotRuleLogEditParam.class).sheet("规则执行日志表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 规则执行日志表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "规则执行日志表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotRuleLogImportTemplate.xlsx"));
            // 读取excel
            List<IotRuleLogEditParam> iotRuleLogEditParamList =  EasyExcel.read(tempFile).head(IotRuleLogEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotRuleLog> allDataList = this.list();
            for (int i = 0; i < iotRuleLogEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotRuleLogEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotRuleLogEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 规则执行日志表导入失败：", e);
            throw new CommonException("规则执行日志表导入失败");
        }
    }

    public JSONObject doImport(List<IotRuleLog> allDataList, IotRuleLogEditParam iotRuleLogEditParam, int i) {
        String id = iotRuleLogEditParam.getId();
        String ruleId = iotRuleLogEditParam.getRuleId();
        String ruleName = iotRuleLogEditParam.getRuleName();
        String triggerData = iotRuleLogEditParam.getTriggerData();
        if(ObjectUtil.hasEmpty(id, ruleId, ruleName, triggerData)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotRuleLog::getId).indexOf(iotRuleLogEditParam.getId());
                IotRuleLog iotRuleLog;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotRuleLog = new IotRuleLog();
                } else {
                    iotRuleLog = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotRuleLogEditParam, iotRuleLog);
                if(isAdd) {
                    allDataList.add(iotRuleLog);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotRuleLog);
                }
                this.saveOrUpdate(iotRuleLog);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotRuleLogIdParam> iotRuleLogIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotRuleLogEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotRuleLogIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotRuleLogIdParamList, IotRuleLogIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotRuleLogEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotRuleLogEditParam.class);
         }
         String fileName = "规则执行日志表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotRuleLogEditParam.class).sheet("规则执行日志表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 规则执行日志表导出失败：", e);
         CommonResponseUtil.renderError(response, "规则执行日志表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
