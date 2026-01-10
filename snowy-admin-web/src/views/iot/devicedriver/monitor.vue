<template>
	<a-card :bordered="false" title="驱动监控Dashboard">
		<!-- 统计卡片 -->
		<a-row :gutter="16" class="mb-4">
			<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<a-statistic title="运行中驱动" :value="statistics.runningCount" :value-style="{ color: '#3f8600' }">
						<template #prefix>
							<check-circle-outlined />
						</template>
					</a-statistic>
				</a-card>
			</a-col>
			<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<a-statistic title="已停止驱动" :value="statistics.stoppedCount" :value-style="{ color: '#999' }">
						<template #prefix>
							<stop-outlined />
						</template>
					</a-statistic>
				</a-card>
			</a-col>
			<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<a-statistic title="错误驱动" :value="statistics.errorCount" :value-style="{ color: '#cf1322' }">
						<template #prefix>
							<warning-outlined />
						</template>
					</a-statistic>
				</a-card>
			</a-col>
			<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
				<a-card>
					<a-statistic title="总驱动数" :value="statistics.totalCount">
						<template #prefix>
							<dashboard-outlined />
						</template>
					</a-statistic>
				</a-card>
			</a-col>
		</a-row>

		<!-- 驱动列表 -->
		<a-card title="驱动运行详情" class="mt-4">
			<a-table
				:columns="columns"
				:data-source="driverList"
				:loading="loading"
				:pagination="false"
				bordered
				size="middle"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'status'">
						<a-tag v-if="record.status === 'RUNNING'" color="success">运行中</a-tag>
						<a-tag v-else-if="record.status === 'STOPPED'" color="default">已停止</a-tag>
						<a-tag v-else-if="record.status === 'ERROR'" color="error">错误</a-tag>
					</template>
					<template v-if="column.dataIndex === 'driverType'">
						<span>{{ getDriverTypeName(record.driverType) }}</span>
					</template>
					<template v-if="column.dataIndex === 'uptime'">
						<span v-if="record.status === 'RUNNING' && record.uptime">
							{{ formatUptime(record.uptime) }}
						</span>
						<span v-else>-</span>
					</template>
					<template v-if="column.dataIndex === 'action'">
						<a-space>
							<a-button v-if="record.status !== 'RUNNING'" type="link" size="small" @click="startDriver(record.id)">
								启动
							</a-button>
							<a-button
								v-if="record.status === 'RUNNING'"
								type="link"
								danger
								size="small"
								@click="stopDriver(record.id)"
							>
								停止
							</a-button>
							<a-button v-if="record.status === 'RUNNING'" type="link" size="small" @click="restartDriver(record.id)">
								重启
							</a-button>
						</a-space>
					</template>
				</template>
			</a-table>
		</a-card>

		<!-- 最近日志 -->
		<a-card class="mt-4">
			<template #title>
				<a-space>
					<span>最近日志</span>
					<a-select v-model:value="logLimit" style="width: 120px" size="small" @change="loadRecentLogs">
						<a-select-option :value="10">最近10条</a-select-option>
						<a-select-option :value="20">最近20条</a-select-option>
						<a-select-option :value="50">最近50条</a-select-option>
						<a-select-option :value="100">最近100条</a-select-option>
					</a-select>
				</a-space>
			</template>
			<a-table
				:columns="logColumns"
				:data-source="recentLogs"
				:pagination="false"
				:scroll="{ y: 400 }"
				size="small"
				bordered
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'logType'">
						<a-tag :color="getLogColor(record.logType)">
							{{ $TOOL.dictTypeData('DRIVER_LOG_TYPE', record.logType) }}
						</a-tag>
					</template>
					<template v-if="column.dataIndex === 'logContent'">
						<a-tooltip>
							<template #title>{{ record.logContent }}</template>
							<div style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
								{{ record.logContent }}
							</div>
						</a-tooltip>
					</template>
					<template v-if="column.dataIndex === 'errorMsg'">
						<a-tooltip v-if="record.errorMsg">
							<template #title>{{ record.errorMsg }}</template>
							<div style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #ff4d4f">
								{{ record.errorMsg }}
							</div>
						</a-tooltip>
						<span v-else>-</span>
					</template>
					<template v-if="column.dataIndex === 'createTime'">
						{{ formatDateTime(record.createTime) }}
					</template>
				</template>
			</a-table>
			<a-empty v-if="recentLogs.length === 0" description="暂无日志" />
		</a-card>
	</a-card>
</template>

<script setup name="driverMonitor">
	import { message } from 'ant-design-vue'
	import iotDeviceDriverApi from '@/api/iot/iotDeviceDriverApi'
	import iotDriverLogApi from '@/api/iot/iotDriverLogApi'

	// 驱动类型映射（从注册中心动态加载）
	const driverTypeMap = ref({})

	const loading = ref(false)
	const driverList = ref([])
	const recentLogs = ref([])
	const logLimit = ref(10)
	const statistics = ref({
		totalCount: 0,
		runningCount: 0,
		stoppedCount: 0,
		errorCount: 0
	})

	const columns = [
		{
			title: '驱动名称',
			dataIndex: 'driverName',
			width: 200
		},
		{
			title: '驱动类型',
			dataIndex: 'driverType',
			width: 150
		},
		{
			title: '状态',
			dataIndex: 'status',
			width: 100
		},
		{
			title: '运行时长',
			dataIndex: 'uptime',
			width: 150
		},
		{
			title: '描述',
			dataIndex: 'description'
		},
		{
			title: '操作',
			dataIndex: 'action',
			width: 200,
			fixed: 'right'
		}
	]

	// 日志表格列
	const logColumns = [
		{
			title: '驱动名称',
			dataIndex: 'driverName',
			width: 150
		},
		{
			title: '日志类型',
			dataIndex: 'logType',
			width: 100
		},
		{
			title: '日志内容',
			dataIndex: 'logContent'
		},
		{
			title: '错误信息',
			dataIndex: 'errorMsg',
			width: 200
		},
		{
			title: '记录时间',
			dataIndex: 'createTime',
			width: 160
		}
	]

	// 格式化运行时长
	const formatUptime = (milliseconds) => {
		if (!milliseconds) return '-'
		const seconds = Math.floor(milliseconds / 1000)
		const minutes = Math.floor(seconds / 60)
		const hours = Math.floor(minutes / 60)
		const days = Math.floor(hours / 24)

		if (days > 0) {
			return `${days}天 ${hours % 24}小时`
		} else if (hours > 0) {
			return `${hours}小时 ${minutes % 60}分钟`
		} else if (minutes > 0) {
			return `${minutes}分钟 ${seconds % 60}秒`
		} else {
			return `${seconds}秒`
		}
	}

	// 日志颜色
	const getLogColor = (logType) => {
		const colorMap = {
			INFO: 'blue',
			WARN: 'orange',
			ERROR: 'red',
			START: 'green',
			STOP: 'gray'
		}
		return colorMap[logType] || 'blue'
	}

	// 获取驱动类型名称
	const getDriverTypeName = (type) => {
		const driverInfo = driverTypeMap.value[type]
		return driverInfo ? driverInfo.label : type
	}

	// 格式化日期时间
	const formatDateTime = (dateTime) => {
		if (!dateTime) return '-'
		const date = new Date(dateTime)
		const year = date.getFullYear()
		const month = String(date.getMonth() + 1).padStart(2, '0')
		const day = String(date.getDate()).padStart(2, '0')
		const hours = String(date.getHours()).padStart(2, '0')
		const minutes = String(date.getMinutes()).padStart(2, '0')
		const seconds = String(date.getSeconds()).padStart(2, '0')
		return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
	}

	// 加载驱动列表
	const loadDriverList = () => {
		loading.value = true
		iotDeviceDriverApi
			.iotDeviceDriverPage({
				pageSize: 100,
				current: 1
			})
			.then((res) => {
				driverList.value = res.records || []

				// 计算统计数据
				statistics.value.totalCount = driverList.value.length
				statistics.value.runningCount = driverList.value.filter((d) => d.status === 'RUNNING').length
				statistics.value.stoppedCount = driverList.value.filter((d) => d.status === 'STOPPED').length
				statistics.value.errorCount = driverList.value.filter((d) => d.status === 'ERROR').length

				// 获取运行中驱动的运行时长
				const promises = driverList.value
					.filter((driver) => driver.status === 'RUNNING')
					.map((driver) => {
						return iotDeviceDriverApi.iotDeviceDriverStatus({ id: driver.id }).then((statusRes) => {
							if (statusRes && statusRes.uptime) {
								driver.uptime = statusRes.uptime
							}
						})
					})

				// 等待所有状态查询完成
				Promise.all(promises).finally(() => {
					loading.value = false
				})
			})
			.catch(() => {
				loading.value = false
			})
	}

	// 加载最近日志
	const loadRecentLogs = () => {
		iotDriverLogApi
			.iotDriverLogPage({
				pageSize: logLimit.value,
				current: 1,
				sortField: 'createTime',
				sortOrder: 'descend'
			})
			.then((res) => {
				recentLogs.value = res.records || []
			})
	}

	// 启动驱动
	const startDriver = (driverId) => {
		iotDeviceDriverApi.iotDeviceDriverStart({ id: driverId }).then(() => {
			message.success('驱动启动成功')
			setTimeout(() => {
				loadDriverList()
				loadRecentLogs()
			}, 500)
		})
	}

	// 停止驱动
	const stopDriver = (driverId) => {
		iotDeviceDriverApi.iotDeviceDriverStop({ id: driverId }).then(() => {
			message.success('驱动停止成功')
			setTimeout(() => {
				loadDriverList()
				loadRecentLogs()
			}, 500)
		})
	}

	// 重启驱动
	const restartDriver = (driverId) => {
		iotDeviceDriverApi.iotDeviceDriverRestart({ id: driverId }).then(() => {
			message.success('驱动重启成功')
			setTimeout(() => {
				loadDriverList()
				loadRecentLogs()
			}, 500)
		})
	}

	// 自动刷新
	let refreshTimer = null
	onMounted(() => {
		loadDriverList()
		loadRecentLogs()

		// 每30秒自动刷新
		refreshTimer = setInterval(() => {
			loadDriverList()
			loadRecentLogs()
		}, 30000)
	})

	// 加载驱动类型映射
	const loadDriverTypes = async () => {
		try {
			const types = await iotDeviceDriverApi.iotDeviceDriverTypes()
			const map = {}
			types.forEach(item => {
				map[item.value] = item
			})
			driverTypeMap.value = map
		} catch (e) {
			console.error('加载驱动类型失败', e)
		}
	}

	// 页面初始化时加载驱动类型
	onMounted(async () => {
		await loadDriverTypes()
		loadDriverList()
		loadRecentLogs()

		// 每30秒自动刷新
		refreshTimer = setInterval(() => {
			loadDriverList()
			loadRecentLogs()
		}, 30000)
	})

	onUnmounted(() => {
		if (refreshTimer) {
			clearInterval(refreshTimer)
		}
	})
</script>

<style scoped>
	.mb-3 {
		margin-bottom: 16px;
	}
	.mb-4 {
		margin-bottom: 24px;
	}
	.mt-4 {
		margin-top: 24px;
	}
	.font-bold {
		font-weight: 600;
	}
	.text-gray-600 {
		color: #666;
	}
	.text-gray-400 {
		color: #999;
	}
	.text-sm {
		font-size: 12px;
	}
	.text-red-500 {
		color: #ff4d4f;
	}
	.ml-2 {
		margin-left: 8px;
	}
</style>
