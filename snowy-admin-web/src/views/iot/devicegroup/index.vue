<template>
	<a-card :bordered="false" class="xn-mb10">
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
					<a-form-item name="searchKey" label="分组名称">
						<a-input v-model:value="searchFormState.searchKey" placeholder="请输入分组名称" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="16" :md="16" :lg="16" :xl="16">
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
	</a-card>
	<a-card :bordered="false">
		<s-table
			ref="tableRef"
			:columns="columns"
			:data="loadData"
			:expand-row-by-click="true"
			:alert="options.alert.show"
			bordered
			:row-key="(record) => record.id"
			:tool-config="toolConfig"
			:row-selection="options.rowSelection"
			:scroll="{ x: 'max-content' }"
		>
			<template #operator>
				<a-space>
					<a-button
						type="primary"
						@click="formRef.onOpen(undefined, searchFormState.parentId)"
						v-if="hasPerm('iotDeviceGroupAdd')"
					>
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotDeviceGroupImport')">
						<template #icon><import-outlined /></template>
						<span>导入</span>
					</a-button>
					<a-button @click="exportData" v-if="hasPerm('iotDeviceGroupExport')">
						<template #icon><export-outlined /></template>
						<span>导出</span>
					</a-button>
					<xn-batch-button
						v-if="hasPerm('iotDeviceGroupBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotDeviceGroup"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'groupType'">
					{{ $TOOL.dictTypeData('DEVICE_GROUP_TYPE', record.groupType) }}
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="deviceRelateRef.onOpen(record)" v-if="hasPerm('iotDeviceGroupRelate')">关联设备</a>
						<a-divider
							type="vertical"
							v-if="hasPerm('iotDeviceGroupRelate') && hasPerm(['iotDeviceGroupEdit', 'iotDeviceGroupDelete'])"
						/>
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotDeviceGroupEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotDeviceGroupEdit', 'iotDeviceGroupDelete'], 'and')" />
						<a-popconfirm title="删除此分组与下级分组吗?" @confirm="deleteIotDeviceGroup(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotDeviceGroupDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
	<DeviceRelate ref="deviceRelateRef" />
</template>

<script setup name="devicegroup">
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import DeviceRelate from './deviceRelate.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotDeviceGroupApi from '@/api/iot/iotDeviceGroupApi'

	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const deviceRelateRef = ref()
	const searchFormRef = ref()
	const searchFormState = ref({})
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '分组名称',
			dataIndex: 'groupName'
		},
		{
			title: '分组类型',
			dataIndex: 'groupType'
		},
		{
			title: '备注',
			dataIndex: 'remark'
		},
		{
			title: '排序码',
			dataIndex: 'sortCode'
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotDeviceGroupEdit', 'iotDeviceGroupDelete'])) {
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
			show: false,
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

	// 表格查询 返回 Promise 对象
	const loadData = (parameter) => {
		// 使用树接口获取数据
		return iotDeviceGroupApi.iotDeviceGroupTree().then((res) => {
			// 将树形数据转换为表格数据格式
			return {
				records: res || [],
				total: 0,
				size: 0,
				current: 1
			}
		})
	}

	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}

	// 递归过滤树数据
	const filterTreeData = (data, searchKey) => {
		if (!searchKey) return data
		const result = []
		for (const item of data) {
			if (item.groupName && item.groupName.includes(searchKey)) {
				result.push(item)
			} else if (item.children && item.children.length > 0) {
				const filteredChildren = filterTreeData(item.children, searchKey)
				if (filteredChildren.length > 0) {
					result.push({ ...item, children: filteredChildren })
				}
			}
		}
		return result
	}
	// 删除
	const deleteIotDeviceGroup = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotDeviceGroupApi.iotDeviceGroupDelete(params).then(() => {
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
			iotDeviceGroupApi.iotDeviceGroupExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotDeviceGroupApi.iotDeviceGroupExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}

	// 批量删除
	const deleteBatchIotDeviceGroup = (params) => {
		iotDeviceGroupApi.iotDeviceGroupDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
</script>
