import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/statistics/` + url, ...arg)

/**
 * IoT统计数据Api接口管理器
 *
 * @author jetox
 * @date  2026/01/09 17:00
 **/
export default {
	// 获取设备统计数据
	getDeviceStatistics(data) {
		return request('device', data, 'get')
	},
	// 获取驱动统计数据
	getDriverStatistics(data) {
		return request('driver', data, 'get')
	},
	// 获取告警统计数据
	getAlarmStatistics(data) {
		return request('alarm', data, 'get')
	},
	// 获取数据采集趋势
	getDataTrend(data) {
		return request('dataTrend', data, 'get')
	},
	// 获取系统资源统计
	getSystemResource(data) {
		return request('systemResource', data, 'get')
	}
}
