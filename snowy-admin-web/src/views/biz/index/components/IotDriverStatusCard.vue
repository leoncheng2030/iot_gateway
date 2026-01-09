<template>
	<a-card title="驱动运行状态" :bordered="false" class="driver-status-card">
		<template #extra>
			<a-button type="link" size="small" @click="goToDriverManage">管理驱动</a-button>
		</template>

		<div class="driver-overview">
			<div class="overview-item">
				<div class="overview-icon" style="background: #e6f7ff">
					<deployment-unit-outlined style="color: #1890ff; font-size: 24px" />
				</div>
				<div class="overview-content">
					<div class="overview-value">{{ driverStatus.total }}</div>
					<div class="overview-label">驱动总数</div>
				</div>
			</div>

			<div class="overview-item">
				<div class="overview-icon" style="background: #f6ffed">
					<play-circle-outlined style="color: #52c41a; font-size: 24px" />
				</div>
				<div class="overview-content">
					<div class="overview-value" style="color: #52c41a">{{ driverStatus.running }}</div>
					<div class="overview-label">运行中</div>
				</div>
			</div>

			<div class="overview-item">
				<div class="overview-icon" style="background: #fff2e8">
					<pause-circle-outlined style="color: #faad14; font-size: 24px" />
				</div>
				<div class="overview-content">
					<div class="overview-value" style="color: #faad14">{{ driverStatus.stopped }}</div>
					<div class="overview-label">已停止</div>
				</div>
			</div>

			<div class="overview-item">
				<div class="overview-icon" style="background: #fff1f0">
					<exclamation-circle-outlined style="color: #ff4d4f; font-size: 24px" />
				</div>
				<div class="overview-content">
					<div class="overview-value" style="color: #ff4d4f">{{ driverStatus.error }}</div>
					<div class="overview-label">异常</div>
				</div>
			</div>
		</div>

		<a-divider style="margin: 16px 0" />

		<div class="driver-extra-info">
			<a-row :gutter="16">
				<a-col :span="12">
					<div class="info-item">
						<clock-circle-outlined class="info-icon" />
						<div class="info-content">
							<div class="info-label">最近启动时间</div>
							<div class="info-value">{{ driverInfo.lastStartTime }}</div>
						</div>
					</div>
				</a-col>
				<a-col :span="12">
					<div class="info-item">
						<thunderbolt-outlined class="info-icon" />
						<div class="info-content">
							<div class="info-label">当前通信速率</div>
							<div class="info-value">{{ driverInfo.communicationRate }}条/秒</div>
						</div>
					</div>
				</a-col>
			</a-row>
		</div>
	</a-card>
</template>

<script setup>
	import { ref, onMounted, onUnmounted } from 'vue'
	import { message } from 'ant-design-vue'
	import { useRouter } from 'vue-router'
	import iotStatisticsApi from '@/api/iot/iotStatisticsApi'

	const router = useRouter()

	const driverStatus = ref({
		total: 0,
		running: 0,
		stopped: 0,
		error: 0
	})

	const driverInfo = ref({
		lastStartTime: '--',
		communicationRate: 0
	})

	const driverList = ref([])

	let refreshTimer = null

	// 获取驱动状态数据
	const fetchDriverStatus = async () => {
		try {
			const res = await iotStatisticsApi.getDriverStatistics()
			if (res) {
				driverStatus.value = {
					total: res.total || 0,
					running: res.running || 0,
					stopped: res.stopped || 0,
					error: res.error || 0
				}
				driverInfo.value = {
					lastStartTime: res.lastStartTime || '--',
					communicationRate: res.communicationRate || 0
				}
			}
		} catch (error) {
			console.error('获取驱动状态失败:', error)
			message.error('获取驱动状态失败')
		}
	}

	// 跳转到驱动管理
	const goToDriverManage = () => {
		router.push('/iot/driver')
	}

	onMounted(() => {
		fetchDriverStatus()
		// 每30秒刷新一次
		refreshTimer = setInterval(fetchDriverStatus, 30000)
	})

	onUnmounted(() => {
		if (refreshTimer) {
			clearInterval(refreshTimer)
			refreshTimer = null
		}
	})
</script>

<style scoped lang="less">
	.driver-status-card {
		height: 100%;

		:deep(.ant-card-body) {
			padding: 20px;
		}
	}

	.driver-overview {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 12px;

		.overview-item {
			display: flex;
			align-items: center;
			padding: 12px;
			background: #fafafa;
			border-radius: 8px;
			transition: all 0.3s ease;

			&:hover {
				background: #f0f0f0;
				transform: translateY(-2px);
				box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
			}

			.overview-icon {
				width: 48px;
				height: 48px;
				display: flex;
				align-items: center;
				justify-content: center;
				border-radius: 8px;
				margin-right: 12px;
			}

			.overview-content {
				flex: 1;

				.overview-value {
					font-size: 24px;
					font-weight: 600;
					color: #1890ff;
					line-height: 1;
					margin-bottom: 4px;
				}

				.overview-label {
					font-size: 12px;
					color: rgba(0, 0, 0, 0.45);
				}
			}
		}
	}

	.driver-extra-info {
		margin-top: 8px;

		.info-item {
			display: flex;
			align-items: center;
			padding: 12px;
			background: #fafafa;
			border-radius: 6px;
			transition: all 0.3s ease;

			&:hover {
				background: #f0f0f0;
			}

			.info-icon {
				font-size: 20px;
				color: #1890ff;
				margin-right: 12px;
			}

			.info-content {
				flex: 1;

				.info-label {
					font-size: 12px;
					color: rgba(0, 0, 0, 0.45);
					margin-bottom: 4px;
				}

				.info-value {
					font-size: 14px;
					font-weight: 500;
					color: rgba(0, 0, 0, 0.85);
				}
			}
		}
	}
</style>
