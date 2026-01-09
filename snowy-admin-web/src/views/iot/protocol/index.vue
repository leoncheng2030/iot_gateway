<template>
	<a-card :bordered="false" style="width: 100%">
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="协议名称" name="protocolName">
						<a-input v-model:value="searchFormState.protocolName" placeholder="请输入协议名称" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="协议类型" name="protocolType">
						<a-select v-model:value="searchFormState.protocolType" placeholder="请选择协议类型" :options="protocolTypeOptions" />
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
			<template #operator class="table-operator">
				<a-space>
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotProtocolAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotProtocolImport')">
                        <template #icon><import-outlined /></template>
                        <span>导入</span>
                    </a-button>
                    <a-button @click="exportData" v-if="hasPerm('iotProtocolExport')">
                        <template #icon><export-outlined /></template>
                        <span>导出</span>
                    </a-button>
					<xn-batch-button
						v-if="hasPerm('iotProtocolBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotProtocol"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'protocolType'">
					{{ $TOOL.dictTypeData('PROTOCOL_TYPE', record.protocolType) }}
				</template>
				<template v-if="column.dataIndex === 'status'">
					<a-space>
						<a-badge v-if="record.runStatus" status="processing" text="运行中" />
						<a-badge v-else-if="record.status === 'ENABLE'" status="default" text="已停止" />
						<a-badge v-else status="error" text="已禁用" />
					</a-space>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a-button 
							v-if="!record.runStatus && record.status === 'ENABLE'" 
							type="primary" 
							size="small" 
							@click="startProtocol(record)"
						>
							启动
						</a-button>
						<a-button 
							v-if="record.runStatus" 
							danger 
							size="small" 
							@click="stopProtocol(record)"
						>
							停止
						</a-button>
						<a-button 
							v-if="record.runStatus" 
							size="small" 
							@click="restartProtocol(record)"
						>
							重启
						</a-button>
						<a-divider type="vertical" v-if="hasPerm(['iotProtocolEdit', 'iotProtocolDelete'], 'or')" />
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotProtocolEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotProtocolEdit', 'iotProtocolDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotProtocol(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotProtocolDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
</template>

<script setup name="protocol">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotProtocolApi from '@/api/iot/iotProtocolApi'
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '协议名称',
			dataIndex: 'protocolName'
		},
		{
			title: '协议类型',
			dataIndex: 'protocolType'
		},
		{
			title: '协议端口',
			dataIndex: 'protocolPort'
		},
		{
			title: '状态',
			dataIndex: 'status'
		},
		{
			title: '备注',
			dataIndex: 'remark',
			width: 200,
			ellipsis: true
		},

	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotProtocolEdit', 'iotProtocolDelete'])) {
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
		return iotProtocolApi.iotProtocolPage(Object.assign(parameter, searchFormParam)).then(async (data) => {
			// 获取每个协议的运行状态
			if (data.records && data.records.length > 0) {
				for (let row of data.records) {
					try {
						const statusRes = await iotProtocolApi.iotProtocolStatus({ id: row.id })
						// 判断返回格式：可能是 {data: true} 或直接 true
						row.runStatus = statusRes.data !== undefined ? statusRes.data : statusRes
					} catch (e) {
						row.runStatus = false
					}
				}
			}
			return data
		})
	}
	// 启动协议
	const startProtocol = (record) => {
		iotProtocolApi.iotProtocolStart({ id: record.id }).then(() => {
			tableRef.value.refresh(true)
		})
	}
	// 停止协议
	const stopProtocol = (record) => {
		iotProtocolApi.iotProtocolStop({ id: record.id }).then(() => {
			tableRef.value.refresh(true)
		})
	}
	// 重启协议
	const restartProtocol = (record) => {
		iotProtocolApi.iotProtocolRestart({ id: record.id }).then(() => {
			tableRef.value.refresh(true)
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotProtocol = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotProtocolApi.iotProtocolDelete(params).then(() => {
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
            iotProtocolApi.iotProtocolExport(params).then((res) => {
                downloadUtil.resultDownload(res)
            })
        } else {
            iotProtocolApi.iotProtocolExport([]).then((res) => {
                downloadUtil.resultDownload(res)
            })
        }
    }
	// 批量删除
	const deleteBatchIotProtocol = (params) => {
		iotProtocolApi.iotProtocolDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
	const protocolTypeOptions = tool.dictList('PROTOCOL_TYPE')
	const statusOptions = tool.dictList('COMMON_STATUS')
</script>
