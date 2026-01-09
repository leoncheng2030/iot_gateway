import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/rule/` + url, ...arg)

/**
 * 规则引擎Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:32
 **/
export default {
	// 获取规则引擎分页
	iotRulePage(data) {
		return request('page', data, 'get')
	},
	// 提交规则引擎表单 edit为true时为编辑，默认为新增
	iotRuleSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除规则引擎
	iotRuleDelete(data) {
		return request('delete', data)
	},
	// 获取规则引擎详情
	iotRuleDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载规则引擎导入模板
    iotRuleDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入规则引擎
    iotRuleImport(data) {
        return request('importData', data)
    },
    // 导出规则引擎
    iotRuleExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
