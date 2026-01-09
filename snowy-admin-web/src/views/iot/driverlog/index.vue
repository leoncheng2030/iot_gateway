<template>
	<a-card :bordered="false" style="width: 100%">
		<!-- 搜索区域 -->
		<a-form ref="searchFormRef" :model="searchFormState" layout="inline" class="mb-3">
			<a-form-item label="驱动名称" name="driverName">
				<a-input
					v-model:value="searchFormState.driverName"
					placeholder="请输入驱动名称"
					allow-clear
					style="width: 200px"
				/>
			</a-form-item>
			<a-form-item label="日志类型" name="logType">
				<a-select v-model:value="searchFormState.logType" placeholder="请选择日志类型" allow-clear style="width: 150px">
					<a-select-option value="INFO">信息</a-select-option>
					<a-select-option value="WARN">警告</a-select-option>
					<a-select-option value="ERROR">错误</a-select-option>
					<a-select-option value="START">启动</a-select-option>
					<a-select-option value="STOP">停止</a-select-option>
				</a-select>
			</a-form-item>
			<a-form-item label="设备标识" name="deviceKey">
				<a-input
					v-model:value="searchFormState.deviceKey"
					placeholder="请输入设备标识"
					allow-clear
					style="width: 200px"
				/>
			</a-form-item>
			<a-form-item>
				<a-button type="primary" @click="tableRef.refresh(true)">查询</a-button>
				<a-button class="ml-2" @click="reset">重置</a-button>
			</a-form-item>
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
					<a-button type="primary" @click="tableRef.refresh()">
						<template #icon><reload-outlined /></template>
						刷新
					</a-button>
					<a-button @click="exportData" v-if="hasPerm('iotDriverLogExport')">
						<template #icon><export-outlined /></template>
						导出
					</a-button>
					<xn-batch-button
						v-if="hasPerm('iotDriverLogBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotDriverLog"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'logType'">
					<a-tag :color="getLogTypeColor(record.logType)">
						{{ $TOOL.dictTypeData('DRIVER_LOG_TYPE', record.logType) }}
					</a-tag>
				</template>
				<template v-if="column.dataIndex === 'createTime'">
					{{ $TOOL.formatDate(record.createTime, 'YYYY-MM-DD HH:mm:ss') }}
				</template>
				<template v-if="column.dataIndex === 'logContent'">
					<a-tooltip>
						<template #title>{{ record.logContent }}</template>
						<div style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
							{{ record.logContent }}
						</div>
					</a-tooltip>
				</template>
				<template v-if="column.dataIndex === 'errorMsg'">
					<a-tooltip v-if="record.errorMsg">
						<template #title>{{ record.errorMsg }}</template>
						<div
							style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #ff4d4f"
						>
							{{ record.errorMsg }}
						</div>
					</a-tooltip>
					<span v-else>-</span>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a-popconfirm title="确定要删除吗?" @confirm="deleteIotDriverLog(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotDriverLogDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
</template>

<script setup name="driverlog">
	import { cloneDeep } from 'lodash-es'
	import ImportModel from './importModel.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotDriverLogApi from '@/api/iot/iotDriverLogApi'

	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }

	// 搜索表单
	const searchFormState = ref({
		driverName: undefined,
		logType: undefined,
		deviceKey: undefined
	})

	const columns = [
		{
			title: '驱动名称',
			dataIndex: 'driverName',
			width: 150
		},
		{
			title: '日志类型',
			dataIndex: 'logType',
			width: 100
		},
		{
			title: '日志内容',
			dataIndex: 'logContent'
		},
		{
			title: '设备标识',
			dataIndex: 'deviceKey',
			width: 150
		},
		{
			title: '错误信息',
			dataIndex: 'errorMsg',
			width: 200
		},
		{
			title: '记录时间',
			dataIndex: 'createTime',
			width: 180,
			sorter: true
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm('iotDriverLogDelete')) {
		columns.push({
			title: '操作',
			dataIndex: 'action',
			align: 'center',
			width: 100,
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
		// 合并搜索条件
		const params = Object.assign({}, parameter, searchFormState.value)
		return iotDriverLogApi.iotDriverLogPage(params).then((data) => {
			return data
		})
	}

	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}

	// 日志类型颜色
	const getLogTypeColor = (logType) => {
		const colorMap = {
			INFO: 'blue',
			WARN: 'orange',
			ERROR: 'red',
			START: 'green',
			STOP: 'default'
		}
		return colorMap[logType] || 'default'
	}
	// 删除
	const deleteIotDriverLog = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotDriverLogApi.iotDriverLogDelete(params).then(() => {
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
			iotDriverLogApi.iotDriverLogExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotDriverLogApi.iotDriverLogExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}
	// 批量删除
	const deleteBatchIotDriverLog = (params) => {
		iotDriverLogApi.iotDriverLogDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
</script>
