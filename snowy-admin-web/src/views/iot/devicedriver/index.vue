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
			<template #operator>
				<a-space>
					<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotDeviceDriverAdd')">
						<template #icon><plus-outlined /></template>
						新增
					</a-button>
					<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotDeviceDriverImport')">
						<template #icon><import-outlined /></template>
						<span>导入</span>
					</a-button>
					<a-button @click="exportData" v-if="hasPerm('iotDeviceDriverExport')">
						<template #icon><export-outlined /></template>
						<span>导出</span>
					</a-button>
					<xn-batch-button
						v-if="hasPerm('iotDeviceDriverBatchDelete')"
						buttonName="批量删除"
						icon="DeleteOutlined"
						buttonDanger
						:selectedRowKeys="selectedRowKeys"
						@batchCallBack="deleteBatchIotDeviceDriver"
					/>
				</a-space>
			</template>
			<template #bodyCell="{ column, record }">
				<template v-if="column.dataIndex === 'status'">
					<a-space>
						<a-badge v-if="record.runStatus" status="processing" text="运行中" />
						<a-badge v-else-if="record.status === 'STOPPED'" status="default" text="已停止" />
						<a-badge v-else status="error" text="错误" />
					</a-space>
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a-space>
						<a-button
							v-if="!record.runStatus && (record.status === 'STOPPED' || record.status === 'ERROR')"
							type="primary"
							size="small"
							@click="startDriver(record)"
						>
							启动
						</a-button>
						<a-button v-if="record.runStatus" danger size="small" @click="stopDriver(record)"> 停止 </a-button>
						<a-button v-if="record.runStatus" size="small" @click="restartDriver(record)"> 重启 </a-button>
						<a-divider type="vertical" v-if="hasPerm(['iotDeviceDriverEdit', 'iotDeviceDriverDelete'], 'or')" />
						<a @click="formRef.onOpen(record)" v-if="hasPerm('iotDeviceDriverEdit')">编辑</a>
						<a-divider type="vertical" v-if="hasPerm(['iotDeviceDriverEdit', 'iotDeviceDriverDelete'], 'and')" />
						<a-popconfirm title="确定要删除吗？" @confirm="deleteIotDeviceDriver(record)">
							<a-button type="link" danger size="small" v-if="hasPerm('iotDeviceDriverDelete')">删除</a-button>
						</a-popconfirm>
					</a-space>
				</template>
			</template>
		</s-table>
	</a-card>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
</template>

<script setup name="devicedriver">
	import { message } from 'ant-design-vue'
	import { cloneDeep } from 'lodash-es'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotDeviceDriverApi from '@/api/iot/iotDeviceDriverApi'
	
	// 驱动类型映射（从注册中心动态加载）
	const driverTypeMap = ref({})
	
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '驱动名称',
			dataIndex: 'driverName',
			width: 180
		},
		{
			title: '驱动类型',
			dataIndex: 'driverType',
			width: 150,
			customRender: ({ text }) => {
				// 从驱动注册中心动态获取驱动名称
				const driverInfo = driverTypeMap.value[text]
				return driverInfo ? driverInfo.label : text
			}
		},
		{
			title: '运行状态',
			dataIndex: 'status',
			width: 120,
			customRender: ({ text }) => {
				const statusMap = {
					RUNNING: '运行中',
					STOPPED: '已停止',
					ERROR: '错误'
				}
				return statusMap[text] || text
			}
		},
		{
			title: '驱动配置',
			dataIndex: 'configJson',
			width: 200,
			ellipsis: true,
			customRender: ({ text }) => {
				if (!text) return '-'
				try {
					const config = JSON.parse(text)
					return JSON.stringify(config)
				} catch (e) {
					return text
				}
			}
		},
		{
			title: '驱动描述',
			dataIndex: 'description',
			width: 200,
			ellipsis: true
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotDeviceDriverEdit', 'iotDeviceDriverDelete'])) {
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
	
	// 加载驱动类型映射
	const loadDriverTypes = async () => {
		try {
			const types = await iotDeviceDriverApi.iotDeviceDriverTypes()
			const map = {}
			types.forEach(item => {
				map[item.value] = item
			})
			driverTypeMap.value = map
		} catch (e) {
			console.error('加载驱动类型失败', e)
		}
	}
	
	// 页面加载时获取驱动类型
	onMounted(() => {
		loadDriverTypes()
	})
	
	const loadData = (parameter) => {
		return iotDeviceDriverApi.iotDeviceDriverPage(parameter).then(async (data) => {
			// 获取每个驱动的运行状态
			if (data.records && data.records.length > 0) {
				for (let row of data.records) {
					try {
						const statusRes = await iotDeviceDriverApi.iotDeviceDriverStatus({ id: row.id })
						// 正确解析运行状态: statusRes.data = {running: true/false, uptime: xxx}
						const statusData = statusRes.data !== undefined ? statusRes.data : statusRes
						row.runStatus = statusData.running || false
					} catch (e) {
						row.runStatus = false
					}
				}
			}
			return data
		})
	}
	// 启动驱动
	const startDriver = (record) => {
		iotDeviceDriverApi.iotDeviceDriverStart({ id: record.id }).then(() => {
			message.success('驱动启动成功')
			// 延迟刷新确保后端状态已更新
			setTimeout(() => {
				tableRef.value.refresh()
			}, 300)
		})
	}
	// 停止驱动
	const stopDriver = (record) => {
		iotDeviceDriverApi.iotDeviceDriverStop({ id: record.id }).then(() => {
			message.success('驱动已停止')
			// 延迟刷新确保后端状态已更新
			setTimeout(() => {
				tableRef.value.refresh()
			}, 300)
		})
	}
	// 重启驱动
	const restartDriver = (record) => {
		iotDeviceDriverApi.iotDeviceDriverRestart({ id: record.id }).then(() => {
			message.success('驱动重启成功')
			// 延迟刷新确保后端状态已更新
			setTimeout(() => {
				tableRef.value.refresh()
			}, 300)
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotDeviceDriver = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotDeviceDriverApi.iotDeviceDriverDelete(params).then(() => {
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
			iotDeviceDriverApi.iotDeviceDriverExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotDeviceDriverApi.iotDeviceDriverExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}
	// 批量删除
	const deleteBatchIotDeviceDriver = (params) => {
		iotDeviceDriverApi.iotDeviceDriverDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}
</script>
