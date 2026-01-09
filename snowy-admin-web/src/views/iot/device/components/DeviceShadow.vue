<template>
	<a-table
		:columns="deviceShadowColumns"
		:data-source="deviceShadowList"
		:pagination="false"
		:loading="deviceShadowLoading"
		size="middle"
	>
		<template #bodyCell="{ column, record }">
			<template v-if="column.dataIndex === 'desired'">
				<a-typography-text v-if="record.desired !== '-'" copyable>
					{{ record.desired }}
				</a-typography-text>
				<span v-else style="color: #999">-</span>
			</template>
			<template v-if="column.dataIndex === 'reported'">
				<a-typography-text v-if="record.reported !== '-'" copyable>
					{{ record.reported }}
				</a-typography-text>
				<span v-else style="color: #999">-</span>
			</template>
		</template>
	</a-table>
</template>

<script setup>
	defineProps({
		deviceShadowList: {
			type: Array,
			default: () => []
		},
		deviceShadowLoading: {
			type: Boolean,
			default: false
		}
	})

	const deviceShadowColumns = [
		{ title: '属性名称', dataIndex: 'propertyName', width: 150 },
		{ title: '期望值', dataIndex: 'desired', width: 200 },
		{ title: '上报值', dataIndex: 'reported', width: 200 },
		{ title: '版本号', dataIndex: 'version', width: 100 },
		{ title: '更新时间', dataIndex: 'updateTime', width: 180 }
	]
</script>
