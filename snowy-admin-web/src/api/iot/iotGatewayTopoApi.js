import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/gatewaytopo/` + url, ...arg)

/**
 * 网关拓扑Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:29
 **/
export default {
	// 获取网关拓扑分页
	iotGatewayTopoPage(data) {
		return request('page', data, 'get')
	},
	// 提交网关拓扑表单 edit为true时为编辑，默认为新增
	iotGatewayTopoSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除网关拓扑
	iotGatewayTopoDelete(data) {
		return request('delete', data)
	},
	// 获取网关拓扑详情
	iotGatewayTopoDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载网关拓扑导入模板
    iotGatewayTopoDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入网关拓扑
    iotGatewayTopoImport(data) {
        return request('importData', data)
    },
    // 导出网关拓扑
    iotGatewayTopoExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    }
}
