<!-- eslint-disable vue/no-useless-template-attributes -->
<template>
	<a-card :bordered="false" style="width: 100%">
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="配置名称" name="name">
						<a-input v-model:value="searchFormState.name" placeholder="请输入配置名称" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item>
						<a-space>
							<a-button type="primary" @click="tableRef.refresh(true)">
								<template #icon><SearchOutlined /></template>
								查询
							</a-button>
							<a-button @click="reset">
								<template #icon><redo-outlined /></template>
								重置
							</a-button>
						</a-space>
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
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
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotNorthboundConfigAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotNorthboundConfigImport')">
						<template #icon><import-outlined /></template>
						<span>导入</span>
					</a-button>
					<a-button @click="exportData" v-if="hasPerm('iotNorthboundConfigExport')">
						<template #icon><export-outlined /></template>
						<span>导出</span>
					</a-button>
					<xn-batch-button
						v-if="hasPerm('iotNorthboundConfigBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotNorthboundConfig"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'pushType'">
					<a-tag :color="record.pushType === 'WEBHOOK' ? 'blue' : 'green'">
						{{ $TOOL.dictTypeData('NORTHBOUND_PUSH_TYPE', record.pushType) }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'pushTrigger'">
					<div style="display: flex; flex-wrap: wrap; gap: 4px">
						<a-tag
							v-for="trigger in (record.pushTrigger || '').split(',').filter((t) => t)"
							:key="trigger"
							style="margin: 0"
						>
							{{ getTriggerLabel(trigger) }}
						</a-tag>
					</div>
				</template>
				<template v-if="column.dataIndex === 'enabled'">
					<a-badge
						:status="record.enabled === 'ENABLE' ? 'success' : 'default'"
						:text="$TOOL.dictTypeData('NORTHBOUND_PUSH_STATUS', record.enabled)"
					/>
				</template>
				<template v-if="column.dataIndex === 'retryTimeout'">
					{{ record.retryTimes || 3 }}次 / {{ record.timeout || 5000 }}ms
				</template>
				<template v-if="column.dataIndex === 'statistics'">
					<StatisticsCell :stats="record.todayStats" @click="showStatistics(record)" />
				</template>
				<template v-if="column.dataIndex === 'config'">
					<a @click="showConfigDetail(record)">
						<SettingOutlined />
					</a>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotNorthboundConfigEdit')">编辑</a>
						<a-divider
							type="vertical"
							v-if="hasPerm(['iotNorthboundConfigEdit', 'iotNorthboundConfigDelete'], 'and')"
						/>
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotNorthboundConfig(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotNorthboundConfigDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
	<ConfigDetailModal v-model:open="configDetailVisible" :config="currentConfigDetail" />
	<StatisticsModal v-model:open="statisticsVisible" :config-id="currentConfigId" />
</template>

<script setup name="northboundconfig">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import StatisticsCell from './components/StatisticsCell.vue'
	import ConfigDetailModal from './components/ConfigDetailModal.vue'
	import StatisticsModal from './components/StatisticsModal.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotNorthboundConfigApi from '@/api/iot/iotNorthboundConfigApi'
	import iotNorthboundStatisticsApi from '@/api/iot/iotNorthboundStatisticsApi'
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '配置名称',
			dataIndex: 'name',
			width: 180
		},
		{
			title: '推送类型',
			dataIndex: 'pushType',
			width: 100
		},
		{
			title: '目标地址',
			dataIndex: 'targetUrl',
			width: 250,
			ellipsis: true
		},
		{
			title: '推送时机',
			dataIndex: 'pushTrigger',
			width: 200
		},
		{
			title: '状态',
			dataIndex: 'enabled',
			width: 80
		},
		{
			title: '今日推送',
			dataIndex: 'statistics',
			width: 100,
			align: 'center'
		},
		{
			title: '重试/超时',
			dataIndex: 'retryTimeout',
			width: 100
		},
		{
			title: '配置',
			dataIndex: 'config',
			width: 80,
			align: 'center'
		},
		{
			title: '备注',
			dataIndex: 'remark',
			width: 200,
			ellipsis: true
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotNorthboundConfigEdit', 'iotNorthboundConfigDelete'])) {
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
	const loadData = async (parameter) => {
		const searchFormParam = cloneDeep(searchFormState.value)
		const data = await iotNorthboundConfigApi.iotNorthboundConfigPage(Object.assign(parameter, searchFormParam))
		// 加载今日统计数据
		if (data.records && data.records.length > 0) {
			const today = new Date().toISOString().split('T')[0]
			try {
				const statsData = await iotNorthboundStatisticsApi.iotNorthboundStatisticsPage({
					current: 1,
					size: 1000,
					statDate: today
				})
				if (statsData.records) {
					const statsMap = {}
					statsData.records.forEach((stat) => {
						statsMap[stat.configId] = stat
					})
					data.records.forEach((row) => {
						row.todayStats = statsMap[row.id] || null
					})
				}
			} catch (e) {
				// 统计数据加载失败，不影响主数据展示
			}
		}
		return data
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotNorthboundConfig = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotNorthboundConfigApi.iotNorthboundConfigDelete(params).then(() => {
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
			iotNorthboundConfigApi.iotNorthboundConfigExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotNorthboundConfigApi.iotNorthboundConfigExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}
	// 批量删除
	const deleteBatchIotNorthboundConfig = (params) => {
		iotNorthboundConfigApi.iotNorthboundConfigDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}

	// 显示配置详情
	const configDetailVisible = ref(false)
	const currentConfigDetail = ref({})
	const showConfigDetail = (record) => {
		currentConfigDetail.value = record
		configDetailVisible.value = true
	}

	// 显示统计详情
	const statisticsVisible = ref(false)
	const currentConfigId = ref('')
	const showStatistics = (record) => {
		currentConfigId.value = record.id
		statisticsVisible.value = true
	}

	// 获取触发时机标签
	const getTriggerLabel = (trigger) => {
		const labels = {
			PROPERTY_REPORT: '属性上报',
			EVENT: '事件',
			STATUS_CHANGE: '状态变化',
			MANUAL: '手动',
			SCHEDULE: '定时'
		}
		return labels[trigger] || trigger
	}
</script>
