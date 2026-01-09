import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/device/` + url, ...arg)

/**
 * 设备Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:24
 **/
export default {
	// 获取设备分页
	iotDevicePage(data) {
		return request('page', data, 'get')
	},
	// 提交设备表单 edit为true时为编辑，默认为新增
	iotDeviceSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备
	iotDeviceDelete(data) {
		return request('delete', data)
	},
	// 获取设备详情
	iotDeviceDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备导入模板
	iotDeviceDownloadTemplate(data) {
		return request('downloadImportTemplate', data, 'get', {
			responseType: 'blob'
		})
	},
	// 导入设备
	iotDeviceImport(data) {
		return request('importData', data)
	},
	// 导出设备
	iotDeviceExport(data) {
		return request('exportData', data, 'post', {
			responseType: 'blob'
		})
	},
	// 设置设备属性
	iotDeviceSetProperty(data) {
		return request('setProperty', data)
	},
	// 下发设备指令
	iotDeviceSendCommand(data) {
		return request('sendCommand', data)
	},
	// 调用设备服务
	iotDeviceInvokeService(data) {
		return request('invokeService', data)
	}
}
