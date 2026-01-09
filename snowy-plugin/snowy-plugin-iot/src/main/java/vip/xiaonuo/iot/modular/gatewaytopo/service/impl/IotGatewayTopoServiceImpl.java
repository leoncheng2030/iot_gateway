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
package vip.xiaonuo.iot.modular.gatewaytopo.service.impl;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import java.math.BigDecimal;
import java.util.Date;
import vip.xiaonuo.iot.modular.gatewaytopo.entity.IotGatewayTopo;
import vip.xiaonuo.iot.modular.gatewaytopo.mapper.IotGatewayTopoMapper;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoAddParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoEditParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoIdParam;
import vip.xiaonuo.iot.modular.gatewaytopo.param.IotGatewayTopoPageParam;
import vip.xiaonuo.iot.modular.gatewaytopo.service.IotGatewayTopoService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 网关拓扑Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:29
 **/
@Service
@Slf4j
public class IotGatewayTopoServiceImpl extends ServiceImpl<IotGatewayTopoMapper, IotGatewayTopo> implements IotGatewayTopoService {

    @Override
    public Page<IotGatewayTopo> page(IotGatewayTopoPageParam iotGatewayTopoPageParam) {
        QueryWrapper<IotGatewayTopo> queryWrapper = new QueryWrapper<IotGatewayTopo>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotGatewayTopoPageParam.getGatewayId())) {
            queryWrapper.lambda().eq(IotGatewayTopo::getGatewayId, iotGatewayTopoPageParam.getGatewayId());
        }
        if(ObjectUtil.isNotEmpty(iotGatewayTopoPageParam.getSubDeviceId())) {
            queryWrapper.lambda().eq(IotGatewayTopo::getSubDeviceId, iotGatewayTopoPageParam.getSubDeviceId());
        }
        if(ObjectUtil.isAllNotEmpty(iotGatewayTopoPageParam.getSortField(), iotGatewayTopoPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotGatewayTopoPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotGatewayTopoPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotGatewayTopoPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotGatewayTopo::getId);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotGatewayTopoAddParam iotGatewayTopoAddParam) {
        IotGatewayTopo iotGatewayTopo = BeanUtil.toBean(iotGatewayTopoAddParam, IotGatewayTopo.class);
        this.save(iotGatewayTopo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotGatewayTopoEditParam iotGatewayTopoEditParam) {
        IotGatewayTopo iotGatewayTopo = this.queryEntity(iotGatewayTopoEditParam.getId());
        BeanUtil.copyProperties(iotGatewayTopoEditParam, iotGatewayTopo);
        this.updateById(iotGatewayTopo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotGatewayTopoIdParam> iotGatewayTopoIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotGatewayTopoIdParamList, IotGatewayTopoIdParam::getId));
    }

    @Override
    public IotGatewayTopo detail(IotGatewayTopoIdParam iotGatewayTopoIdParam) {
        return this.queryEntity(iotGatewayTopoIdParam.getId());
    }

    @Override
    public IotGatewayTopo queryEntity(String id) {
        IotGatewayTopo iotGatewayTopo = this.getById(id);
        if(ObjectUtil.isEmpty(iotGatewayTopo)) {
            throw new CommonException("网关拓扑不存在，id值为：{}", id);
        }
        return iotGatewayTopo;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotGatewayTopoEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "网关拓扑导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotGatewayTopoEditParam.class).sheet("网关拓扑").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 网关拓扑导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "网关拓扑导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotGatewayTopoImportTemplate.xlsx"));
            // 读取excel
            List<IotGatewayTopoEditParam> iotGatewayTopoEditParamList =  EasyExcel.read(tempFile).head(IotGatewayTopoEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotGatewayTopo> allDataList = this.list();
            for (int i = 0; i < iotGatewayTopoEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotGatewayTopoEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotGatewayTopoEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 网关拓扑导入失败：", e);
            throw new CommonException("网关拓扑导入失败");
        }
    }

    public JSONObject doImport(List<IotGatewayTopo> allDataList, IotGatewayTopoEditParam iotGatewayTopoEditParam, int i) {
        String id = iotGatewayTopoEditParam.getId();
        String gatewayId = iotGatewayTopoEditParam.getGatewayId();
        String subDeviceId = iotGatewayTopoEditParam.getSubDeviceId();
        if(ObjectUtil.hasEmpty(id, gatewayId, subDeviceId)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotGatewayTopo::getId).indexOf(iotGatewayTopoEditParam.getId());
                IotGatewayTopo iotGatewayTopo;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotGatewayTopo = new IotGatewayTopo();
                } else {
                    iotGatewayTopo = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotGatewayTopoEditParam, iotGatewayTopo);
                if(isAdd) {
                    allDataList.add(iotGatewayTopo);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotGatewayTopo);
                }
                this.saveOrUpdate(iotGatewayTopo);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotGatewayTopoIdParam> iotGatewayTopoIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotGatewayTopoEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotGatewayTopoIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotGatewayTopoIdParamList, IotGatewayTopoIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotGatewayTopoEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotGatewayTopoEditParam.class);
         }
         String fileName = "网关拓扑_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotGatewayTopoEditParam.class).sheet("网关拓扑").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 网关拓扑导出失败：", e);
         CommonResponseUtil.renderError(response, "网关拓扑导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bindSubDevice(String gatewayId, String subDeviceId) {
        // 检查是否已经绑定
        LambdaQueryWrapper<IotGatewayTopo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IotGatewayTopo::getGatewayId, gatewayId)
                .eq(IotGatewayTopo::getSubDeviceId, subDeviceId);
        IotGatewayTopo existTopo = this.getOne(queryWrapper);
        
        if (ObjectUtil.isNotNull(existTopo)) {
            log.warn("子设备已经绑定到该网关 - GatewayId: {}, SubDeviceId: {}", gatewayId, subDeviceId);
            return;
        }

        // 创建拓扑关系
        IotGatewayTopo topo = new IotGatewayTopo();
        topo.setGatewayId(gatewayId);
        topo.setSubDeviceId(subDeviceId);
        topo.setBindTime(new Date());
        this.save(topo);
        
        log.info("绑定子设备成功 - GatewayId: {}, SubDeviceId: {}", gatewayId, subDeviceId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unbindSubDevice(String gatewayId, String subDeviceId) {
        LambdaQueryWrapper<IotGatewayTopo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IotGatewayTopo::getGatewayId, gatewayId)
                .eq(IotGatewayTopo::getSubDeviceId, subDeviceId);
        
        this.remove(queryWrapper);
        log.info("解绑子设备成功 - GatewayId: {}, SubDeviceId: {}", gatewayId, subDeviceId);
    }

    @Override
    public boolean checkTopoRelation(String gatewayId, String subDeviceId) {
        LambdaQueryWrapper<IotGatewayTopo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IotGatewayTopo::getGatewayId, gatewayId)
                .eq(IotGatewayTopo::getSubDeviceId, subDeviceId);
        
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<IotGatewayTopo> getSubDevicesByGatewayId(String gatewayId) {
        LambdaQueryWrapper<IotGatewayTopo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IotGatewayTopo::getGatewayId, gatewayId);
        
        return this.list(queryWrapper);
    }
}
