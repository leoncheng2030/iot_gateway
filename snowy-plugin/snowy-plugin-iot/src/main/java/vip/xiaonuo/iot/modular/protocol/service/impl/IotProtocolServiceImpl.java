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
package vip.xiaonuo.iot.modular.protocol.service.impl;

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
import vip.xiaonuo.iot.modular.protocol.entity.IotProtocol;
import vip.xiaonuo.iot.modular.protocol.mapper.IotProtocolMapper;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolAddParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolEditParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolIdParam;
import vip.xiaonuo.iot.modular.protocol.param.IotProtocolPageParam;
import vip.xiaonuo.iot.modular.protocol.service.IotProtocolService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 协议配置Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:09
 **/
@Service
public class IotProtocolServiceImpl extends ServiceImpl<IotProtocolMapper, IotProtocol> implements IotProtocolService {

    @Override
    public Page<IotProtocol> page(IotProtocolPageParam iotProtocolPageParam) {
        QueryWrapper<IotProtocol> queryWrapper = new QueryWrapper<IotProtocol>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotProtocolPageParam.getProtocolName())) {
            queryWrapper.lambda().like(IotProtocol::getProtocolName, iotProtocolPageParam.getProtocolName());
        }
        if(ObjectUtil.isNotEmpty(iotProtocolPageParam.getProtocolType())) {
            queryWrapper.lambda().eq(IotProtocol::getProtocolType, iotProtocolPageParam.getProtocolType());
        }
        if(ObjectUtil.isNotEmpty(iotProtocolPageParam.getStatus())) {
            queryWrapper.lambda().eq(IotProtocol::getStatus, iotProtocolPageParam.getStatus());
        }
        if(ObjectUtil.isAllNotEmpty(iotProtocolPageParam.getSortField(), iotProtocolPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotProtocolPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotProtocolPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotProtocolPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotProtocol::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotProtocolAddParam iotProtocolAddParam) {
        IotProtocol iotProtocol = BeanUtil.toBean(iotProtocolAddParam, IotProtocol.class);
        this.save(iotProtocol);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotProtocolEditParam iotProtocolEditParam) {
        IotProtocol iotProtocol = this.queryEntity(iotProtocolEditParam.getId());
        BeanUtil.copyProperties(iotProtocolEditParam, iotProtocol);
        this.updateById(iotProtocol);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotProtocolIdParam> iotProtocolIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotProtocolIdParamList, IotProtocolIdParam::getId));
    }

    @Override
    public IotProtocol detail(IotProtocolIdParam iotProtocolIdParam) {
        return this.queryEntity(iotProtocolIdParam.getId());
    }

    @Override
    public IotProtocol queryEntity(String id) {
        IotProtocol iotProtocol = this.getById(id);
        if(ObjectUtil.isEmpty(iotProtocol)) {
            throw new CommonException("协议配置不存在，id值为：{}", id);
        }
        return iotProtocol;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotProtocolEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "协议配置导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotProtocolEditParam.class).sheet("协议配置").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 协议配置导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "协议配置导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotProtocolImportTemplate.xlsx"));
            // 读取excel
            List<IotProtocolEditParam> iotProtocolEditParamList =  EasyExcel.read(tempFile).head(IotProtocolEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotProtocol> allDataList = this.list();
            for (int i = 0; i < iotProtocolEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotProtocolEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotProtocolEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 协议配置导入失败：", e);
            throw new CommonException("协议配置导入失败");
        }
    }

    public JSONObject doImport(List<IotProtocol> allDataList, IotProtocolEditParam iotProtocolEditParam, int i) {
        String id = iotProtocolEditParam.getId();
        String protocolName = iotProtocolEditParam.getProtocolName();
        String protocolType = iotProtocolEditParam.getProtocolType();
        Integer protocolPort = iotProtocolEditParam.getProtocolPort();
        String configJson = iotProtocolEditParam.getConfigJson();
        String status = iotProtocolEditParam.getStatus();
        if(ObjectUtil.hasEmpty(id, protocolName, protocolType, protocolPort, configJson, status)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotProtocol::getId).indexOf(iotProtocolEditParam.getId());
                IotProtocol iotProtocol;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotProtocol = new IotProtocol();
                } else {
                    iotProtocol = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotProtocolEditParam, iotProtocol);
                if(isAdd) {
                    allDataList.add(iotProtocol);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotProtocol);
                }
                this.saveOrUpdate(iotProtocol);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotProtocolIdParam> iotProtocolIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotProtocolEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotProtocolIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotProtocolIdParamList, IotProtocolIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotProtocolEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotProtocolEditParam.class);
         }
         String fileName = "协议配置_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotProtocolEditParam.class).sheet("协议配置").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 协议配置导出失败：", e);
         CommonResponseUtil.renderError(response, "协议配置导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
