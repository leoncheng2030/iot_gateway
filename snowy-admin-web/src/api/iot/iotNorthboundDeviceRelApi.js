import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/northbounddevicerel/` + url, ...arg)

/**
 * 北向推送设备关联表Api接口管理器
 *
 * @author jetox
 * @date  2026/01/08 10:25
 **/
export default {
	// 获取北向推送设备关联表分页
	iotNorthboundDeviceRelPage(data) {
		return request('page', data, 'get')
	},
	// 提交北向推送设备关联表表单 edit为true时为编辑，默认为新增
	iotNorthboundDeviceRelSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除北向推送设备关联表
	iotNorthboundDeviceRelDelete(data) {
		return request('delete', data)
	},
	// 获取北向推送设备关联表详情
	iotNorthboundDeviceRelDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载北向推送设备关联表导入模板
    iotNorthboundDeviceRelDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入北向推送设备关联表
    iotNorthboundDeviceRelImport(data) {
        return request('importData', data)
    },
    // 导出北向推送设备关联表
    iotNorthboundDeviceRelExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    },
	// 设备绑定配置
	iotNorthboundDeviceRelBind(data) {
		return request('bind', data, 'post')
	}
}
