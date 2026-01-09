import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/northboundstatistics/` + url, ...arg)

/**
 * 北向推送统计表Api接口管理器
 *
 * @author jetox
 * @date  2026/01/08 10:26
 **/
export default {
	// 获取北向推送统计表分页
	iotNorthboundStatisticsPage(data) {
		return request('page', data, 'get')
	},
	// 提交北向推送统计表表单 edit为true时为编辑，默认为新增
	iotNorthboundStatisticsSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除北向推送统计表
	iotNorthboundStatisticsDelete(data) {
		return request('delete', data)
	},
	// 获取北向推送统计表详情
	iotNorthboundStatisticsDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载北向推送统计表导入模板
    iotNorthboundStatisticsDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入北向推送统计表
    iotNorthboundStatisticsImport(data) {
        return request('importData', data)
    },
    // 导出北向推送统计表
    iotNorthboundStatisticsExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
