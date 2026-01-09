<template>
	<a-row :gutter="[16, 16]">
		<!-- 核心统计卡片 -->
		<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
			<a-card :bordered="false" class="stat-card stat-card-primary">
				<a-statistic title="设备总数" :value="statistics.totalDevices" :suffix="'台'">
					<template #prefix>
						<api-outlined class="stat-icon" />
					</template>
				</a-statistic>
			</a-card>
		</a-col>

		<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
			<a-card :bordered="false" class="stat-card stat-card-success">
				<a-statistic title="在线设备" :value="statistics.onlineDevices" :suffix="'台'">
					<template #prefix>
						<check-circle-outlined class="stat-icon" />
					</template>
					<template #suffix>
						<span class="stat-percent">{{ onlinePercent }}%</span>
					</template>
				</a-statistic>
			</a-card>
		</a-col>

		<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
			<a-card :bordered="false" class="stat-card stat-card-warning">
				<a-statistic title="离线设备" :value="statistics.offlineDevices" :suffix="'台'">
					<template #prefix>
						<exclamation-circle-outlined class="stat-icon" />
					</template>
				</a-statistic>
			</a-card>
		</a-col>

		<a-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
			<a-card :bordered="false" class="stat-card stat-card-info">
				<a-statistic title="今日数据量" :value="statistics.todayDataCount">
					<template #prefix>
						<database-outlined class="stat-icon" />
					</template>
					<template #suffix>
						<span class="stat-unit">条</span>
					</template>
				</a-statistic>
			</a-card>
		</a-col>
	</a-row>
</template>

<script setup>
	import { ref, computed, onMounted, onUnmounted } from 'vue'
	import { message } from 'ant-design-vue'
	import iotStatisticsApi from '@/api/iot/iotStatisticsApi'

	const statistics = ref({
		totalDevices: 0,
		onlineDevices: 0,
		offlineDevices: 0,
		todayDataCount: 0
	})

	const onlinePercent = computed(() => {
		if (statistics.value.totalDevices === 0) return 0
		return ((statistics.value.onlineDevices / statistics.value.totalDevices) * 100).toFixed(1)
	})

	let refreshTimer = null

	// 获取统计数据
	const fetchStatistics = async () => {
		try {
			const res = await iotStatisticsApi.getDeviceStatistics()
			if (res) {
				statistics.value = {
					totalDevices: res.totalDevices || 0,
					onlineDevices: res.onlineDevices || 0,
					offlineDevices: res.offlineDevices || 0,
					todayDataCount: res.todayDataCount || 0
				}
			}
		} catch (error) {
			console.error('获取设备统计数据失败:', error)
			message.error('获取统计数据失败')
		}
	}

	onMounted(() => {
		fetchStatistics()
		// 每30秒刷新一次
		refreshTimer = setInterval(fetchStatistics, 30000)
	})

	onUnmounted(() => {
		if (refreshTimer) {
			clearInterval(refreshTimer)
			refreshTimer = null
		}
	})
</script>

<style scoped lang="less">
	.stat-card {
		border-radius: 8px;
		box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
		transition: all 0.3s ease;
		background: #fff;

		&:hover {
			box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
			transform: translateY(-2px);
		}

		:deep(.ant-statistic-title) {
			font-size: 14px;
			color: rgba(0, 0, 0, 0.45);
			margin-bottom: 8px;
		}

		:deep(.ant-statistic-content) {
			font-size: 24px;
			font-weight: 600;
		}

		.stat-icon {
			font-size: 20px;
			margin-right: 8px;
		}

		.stat-percent {
			font-size: 14px;
			margin-left: 8px;
			color: #52c41a;
		}

		.stat-unit {
			font-size: 14px;
			margin-left: 4px;
		}
	}

	.stat-card-primary {
		.stat-icon {
			color: #1890ff;
		}

		:deep(.ant-statistic-content) {
			color: #1890ff;
		}
	}

	.stat-card-success {
		.stat-icon {
			color: #52c41a;
		}

		:deep(.ant-statistic-content) {
			color: #52c41a;
		}
	}

	.stat-card-warning {
		.stat-icon {
			color: #faad14;
		}

		:deep(.ant-statistic-content) {
			color: #faad14;
		}
	}

	.stat-card-info {
		.stat-icon {
			color: #13c2c2;
		}

		:deep(.ant-statistic-content) {
			color: #13c2c2;
		}
	}
</style>
