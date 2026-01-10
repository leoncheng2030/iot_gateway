import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/devicedriver/` + url, ...arg)

/**
 * 设备驱动配置表Api接口管理器
 *
 * @author jetox
 * @date  2025/12/13 09:45
 **/
export default {
	// 获取设备驱动配置表分页
	iotDeviceDriverPage(data) {
		return request('page', data, 'get')
	},
	// 提交设备驱动配置表表单 edit为true时为编辑，默认为新增
	iotDeviceDriverSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备驱动配置表
	iotDeviceDriverDelete(data) {
		return request('delete', data)
	},
	// 获取设备驱动配置表详情
	iotDeviceDriverDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备驱动配置表导入模板
	iotDeviceDriverDownloadTemplate(data) {
		return request('downloadImportTemplate', data, 'get', {
			responseType: 'blob'
		})
	},
	// 导入设备驱动配置表
	iotDeviceDriverImport(data) {
		return request('importData', data)
	},
	// 导出设备驱动配置表
	iotDeviceDriverExport(data) {
		return request('exportData', data, 'post', {
			responseType: 'blob'
		})
	},
	// 启动驱动服务
	iotDeviceDriverStart(data) {
		return request('start', data)
	},
	// 停止驱动服务
	iotDeviceDriverStop(data) {
		return request('stop', data)
	},
	// 重启驱动服务
	iotDeviceDriverRestart(data) {
		return request('restart', data)
	},
	// 获取驱动运行状态
	iotDeviceDriverStatus(data) {
		return request('status', data, 'get')
	},
	// 获取驱动配置模板
	iotDeviceDriverConfigTemplate(driverType) {
		return request('configTemplate', { driverType }, 'get')
	},
	// 获取所有已注册的驱动类型
	iotDeviceDriverTypes() {
		return request('types', {}, 'get')
	}
}
