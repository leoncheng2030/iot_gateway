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
package vip.xiaonuo.iot.modular.rule.service.impl;

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
import vip.xiaonuo.iot.modular.rule.entity.IotRule;
import vip.xiaonuo.iot.modular.rule.mapper.IotRuleMapper;
import vip.xiaonuo.iot.modular.rule.param.IotRuleAddParam;
import vip.xiaonuo.iot.modular.rule.param.IotRuleEditParam;
import vip.xiaonuo.iot.modular.rule.param.IotRuleIdParam;
import vip.xiaonuo.iot.modular.rule.param.IotRulePageParam;
import vip.xiaonuo.iot.modular.rule.service.IotRuleService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 规则引擎Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:32
 **/
@Service
public class IotRuleServiceImpl extends ServiceImpl<IotRuleMapper, IotRule> implements IotRuleService {

    @Override
    public Page<IotRule> page(IotRulePageParam iotRulePageParam) {
        QueryWrapper<IotRule> queryWrapper = new QueryWrapper<IotRule>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotRulePageParam.getRuleName())) {
            queryWrapper.lambda().like(IotRule::getRuleName, iotRulePageParam.getRuleName());
        }
        if(ObjectUtil.isNotEmpty(iotRulePageParam.getRuleType())) {
            queryWrapper.lambda().eq(IotRule::getRuleType, iotRulePageParam.getRuleType());
        }
        if(ObjectUtil.isNotEmpty(iotRulePageParam.getStatus())) {
            queryWrapper.lambda().eq(IotRule::getStatus, iotRulePageParam.getStatus());
        }
        if(ObjectUtil.isAllNotEmpty(iotRulePageParam.getSortField(), iotRulePageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotRulePageParam.getSortOrder());
            queryWrapper.orderBy(true, iotRulePageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotRulePageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotRule::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotRuleAddParam iotRuleAddParam) {
        IotRule iotRule = BeanUtil.toBean(iotRuleAddParam, IotRule.class);
        this.save(iotRule);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotRuleEditParam iotRuleEditParam) {
        IotRule iotRule = this.queryEntity(iotRuleEditParam.getId());
        BeanUtil.copyProperties(iotRuleEditParam, iotRule);
        this.updateById(iotRule);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotRuleIdParam> iotRuleIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotRuleIdParamList, IotRuleIdParam::getId));
    }

    @Override
    public IotRule detail(IotRuleIdParam iotRuleIdParam) {
        return this.queryEntity(iotRuleIdParam.getId());
    }

    @Override
    public IotRule queryEntity(String id) {
        IotRule iotRule = this.getById(id);
        if(ObjectUtil.isEmpty(iotRule)) {
            throw new CommonException("规则引擎不存在，id值为：{}", id);
        }
        return iotRule;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotRuleEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "规则引擎导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotRuleEditParam.class).sheet("规则引擎").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 规则引擎导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "规则引擎导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotRuleImportTemplate.xlsx"));
            // 读取excel
            List<IotRuleEditParam> iotRuleEditParamList =  EasyExcel.read(tempFile).head(IotRuleEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotRule> allDataList = this.list();
            for (int i = 0; i < iotRuleEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotRuleEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotRuleEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 规则引擎导入失败：", e);
            throw new CommonException("规则引擎导入失败");
        }
    }

    public JSONObject doImport(List<IotRule> allDataList, IotRuleEditParam iotRuleEditParam, int i) {
        String id = iotRuleEditParam.getId();
        String ruleName = iotRuleEditParam.getRuleName();
        String ruleType = iotRuleEditParam.getRuleType();
        String triggerCondition = iotRuleEditParam.getTriggerCondition();
        String actions = iotRuleEditParam.getActions();
        String status = iotRuleEditParam.getStatus();
        if(ObjectUtil.hasEmpty(id, ruleName, ruleType, triggerCondition, actions, status)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotRule::getId).indexOf(iotRuleEditParam.getId());
                IotRule iotRule;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotRule = new IotRule();
                } else {
                    iotRule = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotRuleEditParam, iotRule);
                if(isAdd) {
                    allDataList.add(iotRule);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotRule);
                }
                this.saveOrUpdate(iotRule);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotRuleIdParam> iotRuleIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotRuleEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotRuleIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotRuleIdParamList, IotRuleIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotRuleEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotRuleEditParam.class);
         }
         String fileName = "规则引擎_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotRuleEditParam.class).sheet("规则引擎").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 规则引擎导出失败：", e);
         CommonResponseUtil.renderError(response, "规则引擎导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
