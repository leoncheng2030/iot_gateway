import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/product/register/` + url, ...arg)

/**
 * 产品寄存器映射API
 *
 * @author jetox
 * @date  2025/12/13 07:45
 **/
export default {
	// 获取产品寄存器映射列表
	iotProductRegisterList(data) {
		return request('list/' + data.productId, data, 'get')
	},
	// 批量保存产品寄存器映射
	iotProductRegisterBatchSave(data) {
		return request('batchSave/' + data.productId, data.mappings, 'post')
	},
	// 删除产品所有寄存器映射
	iotProductRegisterDelete(data) {
		return request('delete/' + data.productId, data, 'delete')
	}
}
