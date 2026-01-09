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
package vip.xiaonuo.iot.modular.devicegroup.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
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
import vip.xiaonuo.iot.modular.devicegroup.entity.IotDeviceGroup;
import vip.xiaonuo.iot.modular.devicegroup.mapper.IotDeviceGroupMapper;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupAddParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupEditParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupIdParam;
import vip.xiaonuo.iot.modular.devicegroup.param.IotDeviceGroupPageParam;
import vip.xiaonuo.iot.modular.devicegroup.service.IotDeviceGroupService;
import vip.xiaonuo.iot.modular.devicegrouprel.entity.IotDeviceGroupRel;
import vip.xiaonuo.iot.modular.devicegrouprel.service.IotDeviceGroupRelService;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;

import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备分组表Service接口实现类
 *
 * @author jetox
 * @date  2025/12/13 18:30
 **/
@Service
public class IotDeviceGroupServiceImpl extends ServiceImpl<IotDeviceGroupMapper, IotDeviceGroup> implements IotDeviceGroupService {

    @Resource
    private IotDeviceGroupRelService iotDeviceGroupRelService;

    @Resource
    private IotDeviceService iotDeviceService;

    @Override
    public Page<IotDeviceGroup> page(IotDeviceGroupPageParam iotDeviceGroupPageParam) {
        QueryWrapper<IotDeviceGroup> queryWrapper = new QueryWrapper<IotDeviceGroup>().checkSqlInjection();
        if(ObjectUtil.isAllNotEmpty(iotDeviceGroupPageParam.getSortField(), iotDeviceGroupPageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDeviceGroupPageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDeviceGroupPageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDeviceGroupPageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotDeviceGroup::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Override
    public List<Tree<String>> tree() {
        List<IotDeviceGroup> deviceGroupList = this.list(new LambdaQueryWrapper<IotDeviceGroup>()
                .orderByAsc(IotDeviceGroup::getSortCode));
        List<TreeNode<String>> treeNodeList = deviceGroupList.stream().map(group ->
                new TreeNode<>(group.getId(), group.getParentId(),
                        group.getGroupName(), group.getSortCode()).setExtra(JSONUtil.parseObj(group))
        ).collect(Collectors.toList());
        return TreeUtil.build(treeNodeList, "0");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(IotDeviceGroupAddParam iotDeviceGroupAddParam) {
        IotDeviceGroup iotDeviceGroup = BeanUtil.toBean(iotDeviceGroupAddParam, IotDeviceGroup.class);
        // 计算path和level
        calculatePathAndLevel(iotDeviceGroup);
        this.save(iotDeviceGroup);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDeviceGroupEditParam iotDeviceGroupEditParam) {
        IotDeviceGroup iotDeviceGroup = this.queryEntity(iotDeviceGroupEditParam.getId());
        BeanUtil.copyProperties(iotDeviceGroupEditParam, iotDeviceGroup);
        // 重新计算path和level
        calculatePathAndLevel(iotDeviceGroup);
        this.updateById(iotDeviceGroup);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDeviceGroupIdParam> iotDeviceGroupIdParamList) {
        List<String> groupIdList = CollStreamUtil.toList(iotDeviceGroupIdParamList, IotDeviceGroupIdParam::getId);
        if(ObjectUtil.isNotEmpty(groupIdList)) {
            List<IotDeviceGroup> allGroupList = this.list();
            // 获取所有子分组
            List<String> toDeleteGroupIdList = CollectionUtil.newArrayList();
            groupIdList.forEach(groupId -> {
                toDeleteGroupIdList.add(groupId);
                // 添加所有子分组
                toDeleteGroupIdList.addAll(getChildGroupIds(allGroupList, groupId));
            });
            
            // 执行删除
            this.removeByIds(toDeleteGroupIdList);
        }
    }

    /**
     * 计算分组的路径和层级
     */
    private void calculatePathAndLevel(IotDeviceGroup deviceGroup) {
        String parentId = deviceGroup.getParentId();
        if (ObjectUtil.isEmpty(parentId) || "0".equals(parentId)) {
            // 顶级分组
            deviceGroup.setPath("/" + deviceGroup.getId());
            deviceGroup.setLevel(1);
        } else {
            // 子分组
            IotDeviceGroup parentGroup = this.getById(parentId);
            if (ObjectUtil.isEmpty(parentGroup)) {
                throw new CommonException("父分组不存在，ID为：{}", parentId);
            }
            deviceGroup.setPath(parentGroup.getPath() + "/" + deviceGroup.getId());
            deviceGroup.setLevel(parentGroup.getLevel() + 1);
        }
    }

    /**
     * 获取所有子分组ID
     */
    private List<String> getChildGroupIds(List<IotDeviceGroup> allGroups, String parentId) {
        List<String> childIds = CollectionUtil.newArrayList();
        for (IotDeviceGroup group : allGroups) {
            if (parentId.equals(group.getParentId())) {
                childIds.add(group.getId());
                // 递归获取子分组
                childIds.addAll(getChildGroupIds(allGroups, group.getId()));
            }
        }
        return childIds;
    }

    @Override
    public IotDeviceGroup detail(IotDeviceGroupIdParam iotDeviceGroupIdParam) {
        return this.queryEntity(iotDeviceGroupIdParam.getId());
    }

    @Override
    public IotDeviceGroup queryEntity(String id) {
        IotDeviceGroup iotDeviceGroup = this.getById(id);
        if(ObjectUtil.isEmpty(iotDeviceGroup)) {
            throw new CommonException("设备分组表不存在，id值为：{}", id);
        }
        return iotDeviceGroup;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceGroupEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "设备分组表导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceGroupEditParam.class).sheet("设备分组表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备分组表导入模板下载失败：", e);
         CommonResponseUtil.renderError(response, "设备分组表导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotDeviceGroupImportTemplate.xlsx"));
            // 读取excel
            List<IotDeviceGroupEditParam> iotDeviceGroupEditParamList =  EasyExcel.read(tempFile).head(IotDeviceGroupEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDeviceGroup> allDataList = this.list();
            for (int i = 0; i < iotDeviceGroupEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDeviceGroupEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDeviceGroupEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            log.error(">>> 设备分组表导入失败：", e);
            throw new CommonException("设备分组表导入失败");
        }
    }

    public JSONObject doImport(List<IotDeviceGroup> allDataList, IotDeviceGroupEditParam iotDeviceGroupEditParam, int i) {
        String id = iotDeviceGroupEditParam.getId();
        String groupName = iotDeviceGroupEditParam.getGroupName();
        String groupType = iotDeviceGroupEditParam.getGroupType();
        String parentId = iotDeviceGroupEditParam.getParentId();
        String path = iotDeviceGroupEditParam.getPath();
        Integer level = iotDeviceGroupEditParam.getLevel();
        if(ObjectUtil.hasEmpty(id, groupName, groupType, parentId, path, level)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDeviceGroup::getId).indexOf(iotDeviceGroupEditParam.getId());
                IotDeviceGroup iotDeviceGroup;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDeviceGroup = new IotDeviceGroup();
                } else {
                    iotDeviceGroup = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDeviceGroupEditParam, iotDeviceGroup);
                if(isAdd) {
                    allDataList.add(iotDeviceGroup);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDeviceGroup);
                }
                this.saveOrUpdate(iotDeviceGroup);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              log.error(">>> 数据导入异常：", e);
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDeviceGroupIdParam> iotDeviceGroupIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceGroupEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDeviceGroupIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDeviceGroupIdParamList, IotDeviceGroupIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDeviceGroupEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDeviceGroupEditParam.class);
         }
         String fileName = "设备分组表_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceGroupEditParam.class).sheet("设备分组表").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         log.error(">>> 设备分组表导出失败：", e);
         CommonResponseUtil.renderError(response, "设备分组表导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchRelateDevices(String groupId, List<String> deviceIds) {
        if (ObjectUtil.isEmpty(deviceIds)) {
            return;
        }
        // 验证分组是否存在
        IotDeviceGroup deviceGroup = this.getById(groupId);
        if (ObjectUtil.isEmpty(deviceGroup)) {
            throw new CommonException("设备分组不存在，ID为：{}", groupId);
        }
        // 批量创建关联关系
        List<IotDeviceGroupRel> relList = CollectionUtil.newArrayList();
        deviceIds.forEach(deviceId -> {
            // 检查是否已存在关联
            long count = iotDeviceGroupRelService.count(new LambdaQueryWrapper<IotDeviceGroupRel>()
                    .eq(IotDeviceGroupRel::getGroupId, groupId)
                    .eq(IotDeviceGroupRel::getDeviceId, deviceId));
            if (count == 0) {
                IotDeviceGroupRel rel = new IotDeviceGroupRel();
                rel.setGroupId(groupId);
                rel.setDeviceId(deviceId);
                relList.add(rel);
            }
        });
        if (CollectionUtil.isNotEmpty(relList)) {
            iotDeviceGroupRelService.saveBatch(relList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchRemoveDevices(String groupId, List<String> deviceIds) {
        if (ObjectUtil.isEmpty(deviceIds)) {
            return;
        }
        // 删除关联关系
        iotDeviceGroupRelService.remove(new LambdaQueryWrapper<IotDeviceGroupRel>()
                .eq(IotDeviceGroupRel::getGroupId, groupId)
                .in(IotDeviceGroupRel::getDeviceId, deviceIds));
    }

    @Override
    public List<String> getDeviceIdsByGroupId(String groupId) {
        List<IotDeviceGroupRel> relList = iotDeviceGroupRelService.list(
                new LambdaQueryWrapper<IotDeviceGroupRel>()
                        .eq(IotDeviceGroupRel::getGroupId, groupId));
        return CollStreamUtil.toList(relList, IotDeviceGroupRel::getDeviceId);
    }

    @Override
    public List<String> getGroupIdsByDeviceId(String deviceId) {
        if (ObjectUtil.isEmpty(deviceId)) {
            return CollectionUtil.newArrayList();
        }
        List<IotDeviceGroupRel> relList = iotDeviceGroupRelService.list(
                new LambdaQueryWrapper<IotDeviceGroupRel>()
                        .eq(IotDeviceGroupRel::getDeviceId, deviceId));
        return CollStreamUtil.toList(relList, IotDeviceGroupRel::getGroupId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void syncDeviceGroups(String deviceId, List<String> groupIds) {
        if (ObjectUtil.isEmpty(deviceId)) {
            throw new CommonException("设备ID不能为空");
        }
        // 先删除该设备的所有分组关联
        iotDeviceGroupRelService.remove(new LambdaQueryWrapper<IotDeviceGroupRel>()
                .eq(IotDeviceGroupRel::getDeviceId, deviceId));
        
        // 如果新分组列表不为空，创建新的关联
        if (ObjectUtil.isNotEmpty(groupIds)) {
            List<IotDeviceGroupRel> relList = CollectionUtil.newArrayList();
            groupIds.forEach(groupId -> {
                // 验证分组是否存在
                IotDeviceGroup deviceGroup = this.getById(groupId);
                if (ObjectUtil.isNotEmpty(deviceGroup)) {
                    IotDeviceGroupRel rel = new IotDeviceGroupRel();
                    rel.setGroupId(groupId);
                    rel.setDeviceId(deviceId);
                    relList.add(rel);
                }
            });
            if (CollectionUtil.isNotEmpty(relList)) {
                iotDeviceGroupRelService.saveBatch(relList);
            }
        }
    }

    @Override
    public List<String> getGroupAndChildIds(String groupId) {
        if (ObjectUtil.isEmpty(groupId)) {
            return CollectionUtil.newArrayList();
        }
        // 获取所有分组
        List<IotDeviceGroup> allGroups = this.list();
        // 结果集合，包含自身
        List<String> result = CollectionUtil.newArrayList();
        result.add(groupId);
        // 递归获取所有子分组ID
        result.addAll(getChildGroupIds(allGroups, groupId));
        return result;
    }

    @Override
    public Page<IotDevice> getRelatedDevices(String groupId, String searchKey, Integer pageSize) {
        // 获取分组下的设备ID列表
        List<String> deviceIds = getDeviceIdsByGroupId(groupId);
        
        if (CollectionUtil.isEmpty(deviceIds)) {
            // 如果分组下没有设备，返回空页面
            return new Page<>(1, pageSize != null ? pageSize : 1000);
        }
        
        // 构建查询条件
        LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(IotDevice::getId, deviceIds);
        
        // 搜索条件
        if (ObjectUtil.isNotEmpty(searchKey)) {
            queryWrapper.and(wrapper -> wrapper
                .like(IotDevice::getDeviceName, searchKey)
                .or()
                .like(IotDevice::getDeviceKey, searchKey)
            );
        }
        
        queryWrapper.orderByDesc(IotDevice::getCreateTime);
        
        // 分页查询
        Page<IotDevice> page = new Page<>(1, pageSize != null ? pageSize : 1000);
        return iotDeviceService.page(page, queryWrapper);
    }

    @Override
    public Page<IotDevice> getUnrelatedDevices(String searchKey, Integer pageSize) {
        // 获取所有已关联的设备ID
        List<String> relatedDeviceIds = iotDeviceGroupRelService.list()
            .stream()
            .map(IotDeviceGroupRel::getDeviceId)
            .distinct()
            .collect(Collectors.toList());
        
        // 构建查询条件
        LambdaQueryWrapper<IotDevice> queryWrapper = new LambdaQueryWrapper<>();
        
        // 排除已关联的设备
        if (CollectionUtil.isNotEmpty(relatedDeviceIds)) {
            queryWrapper.notIn(IotDevice::getId, relatedDeviceIds);
        }
        
        // 搜索条件
        if (ObjectUtil.isNotEmpty(searchKey)) {
            queryWrapper.and(wrapper -> wrapper
                .like(IotDevice::getDeviceName, searchKey)
                .or()
                .like(IotDevice::getDeviceKey, searchKey)
            );
        }
        
        queryWrapper.orderByDesc(IotDevice::getCreateTime);
        
        // 分页查询
        Page<IotDevice> page = new Page<>(1, pageSize != null ? pageSize : 1000);
        return iotDeviceService.page(page, queryWrapper);
    }
}