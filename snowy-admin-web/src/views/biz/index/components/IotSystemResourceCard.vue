<template>
	<a-card title="系统资源监控" :bordered="false" class="system-resource-card">
		<div class="resource-list">
			<div class="resource-item">
				<div class="resource-header">
					<span class="resource-label">CPU使用率</span>
					<span class="resource-value">{{ systemResource.cpuUsage }}%</span>
				</div>
				<a-progress
					:percent="systemResource.cpuUsage"
					:stroke-color="getProgressColor(systemResource.cpuUsage)"
					:show-info="false"
				/>
			</div>

			<div class="resource-item">
				<div class="resource-header">
					<span class="resource-label">内存使用率</span>
					<span class="resource-value">{{ systemResource.memoryUsage }}%</span>
				</div>
				<a-progress
					:percent="systemResource.memoryUsage"
					:stroke-color="getProgressColor(systemResource.memoryUsage)"
					:show-info="false"
				/>
				<div class="resource-detail">{{ systemResource.memoryUsed }}GB / {{ systemResource.memoryTotal }}GB</div>
			</div>

			<div class="resource-item">
				<div class="resource-header">
					<span class="resource-label">存储空间</span>
					<span class="resource-value">{{ systemResource.diskUsage }}%</span>
				</div>
				<a-progress
					:percent="systemResource.diskUsage"
					:stroke-color="getProgressColor(systemResource.diskUsage)"
					:show-info="false"
				/>
				<div class="resource-detail">{{ systemResource.diskUsed }}GB / {{ systemResource.diskTotal }}GB</div>
			</div>

			<a-divider style="margin: 16px 0" />

			<div class="uptime-info">
				<div class="uptime-item">
					<clock-circle-outlined class="uptime-icon" />
					<div class="uptime-content">
						<div class="uptime-label">系统运行时长</div>
						<div class="uptime-value">{{ systemResource.uptime }}</div>
					</div>
				</div>
			</div>
		</div>
	</a-card>
</template>

<script setup>
	import { ref, onMounted, onUnmounted } from 'vue'
	import { message } from 'ant-design-vue'
	import iotStatisticsApi from '@/api/iot/iotStatisticsApi'

	const systemResource = ref({
		cpuUsage: 0,
		memoryUsage: 0,
		memoryUsed: 0,
		memoryTotal: 0,
		diskUsage: 0,
		diskUsed: 0,
		diskTotal: 0,
		uptime: '0天0小时'
	})

	// 获取进度条颜色
	const getProgressColor = (percent) => {
		if (percent < 60) {
			return '#52c41a'
		} else if (percent < 80) {
			return '#faad14'
		} else {
			return '#ff4d4f'
		}
	}

	let refreshTimer = null

	// 获取系统资源数据
	const fetchSystemResource = async () => {
		try {
			const res = await iotStatisticsApi.getSystemResource()
			if (res) {
				systemResource.value = {
					cpuUsage: res.cpuUsage || 0,
					memoryUsage: res.memoryUsage || 0,
					memoryUsed: res.memoryUsed || 0,
					memoryTotal: res.memoryTotal || 0,
					diskUsage: res.diskUsage || 0,
					diskUsed: res.diskUsed || 0,
					diskTotal: res.diskTotal || 0,
					uptime: res.uptime || '0天0小时'
				}
			}
		} catch (error) {
			console.error('获取系统资源失败:', error)
			message.error('获取系统资源失败')
		}
	}

	onMounted(() => {
		fetchSystemResource()
		// 每5秒刷新一次
		refreshTimer = setInterval(fetchSystemResource, 5000)
	})

	onUnmounted(() => {
		if (refreshTimer) {
			clearInterval(refreshTimer)
			refreshTimer = null
		}
	})
</script>

<style scoped lang="less">
	.system-resource-card {
		height: 100%;

		:deep(.ant-card-body) {
			padding: 20px;
		}
	}

	.resource-list {
		.resource-item {
			margin-bottom: 20px;

			&:last-child {
				margin-bottom: 0;
			}

			.resource-header {
				display: flex;
				justify-content: space-between;
				align-items: center;
				margin-bottom: 8px;

				.resource-label {
					font-size: 14px;
					color: rgba(0, 0, 0, 0.65);
				}

				.resource-value {
					font-size: 16px;
					font-weight: 600;
					color: #1890ff;
				}
			}

			.resource-detail {
				font-size: 12px;
				color: rgba(0, 0, 0, 0.45);
				margin-top: 4px;
			}
		}
	}

	.uptime-info {
		.uptime-item {
			display: flex;
			align-items: center;
			padding: 12px;
			background: #fafafa;
			border-radius: 4px;

			.uptime-icon {
				font-size: 24px;
				color: #1890ff;
				margin-right: 12px;
			}

			.uptime-content {
				flex: 1;

				.uptime-label {
					font-size: 14px;
					color: rgba(0, 0, 0, 0.45);
					margin-bottom: 4px;
				}

				.uptime-value {
					font-size: 18px;
					font-weight: 600;
					color: #1890ff;
				}
			}
		}
	}
</style>
