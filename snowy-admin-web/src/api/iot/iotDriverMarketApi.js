import { baseRequest } from '@/utils/request'

const request = (url, ...arg) => baseRequest(`/iot/drivermarket/` + url, ...arg)

export default {
	// 分页查询驱动市场
	iotDriverMarketPage(data) {
		return request('page', data, 'get')
	},

	// 获取驱动详情
	iotDriverMarketDetail(data) {
		return request('detail', data, 'get')
	},

	// 安装驱动
	iotDriverMarketInstall(data) {
		return request('install', data)
	},

	// 卸载驱动
	iotDriverMarketUninstall(data) {
		return request('uninstall', data)
	},

	// 启用驱动
	iotDriverMarketEnable(data) {
		return request('enable', data)
	},

	// 禁用驱动
	iotDriverMarketDisable(data) {
		return request('disable', data)
	},

	// 上传驱动JAR
	iotDriverMarketUpload(data) {
		return request('upload', data)
	},

	// 同步内置驱动
	iotDriverMarketSync(data) {
		return request('sync', data)
	},

	// 获取已启用驱动列表
	iotDriverMarketEnabled(data) {
		return request('enabled', data, 'get')
	}
}
