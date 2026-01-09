import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/northboundconfig/` + url, ...arg)

/**
 * 北向推送配置表Api接口管理器
 *
 * @author jetox
 * @date  2026/01/08 10:20
 **/
export default {
	// 获取北向推送配置表分页
	iotNorthboundConfigPage(data) {
		return request('page', data, 'get')
	},
	// 提交北向推送配置表表单 edit为true时为编辑，默认为新增
	iotNorthboundConfigSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除北向推送配置表
	iotNorthboundConfigDelete(data) {
		return request('delete', data)
	},
	// 获取北向推送配置表详情
	iotNorthboundConfigDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载北向推送配置表导入模板
    iotNorthboundConfigDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入北向推送配置表
    iotNorthboundConfigImport(data) {
        return request('importData', data)
    },
    // 导出北向推送配置表
    iotNorthboundConfigExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    },
	// 测试连接
	iotNorthboundConfigTestConnection(data) {
		return request('testConnection', data, 'post')
	},
	// 启用/禁用配置
	iotNorthboundConfigToggleEnabled(data) {
		return request('toggleEnabled', data, 'post')
	}
}
