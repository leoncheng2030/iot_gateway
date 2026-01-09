<template>
	<a-drawer
		:title="'产品详情 - ' + productData.productName"
		:width="1000"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-tabs v-model:activeKey="activeTab">
			<!-- 基本信息 -->
			<a-tab-pane key="basic" tab="基本信息">
				<a-descriptions bordered :column="2">
					<a-descriptions-item label="产品名称">{{ productData.productName }}</a-descriptions-item>
					<a-descriptions-item label="产品标识">{{ productData.productKey }}</a-descriptions-item>
					<a-descriptions-item label="产品类型">
						{{ $TOOL.dictTypeData('PRODUCT_TYPE', productData.productType) }}
					</a-descriptions-item>
					<a-descriptions-item label="接入协议">
						{{ $TOOL.dictTypeData('PROTOCOL_TYPE', productData.protocolType) }}
					</a-descriptions-item>
					<a-descriptions-item label="数据格式">
						{{ $TOOL.dictTypeData('DATA_FORMAT', productData.dataFormat) }}
					</a-descriptions-item>
					<a-descriptions-item label="状态">
						<a-tag :color="productData.status === 'ENABLE' ? 'green' : 'red'">
							{{ $TOOL.dictTypeData('COMMON_STATUS', productData.status) }}
						</a-tag>
					</a-descriptions-item>
					<a-descriptions-item label="产品描述" :span="2">
						{{ productData.productDesc || '-' }}
					</a-descriptions-item>
					<a-descriptions-item label="创建时间">{{ productData.createTime }}</a-descriptions-item>
					<a-descriptions-item label="更新时间">{{ productData.updateTime }}</a-descriptions-item>
				</a-descriptions>
			</a-tab-pane>

			<!-- 物模型配置 -->
			<a-tab-pane key="thingModel" tab="物模型">
				<a-card size="small" style="margin-bottom: 16px">
					<a-space>
						<a-button type="primary" @click="addThingModel">
							<template #icon>
								<PlusOutlined />
							</template>
							添加功能
						</a-button>
						<a-button @click="downloadTemplate">
							<template #icon>
								<DownloadOutlined />
							</template>
							下载模板
						</a-button>
						<a-upload :show-upload-list="false" :custom-request="handleImport" accept=".xls,.xlsx">
							<a-button>
								<template #icon>
									<UploadOutlined />
								</template>
								导入
							</a-button>
						</a-upload>
						<a-button @click="exportThingModel" :disabled="thingModelList.length === 0">
							<template #icon>
								<ExportOutlined />
							</template>
							导出
						</a-button>
						<a-radio-group v-model:value="modelTypeFilter" button-style="solid">
							<a-radio-button value="">全部</a-radio-button>
							<a-radio-button :value="ModelType.PROPERTY">属性</a-radio-button>
							<a-radio-button :value="ModelType.EVENT">事件</a-radio-button>
							<a-radio-button :value="ModelType.SERVICE">服务</a-radio-button>
						</a-radio-group>
					</a-space>
				</a-card>
				<a-table
					:columns="thingModelColumns"
					:data-source="filteredThingModelList"
					:pagination="{ pageSize: 20 }"
					size="middle"
					:scroll="{ y: 'calc(100vh - 400px)' }"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'modelType'">
							<a-tag :color="getModelTypeColor(record.modelType)">{{ getModelTypeLabel(record.modelType) }}</a-tag>
						</template>
						<template v-if="column.dataIndex === 'accessMode'">
							<a-tag v-if="record.accessMode === AccessMode.READ" color="cyan">{{
								getAccessModeLabel(AccessMode.READ)
							}}</a-tag>
							<a-tag v-else-if="record.accessMode === AccessMode.WRITE" color="purple">{{
								getAccessModeLabel(AccessMode.WRITE)
							}}</a-tag>
							<a-tag v-else-if="record.accessMode === AccessMode.READ_WRITE" color="blue">{{
								getAccessModeLabel(AccessMode.READ_WRITE)
							}}</a-tag>
							<span v-else>-</span>
						</template>
						<template v-if="column.dataIndex === 'required'">
							<a-tag v-if="record.required" color="red">是</a-tag>
							<a-tag v-else color="default">否</a-tag>
						</template>
						<template v-if="column.dataIndex === 'action'">
							<a-space>
								<a @click="copyThingModel(record)">复制</a>
								<a-divider type="vertical" />
								<a @click="editThingModel(record)">编辑</a>
								<a-divider type="vertical" />
								<a-popconfirm title="确定要删除吗？" @confirm="deleteThingModel(record)">
									<a style="color: red">删除</a>
								</a-popconfirm>
							</a-space>
						</template>
					</template>
				</a-table>
			</a-tab-pane>

			<!-- Modbus寄存器映射(仅Modbus协议) -->
			<a-tab-pane v-if="productData.protocolType === 'MODBUS_TCP'" key="registerMapping" tab="寄存器映射">
				<RegisterMappingConfig
					v-model:mapping-list="productMappingList"
					:loading="mappingLoading"
					:saving="mappingSaving"
					tip="产品级配置将作为新增设备的默认配置,设备可按需覆盖"
					@save="saveProductMapping"
					@refresh="loadProductMapping"
				/>
			</a-tab-pane>

			<!-- 接入信息 -->
			<a-tab-pane key="access" tab="接入信息">
				<!-- MQTT协议接入信息 -->
				<template v-if="productData.protocolType === 'MQTT'">
					<a-descriptions bordered :column="1">
						<a-descriptions-item label="协议类型">
							<a-tag color="blue">MQTT</a-tag>
						</a-descriptions-item>
						<a-descriptions-item label="MQTT Broker地址">
							<a-typography-text copyable>tcp://localhost:1883</a-typography-text>
						</a-descriptions-item>
						<a-descriptions-item label="产品标识（Product Key）">
							<a-typography-text copyable>{{ productData.productKey }}</a-typography-text>
						</a-descriptions-item>
						<a-descriptions-item label="Topic前缀">
							<a-typography-text copyable>/{{ productData.productKey }}/+/</a-typography-text>
						</a-descriptions-item>
					</a-descriptions>

					<a-divider orientation="left">Topic说明</a-divider>
					<a-table :columns="topicColumns" :data-source="topicList" :pagination="false" size="small">
						<template #bodyCell="{ column, record }">
							<template v-if="column.dataIndex === 'topic'">
								<a-typography-text copyable code>{{ record.topic }}</a-typography-text>
							</template>
						</template>
					</a-table>

					<a-divider orientation="left">数据格式示例</a-divider>
					<a-card size="small" title="属性上报">
						<pre style="background: #f5f5f5; padding: 12px; border-radius: 4px">{{ propertyPostExample }}</pre>
					</a-card>
				</template>

				<!-- Modbus TCP协议接入信息 -->
				<template v-else-if="productData.protocolType === 'MODBUS_TCP'">
					<a-descriptions bordered :column="1">
						<a-descriptions-item label="协议类型">
							<a-tag color="green">Modbus TCP</a-tag>
						</a-descriptions-item>
						<a-descriptions-item label="通信方式">
							<a-tag color="orange">平台主动轮询</a-tag>
							平台定时读取设备寄存器数据
						</a-descriptions-item>
						<a-descriptions-item label="设备要求">
							• 设备需要作为Modbus TCP从站（Slave）运行<br />
							• 设备需要有固定IP地址或域名<br />
							• 设备需要监听Modbus TCP端口（默认502）
						</a-descriptions-item>
						<a-descriptions-item label="产品标识（Product Key）">
							<a-typography-text copyable>{{ productData.productKey }}</a-typography-text>
						</a-descriptions-item>
					</a-descriptions>

					<a-divider orientation="left">配置说明</a-divider>
					<a-steps direction="vertical" size="small" :current="3">
						<a-step title="配置物模型" description="在'物模型'Tab中定义设备的属性、事件和服务" />
						<a-step
							title="配置产品寄存器映射"
							description="在'寄存器映射'Tab中配置产品级的寄存器映射，作为设备默认配置"
						/>
						<a-step
							title="添加设备"
							description="创建设备实例，填写设备IP地址和Modbus从站地址，设备将自动继承产品配置"
						/>
						<a-step title="启动轮询" description="平台将自动按配置的功能码和地址轮询读取设备数据" />
					</a-steps>

					<a-divider orientation="left">支持的功能码</a-divider>
					<a-descriptions bordered :column="2" size="small">
						<a-descriptions-item label="0x01" :span="1">读线圈（Coils）</a-descriptions-item>
						<a-descriptions-item label="0x02" :span="1">读离散输入（Discrete Inputs）</a-descriptions-item>
						<a-descriptions-item label="0x03" :span="1">读保持寄存器（Holding Registers）</a-descriptions-item>
						<a-descriptions-item label="0x04" :span="1">读输入寄存器（Input Registers）</a-descriptions-item>
						<a-descriptions-item label="0x05" :span="1">写单个线圈</a-descriptions-item>
						<a-descriptions-item label="0x06" :span="1">写单个寄存器</a-descriptions-item>
						<a-descriptions-item label="0x0F" :span="1">写多个线圈</a-descriptions-item>
						<a-descriptions-item label="0x10" :span="1">写多个寄存器</a-descriptions-item>
					</a-descriptions>
				</template>

				<!-- 其他协议提示 -->
				<template v-else>
					<a-empty description="暂不支持该协议类型的接入信息展示" />
				</template>
			</a-tab-pane>

			<!-- 设备列表 -->
			<a-tab-pane key="devices" tab="关联设备">
				<a-table
					:columns="deviceColumns"
					:data-source="deviceList"
					:pagination="devicePagination"
					:loading="deviceLoading"
					size="middle"
					@change="onDeviceTableChange"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'deviceStatus'">
							<a-badge
								:status="record.deviceStatus === 'ONLINE' ? 'success' : 'default'"
								:text="$TOOL.dictTypeData('DEVICE_STATUS', record.deviceStatus)"
							/>
						</template>
					</template>
				</a-table>
			</a-tab-pane>
		</a-tabs>
	</a-drawer>

	<!-- 物模型表单 -->
	<ThingModelForm ref="thingModelFormRef" @successful="loadThingModelList" />
</template>

<script setup name="productDetail">
	import { ref, computed } from 'vue'
	import { PlusOutlined, DownloadOutlined, UploadOutlined, ExportOutlined } from '@ant-design/icons-vue'
	import { message } from 'ant-design-vue'
	import iotProductApi from '@/api/iot/iotProductApi'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotProductRegisterApi from '@/api/iot/iotProductRegisterApi'
	import ThingModelForm from './thingModelForm.vue'
	import RegisterMappingConfig from '../components/RegisterMappingConfig.vue'
	import downloadUtil from '@/utils/downloadUtil'
	import tool from '@/utils/tool'
	import { ModelType, AccessMode, getModelTypeLabel, getModelTypeColor, getAccessModeLabel } from '@/utils/iotConstants'

	const open = ref(false)
	const activeTab = ref('basic')
	const productData = ref({})
	const thingModelList = ref([])
	const modelTypeFilter = ref('')
	const deviceList = ref([])
	const deviceLoading = ref(false)
	const devicePagination = ref({
		current: 1,
		pageSize: 10,
		total: 0
	})
	const thingModelFormRef = ref()

	// 产品级寄存器映射相关
	const productMappingList = ref([])
	const mappingLoading = ref(false)
	const mappingSaving = ref(false)

	// 物模型列表列定义
	const thingModelColumns = [
		{ title: '功能类型', dataIndex: 'modelType', width: 100 },
		{ title: '标识符', dataIndex: 'identifier', width: 150 },
		{ title: '功能名称', dataIndex: 'name', width: 150 },
		{ title: '值类型', dataIndex: 'valueType', width: 100 },
		{ title: '读写类型', dataIndex: 'accessMode', width: 100 },
		{ title: '是否必须', dataIndex: 'required', width: 100 },
		{ title: '描述', dataIndex: 'description', ellipsis: true },
		{ title: '操作', dataIndex: 'action', width: 200, fixed: 'right' }
	]

	// 过滤后的物模型列表
	const filteredThingModelList = computed(() => {
		if (!modelTypeFilter.value) return thingModelList.value
		return thingModelList.value.filter((item) => item.modelType === modelTypeFilter.value)
	})

	// 设备列表列定义
	const deviceColumns = [
		{ title: '设备名称', dataIndex: 'deviceName', width: 150 },
		{ title: '设备标识', dataIndex: 'deviceKey', width: 150 },
		{ title: '设备状态', dataIndex: 'deviceStatus', width: 120 },
		{ title: '激活时间', dataIndex: 'activeTime', width: 180 },
		{ title: '最后在线时间', dataIndex: 'lastOnlineTime', width: 180 }
	]

	// Topic列表
	const topicColumns = [
		{ title: 'Topic', dataIndex: 'topic', width: 400 },
		{ title: '说明', dataIndex: 'description' }
	]

	const topicList = computed(() => [
		{
			topic: `/${productData.value.productKey}/{deviceKey}/property/post`,
			description: '设备属性上报'
		},
		{
			topic: `/${productData.value.productKey}/{deviceKey}/property/set`,
			description: '设置设备属性'
		},
		{
			topic: `/${productData.value.productKey}/{deviceKey}/event/post`,
			description: '设备事件上报'
		},
		{
			topic: `/${productData.value.productKey}/{deviceKey}/service/call`,
			description: '调用设备服务'
		}
	])

	// 数据格式示例
	const propertyPostExample = computed(() =>
		JSON.stringify(
			{
				method: 'property.post',
				params: {
					temperature: 25.5,
					humidity: 60
				},
				timestamp: Date.now()
			},
			null,
			2
		)
	)

	// 打开详情
	const onOpen = (record) => {
		productData.value = { ...record }
		open.value = true
		activeTab.value = 'basic'
		loadThingModelList()
		loadDeviceList()
		loadProductMapping()
	}

	// 关闭详情
	const onClose = () => {
		open.value = false
		productData.value = {}
		thingModelList.value = []
		deviceList.value = []
		modelTypeFilter.value = ''
	}

	// 加载物模型列表
	const loadThingModelList = () => {
		if (!productData.value.id) return
		iotThingModelApi
			.iotThingModelGetProperties({
				productId: productData.value.id
			})
			.then((data) => {
				thingModelList.value = data || []
			})
	}

	// 添加物模型
	const addThingModel = () => {
		thingModelFormRef.value.onOpen({ productId: productData.value.id })
	}

	// 编辑物模型
	const editThingModel = (record) => {
		thingModelFormRef.value.onOpen(record)
	}

	// 复制物模型
	const copyThingModel = (record) => {
		// 复制并清空id，保持标识符和名称不变
		const copyData = {
			...record,
			id: undefined // 清空id，让系统自动生成新id
		}
		thingModelFormRef.value.onOpen(copyData)
	}

	// 删除物模型
	const deleteThingModel = (record) => {
		iotThingModelApi.iotThingModelDelete([{ id: record.id }]).then(() => {
			loadThingModelList()
		})
	}

	// 加载设备列表
	const loadDeviceList = () => {
		if (!productData.value.id) return
		deviceLoading.value = true
		iotDeviceApi
			.iotDevicePage({
				current: devicePagination.value.current,
				size: devicePagination.value.pageSize,
				productId: productData.value.id
			})
			.then((data) => {
				deviceList.value = data.records || []
				devicePagination.value.total = data.total
			})
			.finally(() => {
				deviceLoading.value = false
			})
	}

	// 设备表格分页变化
	const onDeviceTableChange = (pagination) => {
		devicePagination.value.current = pagination.current
		devicePagination.value.pageSize = pagination.pageSize
		loadDeviceList()
	}

	// 加载产品寄存器映射
	const loadProductMapping = async () => {
		if (!productData.value.id || productData.value.protocolType !== 'MODBUS_TCP') return

		mappingLoading.value = true
		try {
			// 1. 先查询产品级映射
			const productMappings = await iotProductRegisterApi.iotProductRegisterList({
				productId: productData.value.id
			})

			// 2. 获取物模型属性列表
			const properties = await iotThingModelApi.iotThingModelGetProperties({
				productId: productData.value.id,
				modelType: 'PROPERTY'
			})

			// 3. 合并数据：以物模型为基础，填充已配置的映射
			const mappingMap = new Map()
			productMappings.forEach((m) => mappingMap.set(m.identifier, m))

			productMappingList.value = (properties || []).map((prop) => {
				const existMapping = mappingMap.get(prop.identifier)
				return {
					id: existMapping?.id,
					thingModelId: prop.id,
					identifier: prop.identifier,
					name: prop.name,
					description: prop.description,
					registerAddress: existMapping?.registerAddress,
					functionCode: existMapping?.functionCode,
					dataType: existMapping?.dataType || 'int',
					scaleFactor: existMapping?.scaleFactor ?? 1.0,
					offset: existMapping?.offset ?? 0.0,
					bitIndex: existMapping?.bitIndex,
					byteOrder: existMapping?.byteOrder || 'BIG_ENDIAN',
					enabled: existMapping?.enabled ?? true
				}
			})
		} finally {
			mappingLoading.value = false
		}
	}

	// 保存产品寄存器映射
	const saveProductMapping = async () => {
		mappingSaving.value = true
		try {
			const mappings = productMappingList.value
				.filter((item) => item.registerAddress != null && item.functionCode)
				.map((item) => ({
					thingModelId: item.thingModelId,
					identifier: item.identifier,
					registerAddress: item.registerAddress,
					functionCode: item.functionCode,
					dataType: item.dataType,
					scaleFactor: item.scaleFactor ?? 1.0,
					offset: item.offset ?? 0.0,
					bitIndex: item.bitIndex,
					byteOrder: item.byteOrder || 'BIG_ENDIAN',
					enabled: item.enabled ?? true
				}))

			if (mappings.length === 0) {
				message.warning('请至少配置一个寄存器映射')
				return
			}

			await iotProductRegisterApi.iotProductRegisterBatchSave({
				productId: productData.value.id,
				mappings: mappings
			})

			message.success('产品寄存器映射保存成功')
			loadProductMapping()
		} catch (e) {
			message.error('保存失败: ' + e.message)
		} finally {
			mappingSaving.value = false
		}
	}

	// 下载导入模板
	const downloadTemplate = () => {
		iotThingModelApi.iotThingModelDownloadTemplate().then((res) => {
			downloadUtil.resultDownload(res)
		})
	}

	// 导入物模型
	const handleImport = (data) => {
		const fileData = new FormData()
		fileData.append('file', data.file)

		iotThingModelApi
			.iotThingModelImport(fileData)
			.then((res) => {
				if (res.successCount > 0) {
					message.success(`导入成功 ${res.successCount} 条`)
					loadThingModelList()
				}
				if (res.errorCount > 0) {
					message.warning(`导入失败 ${res.errorCount} 条，请检查数据格式`)
				}
			})
			.catch((err) => {
				message.error('导入失败: ' + err.message)
			})
	}

	// 导出物模型
	const exportThingModel = () => {
		const ids = thingModelList.value.map((item) => ({ id: item.id }))
		iotThingModelApi.iotThingModelExport(ids).then((res) => {
			downloadUtil.resultDownload(res)
		})
	}

	defineExpose({
		onOpen
	})
</script>

<style scoped>
	pre {
		margin: 0;
	}
</style>
