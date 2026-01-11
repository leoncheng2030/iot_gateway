import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/productPropertyMapping/${url}`, ...arg)

/**
 * 产品属性映射API
 *
 * @author gtc
 * @date 2026/01/11
 */
export default {
	// 获取产品属性映射列表（带地址配置）
	iotProductPropertyMappingList(data) {
		return request('list/' + data.productId, data, 'get')
	},
	
	// 批量保存产品属性映射（含地址配置）
	iotProductPropertyMappingBatchSave(data) {
		return request('batchSave/' + data.productId, data.mappings, 'post')
	},
	
	// 删除产品属性映射
	iotProductPropertyMappingDelete(data) {
		return request('delete/' + data.productId, data, 'delete')
	}
}
