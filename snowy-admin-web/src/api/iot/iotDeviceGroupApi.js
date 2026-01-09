import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/devicegroup/` + url, ...arg)

/**
 * 设备分组表Api接口管理器
 *
 * @author jetox
 * @date  2025/12/13 18:30
 **/
export default {
	// 获取设备分组表分页
	iotDeviceGroupPage(data) {
		return request('page', data, 'get')
	},
	// 获取设备分组树
	iotDeviceGroupTree(data) {
		return request('tree', data, 'get')
	},
	// 提交设备分组表表单 edit为true时为编辑，默认为新增
	iotDeviceGroupSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备分组表
	iotDeviceGroupDelete(data) {
		return request('delete', data)
	},
	// 获取设备分组表详情
	iotDeviceGroupDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备分组表导入模板
	iotDeviceGroupDownloadTemplate(data) {
		return request('downloadImportTemplate', data, 'get', {
			responseType: 'blob'
		})
	},
	// 导入设备分组表
	iotDeviceGroupImport(data) {
		return request('importData', data)
	},
	// 导出设备分组表
	iotDeviceGroupExport(data) {
		return request('exportData', data, 'post', {
			responseType: 'blob'
		})
	},
	// 批量关联设备到分组
	batchRelateDevices(data) {
		return request('batchRelateDevices', data)
	},
	// 批量移除分组下的设备
	batchRemoveDevices(data) {
		return request('batchRemoveDevices', data)
	},
	// 获取分组下的设备ID列表
	getDeviceIds(data) {
		return request('getDeviceIds', data, 'get')
	},
	// 获取设备关联的所有分组ID
	getGroupIdsByDeviceId(data) {
		return request('getGroupIdsByDeviceId', data, 'get')
	},
	// 同步设备分组（全量替换）
	syncDeviceGroups(data) {
		return request('syncDeviceGroups', data)
	},
	// 获取分组的已关联设备列表
	getRelatedDevices(data) {
		return request('getRelatedDevices', data, 'get')
	},
	// 获取未被任何分组关联的设备列表
	getUnrelatedDevices(data) {
		return request('getUnrelatedDevices', data, 'get')
	}
}
