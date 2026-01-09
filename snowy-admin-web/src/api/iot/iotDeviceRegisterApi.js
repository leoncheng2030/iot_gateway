import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/device/register/` + url, ...arg)

/**
 * 设备寄存器映射API
 *
 * @author jetox
 * @date  2025/12/12 12:33
 **/
export default {
	// 获取设备寄存器映射列表
	iotDeviceRegisterList(data) {
		return request('list/' + data.deviceId, data, 'get')
	},
	// 批量保存设备寄存器映射
	iotDeviceRegisterBatchSave(data) {
		return request('batchSave/' + data.deviceId, data.mappings, 'post')
	},
	// 删除设备所有寄存器映射
	iotDeviceRegisterDelete(data) {
		return request('delete/' + data.deviceId, data, 'delete')
	}
}
