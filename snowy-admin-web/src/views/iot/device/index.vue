<template>
	<a-row :gutter="10">
		<a-col :xs="0" :sm="0" :md="0" :lg="3" :xl="3">
			<!-- 左侧分组树 -->
			<a-card :bordered="false" :loading="cardLoading" class="left-tree-container">
				<div style="margin-bottom: 12px">
					<a-button
						type="link"
						block
						:style="{
							backgroundColor: !selectedGroupId ? '#e6f7ff' : 'transparent',
							borderColor: !selectedGroupId ? '#1890ff' : '#d9d9d9',
							color: !selectedGroupId ? '#1890ff' : 'rgba(0, 0, 0, 0.85)'
						}"
						@click="showAllDevices"
					>
						<template #icon><UnorderedListOutlined /></template>
						全部设备
					</a-button>
				</div>
				<a-tree
					v-if="treeData.length > 0"
					v-model:expandedKeys="defaultExpandedKeys"
					v-model:selectedKeys="selectedTreeKeys"
					:tree-data="treeData"
					:field-names="treeFieldNames"
					show-line
					@select="treeSelect"
				>
					<template #title="{ name }">
						<span>{{ name }}</span>
					</template>
				</a-tree>
				<a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
			</a-card>
		</a-col>
		<a-col :xs="24" :sm="24" :md="24" :lg="21" :xl="21">
			<!-- 右侧设备列表 -->
			<a-card :bordered="false" style="width: 100%">
				<a-form ref="searchFormRef" :model="searchFormState">
					<a-row :gutter="10">
						<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
							<a-form-item label="设备名称" name="deviceName">
								<a-input v-model:value="searchFormState.deviceName" placeholder="请输入设备名称" />
							</a-form-item>
						</a-col>
						<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
							<a-form-item label="产品" name="productId">
								<a-select
									v-model:value="searchFormState.productId"
									placeholder="请选择产品"
									allow-clear
									show-search
									:filter-option="filterProductOption"
								>
									<a-select-option v-for="product in productList" :key="product.id" :value="product.id">
										{{ product.productName }}
									</a-select-option>
								</a-select>
							</a-form-item>
						</a-col>
						<a-col :xs="24" :sm="6" :md="6" :lg="6" :xl="6">
							<a-form-item label="设备状态" name="deviceStatus">
								<a-select
									v-model:value="searchFormState.deviceStatus"
									placeholder="请选择设备状态"
									:options="deviceStatusOptions"
								/>
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
							<a-button type="primary" @click="formRef.onOpen()" v-if="hasPerm('iotDeviceAdd')">
								<template #icon><plus-outlined /></template>
								新增
							</a-button>
							<a-button @click="importModelRef.onOpen()" v-if="hasPerm('iotDeviceImport')">
								<template #icon><import-outlined /></template>
								<span>导入</span>
							</a-button>
							<a-button @click="exportData" v-if="hasPerm('iotDeviceExport')">
								<template #icon><export-outlined /></template>
								<span>导出</span>
							</a-button>
							<xn-batch-button
								v-if="hasPerm('iotDeviceBatchDelete')"
								buttonName="批量删除"
								icon="DeleteOutlined"
								buttonDanger
								:selectedRowKeys="selectedRowKeys"
								@batchCallBack="deleteBatchIotDevice"
							/>
							<a-button @click="batchConfigNorthbound" :disabled="selectedRowKeys.length === 0">
								<template #icon><ApiOutlined /></template>
								批量配置推送
							</a-button>
						</a-space>
					</template>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'productId'">
							{{ getProductName(record.productId) }}
						</template>
						<template v-if="column.dataIndex === 'deviceStatus'">
							<a-badge
								:status="
									record.deviceStatus === 'ONLINE' ? 'success' : record.deviceStatus === 'OFFLINE' ? 'error' : 'default'
								"
								:text="$TOOL.dictTypeData('DEVICE_STATUS', record.deviceStatus)"
							/>
						</template>
						<template v-if="column.dataIndex === 'action'">
							<a-space>
								<a @click="detailRef.onOpen(record)" v-if="hasPerm('iotDeviceDetail')">详情</a>
								<a-divider type="vertical" v-if="hasPerm(['iotDeviceDetail', 'iotDeviceEdit'], 'and')" />
								<a @click="formRef.onOpen(record)" v-if="hasPerm('iotDeviceEdit')">编辑</a>
								<a-divider type="vertical" v-if="hasPerm('iotDeviceEdit')" />
								<a @click="configNorthbound(record)">推送配置</a>
								<a-divider type="vertical" v-if="hasPerm('iotDeviceDelete')" />
								<a-popconfirm title="确定要删除吗？" @confirm="deleteIotDevice(record)">
									<a-button type="link" danger size="small" v-if="hasPerm('iotDeviceDelete')">删除</a-button>
								</a-popconfirm>
							</a-space>
						</template>
					</template>
				</s-table>
			</a-card>
		</a-col>
	</a-row>
	<ImportModel ref="importModelRef" />
	<Form ref="formRef" @successful="tableRef.refresh()" />
	<Detail ref="detailRef" />
	<NorthboundConfigModal ref="northboundConfigModalRef" @success="tableRef.refresh()" />
</template>

<script setup name="device">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { isEmpty } from 'lodash-es'
	import { Empty } from 'ant-design-vue'
	import { onMounted, onBeforeUnmount } from 'vue'
	import sysConfig from '@/config'
	import { convertUrl } from '@/utils/apiAdaptive'
	import { EventSourcePolyfill } from 'event-source-polyfill'
	import Form from './form.vue'
	import ImportModel from './importModel.vue'
	import Detail from './detail.vue'
	import NorthboundConfigModal from './components/NorthboundConfigModal.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotDeviceGroupApi from '@/api/iot/iotDeviceGroupApi'
	import iotProductApi from '@/api/iot/iotProductApi'
	import { SSEMessageType } from '@/utils/iotConstants'

	// SSE连接
	let eventSource = null
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	const importModelRef = ref()
	const formRef = ref()
	const detailRef = ref()
	const northboundConfigModalRef = ref()

	// 分组树相关
	const cardLoading = ref(true)
	const treeData = ref([])
	const defaultExpandedKeys = ref([])
	const selectedTreeKeys = ref([])
	const selectedGroupId = ref('')
	const treeFieldNames = { children: 'children', title: 'name', key: 'id' }

	// 产品列表
	const productList = ref([])

	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }
	const columns = [
		{
			title: '设备名称',
			dataIndex: 'deviceName'
		},
		{
			title: '设备标识',
			dataIndex: 'deviceKey'
		},
		{
			title: '设备密钥',
			dataIndex: 'deviceSecret'
		},
		{
			title: '产品名称',
			dataIndex: 'productId'
		},
		{
			title: '网关设备ID',
			dataIndex: 'gatewayId'
		},
		{
			title: '设备状态',
			dataIndex: 'deviceStatus'
		},
		{
			title: '激活时间',
			dataIndex: 'activeTime'
		},
		{
			title: '最后在线时间',
			dataIndex: 'lastOnlineTime'
		},
		{
			title: 'IP地址',
			dataIndex: 'ipAddress'
		},
		{
			title: '固件版本',
			dataIndex: 'firmwareVersion'
		}
	]
	// 操作栏通过权限判断是否显示
	if (hasPerm(['iotDeviceDetail', 'iotDeviceEdit', 'iotDeviceDelete'])) {
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
		// 如果选中了分组，添加分组查询条件
		if (selectedGroupId.value) {
			searchFormParam.groupId = selectedGroupId.value
		}
		return iotDeviceApi.iotDevicePage(Object.assign(parameter, searchFormParam)).then((data) => {
			return data
		})
	}
	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		selectedTreeKeys.value = []
		selectedGroupId.value = ''
		tableRef.value.refresh(true)
	}
	// 删除
	const deleteIotDevice = (record) => {
		let params = [
			{
				id: record.id
			}
		]
		iotDeviceApi.iotDeviceDelete(params).then(() => {
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
			iotDeviceApi.iotDeviceExport(params).then((res) => {
				downloadUtil.resultDownload(res)
			})
		} else {
			iotDeviceApi.iotDeviceExport([]).then((res) => {
				downloadUtil.resultDownload(res)
			})
		}
	}
	// 批量删除
	const deleteBatchIotDevice = (params) => {
		iotDeviceApi.iotDeviceDelete(params).then(() => {
			tableRef.value.clearRefreshSelected()
		})
	}

	// 配置北向推送
	const configNorthbound = (record) => {
		northboundConfigModalRef.value.onOpen(record)
	}

	// 批量配置北向推送
	const batchConfigNorthbound = () => {
		if (selectedRowKeys.value.length === 0) {
			message.warning('请选择要配置的设备')
			return
		}
		// 获取选中的设备记录
		const selectedRecords = tableRef.value.dataSource.filter((item) => selectedRowKeys.value.includes(item.id))
		northboundConfigModalRef.value.onOpen(selectedRecords)
	}

	const deviceStatusOptions = tool.dictList('DEVICE_STATUS')

	// 加载产品列表
	const loadProductList = () => {
		iotProductApi.iotProductPage({ current: 1, size: 1000 }).then((data) => {
			productList.value = data.records || []
		})
	}

	// 产品搜索过滤
	const filterProductOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 根据产品ID获取产品名称
	const getProductName = (productId) => {
		if (!productId) return '-'
		const product = productList.value.find((p) => p.id === productId)
		return product ? product.productName : productId
	}

	// 加载分组树
	const loadTreeData = () => {
		iotDeviceGroupApi.iotDeviceGroupTree().then((res) => {
			cardLoading.value = false
			if (res !== null) {
				treeData.value = res
				if (isEmpty(defaultExpandedKeys.value)) {
					// 默认展开2级
					treeData.value.forEach((item) => {
						if (item.parentId === '0') {
							defaultExpandedKeys.value.push(item.id)
							if (item.children) {
								item.children.forEach((items) => {
									defaultExpandedKeys.value.push(items.id)
								})
							}
						}
					})
				}
			}
		})
	}

	// 树节点选择
	const treeSelect = (selectedKeys, e) => {
		if (selectedKeys.length > 0) {
			selectedGroupId.value = selectedKeys[0]
		} else {
			selectedGroupId.value = ''
		}
		// 刷新设备列表
		tableRef.value.refresh(true)
	}

	// 显示全部设备
	const showAllDevices = () => {
		selectedTreeKeys.value = []
		selectedGroupId.value = ''
		tableRef.value.refresh(true)
	}

	// 建立SSE连接
	const connectSSE = () => {
		if (window.EventSource) {
			const clientId = tool.data.get('CLIENTID') ? tool.data.get('CLIENTID') : ''
			const url = sysConfig.API_URL + convertUrl('/dev/sse/createConnect?clientId=') + clientId
			eventSource = new EventSourcePolyfill(url, {
				headers: { token: tool.data.get('TOKEN') },
				heartbeatTimeout: 300000
			})

			// 监听打开事件
			eventSource.addEventListener('open', (e) => {
				// console.log('SSE连接已建立')
			})

			// 监听消息事件
			eventSource.addEventListener('message', (e) => {
				try {
					const result = JSON.parse(e.data)
					const code = result.code
					const data = result.data

					if (code === 0) {
						// 初次建立连接，客户端id储存本地
						tool.data.set('CLIENTID', data)
					} else if (code === 200 && data) {
						// 处理设备消息
						const message = typeof data === 'string' ? JSON.parse(data) : data
						// 如果设备详情页面打开，传递给详情页面处理
						if (window.__deviceDetailSSEHandler__ && typeof window.__deviceDetailSSEHandler__ === 'function') {
							window.__deviceDetailSSEHandler__(message)
						}

						// 列表页面也需要刷新设备状态
						if (message.type === SSEMessageType.DEVICE_STATUS) {
							// 刷新列表
							if (tableRef.value) {
								tableRef.value.refresh(false)
							}
						}
					}
				} catch (error) {
					console.error('解析SSE消息失败:', error, e.data)
				}
			})

			// 监听错误事件
			eventSource.addEventListener('error', (e) => {
				console.error('SSE连接错误:', e)
				eventSource.close()
			})
		} else {
			console.warn('该浏览器不支持SSE功能')
		}
	}

	// 关闭SSE连接
	const closeSSE = () => {
		if (eventSource) {
			eventSource.close()
			eventSource = null
			// console.log('SSE连接已关闭')
		}
	}

	// 组件挂载时建立SSE连接
	onMounted(() => {
		connectSSE()
		loadTreeData()
		loadProductList()
	})

	// 组件卸载时关闭SSE连接
	onBeforeUnmount(() => {
		closeSSE()
	})
</script>
