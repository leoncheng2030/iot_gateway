import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/thingmodel/${url}`, ...arg)

/**
 * 物模型API
 *
 * @author jetox
 * @date 2025/12/14
 */
export default {
	// 根据产品ID获取物模型列表
	iotThingModelListByProduct(data) {
		return request('listByProduct', data, 'get')
	},
	// 根据产品ID获取物模型列表（别名，兼容旧代码）
	iotThingModelGetProperties(data) {
		return request('listByProduct', data, 'get')
	},
	// 分页查询
	iotThingModelPage(data) {
		return request('page', data, 'get')
	},
	// 提交物模型表单 edit为true时为编辑，默认为新增
	iotThingModelSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 添加
	iotThingModelAdd(data) {
		return request('add', data)
	},
	// 编辑
	iotThingModelEdit(data) {
		return request('edit', data)
	},
	// 删除
	iotThingModelDelete(data) {
		return request('delete', data)
	},
	// 详情
	iotThingModelDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载物模型导入模板
	iotThingModelDownloadTemplate(data) {
		return request('downloadImportTemplate', data, 'get', {
			responseType: 'blob'
		})
	},
	// 导入物模型
	iotThingModelImport(data) {
		return request('importData', data)
	},
	// 导出物模型
	iotThingModelExport(data) {
		return request('exportData', data, 'post', {
			responseType: 'blob'
		})
	}
}
