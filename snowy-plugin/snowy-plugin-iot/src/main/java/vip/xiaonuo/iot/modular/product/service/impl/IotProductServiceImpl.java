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
package vip.xiaonuo.iot.modular.product.service.impl;

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
import vip.xiaonuo.iot.modular.product.entity.IotProduct;
import vip.xiaonuo.iot.modular.product.mapper.IotProductMapper;
import vip.xiaonuo.iot.modular.product.param.IotProductAddParam;
import vip.xiaonuo.iot.modular.product.param.IotProductEditParam;
import vip.xiaonuo.iot.modular.product.param.IotProductIdParam;
import vip.xiaonuo.iot.modular.product.param.IotProductPageParam;
import vip.xiaonuo.iot.modular.product.service.IotProductService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 产品Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 06:57
 **/
@Service
public class IotProductServiceImpl extends ServiceImpl<IotProductMapper, IotProduct> implements IotProductService {

    @Override
    public Page<IotProduct> page(IotProductPageParam iotProductPageParam) {
        QueryWrapper<IotProduct> queryWrapper = new QueryWrapper<IotProduct>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotProductPageParam.getProductName())) {
            queryWrapper.lambda().like(IotProduct::getProductName, iotProductPageParam.getProductName());
        }
        if(ObjectUtil.isNotEmpty(iotProductPageParam.getProductKey())) {
            queryWrapper.lambda().like(IotProduct::getProductKey, iotProductPageParam.getProductKey());
        }
        if(ObjectUtil.isNotEmpty(iotProductPageParam.getProductType())) {
            queryWrapper.lambda().eq(IotProduct::getProductType, iotProductPageParam.getProductType());
        }
        if(ObjectUtil.isNotEmpty(iotProductPageParam.getProtocolType())) {
            queryWrapper.lambda().eq(IotProduct::getProtocolType, iotProductPageParam.getProtocolType());
        }
        if(ObjectUtil.isNotEmpty(iotProductPageParam.getDataFormat())) {
            queryWrapper.lambda().eq(IotProduct::getDataFormat, iotProductPageParam.getDataFormat());
        }
        if(ObjectUtil.isNotEmpty(iotProductPageParam.getStatus())) {
            queryWrapper.lambda().eq(IotProduct::getStatus, iotProductPageParam.getStatus());
        }
        if(ObjectUtil.isAllNotEmpty(iotProductPageParam.getSortField(), iotProductPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotProductPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotProductPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotProductPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotProduct::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotProductAddParam iotProductAddParam) {
        IotProduct iotProduct = BeanUtil.toBean(iotProductAddParam, IotProduct.class);
        this.save(iotProduct);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotProductEditParam iotProductEditParam) {
        IotProduct iotProduct = this.queryEntity(iotProductEditParam.getId());
        BeanUtil.copyProperties(iotProductEditParam, iotProduct);
        this.updateById(iotProduct);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotProductIdParam> iotProductIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotProductIdParamList, IotProductIdParam::getId));
    }

    @Override
    public IotProduct detail(IotProductIdParam iotProductIdParam) {
        return this.queryEntity(iotProductIdParam.getId());
    }

    @Override
    public IotProduct queryEntity(String id) {
        IotProduct iotProduct = this.getById(id);
        if(ObjectUtil.isEmpty(iotProduct)) {
            throw new CommonException("产品不存在，id值为：{}", id);
        }
        return iotProduct;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotProductEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "产品导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotProductEditParam.class).sheet("产品").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 产品导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "产品导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotProductImportTemplate.xlsx"));
            // 读取excel
            List<IotProductEditParam> iotProductEditParamList =  EasyExcel.read(tempFile).head(IotProductEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotProduct> allDataList = this.list();
            for (int i = 0; i < iotProductEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotProductEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotProductEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 产品导入失败：", e);
            throw new CommonException("产品导入失败");
        }
    }

    public JSONObject doImport(List<IotProduct> allDataList, IotProductEditParam iotProductEditParam, int i) {
        String id = iotProductEditParam.getId();
        if(ObjectUtil.hasEmpty(id)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotProduct::getId).indexOf(iotProductEditParam.getId());
                IotProduct iotProduct;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotProduct = new IotProduct();
                } else {
                    iotProduct = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotProductEditParam, iotProduct);
                if(isAdd) {
                    allDataList.add(iotProduct);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotProduct);
                }
                this.saveOrUpdate(iotProduct);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotProductIdParam> iotProductIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotProductEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotProductIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotProductIdParamList, IotProductIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotProductEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotProductEditParam.class);
         }
         String fileName = "产品_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotProductEditParam.class).sheet("产品").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 产品导出失败：", e);
         CommonResponseUtil.renderError(response, "产品导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }
}
