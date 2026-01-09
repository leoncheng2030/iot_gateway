import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/devicedriverrel/` + url, ...arg)

/**
 * 设备驱动Api接口管理器
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
export default {
	// 获取设备驱动分页
	iotDeviceDriverRelPage(data) {
		return request('page', data, 'get')
	},
	// 提交设备驱动表单 edit为true时为编辑，默认为新增
	iotDeviceDriverRelSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备驱动
	iotDeviceDriverRelDelete(data) {
		return request('delete', data)
	},
	// 获取设备驱动详情
	iotDeviceDriverRelDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备驱动导入模板
	iotDeviceDriverRelDownloadTemplate(data) {
		return request('downloadImportTemplate', data, 'get', {
			responseType: 'blob'
		})
	},
	// 导入设备驱动
	iotDeviceDriverRelImport(data) {
		return request('importData', data)
	},
	// 导出设备驱动
	iotDeviceDriverRelExport(data) {
		return request('exportData', data, 'post', {
			responseType: 'blob'
		})
	},
	// 获取设备关联的驱动列表
	iotDeviceDriverRelListByDeviceId(data) {
		return request('listByDeviceId', data, 'get')
	},
	// 绑定设备驱动
	iotDeviceDriverRelBindDriver(data) {
		return request('bindDriver', data, 'post')
	},
	// 解绑设备驱动
	iotDeviceDriverRelUnbindDriver(data) {
		return request('unbindDriver', data, 'post')
	}
}
