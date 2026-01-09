<template>
	<a-card :bordered="false" style="width: 100%">
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			:alert="options.alert.show"
			bordered
			:row-key="(record) => record.id"
			:tool-config="toolConfig"
			:row-selection="options.rowSelection"
			:scroll="{ x: 'max-content' }"
		>
			<template #operator class="table-operator">
				<a-space>
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotNorthboundStatisticsAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotNorthboundStatisticsImport')">
                        <template #icon><import-outlined /></template>
                        <span>导入</span>
                    </a-button>
                    <a-button @click="exportData" v-if="hasPerm('iotNorthboundStatisticsExport')">
                        <template #icon><export-outlined /></template>
                        <span>导出</span>
                    </a-button>
					<xn-batch-button
						v-if="hasPerm('iotNorthboundStatisticsBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotNorthboundStatistics"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotNorthboundStatisticsEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotNorthboundStatisticsEdit', 'iotNorthboundStatisticsDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotNorthboundStatistics(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotNorthboundStatisticsDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
</template>

<script setup name="northboundstatistics">
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotNorthboundStatisticsApi from '@/api/iot/iotNorthboundStatisticsApi'
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '推送配置ID',
			dataIndex: 'configId'
		},
		{
			title: '统计日期',
			dataIndex: 'statDate'
		},
		{
			title: '总推送次数',
			dataIndex: 'totalCount'
		},
		{
			title: '成功次数',
			dataIndex: 'successCount'
		},
		{
			title: '失败次数',
			dataIndex: 'failedCount'
		},
		{
			title: '平均耗时(毫秒)',
			dataIndex: 'avgCostTime'
		},
		{
			title: '最大耗时(毫秒)',
			dataIndex: 'maxCostTime'
		},
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotNorthboundStatisticsEdit', 'iotNorthboundStatisticsDelete'])) {
		columns.push({
			title: '操作',
			dataIndex: 'action',
			align: 'center',
			fixed: 'right'
		})
	}
	const selectedRowKeys = ref([])
	// 列表选择配置
	const options = {
		alert: {
			show: true,
			clear: () => {
				selectedRowKeys.value = ref([])
			}
		},
		rowSelection: {
			onChange: (selectedRowKey, selectedRows) => {
				selectedRowKeys.value = selectedRowKey
			}
		}
	}
	const loadData = (parameter) => {
		return iotNorthboundStatisticsApi.iotNorthboundStatisticsPage(parameter).then((data) => {
			return data
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotNorthboundStatistics = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotNorthboundStatisticsApi.iotNorthboundStatisticsDelete(params).then(() => {
			tableRef.value.refresh(true)
		})
	}
	// 导出
    const exportData = () => {
        if (selectedRowKeys.value.length > 0) {
            const params = selectedRowKeys.value.map((m) => {
                return {
                    id: m
                }
            })
            iotNorthboundStatisticsApi.iotNorthboundStatisticsExport(params).then((res) => {
                downloadUtil.resultDownload(res)
            })
        } else {
            iotNorthboundStatisticsApi.iotNorthboundStatisticsExport([]).then((res) => {
                downloadUtil.resultDownload(res)
            })
        }
    }
	// 批量删除
	const deleteBatchIotNorthboundStatistics = (params) => {
		iotNorthboundStatisticsApi.iotNorthboundStatisticsDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
</script>
