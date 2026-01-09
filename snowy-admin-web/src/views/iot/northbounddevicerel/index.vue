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
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotNorthboundDeviceRelAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotNorthboundDeviceRelImport')">
                        <template #icon><import-outlined /></template>
                        <span>导入</span>
                    </a-button>
                    <a-button @click="exportData" v-if="hasPerm('iotNorthboundDeviceRelExport')">
                        <template #icon><export-outlined /></template>
                        <span>导出</span>
                    </a-button>
					<xn-batch-button
						v-if="hasPerm('iotNorthboundDeviceRelBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotNorthboundDeviceRel"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotNorthboundDeviceRelEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotNorthboundDeviceRelEdit', 'iotNorthboundDeviceRelDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotNorthboundDeviceRel(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotNorthboundDeviceRelDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
</template>

<script setup name="northbounddevicerel">
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotNorthboundDeviceRelApi from '@/api/iot/iotNorthboundDeviceRelApi'
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
			title: '设备ID(为NULL表示推送所有设备)',
			dataIndex: 'deviceId'
		},
		{
			title: '产品ID(为NULL表示不限产品)',
			dataIndex: 'productId'
		},
		{
			title: '设备分组ID',
			dataIndex: 'deviceGroupId'
		},
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotNorthboundDeviceRelEdit', 'iotNorthboundDeviceRelDelete'])) {
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
		return iotNorthboundDeviceRelApi.iotNorthboundDeviceRelPage(parameter).then((data) => {
			return data
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotNorthboundDeviceRel = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotNorthboundDeviceRelApi.iotNorthboundDeviceRelDelete(params).then(() => {
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
            iotNorthboundDeviceRelApi.iotNorthboundDeviceRelExport(params).then((res) => {
                downloadUtil.resultDownload(res)
            })
        } else {
            iotNorthboundDeviceRelApi.iotNorthboundDeviceRelExport([]).then((res) => {
                downloadUtil.resultDownload(res)
            })
        }
    }
	// 批量删除
	const deleteBatchIotNorthboundDeviceRel = (params) => {
		iotNorthboundDeviceRelApi.iotNorthboundDeviceRelDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
</script>
