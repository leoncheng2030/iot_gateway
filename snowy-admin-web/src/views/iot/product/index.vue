<template>
	<a-card :bordered="false" style="width: 100%">
		<a-form ref="searchFormRef" :model="searchFormState">
			<a-row :gutter="10">
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="产品名称" name="productName">
						<a-input v-model:value="searchFormState.productName" placeholder="请输入产品名称" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="产品标识" name="productKey">
						<a-input v-model:value="searchFormState.productKey" placeholder="请输入产品标识" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
					<a-form-item label="产品类型" name="productType">
						<a-select v-model:value="searchFormState.productType" placeholder="请选择产品类型" :options="productTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6" v-show="advanced">
					<a-form-item label="接入协议" name="protocolType">
						<a-select v-model:value="searchFormState.protocolType" placeholder="请选择接入协议" :options="protocolTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6" v-show="advanced">
					<a-form-item label="数据格式" name="dataFormat">
						<a-select v-model:value="searchFormState.dataFormat" placeholder="请选择数据格式" :options="dataFormatOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6" v-show="advanced">
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
                            <a @click="toggleAdvanced">
                                {{ advanced ? '收起' : '展开' }}
                                <component :is="advanced ? 'up-outlined' : 'down-outlined'"/>
                            </a>
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
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotProductAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotProductImport')">
                        <template #icon><import-outlined /></template>
                        <span>导入</span>
                    </a-button>
                    <a-button @click="exportData" v-if="hasPerm('iotProductExport')">
                        <template #icon><export-outlined /></template>
                        <span>导出</span>
                    </a-button>
					<xn-batch-button
						v-if="hasPerm('iotProductBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotProduct"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'productType'">
					{{ $TOOL.dictTypeData('PRODUCT_TYPE', record.productType) }}
				</template>
				<template v-if="column.dataIndex === 'protocolType'">
					{{ getProtocolTypeName(record.protocolType) }}
				</template>
				<template v-if="column.dataIndex === 'dataFormat'">
					{{ $TOOL.dictTypeData('DATA_FORMAT', record.dataFormat) }}
				</template>
				<template v-if="column.dataIndex === 'status'">
					{{ $TOOL.dictTypeData('COMMON_STATUS', record.status) }}
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="detailRef.onOpen(record)" v-if="hasPerm('iotProductEdit')">详情</a>
						<a-divider type="vertical" v-if="hasPerm('iotProductEdit')" />
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotProductEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotProductEdit', 'iotProductDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotProduct(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotProductDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
	<Detail ref="detailRef" />
</template>

<script setup name="product">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import Detail from './detail.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotProductApi from '@/api/iot/iotProductApi'
	import iotProtocolApi from '@/api/iot/iotProtocolApi'
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const detailRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	// 查询区域显示更多控制
	const advanced = ref(false)
	const toggleAdvanced = () => {
		advanced.value = !advanced.value
	}
	const columns = [
		{
			title: '产品名称',
			dataIndex: 'productName'
		},
		{
			title: '产品标识',
			dataIndex: 'productKey'
		},
		{
			title: '产品类型',
			dataIndex: 'productType'
		},
		{
			title: '接入协议',
			dataIndex: 'protocolType'
		},
		{
			title: '数据格式',
			dataIndex: 'dataFormat'
		},
		{
			title: '产品描述',
			dataIndex: 'productDesc'
		},
		{
			title: '状态',
			dataIndex: 'status'
		},
		{
			title: '排序码',
			dataIndex: 'sortCode'
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotProductEdit', 'iotProductDelete'])) {
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
		return iotProductApi.iotProductPage(Object.assign(parameter, searchFormParam)).then((data) => {
			return data
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotProduct = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotProductApi.iotProductDelete(params).then(() => {
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
            iotProductApi.iotProductExport(params).then((res) => {
                downloadUtil.resultDownload(res)
            })
        } else {
            iotProductApi.iotProductExport([]).then((res) => {
                downloadUtil.resultDownload(res)
            })
        }
    }
	// 批量删除
	const deleteBatchIotProduct = (params) => {
		iotProductApi.iotProductDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
	const productTypeOptions = tool.dictList('PRODUCT_TYPE')
	// 从后端动态获取协议类型选项
	const protocolTypeOptions = ref([])
	const protocolTypeMap = ref({})
	iotProtocolApi.iotProtocolTypes().then((data) => {
		if (data && Array.isArray(data)) {
			protocolTypeOptions.value = data.map((item) => ({
				label: item.name,
				value: item.type
			}))
			// 创建类型映射对象，用于列表显示
			protocolTypeMap.value = data.reduce((map, item) => {
				map[item.type] = item.name
				return map
			}, {})
		}
	}).catch((err) => {
		console.error('获取协议类型失败:', err)
	})
	// 获取协议类型显示名称
	const getProtocolTypeName = (type) => {
		return protocolTypeMap.value[type] || type || '-'
	}
	const dataFormatOptions = tool.dictList('DATA_FORMAT')
	const statusOptions = tool.dictList('COMMON_STATUS')
</script>
