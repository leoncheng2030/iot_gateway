import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/northboundlog/` + url, ...arg)

/**
 * 北向推送日志表Api接口管理器
 *
 * @author jetox
 * @date  2026/01/08 10:25
 **/
export default {
	// 获取北向推送日志表分页
	iotNorthboundLogPage(data) {
		return request('page', data, 'get')
	},
	// 提交北向推送日志表表单 edit为true时为编辑，默认为新增
	iotNorthboundLogSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除北向推送日志表
	iotNorthboundLogDelete(data) {
		return request('delete', data)
	},
	// 获取北向推送日志表详情
	iotNorthboundLogDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载北向推送日志表导入模板
    iotNorthboundLogDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入北向推送日志表
    iotNorthboundLogImport(data) {
        return request('importData', data)
    },
    // 导出北向推送日志表
    iotNorthboundLogExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
