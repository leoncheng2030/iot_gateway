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
package vip.xiaonuo.iot.modular.driverlog.service.impl;

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
import vip.xiaonuo.iot.modular.driverlog.entity.IotDriverLog;
import vip.xiaonuo.iot.modular.driverlog.mapper.IotDriverLogMapper;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogAddParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogEditParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogIdParam;
import vip.xiaonuo.iot.modular.driverlog.param.IotDriverLogPageParam;
import vip.xiaonuo.iot.modular.driverlog.service.IotDriverLogService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 运行日志Service接口实现类
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
@Service
public class IotDriverLogServiceImpl extends ServiceImpl<IotDriverLogMapper, IotDriverLog> implements IotDriverLogService {

    @Override
    public Page<IotDriverLog> page(IotDriverLogPageParam iotDriverLogPageParam) {
        QueryWrapper<IotDriverLog> queryWrapper = new QueryWrapper<IotDriverLog>().checkSqlInjection();
        if(ObjectUtil.isAllNotEmpty(iotDriverLogPageParam.getSortField(), iotDriverLogPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDriverLogPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDriverLogPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDriverLogPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotDriverLog::getId);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotDriverLogAddParam iotDriverLogAddParam) {
        IotDriverLog iotDriverLog = BeanUtil.toBean(iotDriverLogAddParam, IotDriverLog.class);
        this.save(iotDriverLog);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDriverLogEditParam iotDriverLogEditParam) {
        IotDriverLog iotDriverLog = this.queryEntity(iotDriverLogEditParam.getId());
        BeanUtil.copyProperties(iotDriverLogEditParam, iotDriverLog);
        this.updateById(iotDriverLog);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDriverLogIdParam> iotDriverLogIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotDriverLogIdParamList, IotDriverLogIdParam::getId));
    }

    @Override
    public IotDriverLog detail(IotDriverLogIdParam iotDriverLogIdParam) {
        return this.queryEntity(iotDriverLogIdParam.getId());
    }

    @Override
    public IotDriverLog queryEntity(String id) {
        IotDriverLog iotDriverLog = this.getById(id);
        if(ObjectUtil.isEmpty(iotDriverLog)) {
            throw new CommonException("运行日志不存在，id值为：{}", id);
        }
        return iotDriverLog;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDriverLogEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "运行日志导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDriverLogEditParam.class).sheet("运行日志").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 运行日志导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "运行日志导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotDriverLogImportTemplate.xlsx"));
            // 读取excel
            List<IotDriverLogEditParam> iotDriverLogEditParamList =  EasyExcel.read(tempFile).head(IotDriverLogEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDriverLog> allDataList = this.list();
            for (int i = 0; i < iotDriverLogEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDriverLogEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDriverLogEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 运行日志导入失败：", e);
            throw new CommonException("运行日志导入失败");
        }
    }

    public JSONObject doImport(List<IotDriverLog> allDataList, IotDriverLogEditParam iotDriverLogEditParam, int i) {
        String id = iotDriverLogEditParam.getId();
        if(ObjectUtil.hasEmpty(id)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDriverLog::getId).indexOf(iotDriverLogEditParam.getId());
                IotDriverLog iotDriverLog;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDriverLog = new IotDriverLog();
                } else {
                    iotDriverLog = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDriverLogEditParam, iotDriverLog);
                if(isAdd) {
                    allDataList.add(iotDriverLog);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDriverLog);
                }
                this.saveOrUpdate(iotDriverLog);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDriverLogIdParam> iotDriverLogIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDriverLogEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDriverLogIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDriverLogIdParamList, IotDriverLogIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDriverLogEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDriverLogEditParam.class);
         }
         String fileName = "运行日志_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDriverLogEditParam.class).sheet("运行日志").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 运行日志导出失败：", e);
         CommonResponseUtil.renderError(response, "运行日志导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
