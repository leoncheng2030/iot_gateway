import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/driverlog/` + url, ...arg)

/**
 * 运行日志Api接口管理器
 *
 * @author jetox
 * @date  2025/12/13 09:46
 **/
export default {
	// 获取运行日志分页
	iotDriverLogPage(data) {
		return request('page', data, 'get')
	},
	// 提交运行日志表单 edit为true时为编辑，默认为新增
	iotDriverLogSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除运行日志
	iotDriverLogDelete(data) {
		return request('delete', data)
	},
	// 获取运行日志详情
	iotDriverLogDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载运行日志导入模板
    iotDriverLogDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入运行日志
    iotDriverLogImport(data) {
        return request('importData', data)
    },
    // 导出运行日志
    iotDriverLogExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
