import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/devicegrouprel/` + url, ...arg)

/**
 * 设备分组关联表Api接口管理器
 *
 * @author jetox
 * @date  2025/12/13 18:31
 **/
export default {
	// 获取设备分组关联表分页
	iotDeviceGroupRelPage(data) {
		return request('page', data, 'get')
	},
	// 提交设备分组关联表表单 edit为true时为编辑，默认为新增
	iotDeviceGroupRelSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备分组关联表
	iotDeviceGroupRelDelete(data) {
		return request('delete', data)
	},
	// 获取设备分组关联表详情
	iotDeviceGroupRelDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备分组关联表导入模板
    iotDeviceGroupRelDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入设备分组关联表
    iotDeviceGroupRelImport(data) {
        return request('importData', data)
    },
    // 导出设备分组关联表
    iotDeviceGroupRelExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
