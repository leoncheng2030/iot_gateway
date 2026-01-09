<template>
	<div>
		<a-card size="small" style="margin-bottom: 16px">
			<a-space>
				<a-tag color="orange">事件日志</a-tag>
				<a-tag v-if="recentEvents.length > 0" color="blue">共 {{ recentEvents.length }} 条记录</a-tag>
			</a-space>
		</a-card>

		<a-empty v-if="!thingModelEvents || thingModelEvents.length === 0" description="该产品暂无事件定义" />
		<div v-else>
			<a-card size="small" title="事件记录">
				<a-empty v-if="recentEvents.length === 0" description="暂无事件记录" />
				<a-table v-else :columns="eventColumns" :data-source="recentEvents" :pagination="{ pageSize: 10 }" size="small">
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'eventData'">
							<a-typography-text v-if="record.eventData" copyable code>
								{{ record.eventData }}
							</a-typography-text>
							<span v-else>-</span>
						</template>
					</template>
				</a-table>
			</a-card>
		</div>
	</div>
</template>

<script setup>
	defineProps({
		thingModelEvents: {
			type: Array,
			default: () => []
		},
		recentEvents: {
			type: Array,
			default: () => []
		}
	})

	const eventColumns = [
		{ title: '事件名称', dataIndex: 'eventName', width: 150 },
		{ title: '事件数据', dataIndex: 'eventData', width: 300, ellipsis: true },
		{ title: '触发时间', dataIndex: 'timestamp', width: 180 }
	]
</script>
