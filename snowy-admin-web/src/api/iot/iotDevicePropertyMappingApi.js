import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/devicePropertyMapping/${url}`, ...arg)

/**
 * 设备属性映射API
 *
 * @author gtc
 * @date 2026/01/11
 */
export default {
	// 获取设备属性映射列表（带地址配置）
	iotDevicePropertyMappingList(data) {
		return request('list/' + data.deviceId, data, 'get')
	},
	
	// 批量保存设备属性映射（含地址配置）
	iotDevicePropertyMappingBatchSave(data) {
		return request('batchSave/' + data.deviceId, data.mappings, 'post')
	},
	
	// 删除设备属性映射
	iotDevicePropertyMappingDelete(data) {
		return request('delete/' + data.deviceId, data, 'delete')
	},
	
	// 清除设备级映射（恢复使用产品级配置）
	iotDevicePropertyMappingClear(data) {
		return request('clear/' + data.deviceId, {}, 'delete')
	}
}
