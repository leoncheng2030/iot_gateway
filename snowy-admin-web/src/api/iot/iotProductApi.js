import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/product/` + url, ...arg)

/**
 * 产品Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 06:57
 **/
export default {
	// 获取产品分页
	iotProductPage(data) {
		return request('page', data, 'get')
	},
	// 提交产品表单 edit为true时为编辑，默认为新增
	iotProductSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除产品
	iotProductDelete(data) {
		return request('delete', data)
	},
	// 获取产品详情
	iotProductDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载产品导入模板
    iotProductDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入产品
    iotProductImport(data) {
        return request('importData', data)
    },
    // 导出产品
    iotProductExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
