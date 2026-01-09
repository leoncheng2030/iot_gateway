<template>
	<a-modal v-model:open="visible" title="推送统计" width="1000px" :footer="null" @cancel="handleClose">
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			bordered
			:row-key="(record) => record.id"
			:tool-config="{ refresh: true, height: false }"
			:pagination="{ pageSize: 10 }"
			:scroll="{ x: 'max-content' }"
		>
		</s-table>
	</a-modal>
</template>

<script setup>
	import iotNorthboundStatisticsApi from '@/api/iot/iotNorthboundStatisticsApi'

	const visible = defineModel('open', { type: Boolean, default: false })
	const props = defineProps({
		configId: {
			type: String,
			default: ''
		}
	})

	const tableRef = ref()

	const columns = [
		{
			title: '统计日期',
			dataIndex: 'statDate',
			width: 120
		},
		{
			title: '总推送次数',
			dataIndex: 'totalCount',
			width: 100
		},
		{
			title: '成功次数',
			dataIndex: 'successCount',
			width: 100
		},
		{
			title: '失败次数',
			dataIndex: 'failedCount',
			width: 100
		},
		{
			title: '成功率',
			dataIndex: 'successRate',
			width: 100,
			customRender: ({ record }) => {
				if (!record.totalCount || record.totalCount === 0) return '0%'
				const rate = ((record.successCount / record.totalCount) * 100).toFixed(2)
				return `${rate}%`
			}
		},
		{
			title: '平均耗时(ms)',
			dataIndex: 'avgCostTime',
			width: 120
		},
		{
			title: '最大耗时(ms)',
			dataIndex: 'maxCostTime',
			width: 120
		}
	]

	const loadData = (parameter) => {
		return iotNorthboundStatisticsApi
			.iotNorthboundStatisticsPage(
				Object.assign(parameter, {
					configId: props.configId
				})
			)
			.then((data) => {
				return data
			})
	}

	const handleClose = () => {
		visible.value = false
	}
</script>
