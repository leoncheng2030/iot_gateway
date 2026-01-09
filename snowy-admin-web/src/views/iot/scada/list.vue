<template>
	<a-card :bordered="false">
		<a-form ref="searchFormRef" name="advanced_search" :model="searchFormState" class="ant-advanced-search-form">
			<a-row :gutter="24">
				<a-col :span="6">
					<a-form-item label="组态名称" name="name">
						<a-input v-model:value="searchFormState.name" placeholder="请输入组态名称" />
					</a-form-item>
				</a-col>
				<a-col :span="6">
					<a-button type="primary" @click="loadData">
						<template #icon><SearchOutlined /></template>
						查询
					</a-button>
					<a-button style="margin: 0 8px" @click="reset">
						<template #icon><redo-outlined /></template>
						重置
					</a-button>
				</a-col>
			</a-row>
		</a-form>

		<s-table
			ref="table"
			:columns="columns"
			:data="loadData"
			:alert="options.alert.show"
			bordered
			:row-key="(record) => record.id"
			:row-selection="options.rowSelection"
		>
			<template #operator>
				<a-space>
					<a-button type="primary" @click="handleAdd">
						<template #icon><plus-outlined /></template>
						新增组态
					</a-button>
					<xn-batch-delete
						v-if="selectedRowKeys.length > 0"
						:selectedRowKeys="selectedRowKeys"
						@batchDelete="deleteBatchScada"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'createTime'">
					<span>{{ formatDate(record.createTime) }}</span>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a @click="handleView(record)">查看</a>
						<a-divider type="vertical" />
						<a @click="handleEdit(record)">编辑</a>
						<a-divider type="vertical" />
						<a-popconfirm title="确定要删除吗？" @confirm="handleDelete(record)">
							<a-button type="link" danger size="small">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
</template>

<script setup name="iotScadaList">
	import { ref, reactive } from 'vue'
	import { message } from 'ant-design-vue'
	import { SearchOutlined, RedoOutlined, PlusOutlined } from '@ant-design/icons-vue'
	import scadaApi from '@/api/iot/scadaApi'

	// 表格
	const table = ref()
	const searchFormRef = ref()
	const searchFormState = reactive({
		name: ''
	})

	// 表格列
	const columns = [
		{
			title: '组态名称',
			dataIndex: 'name',
			width: 200
		},
		{
			title: '创建时间',
			dataIndex: 'createTime',
			width: 180
		},
		{
			title: '操作',
			dataIndex: 'action',
			width: 180,
			fixed: 'right'
		}
	]

	// 选中行
	const selectedRowKeys = ref([])
	const options = reactive({
		alert: {
			show: true,
			clear: () => {
				selectedRowKeys.value = []
			}
		},
		rowSelection: {
			onChange: (selectedKeys) => {
				selectedRowKeys.value = selectedKeys
			}
		}
	})

	// 加载数据
	const loadData = (parameter) => {
		return scadaApi
			.scadaPage(Object.assign(parameter, searchFormState))
			.then((res) => {
				return res
			})
			.catch(() => {
				return []
			})
	}

	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		loadData()
	}

	// 新增
	const handleAdd = () => {
		const url = '/iot/scada/design'
		window.open(url, '_blank')
	}

	// 查看
	const handleView = (record) => {
		const url = `/iot/scada/view?scadaId=${record.id}`
		window.open(url, '_blank')
	}

	// 编辑
	const handleEdit = (record) => {
		const url = `/iot/scada/design?scadaId=${record.id}`
		window.open(url, '_blank')
	}

	// 删除
	const handleDelete = (record) => {
		scadaApi.scadaDelete({ id: record.id }).then(() => {
			message.success('删除成功')
			if (table.value && table.value.refresh) {
				table.value.refresh()
			}
		})
	}

	// 批量删除
	const deleteBatchScada = (params) => {
		scadaApi.scadaDelete(params).then(() => {
			message.success('删除成功')
			if (table.value && table.value.refresh) {
				table.value.refresh()
			}
		})
	}

	// 格式化日期
	const formatDate = (dateStr) => {
		if (!dateStr) return ''
		return dateStr.replace('T', ' ')
	}
</script>
