<template>
	<a-card title="实时告警" :bordered="false" class="alarm-list-card">
		<template #extra>
			<a-badge :count="alarmCount" :overflow-count="99">
				<a-button type="link" size="small" @click="goToAlarmManage">查看全部</a-button>
			</a-badge>
		</template>

		<div class="alarm-list">
			<div v-for="alarm in alarmList" :key="alarm.id" class="alarm-item" :class="`alarm-${alarm.level}`">
				<div class="alarm-icon">
					<warning-outlined v-if="alarm.level === 'high'" />
					<exclamation-circle-outlined v-else-if="alarm.level === 'medium'" />
					<info-circle-outlined v-else />
				</div>
				<div class="alarm-content">
					<div class="alarm-title">{{ alarm.deviceName }} - {{ alarm.message }}</div>
					<div class="alarm-time">{{ alarm.time }}</div>
				</div>
				<div class="alarm-actions">
					<a-button type="text" size="small" @click="handleAlarm(alarm)">处理</a-button>
				</div>
			</div>

			<a-empty v-if="alarmList.length === 0" description="暂无告警" :image="simpleImage" />
		</div>
	</a-card>
</template>

<script setup>
	import { ref, computed, onMounted, onUnmounted } from 'vue'
	import { message, Empty } from 'ant-design-vue'
	import { useRouter } from 'vue-router'
	import iotStatisticsApi from '@/api/iot/iotStatisticsApi'

	const router = useRouter()
	const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

	const alarmList = ref([])

	const alarmCount = computed(() => alarmList.value.length)

	let refreshTimer = null

	// 获取告警列表
	const fetchAlarmList = async () => {
		try {
			const res = await iotStatisticsApi.getAlarmStatistics()
			if (res && res.list) {
				alarmList.value = res.list
			}
		} catch (error) {
			console.error('获取告警列表失败:', error)
			message.error('获取告警列表失败')
		}
	}

	// 处理告警
	const handleAlarm = (alarm) => {
		message.info(`处理告警: ${alarm.message}`)
		// TODO: 实现告警处理逻辑
	}

	// 跳转到告警管理
	const goToAlarmManage = () => {
		router.push('/iot/alarm')
	}

	onMounted(() => {
		fetchAlarmList()
		// 每10秒刷新一次
		refreshTimer = setInterval(fetchAlarmList, 10000)
	})

	onUnmounted(() => {
		if (refreshTimer) {
			clearInterval(refreshTimer)
			refreshTimer = null
		}
	})
</script>

<style scoped lang="less">
	.alarm-list-card {
		height: 100%;

		:deep(.ant-card-body) {
			padding: 20px;
		}
	}

	.alarm-list {
		max-height: 320px;
		overflow-y: auto;

		&::-webkit-scrollbar {
			width: 6px;
		}

		&::-webkit-scrollbar-thumb {
			background: rgba(0, 0, 0, 0.1);
			border-radius: 3px;
		}
	}

	.alarm-item {
		display: flex;
		align-items: flex-start;
		padding: 12px;
		margin-bottom: 8px;
		background: #fafafa;
		border-radius: 4px;
		border-left: 3px solid transparent;
		transition: all 0.3s ease;

		&:hover {
			background: #f0f0f0;
		}

		&.alarm-high {
			background: #fff2f0;
			border-left-color: #ff4d4f;

			.alarm-icon {
				color: #ff4d4f;
			}
		}

		&.alarm-medium {
			background: #fffbe6;
			border-left-color: #faad14;

			.alarm-icon {
				color: #faad14;
			}
		}

		&.alarm-low {
			background: #f0f5ff;
			border-left-color: #1890ff;

			.alarm-icon {
				color: #1890ff;
			}
		}
	}

	.alarm-icon {
		font-size: 20px;
		margin-right: 12px;
		margin-top: 2px;
	}

	.alarm-content {
		flex: 1;
		min-width: 0;

		.alarm-title {
			font-size: 14px;
			font-weight: 500;
			margin-bottom: 4px;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}

		.alarm-time {
			font-size: 12px;
			color: rgba(0, 0, 0, 0.45);
		}
	}

	.alarm-actions {
		flex-shrink: 0;
		margin-left: 8px;
	}
</style>
