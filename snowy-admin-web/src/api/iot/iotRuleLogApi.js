import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/rulelog/` + url, ...arg)

/**
 * 规则执行日志表Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:39
 **/
export default {
	// 获取规则执行日志表分页
	iotRuleLogPage(data) {
		return request('page', data, 'get')
	},
	// 提交规则执行日志表表单 edit为true时为编辑，默认为新增
	iotRuleLogSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除规则执行日志表
	iotRuleLogDelete(data) {
		return request('delete', data)
	},
	// 获取规则执行日志表详情
	iotRuleLogDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载规则执行日志表导入模板
    iotRuleLogDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入规则执行日志表
    iotRuleLogImport(data) {
        return request('importData', data)
    },
    // 导出规则执行日志表
    iotRuleLogExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
