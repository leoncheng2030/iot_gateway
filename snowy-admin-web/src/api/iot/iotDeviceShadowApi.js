import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/deviceshadow/` + url, ...arg)

/**
 * 设备影子表Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:28
 **/
export default {
	// 获取设备影子表分页
	iotDeviceShadowPage(data) {
		return request('page', data, 'get')
	},
	// 提交设备影子表表单 edit为true时为编辑，默认为新增
	iotDeviceShadowSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备影子表
	iotDeviceShadowDelete(data) {
		return request('delete', data)
	},
	// 获取设备影子表详情
	iotDeviceShadowDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备影子表导入模板
    iotDeviceShadowDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入设备影子表
    iotDeviceShadowImport(data) {
        return request('importData', data)
    },
    // 导出设备影子表
    iotDeviceShadowExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
