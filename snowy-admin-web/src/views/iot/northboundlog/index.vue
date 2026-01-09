<template>
	<a-card :bordered="false" style="width: 100%">
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="推送配置ID" name="configId">
						<a-input v-model:value="searchFormState.configId" placeholder="请输入推送配置ID" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="配置名称(冗余)" name="configName">
						<a-input v-model:value="searchFormState.configName" placeholder="请输入配置名称(冗余)" />
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
					<a-button @click="exportData" v-if="hasPerm('iotNorthboundLogExport')">
						<template #icon><export-outlined /></template>
						<span>导出</span>
					</a-button>
					<xn-batch-button
						v-if="hasPerm('iotNorthboundLogBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotNorthboundLog"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="viewDetail(record)" v-if="hasPerm('iotNorthboundLogDetail')">查看</a>
						<a-divider type="vertical" v-if="hasPerm(['iotNorthboundLogDetail', 'iotNorthboundLogDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotNorthboundLog(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotNorthboundLogDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
				<template v-else-if="column.dataIndex === 'payload'">
					<div class="payload-cell">
						<a-tooltip placement="topLeft">
							<template #title>
								<pre style="margin: 0; max-height: 400px; overflow: auto">{{ formatJson(record.payload) }}</pre>
							</template>
							<span class="payload-preview">{{ getPayloadPreview(record.payload) }}</span>
						</a-tooltip>
						<a-button type="link" size="small" @click="showPayloadDetail(record)">详情</a-button>
					</div>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
</template>

<script setup name="northboundlog">
	import { h } from 'vue'
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotNorthboundLogApi from '@/api/iot/iotNorthboundLogApi'
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '配置名称',
			dataIndex: 'configName',
			width: 150
		},
		{
			title: '设备名称',
			dataIndex: 'deviceName',
			width: 150
		},
		{
			title: '推送状态',
			dataIndex: 'status',
			width: 100,
			customRender: ({ text }) => {
				const statusMap = {
					SUCCESS: { text: '成功', color: '#52c41a' },
					FAILED: { text: '失败', color: '#ff4d4f' },
					RETRY: { text: '重试', color: '#faad14' }
				}
				const status = statusMap[text] || { text, color: '#999' }
				return h('span', { style: { color: status.color } }, status.text)
			}
		},
		{
			title: '推送时间',
			dataIndex: 'pushTime',
			width: 160
		},
		{
			title: '耗时(ms)',
			dataIndex: 'costTime',
			width: 100
		},
		{
			title: '响应状态码',
			dataIndex: 'responseCode',
			width: 120
		},
		{
			title: '错误信息',
			dataIndex: 'errorMessage',
			width: 200,
			ellipsis: true
		},
		{
			title: '重试次数',
			dataIndex: 'retryCount',
			width: 100
		},
		{
			title: '推送类型',
			dataIndex: 'pushType',
			width: 100
		},
		{
			title: '目标地址',
			dataIndex: 'targetUrl',
			width: 200,
			ellipsis: true
		},
		{
			title: '推送数据',
			dataIndex: 'payload',
			width: 250
		},
		{
			title: '响应内容',
			dataIndex: 'responseBody',
			width: 200,
			ellipsis: true
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotNorthboundLogDetail', 'iotNorthboundLogDelete'])) {
		columns.push({
			title: '操作',
			dataIndex: 'action',
			align: 'center',
			width: 120,
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
		return iotNorthboundLogApi.iotNorthboundLogPage(Object.assign(parameter, searchFormParam)).then((data) => {
			return data
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotNorthboundLog = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotNorthboundLogApi.iotNorthboundLogDelete(params).then(() => {
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
			iotNorthboundLogApi.iotNorthboundLogExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotNorthboundLogApi.iotNorthboundLogExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}
	// 批量删除
	const deleteBatchIotNorthboundLog = (params) => {
		iotNorthboundLogApi.iotNorthboundLogDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
	// 查看详情
	const viewDetail = (record) => {
		formRef.value.onOpen(record)
	}
	// 格式化JSON
	const formatJson = (jsonStr) => {
		if (!jsonStr) return ''
		try {
			const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
			return JSON.stringify(obj, null, 2)
		} catch (e) {
			return jsonStr
		}
	}
	// 获取payload预览文本
	const getPayloadPreview = (payload) => {
		if (!payload) return '-'
		try {
			const obj = typeof payload === 'string' ? JSON.parse(payload) : payload
			// 显示前3个字段
			const keys = Object.keys(obj).slice(0, 3)
			const preview = keys.map((key) => `${key}: ${obj[key]}`).join(', ')
			return keys.length < Object.keys(obj).length ? preview + '...' : preview
		} catch (e) {
			return payload.length > 50 ? payload.substring(0, 50) + '...' : payload
		}
	}
	// 显示推送数据详情
	const showPayloadDetail = (record) => {
		const { Modal } = require('ant-design-vue')
		const jsonContent = formatJson(record.payload)
		Modal.info({
			title: '推送数据详情',
			width: 800,
			content: h(
				'pre',
				{
					style: {
						background: '#f5f5f5',
						padding: '16px',
						borderRadius: '4px',
						maxHeight: '500px',
						overflow: 'auto',
						margin: 0
					}
				},
				jsonContent
			),
			okText: '关闭'
		})
	}
</script>

<style scoped>
	.payload-cell {
		display: flex;
		align-items: center;
		gap: 8px;
	}
	.payload-preview {
		flex: 1;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		color: #666;
		font-size: 12px;
	}
</style>
