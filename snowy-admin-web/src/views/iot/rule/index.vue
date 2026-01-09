<template>
	<a-card :bordered="false" style="width: 100%">
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="规则名称" name="ruleName">
						<a-input v-model:value="searchFormState.ruleName" placeholder="请输入规则名称" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="规则类型" name="ruleType">
						<a-select
							v-model:value="searchFormState.ruleType"
							placeholder="请选择规则类型"
							:options="ruleTypeOptions"
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="searchFormState.status" placeholder="请选择状态" :options="statusOptions" />
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
			<template #operator>
				<a-space>
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotRuleAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotRuleImport')">
						<template #icon><import-outlined /></template>
						<span>导入</span>
					</a-button>
					<a-button @click="exportData" v-if="hasPerm('iotRuleExport')">
						<template #icon><export-outlined /></template>
						<span>导出</span>
					</a-button>
					<xn-batch-button
						v-if="hasPerm('iotRuleBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotRule"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'ruleType'">
					{{ $TOOL.dictTypeData('RULE_TYPE', record.ruleType) }}
				</template>
				<template v-if="column.dataIndex === 'triggerCondition'">
					<a-tooltip :title="formatTriggerCondition(record.triggerCondition)">
						<a-typography-text
							:ellipsis="{ tooltip: true }"
							style="max-width: 200px"
							:content="formatTriggerCondition(record.triggerCondition)"
						/>
					</a-tooltip>
				</template>
				<template v-if="column.dataIndex === 'actionCount'">
					<a-badge :count="getActionCount(record.actions)" :number-style="{ backgroundColor: '#52c41a' }" />
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-badge
						:status="record.status === 'ENABLE' ? 'success' : 'default'"
						:text="$TOOL.dictTypeData('COMMON_STATUS', record.status)"
					/>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="workflowRef.onOpen(record)">规则编排</a>
						<a-divider type="vertical" v-if="hasPerm('iotRuleDetail')" />
						<a @click="detailRef.onOpen(record)" v-if="hasPerm('iotRuleDetail')">详情</a>
						<a-divider type="vertical" v-if="hasPerm(['iotRuleDetail', 'iotRuleEdit'], 'and')" />
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotRuleEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotRuleEdit', 'iotRuleDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotRule(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotRuleDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
	<Detail ref="detailRef" />
	<Workflow ref="workflowRef" @successful="tableRef.refresh()" />
</template>

<script setup name="rule">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import Detail from './detail.vue'
	import ImportModel from './importModel.vue'
	import Workflow from './workflow.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotRuleApi from '@/api/iot/iotRuleApi'
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const detailRef = ref()
	const workflowRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '规则名称',
			dataIndex: 'ruleName',
			width: 180
		},
		{
			title: '规则描述',
			dataIndex: 'ruleDesc',
			ellipsis: true
		},
		{
			title: '规则类型',
			dataIndex: 'ruleType',
			width: 100,
			align: 'center'
		},
		{
			title: '触发条件',
			dataIndex: 'triggerCondition',
			width: 200,
			ellipsis: true
		},
		{
			title: '执行动作数',
			dataIndex: 'actionCount',
			width: 100,
			align: 'center'
		},
		{
			title: '状态',
			dataIndex: 'status',
			width: 100,
			align: 'center'
		},
		{
			title: '创建时间',
			dataIndex: 'createTime',
			width: 180
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotRuleDetail', 'iotRuleEdit', 'iotRuleDelete'])) {
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
		const searchFormParam = cloneDeep(searchFormState.value)
		return iotRuleApi.iotRulePage(Object.assign(parameter, searchFormParam)).then((data) => {
			return data
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotRule = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotRuleApi.iotRuleDelete(params).then(() => {
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
			iotRuleApi.iotRuleExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotRuleApi.iotRuleExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}
	// 批量删除
	const deleteBatchIotRule = (params) => {
		iotRuleApi.iotRuleDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
	// 格式化触发条件
	const formatTriggerCondition = (condition) => {
		if (!condition) return '-'
		try {
			const obj = JSON.parse(condition)
			if (obj.property && obj.operator && obj.value !== undefined) {
				return `${obj.property} ${obj.operator} ${obj.value}`
			}
			return condition.substring(0, 50) + '...'
		} catch (e) {
			return condition.substring(0, 50) + '...'
		}
	}
	// 获取执行动作数量
	const getActionCount = (actions) => {
		if (!actions) return 0
		try {
			const arr = JSON.parse(actions)
			return Array.isArray(arr) ? arr.length : 0
		} catch (e) {
			return 0
		}
	}
	const ruleTypeOptions = tool.dictList('RULE_TYPE')
	const statusOptions = tool.dictList('COMMON_STATUS')
</script>
