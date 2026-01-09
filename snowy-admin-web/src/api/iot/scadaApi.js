import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/scada/${url}`, ...arg)

/**
 * 组态管理API
 *
 * @author jetox
 * @date 2025/12/14
 */
export default {
	// 分页查询
	scadaPage(data) {
		return request('page', data, 'get')
	},
	// 列表查询
	scadaList(data) {
		return request('list', data, 'get')
	},
	// 添加
	scadaAdd(data) {
		return request('add', data)
	},
	// 编辑
	scadaEdit(data) {
		return request('edit', data)
	},
	// 删除
	scadaDelete(data) {
		return request('delete', data)
	},
	// 详情
	scadaDetail(data) {
		return request('detail', data, 'get')
	}
}
