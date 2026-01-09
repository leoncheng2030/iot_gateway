import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/devicedata/` + url, ...arg)

/**
 * 设备数据Api接口管理器
 *
 * @author jetox
 * @date  2025/12/11 07:27
 **/
export default {
	// 获取设备数据分页
	iotDeviceDataPage(data) {
		return request('page', data, 'get')
	},
	// 提交设备数据表单 edit为true时为编辑，默认为新增
	iotDeviceDataSubmitForm(data, edit = false) {
		return request(edit ? 'edit' : 'add', data)
	},
	// 删除设备数据
	iotDeviceDataDelete(data) {
		return request('delete', data)
	},
	// 获取设备数据详情
	iotDeviceDataDetail(data) {
		return request('detail', data, 'get')
	},
	// 下载设备数据导入模板
    iotDeviceDataDownloadTemplate(data) {
        return request('downloadImportTemplate', data, 'get', {
            responseType: 'blob'
        })
    },
    // 导入设备数据
    iotDeviceDataImport(data) {
        return request('importData', data)
    },
    // 导出设备数据
    iotDeviceDataExport(data) {
        return request('exportData', data, 'post', {
            responseType: 'blob'
        })
    },
	// 获取设备图表数据(按时间分组)
	iotDeviceDataChartData(data) {
		return request('chartData', data, 'get')
	}
}
