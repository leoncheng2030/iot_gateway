import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/protocol/` + url, ...arg)

/**
 * 协议配置Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:09
 **/
export default {
	// 获取协议配置分页
	iotProtocolPage(data) {
		return request('page', data, 'get')
	},
	// 提交协议配置表单 edit为true时为编辑，默认为新增
	iotProtocolSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除协议配置
	iotProtocolDelete(data) {
		return request('delete', data)
	},
	// 获取协议配置详情
	iotProtocolDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载协议配置导入模板
    iotProtocolDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入协议配置
    iotProtocolImport(data) {
        return request('importData', data)
    },
    // 导出协议配置
    iotProtocolExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    },
	// 启动协议服务
	iotProtocolStart(data) {
		return request('start', data)
	},
	// 停止协议服务
	iotProtocolStop(data) {
		return request('stop', data)
	},
	// 重启协议服务
	iotProtocolRestart(data) {
		return request('restart', data)
	},
	// 获取协议运行状态
	iotProtocolStatus(data) {
		return request('status', data, 'get')
	}
}
