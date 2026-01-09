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
package vip.xiaonuo.iot.modular.northboundstatistics.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
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
import vip.xiaonuo.iot.modular.northboundstatistics.entity.IotNorthboundStatistics;
import vip.xiaonuo.iot.modular.northboundstatistics.mapper.IotNorthboundStatisticsMapper;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsAddParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsEditParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsIdParam;
import vip.xiaonuo.iot.modular.northboundstatistics.param.IotNorthboundStatisticsPageParam;
import vip.xiaonuo.iot.modular.northboundstatistics.service.IotNorthboundStatisticsService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 北向推送统计表Service接口实现类
 *
 * @author jetox
 * @date  2026/01/08 10:26
 **/
@Service
public class IotNorthboundStatisticsServiceImpl extends ServiceImpl<IotNorthboundStatisticsMapper, IotNorthboundStatistics> implements IotNorthboundStatisticsService {

    @Override
    public Page<IotNorthboundStatistics> page(IotNorthboundStatisticsPageParam iotNorthboundStatisticsPageParam) {
        QueryWrapper<IotNorthboundStatistics> queryWrapper = new QueryWrapper<IotNorthboundStatistics>().checkSqlInjection();
        // 按配置ID过滤
        if(ObjectUtil.isNotEmpty(iotNorthboundStatisticsPageParam.getConfigId())) {
            queryWrapper.lambda().eq(IotNorthboundStatistics::getConfigId, iotNorthboundStatisticsPageParam.getConfigId());
        }
        // 按统计日期过滤（处理日期字符串与Date类型匹配）
        if(ObjectUtil.isNotEmpty(iotNorthboundStatisticsPageParam.getStatDate())) {
            try {
                // 将日期字符串转换为当天的开始和结束时间
                Date targetDate = DateUtil.parseDate(iotNorthboundStatisticsPageParam.getStatDate());
                Date startOfDay = DateUtil.beginOfDay(targetDate);
                Date endOfDay = DateUtil.endOfDay(targetDate);
                queryWrapper.lambda().between(IotNorthboundStatistics::getStatDate, startOfDay, endOfDay);
            } catch (Exception e) {
                log.warn("日期格式解析失败: " + iotNorthboundStatisticsPageParam.getStatDate());
            }
        }
        if(ObjectUtil.isAllNotEmpty(iotNorthboundStatisticsPageParam.getSortField(), iotNorthboundStatisticsPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotNorthboundStatisticsPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotNorthboundStatisticsPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotNorthboundStatisticsPageParam.getSortField()));
        } else {
            // 默认按统计日期降序
            queryWrapper.lambda().orderByDesc(IotNorthboundStatistics::getStatDate);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotNorthboundStatisticsAddParam iotNorthboundStatisticsAddParam) {
        IotNorthboundStatistics iotNorthboundStatistics = BeanUtil.toBean(iotNorthboundStatisticsAddParam, IotNorthboundStatistics.class);
        this.save(iotNorthboundStatistics);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotNorthboundStatisticsEditParam iotNorthboundStatisticsEditParam) {
        IotNorthboundStatistics iotNorthboundStatistics = this.queryEntity(iotNorthboundStatisticsEditParam.getId());
        BeanUtil.copyProperties(iotNorthboundStatisticsEditParam, iotNorthboundStatistics);
        this.updateById(iotNorthboundStatistics);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotNorthboundStatisticsIdParam> iotNorthboundStatisticsIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotNorthboundStatisticsIdParamList, IotNorthboundStatisticsIdParam::getId));
    }

    @Override
    public IotNorthboundStatistics detail(IotNorthboundStatisticsIdParam iotNorthboundStatisticsIdParam) {
        return this.queryEntity(iotNorthboundStatisticsIdParam.getId());
    }

    @Override
    public IotNorthboundStatistics queryEntity(String id) {
        IotNorthboundStatistics iotNorthboundStatistics = this.getById(id);
        if(ObjectUtil.isEmpty(iotNorthboundStatistics)) {
            throw new CommonException("北向推送统计表不存在，id值为：{}", id);
        }
        return iotNorthboundStatistics;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundStatisticsEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "北向推送统计表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundStatisticsEditParam.class).sheet("北向推送统计表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送统计表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "北向推送统计表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotNorthboundStatisticsImportTemplate.xlsx"));
            // 读取excel
            List<IotNorthboundStatisticsEditParam> iotNorthboundStatisticsEditParamList =  EasyExcel.read(tempFile).head(IotNorthboundStatisticsEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotNorthboundStatistics> allDataList = this.list();
            for (int i = 0; i < iotNorthboundStatisticsEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotNorthboundStatisticsEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotNorthboundStatisticsEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 北向推送统计表导入失败：", e);
            throw new CommonException("北向推送统计表导入失败");
        }
    }

    public JSONObject doImport(List<IotNorthboundStatistics> allDataList, IotNorthboundStatisticsEditParam iotNorthboundStatisticsEditParam, int i) {
        String id = iotNorthboundStatisticsEditParam.getId();
        if(ObjectUtil.hasEmpty(id)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotNorthboundStatistics::getId).indexOf(iotNorthboundStatisticsEditParam.getId());
                IotNorthboundStatistics iotNorthboundStatistics;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotNorthboundStatistics = new IotNorthboundStatistics();
                } else {
                    iotNorthboundStatistics = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotNorthboundStatisticsEditParam, iotNorthboundStatistics);
                if(isAdd) {
                    allDataList.add(iotNorthboundStatistics);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotNorthboundStatistics);
                }
                this.saveOrUpdate(iotNorthboundStatistics);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotNorthboundStatisticsIdParam> iotNorthboundStatisticsIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotNorthboundStatisticsEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotNorthboundStatisticsIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotNorthboundStatisticsIdParamList, IotNorthboundStatisticsIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotNorthboundStatisticsEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotNorthboundStatisticsEditParam.class);
         }
         String fileName = "北向推送统计表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotNorthboundStatisticsEditParam.class).sheet("北向推送统计表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 北向推送统计表导出失败：", e);
         CommonResponseUtil.renderError(response, "北向推送统计表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatistics(String configId, boolean success, int costTime) {
        // 获取今天的日期
        Date today = DateUtil.beginOfDay(new Date());
        
        // 查询今天的统计记录
        LambdaQueryWrapper<IotNorthboundStatistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotNorthboundStatistics::getConfigId, configId);
        wrapper.eq(IotNorthboundStatistics::getStatDate, today);
        wrapper.last("LIMIT 1");
        IotNorthboundStatistics statistics = this.getOne(wrapper);
        
        if (statistics == null) {
            // 创建新记录
            statistics = new IotNorthboundStatistics();
            statistics.setId(IdUtil.getSnowflakeNextIdStr());
            statistics.setConfigId(configId);
            statistics.setStatDate(today);
            statistics.setTotalCount(1);
            statistics.setSuccessCount(success ? 1 : 0);
            statistics.setFailedCount(success ? 0 : 1);
            statistics.setAvgCostTime(costTime);
            statistics.setMaxCostTime(costTime);
            statistics.setCreateTime(new Date());
            this.save(statistics);
        } else {
            // 更新现有记录
            int totalCount = statistics.getTotalCount() + 1;
            int successCount = statistics.getSuccessCount() + (success ? 1 : 0);
            int failedCount = statistics.getFailedCount() + (success ? 0 : 1);
            
            // 计算平均耗时
            int avgCostTime = (statistics.getAvgCostTime() * statistics.getTotalCount() + costTime) / totalCount;
            
            // 更新最大耗时
            int maxCostTime = Math.max(statistics.getMaxCostTime(), costTime);
            
            statistics.setTotalCount(totalCount);
            statistics.setSuccessCount(successCount);
            statistics.setFailedCount(failedCount);
            statistics.setAvgCostTime(avgCostTime);
            statistics.setMaxCostTime(maxCostTime);
            statistics.setUpdateTime(new Date());
            this.updateById(statistics);
        }
    }
}
