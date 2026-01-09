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
package vip.xiaonuo.iot.modular.thingmodel.service.impl;

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
import vip.xiaonuo.iot.modular.thingmodel.entity.IotThingModel;
import vip.xiaonuo.iot.modular.thingmodel.mapper.IotThingModelMapper;
import vip.xiaonuo.iot.modular.thingmodel.param.*;
import vip.xiaonuo.iot.modular.thingmodel.service.IotThingModelService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 物模型Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 09:08
 **/
@Service
public class IotThingModelServiceImpl extends ServiceImpl<IotThingModelMapper, IotThingModel> implements IotThingModelService {

    @Override
    public Page<IotThingModel> page(IotThingModelPageParam iotThingModelPageParam) {
        QueryWrapper<IotThingModel> queryWrapper = new QueryWrapper<IotThingModel>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotThingModelPageParam.getProductId())) {
            queryWrapper.lambda().eq(IotThingModel::getProductId, iotThingModelPageParam.getProductId());
        }
        if(ObjectUtil.isNotEmpty(iotThingModelPageParam.getModelType())) {
            queryWrapper.lambda().eq(IotThingModel::getModelType, iotThingModelPageParam.getModelType());
        }
        if(ObjectUtil.isNotEmpty(iotThingModelPageParam.getAccessMode())) {
            queryWrapper.lambda().eq(IotThingModel::getAccessMode, iotThingModelPageParam.getAccessMode());
        }
        if(ObjectUtil.isNotEmpty(iotThingModelPageParam.getRequired())) {
            queryWrapper.lambda().eq(IotThingModel::getRequired, iotThingModelPageParam.getRequired());
        }
        if(ObjectUtil.isAllNotEmpty(iotThingModelPageParam.getSortField(), iotThingModelPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotThingModelPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotThingModelPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotThingModelPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotThingModel::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotThingModelAddParam iotThingModelAddParam) {
        IotThingModel iotThingModel = BeanUtil.toBean(iotThingModelAddParam, IotThingModel.class);
        this.save(iotThingModel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotThingModelEditParam iotThingModelEditParam) {
        IotThingModel iotThingModel = this.queryEntity(iotThingModelEditParam.getId());
        BeanUtil.copyProperties(iotThingModelEditParam, iotThingModel);
        this.updateById(iotThingModel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotThingModelIdParam> iotThingModelIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotThingModelIdParamList, IotThingModelIdParam::getId));
    }

    @Override
    public IotThingModel detail(IotThingModelIdParam iotThingModelIdParam) {
        return this.queryEntity(iotThingModelIdParam.getId());
    }

    @Override
    public IotThingModel queryEntity(String id) {
        IotThingModel iotThingModel = this.getById(id);
        if(ObjectUtil.isEmpty(iotThingModel)) {
            throw new CommonException("物模型不存在，id值为：{}", id);
        }
        return iotThingModel;
    }

    @Override
    public List<IotThingModel> listByProduct(IotThingModelListParam iotThingModelListParam) {
        LambdaQueryWrapper<IotThingModel> queryWrapper = new LambdaQueryWrapper<>();
        // 按产品ID查询
        if(ObjectUtil.isNotEmpty(iotThingModelListParam.getProductId())) {
            queryWrapper.eq(IotThingModel::getProductId, iotThingModelListParam.getProductId());
        }
        // 按功能类型查询
        if(ObjectUtil.isNotEmpty(iotThingModelListParam.getModelType())) {
            queryWrapper.eq(IotThingModel::getModelType, iotThingModelListParam.getModelType());
        }
        // 按排序码排序
        queryWrapper.orderByAsc(IotThingModel::getSortCode);
        return this.list(queryWrapper);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotThingModelEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "物模型导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotThingModelEditParam.class).sheet("物模型").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 物模型导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "物模型导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotThingModelImportTemplate.xlsx"));
            // 读取excel
            List<IotThingModelEditParam> iotThingModelEditParamList =  EasyExcel.read(tempFile).head(IotThingModelEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotThingModel> allDataList = this.list();
            for (int i = 0; i < iotThingModelEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotThingModelEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotThingModelEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 物模型导入失败：", e);
            throw new CommonException("物模型导入失败");
        }
    }

    public JSONObject doImport(List<IotThingModel> allDataList, IotThingModelEditParam iotThingModelEditParam, int i) {
        String id = iotThingModelEditParam.getId();
        String productId = iotThingModelEditParam.getProductId();
        String modelType = iotThingModelEditParam.getModelType();
        if(ObjectUtil.hasEmpty(id, productId, modelType)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotThingModel::getId).indexOf(iotThingModelEditParam.getId());
                IotThingModel iotThingModel;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotThingModel = new IotThingModel();
                } else {
                    iotThingModel = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotThingModelEditParam, iotThingModel);
                if(isAdd) {
                    allDataList.add(iotThingModel);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotThingModel);
                }
                this.saveOrUpdate(iotThingModel);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotThingModelIdParam> iotThingModelIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotThingModelEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotThingModelIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotThingModelIdParamList, IotThingModelIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotThingModelEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotThingModelEditParam.class);
         }
         String fileName = "物模型_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotThingModelEditParam.class).sheet("物模型").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 物模型导出失败：", e);
         CommonResponseUtil.renderError(response, "物模型导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
