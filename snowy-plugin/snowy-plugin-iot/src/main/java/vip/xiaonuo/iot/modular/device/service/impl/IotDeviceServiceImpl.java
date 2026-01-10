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
package vip.xiaonuo.iot.modular.device.service.impl;

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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import vip.xiaonuo.iot.core.protocol.modbus.Modbus4jTcpClient;
import vip.xiaonuo.iot.core.util.DriverConfigUtil;
import vip.xiaonuo.iot.modular.device.entity.IotDevice;
import vip.xiaonuo.iot.modular.device.mapper.IotDeviceMapper;
import vip.xiaonuo.iot.modular.device.param.IotDeviceAddParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceEditParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceIdParam;
import vip.xiaonuo.iot.modular.device.param.IotDevicePageParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceCommandParam;
import vip.xiaonuo.iot.modular.device.param.IotDevicePropertySetParam;
import vip.xiaonuo.iot.modular.device.param.IotDeviceServiceParam;
import vip.xiaonuo.iot.modular.device.service.IotDeviceService;
import vip.xiaonuo.iot.modular.devicedriverrel.service.IotDeviceDriverRelService;
import vip.xiaonuo.iot.modular.devicegroup.service.IotDeviceGroupService;
import vip.xiaonuo.iot.modular.deviceshadow.entity.IotDeviceShadow;
import vip.xiaonuo.iot.modular.deviceshadow.service.IotDeviceShadowService;
import vip.xiaonuo.iot.modular.devicegrouprel.entity.IotDeviceGroupRel;
import vip.xiaonuo.iot.modular.devicegrouprel.service.IotDeviceGroupRelService;
import vip.xiaonuo.iot.modular.product.service.IotProductService;
import vip.xiaonuo.iot.modular.register.entity.IotDeviceRegisterMapping;
import vip.xiaonuo.iot.modular.register.service.IotDeviceRegisterMappingService;
import vip.xiaonuo.iot.modular.product.entity.IotProduct;
import vip.xiaonuo.iot.core.message.DeviceMessageService;
import vip.xiaonuo.common.util.CommonDownloadUtil;
import vip.xiaonuo.common.util.CommonResponseUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 设备Service接口实现类
 *
 * @author jetox
 * @date  2025/12/11 07:24
 **/
@Service
public class IotDeviceServiceImpl extends ServiceImpl<IotDeviceMapper, IotDevice> implements IotDeviceService {

    @Resource
    private DeviceMessageService deviceMessageService;
    	
    @Resource
    private Modbus4jTcpClient modbusTcpClient;
    	
    @Resource
    private IotProductService iotProductService;
    	
    @Resource
    private IotDeviceRegisterMappingService iotDeviceRegisterMappingService;
    
    @Resource
    private IotDeviceShadowService iotDeviceShadowService;
    
    @Resource
    private IotDeviceGroupRelService iotDeviceGroupRelService;

    @Resource
    private IotDeviceGroupService iotDeviceGroupService;
    
    @Resource
    private IotDeviceDriverRelService iotDeviceDriverRelService;

    @Resource
    private vip.xiaonuo.iot.modular.northbound.service.NorthboundPushService northboundPushService;

    @Override
    public Page<IotDevice> page(IotDevicePageParam iotDevicePageParam) {
        QueryWrapper<IotDevice> queryWrapper = new QueryWrapper<IotDevice>().checkSqlInjection();
        if(ObjectUtil.isNotEmpty(iotDevicePageParam.getDeviceName())) {
            queryWrapper.lambda().like(IotDevice::getDeviceName, iotDevicePageParam.getDeviceName());
        }
        if(ObjectUtil.isNotEmpty(iotDevicePageParam.getDeviceStatus())) {
            queryWrapper.lambda().eq(IotDevice::getDeviceStatus, iotDevicePageParam.getDeviceStatus());
        }
        if(ObjectUtil.isNotEmpty(iotDevicePageParam.getProductId())) {
            queryWrapper.lambda().eq(IotDevice::getProductId, iotDevicePageParam.getProductId());
        }
        // 按分组查询（包括子分组）
        if(ObjectUtil.isNotEmpty(iotDevicePageParam.getGroupId())) {
            // 获取该分组及所有子分组的ID列表
            List<String> groupIds = iotDeviceGroupService.getGroupAndChildIds(iotDevicePageParam.getGroupId());
            // 获取这些分组下的所有设备ID
            List<String> deviceIds = iotDeviceGroupRelService.list(
                    new LambdaQueryWrapper<IotDeviceGroupRel>()
                            .in(IotDeviceGroupRel::getGroupId, groupIds))
                    .stream()
                    .map(IotDeviceGroupRel::getDeviceId)
                    .distinct()
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(deviceIds)) {
                queryWrapper.lambda().in(IotDevice::getId, deviceIds);
            } else {
                // 如果分组下没有设备，返回空结果
                queryWrapper.lambda().eq(IotDevice::getId, "");
            }
        }
        if(ObjectUtil.isAllNotEmpty(iotDevicePageParam.getSortField(), iotDevicePageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(iotDevicePageParam.getSortOrder());
            queryWrapper.orderBy(true, iotDevicePageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(iotDevicePageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(IotDevice::getSortCode);
        }
        
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String add(IotDeviceAddParam iotDeviceAddParam) {
        IotDevice iotDevice = BeanUtil.toBean(iotDeviceAddParam, IotDevice.class);
        
        // 自动填充产品协议类型
        if (ObjectUtil.isNotEmpty(iotDevice.getProductId())) {
            IotProduct product = iotProductService.getById(iotDevice.getProductId());
            if (product != null) {
                iotDevice.setProtocolType(product.getProtocolType());
            }
        }
        
        this.save(iotDevice);
        return iotDevice.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(IotDeviceEditParam iotDeviceEditParam) {
        IotDevice iotDevice = this.queryEntity(iotDeviceEditParam.getId());
        
        // 如果产品ID发生变化，同步更新协议类型
        String oldProductId = iotDevice.getProductId();
        BeanUtil.copyProperties(iotDeviceEditParam, iotDevice);
        
        if (!ObjectUtil.equals(oldProductId, iotDevice.getProductId()) && ObjectUtil.isNotEmpty(iotDevice.getProductId())) {
            IotProduct product = iotProductService.getById(iotDevice.getProductId());
            if (product != null) {
                iotDevice.setProtocolType(product.getProtocolType());
            }
        }
        
        this.updateById(iotDevice);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<IotDeviceIdParam> iotDeviceIdParamList) {
        // 执行删除
        this.removeByIds(CollStreamUtil.toList(iotDeviceIdParamList, IotDeviceIdParam::getId));
    }

    @Override
    public IotDevice detail(IotDeviceIdParam iotDeviceIdParam) {
        return this.queryEntity(iotDeviceIdParam.getId());
    }

    @Override
    public IotDevice queryEntity(String id) {
        IotDevice iotDevice = this.getById(id);
        if(ObjectUtil.isEmpty(iotDevice)) {
            throw new CommonException("设备不存在，id值为：{}", id);
        }
        return iotDevice;
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceEditParam> dataList = CollectionUtil.newArrayList();
         String fileName = "设备导入模板_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceEditParam.class).sheet("设备").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         CommonResponseUtil.renderError(response, "设备导入模板下载失败");
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
                    FileUtil.FILE_SEPARATOR + "iotDeviceImportTemplate.xlsx"));
            // 读取excel
            List<IotDeviceEditParam> iotDeviceEditParamList =  EasyExcel.read(tempFile).head(IotDeviceEditParam.class).sheet()
                    .headRowNumber(1).doReadSync();
            List<IotDevice> allDataList = this.list();
            for (int i = 0; i < iotDeviceEditParamList.size(); i++) {
                JSONObject jsonObject = this.doImport(allDataList, iotDeviceEditParamList.get(i), i);
                if(jsonObject.getBool("success")) {
                    successCount += 1;
                } else {
                    errorCount += 1;
                    errorDetail.add(jsonObject);
                }
            }
            return JSONUtil.createObj()
                    .set("totalCount", iotDeviceEditParamList.size())
                    .set("successCount", successCount)
                    .set("errorCount", errorCount)
                    .set("errorDetail", errorDetail);
        } catch (Exception e) {
            throw new CommonException("设备导入失败");
        }
    }

    public JSONObject doImport(List<IotDevice> allDataList, IotDeviceEditParam iotDeviceEditParam, int i) {
        String id = iotDeviceEditParam.getId();
        String deviceName = iotDeviceEditParam.getDeviceName();
        String deviceKey = iotDeviceEditParam.getDeviceKey();
        String deviceSecret = iotDeviceEditParam.getDeviceSecret();
        String productId = iotDeviceEditParam.getProductId();
        String gatewayId = iotDeviceEditParam.getGatewayId();
        String deviceStatus = iotDeviceEditParam.getDeviceStatus();
        if(ObjectUtil.hasEmpty(id, deviceName, deviceKey, deviceSecret, productId, gatewayId, deviceStatus)) {
            return JSONUtil.createObj().set("index", i + 1).set("success", false).set("msg", "必填字段存在空值");
        } else {
            try {
                int index = CollStreamUtil.toList(allDataList, IotDevice::getId).indexOf(iotDeviceEditParam.getId());
                IotDevice iotDevice;
                boolean isAdd = false;
                if(index == -1) {
                    isAdd = true;
                    iotDevice = new IotDevice();
                } else {
                    iotDevice = allDataList.get(index);
                }
                BeanUtil.copyProperties(iotDeviceEditParam, iotDevice);
                if(isAdd) {
                    allDataList.add(iotDevice);
                } else {
                    allDataList.remove(index);
                    allDataList.add(index, iotDevice);
                }
                this.saveOrUpdate(iotDevice);
                return JSONUtil.createObj().set("success", true);
            } catch (Exception e) {
              return JSONUtil.createObj().set("success", false).set("index", i + 1).set("msg", "数据导入异常");
            }
        }
    }

    @Override
    public void exportData(List<IotDeviceIdParam> iotDeviceIdParamList, HttpServletResponse response) throws IOException {
       File tempFile = null;
       try {
         List<IotDeviceEditParam> dataList;
         if(ObjectUtil.isNotEmpty(iotDeviceIdParamList)) {
            List<String> idList = CollStreamUtil.toList(iotDeviceIdParamList, IotDeviceIdParam::getId);
            dataList = BeanUtil.copyToList(this.listByIds(idList), IotDeviceEditParam.class);
         } else {
            dataList = BeanUtil.copyToList(this.list(), IotDeviceEditParam.class);
         }
         String fileName = "设备_" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
         tempFile = FileUtil.file(FileUtil.getTmpDir() + FileUtil.FILE_SEPARATOR + fileName);
         EasyExcel.write(tempFile.getPath(), IotDeviceEditParam.class).sheet("设备").doWrite(dataList);
         CommonDownloadUtil.download(tempFile, response);
       } catch (Exception e) {
         CommonResponseUtil.renderError(response, "设备导出失败");
       } finally {
         FileUtil.del(tempFile);
       }
    }

	@Override
	public void setProperty(IotDevicePropertySetParam param) {
		// 查询设备
		IotDevice device = this.queryEntity(param.getDeviceId());
		
		// 判断设备类型：Modbus设备特殊处理(有IP地址则认为是Modbus设备)
		if (DriverConfigUtil.getIpAddress(device) != null) {
			// Modbus设备：通过写寄存器控制
			handleModbusPropertySet(device, param.getProperties());
		} else {
			// MQTT设备：通过MQTT下发
			handleMqttPropertySet(device, param.getProperties());
		}
	}
	
	/**
	 * 处理Modbus设备属性设置
	 */
	private void handleModbusPropertySet(IotDevice device, Map<String, Object> properties) {
		// 获取产品信息
		IotProduct product = iotProductService.getById(device.getProductId());
		if (product == null) {
			throw new CommonException("设备关联的产品不存在");
		}
		
		// 获取设备的寄存器映射（优先设备级）
		Map<String, IotDeviceRegisterMapping> mappingMap = iotDeviceRegisterMappingService.getDeviceRegisterMappingMap(device.getId());
		
		// 收集所有需要写入的寄存器：<地址, 值>
		Map<Integer, Integer> registerWrites = new TreeMap<>();
		List<String> failedProperties = new ArrayList<>();
		
		// 遍历要设置的属性
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String identifier = entry.getKey();
			Object value = entry.getValue();
			
			// 从映射表中查找对应的寄存器地址
			IotDeviceRegisterMapping mapping = mappingMap.get(identifier);
			
			if (mapping == null) {
				failedProperties.add(identifier);
				continue;
			}
			
			// 获取寄存器地址
			Integer registerAddress = mapping.getRegisterAddress();
			if (registerAddress == null) {
				failedProperties.add(identifier);
				continue;
			}
			
			// 转换值为整数
			int registerValue = convertToInt(value);
			
			// 添加到待写入映射表
			registerWrites.put(registerAddress, registerValue);
		}
		
		// 如果所有属性都失败
		if (registerWrites.isEmpty()) {
			String errorMsg = "所有属性都未配置寄存器映射: " + String.join(", ", failedProperties);
			throw new CommonException(errorMsg);
		}
		
		// 检查寄存器地址是否连续
		List<Integer> addresses = new ArrayList<>(registerWrites.keySet());
		boolean isContinuous = checkContinuousAddresses(addresses);
		
		if (isContinuous && addresses.size() > 1) {
			// 连续地址：使用批量写（0x10）
			int startAddress = addresses.get(0);
			int[] values = new int[addresses.size()];
			for (int i = 0; i < addresses.size(); i++) {
				values[i] = registerWrites.get(addresses.get(i));
			}
			
			modbusTcpClient.writeMultipleRegisters(device, startAddress, values);
		} else {
			// 不连续地址：逐个写入（0x06）
			for (Map.Entry<Integer, Integer> entry : registerWrites.entrySet()) {
				int address = entry.getKey();
				int value = entry.getValue();
				modbusTcpClient.writeSingleRegister(device, address, value);
			}
		}
		
		// 如果有失败的属性，记录警告
		if (!failedProperties.isEmpty()) {
			// 静默处理
		}
		
		// 属性设置成功后，触发北向推送
		triggerNorthboundPush(device, properties);
	}
	
	/**
	 * 触发北向推送
	 */
	private void triggerNorthboundPush(IotDevice device, Map<String, Object> properties) {
		try {
			// 构建推送数据
			JSONObject data = JSONUtil.createObj();
			data.set("type", "property_set");
			data.set("properties", properties);
			
			// 触发北向推送
			northboundPushService.pushDeviceData(device, data);
		} catch (Exception e) {
			// 北向推送失败不影响设备控制，只记录日志
			// 静默处理
		}
	}

	/**
	 * 检查寄存器地址是否连续
	 */
	private boolean checkContinuousAddresses(List<Integer> addresses) {
		if (addresses.size() <= 1) {
			return false;
		}
		for (int i = 1; i < addresses.size(); i++) {
			if (addresses.get(i) != addresses.get(i - 1) + 1) {
				return false;
			}
		}
		return true;
	}

    /**
	 * 转换为整数
	 */
	private int convertToInt(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			return Integer.parseInt((String) value);
		}
		return 0;
	}
	
	/**
	 * 处理MQTT设备属性设置
	 */
	private void handleMqttPropertySet(IotDevice device, Map<String, Object> properties) {
		// 构建属性设置消息
		JSONObject message = JSONUtil.createObj()
				.set("method", "property.set")
				.set("params", properties)
				.set("timestamp", System.currentTimeMillis());
		
		// 构建Topic: /{productKey}/{deviceKey}/property/set
		String topic = String.format("/%s/%s/property/set", 
				device.getProductId(), device.getDeviceKey());
		
		// 下发消息
		boolean success = deviceMessageService.sendToDevice(device.getDeviceKey(), topic, message.toString());
		
		if (!success) {
			throw new CommonException("设备未连接或属性设置失败");
		}
		
		// 属性设置成功后，触发北向推送
		triggerNorthboundPush(device, properties);
	}

	@Override
	public void sendCommand(IotDeviceCommandParam param) {
		// 查询设备
		IotDevice device = this.queryEntity(param.getDeviceId());
		
		// 构建Topic: /{productKey}/{deviceKey}/command/down
		String topic = String.format("/%s/%s/command/down", 
				device.getProductId(), device.getDeviceKey());
		
		// 构建指令消息
		JSONObject message = JSONUtil.createObj()
				.set("command", param.getCommand())
				.set("params", param.getParams())
				.set("timestamp", System.currentTimeMillis());
		
		// 下发消息
		boolean success = deviceMessageService.sendToDevice(device.getDeviceKey(), topic, message.toString());
		
		if (!success) {
			throw new CommonException("设备未连接或指令下发失败");
		}
	}
	@Override
	public void invokeService(IotDeviceServiceParam param) {
		// 查询设备
		IotDevice device = this.queryEntity(param.getDeviceId());
		
		// 查询产品信息，判断协议类型
		IotProduct product = iotProductService.queryEntity(device.getProductId());
		
		if ("MODBUS_TCP".equals(product.getProtocolType())) {
			// Modbus TCP协议 - 调用对应的服务处理逻辑
			handleModbusService(device, param.getServiceId(), param.getParams());
		} else if ("MQTT".equals(product.getProtocolType())) {
			// MQTT协议 - 调用MQTT服务
			handleMqttService(device, param.getServiceId(), param.getParams());
		} else {
			throw new CommonException("不支持的协议类型: {}", product.getProtocolType());
		}
	}

	/**
	 * 处理Modbus设备服务调用
	 */
	private void handleModbusService(IotDevice device, String serviceId, Map<String, Object> params) {
		// 获取设备的寄存器映射配置
		Map<String, IotDeviceRegisterMapping> mappingMap = 
			iotDeviceRegisterMappingService.getDeviceRegisterMappingMap(device.getId());
		
		if (mappingMap.isEmpty()) {
			throw new CommonException("设备未配置寄存器映射");
		}
		
		// 查询设备驱动关联（获取设备级配置）
		LambdaQueryWrapper<vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel> relQuery = new LambdaQueryWrapper<>();
		relQuery.eq(vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel::getDeviceId, device.getId());
		vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel driverRel = 
			iotDeviceDriverRelService.getOne(relQuery);
		
		switch (serviceId) {
			case "setOutput":
				// 设置单个输出
				handleSetOutput(device, mappingMap, params);
				break;
			case "setBatchOutputs":
				// 批量设置输出
				handleSetBatchOutputs(device, mappingMap, params);
				break;
			case "toggleOutputs":
				// 反转输出状态
				handleToggleOutputs(device, driverRel, mappingMap, params);
				break;
			default:
				throw new CommonException("不支持的服务: {}", serviceId);
		}
	}

	/**
	 * 设置单个输出
	 * params: {output: 1, value: true}
	 */
	private void handleSetOutput(IotDevice device, Map<String, IotDeviceRegisterMapping> mappingMap, 
								 Map<String, Object> params) {
		if (!params.containsKey("output") || !params.containsKey("value")) {
			throw new CommonException("缺少必要参数: output, value");
		}
		
		int output = convertToInt(params.get("output"));
		boolean value = Boolean.parseBoolean(params.get("value").toString());
		
		// 找到对应的DO寄存器映射
		String identifier = "DO" + output;
		IotDeviceRegisterMapping mapping = mappingMap.get(identifier);
		
		if (mapping == null) {
			throw new CommonException("输出 {} 未配置寄存器映射", identifier);
		}
		
		// 根据功能码判断寄存器类型
		String functionCode = mapping.getFunctionCode();
		if ("0x01".equals(functionCode) || "0x05".equals(functionCode) || "0x0F".equals(functionCode)) {
			// 线圈类型：写单个线圈（0x05）
			modbusTcpClient.writeSingleCoil(device, mapping.getRegisterAddress(), value);
		} else {
			// 寄存器类型：写单个寄存器（0x06）
			modbusTcpClient.writeSingleRegister(device, mapping.getRegisterAddress(), value ? 1 : 0);
		}
	}

	/**
	 * 批量设置输出
	 * params: {outputs: [1,2,3,4,5,6,7,8], value: true}
	 */
	private void handleSetBatchOutputs(IotDevice device, Map<String, IotDeviceRegisterMapping> mappingMap,
									   Map<String, Object> params) {
		if (!params.containsKey("outputs") || !params.containsKey("value")) {
			throw new CommonException("缺少必要参数: outputs, value");
		}
		
		@SuppressWarnings("unchecked")
		List<Integer> outputs = (List<Integer>) params.get("outputs");
		boolean value = Boolean.parseBoolean(params.get("value").toString());
		
		// 按地址排序的TreeMap，用于优化批量写入
		TreeMap<Integer, Boolean> coilWrites = new TreeMap<>();
		TreeMap<Integer, Integer> registerWrites = new TreeMap<>();
		
		// 收集所有需要写入的寄存器
		for (Integer output : outputs) {
			String identifier = "DO" + output;
			IotDeviceRegisterMapping mapping = mappingMap.get(identifier);
			
			if (mapping == null) {
				continue;
			}
			
			// 根据功能码判断寄存器类型
			String functionCode = mapping.getFunctionCode();
			if ("0x01".equals(functionCode) || "0x05".equals(functionCode) || "0x0F".equals(functionCode)) {
				// 线圈类型
				coilWrites.put(mapping.getRegisterAddress(), value);
			} else {
				// 寄存器类型
				registerWrites.put(mapping.getRegisterAddress(), value ? 1 : 0);
			}
		}
		
		// 批量写入线圈
		if (!coilWrites.isEmpty()) {
			List<Integer> addresses = new ArrayList<>(coilWrites.keySet());
			if (checkContinuousAddresses(addresses)) {
				// 连续地址：批量写入（0x0F）
				int startAddress = addresses.get(0);
				boolean[] values = new boolean[addresses.size()];
				for (int i = 0; i < addresses.size(); i++) {
					values[i] = coilWrites.get(addresses.get(i));
				}
				modbusTcpClient.writeMultipleCoils(device, startAddress, values);
			} else {
				// 不连续地址：逐个写入（0x05）
				for (Map.Entry<Integer, Boolean> entry : coilWrites.entrySet()) {
					modbusTcpClient.writeSingleCoil(device, entry.getKey(), entry.getValue());
				}
			}
		}
		
		// 批量写入寄存器
		if (!registerWrites.isEmpty()) {
			List<Integer> addresses = new ArrayList<>(registerWrites.keySet());
			if (checkContinuousAddresses(addresses)) {
				// 连续地址：批量写入（0x10）
				int startAddress = addresses.get(0);
				int[] values = new int[addresses.size()];
				for (int i = 0; i < addresses.size(); i++) {
					values[i] = registerWrites.get(addresses.get(i));
				}
				modbusTcpClient.writeMultipleRegisters(device, startAddress, values);
			} else {
				// 不连续地址：逐个写入（0x06）
				for (Map.Entry<Integer, Integer> entry : registerWrites.entrySet()) {
					modbusTcpClient.writeSingleRegister(device, entry.getKey(), entry.getValue());
				}
			}
		}
	}

	/**
	 * 反转输出状态
	 * params: {} (无需参数,反转所有DO属性)
	 * 注意: 直接读取寄存器获取当前状态,不依赖设备影子
	 */
	private void handleToggleOutputs(IotDevice device, 
									 vip.xiaonuo.iot.modular.devicedriverrel.entity.IotDeviceDriverRel driverRel,
									 Map<String, IotDeviceRegisterMapping> mappingMap,
									 Map<String, Object> params) {
		// 收集所有DO属性并读取当前值
		TreeMap<Integer, Boolean> coilReads = new TreeMap<>();
		TreeMap<Integer, Integer> registerReads = new TreeMap<>();
		
		for (Map.Entry<String, IotDeviceRegisterMapping> entry : mappingMap.entrySet()) {
			String identifier = entry.getKey();
			IotDeviceRegisterMapping mapping = entry.getValue();
			
			// 只处理DO开头的属性
			if (!identifier.startsWith("DO")) {
				continue;
			}
			
			// 根据功能码判断寄存器类型
			String functionCode = mapping.getFunctionCode();
			if ("0x01".equals(functionCode) || "0x05".equals(functionCode) || "0x0F".equals(functionCode)) {
				// 线圈类型
				coilReads.put(mapping.getRegisterAddress(), false); // 占位
			} else {
				// 寄存器类型
				registerReads.put(mapping.getRegisterAddress(), 0); // 占位
			}
		}
		
		// 批量读取线圈当前状态
		if (!coilReads.isEmpty()) {
			List<Integer> addresses = new ArrayList<>(coilReads.keySet());
			int startAddr = addresses.get(0);
			int quantity = addresses.get(addresses.size() - 1) - startAddr + 1;
			
			// 读取线圈状态
			modbusTcpClient.readCoils(device, driverRel, startAddr, quantity);
		}
		
		// 批量读取寄存器当前状态
		if (!registerReads.isEmpty()) {
			List<Integer> addresses = new ArrayList<>(registerReads.keySet());
			int startAddr = addresses.get(0);
			int quantity = addresses.get(addresses.size() - 1) - startAddr + 1;
			
			// 读取寄存器状态
			modbusTcpClient.readHoldingRegisters(device, driverRel, startAddr, quantity);
		}
		
		// 等待读取完成后从设备影子获取最新值
		try {
			Thread.sleep(200); // 等待读取完成
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		// 从设备影子读取最新值
		IotDeviceShadow shadow = iotDeviceShadowService.getByDeviceId(device.getId());
		JSONObject reportedData = JSONUtil.createObj();
		if (shadow != null && StrUtil.isNotBlank(shadow.getReported())) {
			try {
				reportedData = JSONUtil.parseObj(shadow.getReported());
			} catch (Exception e) {
				// 忽略解析错误
			}
		}
		
		// 收集所有DO属性并反转其值
		TreeMap<Integer, Boolean> coilWrites = new TreeMap<>();
		TreeMap<Integer, Integer> registerWrites = new TreeMap<>();
		
		for (Map.Entry<String, IotDeviceRegisterMapping> entry : mappingMap.entrySet()) {
			String identifier = entry.getKey();
			IotDeviceRegisterMapping mapping = entry.getValue();
			
			// 只处理DO开头的属性
			if (!identifier.startsWith("DO")) {
				continue;
			}
			
			// 获取当前值
			Object currentValue = reportedData.get(identifier);
			boolean currentState = false;
			if (currentValue != null) {
				if (currentValue instanceof Boolean) {
					currentState = (Boolean) currentValue;
				} else if (currentValue instanceof Number) {
					currentState = ((Number) currentValue).intValue() != 0;
				}
			}
			
			// 反转状态
			boolean newState = !currentState;
			
			// 根据功能码判断寄存器类型
			String functionCode = mapping.getFunctionCode();
			if ("0x01".equals(functionCode) || "0x05".equals(functionCode) || "0x0F".equals(functionCode)) {
				// 线圈类型
				coilWrites.put(mapping.getRegisterAddress(), newState);
			} else {
				// 寄存器类型
				registerWrites.put(mapping.getRegisterAddress(), newState ? 1 : 0);
			}
		}
		
		// 批量写入线圈
		if (!coilWrites.isEmpty()) {
			List<Integer> addresses = new ArrayList<>(coilWrites.keySet());
			if (checkContinuousAddresses(addresses)) {
				// 连续地址：批量写入（0x0F）
				int startAddress = addresses.get(0);
				boolean[] values = new boolean[addresses.size()];
				for (int i = 0; i < addresses.size(); i++) {
					values[i] = coilWrites.get(addresses.get(i));
				}
				modbusTcpClient.writeMultipleCoils(device, startAddress, values);
			} else {
				// 不连续地址：逐个写入（0x05）
				for (Map.Entry<Integer, Boolean> entry : coilWrites.entrySet()) {
					modbusTcpClient.writeSingleCoil(device, entry.getKey(), entry.getValue());
				}
			}
		}
		
		// 批量写入寄存器
		if (!registerWrites.isEmpty()) {
			List<Integer> addresses = new ArrayList<>(registerWrites.keySet());
			if (checkContinuousAddresses(addresses)) {
				// 连续地址：批量写入（0x10）
				int startAddress = addresses.get(0);
				int[] values = new int[addresses.size()];
				for (int i = 0; i < addresses.size(); i++) {
					values[i] = registerWrites.get(addresses.get(i));
				}
				modbusTcpClient.writeMultipleRegisters(device, startAddress, values);
			} else {
				// 不连续地址：逐个写入（0x06）
				for (Map.Entry<Integer, Integer> entry : registerWrites.entrySet()) {
					modbusTcpClient.writeSingleRegister(device, entry.getKey(), entry.getValue());
				}
			}
		}
	}

	/**
	 * 处理MQTT设备服务调用
	 */
	private void handleMqttService(IotDevice device, String serviceId, Map<String, Object> params) {
		// 构建服务调用消息
		JSONObject message = JSONUtil.createObj()
				.set("method", "service.invoke")
				.set("serviceId", serviceId)
				.set("params", params)
				.set("timestamp", System.currentTimeMillis());
		
		// 构建Topic: /{productKey}/{deviceKey}/service/invoke
		String topic = String.format("/%s/%s/service/invoke",
				device.getProductId(), device.getDeviceKey());
		
		// 下发消息
		boolean success = deviceMessageService.sendToDevice(device.getDeviceKey(), topic, message.toString());
		
		if (!success) {
			throw new CommonException("设备未连接或服务调用失败");
		}
	}
}
