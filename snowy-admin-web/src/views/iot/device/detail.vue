<template>
	<a-drawer
		:title="'设备详情 - ' + deviceData.deviceName"
		:width="1200"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-tabs v-model:activeKey="activeTab">
			<!-- 基本信息 -->
			<a-tab-pane key="basic" tab="基本信息">
				<BasicInfo :device-data="deviceData" />
			</a-tab-pane>

			<!-- 设备控制 -->
			<a-tab-pane key="deviceControl" tab="设备控制">
				<DeviceControl
					:device-data="deviceData"
					:thing-model-data="thingModelData"
					:real-time-data-loading="realTimeDataLoading"
					:real-time-data-map="realTimeDataMap"
				/>
			</a-tab-pane>

			<!-- 设备影子 -->
			<a-tab-pane key="deviceShadow" tab="设备影子">
				<RealTimeData
					:device-data="deviceData"
					:thing-model-properties="thingModelProperties"
					:register-mapping-list="propertyList"
					:real-time-data-loading="realTimeDataLoading"
					:real-time-data-map="realTimeDataMap"
					@refresh="loadRealTimeData"
				/>
			</a-tab-pane>

			<!-- 事件记录 -->
			<a-tab-pane key="eventLog" tab="事件记录">
				<EventLog :thing-model-events="thingModelEvents" :recent-events="recentEvents" />
			</a-tab-pane>

			<!-- 实时趋势 -->
			<a-tab-pane key="deviceData" tab="实时趋势">
				<RealtimeTrend
					ref="realtimeTrendRef"
					:thing-model-properties="thingModelProperties"
					:realtime-chart-data="realtimeChartData"
				/>
			</a-tab-pane>

			<!-- 连接信息 -->
			<a-tab-pane key="connection" tab="连接信息">
				<ConnectionInfo :device-data="deviceData" />
			</a-tab-pane>

			<!-- 北向推送 -->
			<a-tab-pane key="northbound" tab="北向推送">
				<NorthboundPush :device-data="deviceData" />
			</a-tab-pane>

			<!-- 寄存器映射 (Modbus/S7等需要映射的协议) -->
			<a-tab-pane v-if="isModbusDevice" key="registerMapping" tab="寄存器映射">
				<RegisterMappingConfig
					v-model:mapping-list="propertyList"
					v-model:use-device-mapping="useDeviceLevelMapping"
					:loading="mappingLoading"
					:protocol-type="deviceData.protocolType"
					show-mode-switch
					@save="saveDeviceLevelMapping"
					@delete="deleteDeviceLevelMapping"
					@mode-change="onMappingModeChange"
				/>
			</a-tab-pane>

			<!-- 驱动配置 -->
			<a-tab-pane key="driverConfig" tab="驱动配置">
				<DriverConfig :device-data="deviceData" />
			</a-tab-pane>
		</a-tabs>
	</a-drawer>
</template>

<script setup name="deviceDetail">
	import { ref, computed, onBeforeUnmount, watch, nextTick } from 'vue'
	import { message } from 'ant-design-vue'
	import iotDeviceShadowApi from '@/api/iot/iotDeviceShadowApi'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'
	import iotDeviceRegisterApi from '@/api/iot/iotDeviceRegisterApi'
	import { ModelType, SSEMessageType } from '@/utils/iotConstants'

	// 导入组件
	import BasicInfo from './components/BasicInfo.vue'
	import DeviceControl from './components/DeviceControl.vue'
	import RealTimeData from './components/RealTimeData.vue'
	import EventLog from './components/EventLog.vue'
	import RealtimeTrend from './components/RealtimeTrend.vue'
	import ConnectionInfo from './components/ConnectionInfo.vue'
	import NorthboundPush from './components/NorthboundPush.vue'
	import RegisterMappingConfig from '../components/RegisterMappingConfig.vue'
	import DriverConfig from './components/DriverConfig.vue'

	const open = ref(false)
	const activeTab = ref('basic')
	const deviceData = ref({})

	// 设备影子(实时数据)
	const thingModelProperties = ref([])
	const realTimeDataLoading = ref(false)
	const realTimeDataMap = ref({})

	// 物模型完整数据（包含属性、事件、服务）
	const thingModelData = ref([])

	// 事件相关
	const recentEvents = ref([])

	// 图表相关
	const realtimeTrendRef = ref(null)
	const realtimeChartData = ref([]) // 实时图表数据

	// SSE消息处理
	const sseMessageHandler = ref(null)

	// Modbus寄存器映射
	const mappingLoading = ref(false)
	const propertyList = ref([])
	const useDeviceLevelMapping = ref(false) // 是否使用设备级配置

	// 判断是否需要寄存器映射（支持Modbus、S7等协议）
	const isModbusDevice = computed(() => {
		// 检查协议类型或是否有寄存器映射配置
		const protocolType = deviceData.value.protocolType
		// S7设备使用TCP协议，也需要寄存器映射
		const supportedProtocols = ['MODBUS_TCP', 'MODBUS_RTU', 'S7', 'TCP']
		
		// 如果是支持的协议类型，直接返回true
		if (supportedProtocols.includes(protocolType)) {
			return true
		}
		
		// 否则检查是否有寄存器映射配置（设备级或产品级）
		if (propertyList.value && propertyList.value.length > 0) {
			// 至少有一个配置了寄存器地址的属性
			return propertyList.value.some((item) => item.registerAddress != null)
		}
		return false
	})

	// 从物模型数据中筛选事件
	const thingModelEvents = computed(() => {
		return thingModelData.value.filter((item) => item.modelType === ModelType.EVENT)
	})

	// 打开详情
	const onOpen = (record) => {
		deviceData.value = { ...record }
		open.value = true
		activeTab.value = 'basic'
		loadRealTimeData()
		loadModbusMapping()
		// 注册SSE消息监听
		registerSSEHandler()
	}

	// 关闭详情
	const onClose = () => {
		open.value = false
		deviceData.value = {}
		thingModelProperties.value = []
		realTimeDataMap.value = {}
		realtimeChartData.value = [] // 清空实时图表数据
		// 注销SSE消息监听
		unregisterSSEHandler()
	}

	// 添加实时数据到图表
	const addRealtimeChartData = (data, timestamp) => {
		if (!data || !thingModelProperties.value.length) return

		const timeStr = new Date(timestamp).toLocaleTimeString()
		const dataPoint = {
			time: timeStr,
			properties: { ...data }
		}

		// 追加数据点
		realtimeChartData.value.push(dataPoint)

		// 保持50个数据点，超过50个时移除最早的
		if (realtimeChartData.value.length > 50) {
			realtimeChartData.value.shift()
		}
	}

	// 加载设备影子(实时数据)
	const loadRealTimeData = async () => {
		if (!deviceData.value.productId) return

		realTimeDataLoading.value = true
		try {
			// 加载产品物模型完整数据（属性、事件、服务）
			const allThingModels = await iotThingModelApi.iotThingModelGetProperties({
				productId: deviceData.value.productId
			})
			thingModelData.value = allThingModels || []

			// 筛选出属性类型
			const properties = (allThingModels || []).filter((item) => item.modelType === ModelType.PROPERTY)
			thingModelProperties.value = properties

			// 从设备影子中获取最新上报值
			if (deviceData.value.id) {
				const shadowData = await iotDeviceShadowApi.iotDeviceShadowPage({
					current: 1,
					size: 1,
					deviceId: deviceData.value.id
				})

				if (shadowData.records && shadowData.records.length > 0) {
					const shadow = shadowData.records[0]
					if (shadow.reported) {
						try {
							const reportedData = JSON.parse(shadow.reported)
							// 构建实时数据映射
							const dataMap = {}
							Object.keys(reportedData).forEach((key) => {
								dataMap[key] = {
									value: reportedData[key],
									updateTime: shadow.updateTime
								}
							})
							realTimeDataMap.value = dataMap
						} catch (e) {
							console.error('解析设备影子数据失败:', e)
						}
					}
				}
			}
		} finally {
			realTimeDataLoading.value = false
		}
	}

	// 加载Modbus寄存器映射
	const loadModbusMapping = async () => {
		if (!deviceData.value.productId) return

		mappingLoading.value = true
		try {
			// 1. 先检查是否有设备级配置
			const deviceMappings = await iotDeviceRegisterApi.iotDeviceRegisterList({
				deviceId: deviceData.value.id
			})

			// 如果有设备级配置，使用设备级
			if (deviceMappings && deviceMappings.length > 0) {
				useDeviceLevelMapping.value = true

				// 加载物模型信息，补充值类型、读写类型等字段
				const properties = await iotThingModelApi.iotThingModelGetProperties({
					productId: deviceData.value.productId,
					modelType: ModelType.PROPERTY
				})

				// 创建物模型映射表
				const thingModelMap = {}
				properties.forEach((prop) => {
					thingModelMap[prop.identifier] = prop
				})

				// 合并设备级配置和物模型信息
				propertyList.value = deviceMappings.map((item) => {
					const thingModel = thingModelMap[item.identifier] || {}
					const result = {
						id: item.thingModelId,
						identifier: item.identifier,
						name: thingModel.name || item.identifier,
						valueType: thingModel.valueType,
						accessMode: thingModel.accessMode,
						valueSpecs: thingModel.valueSpecs,
						registerAddress: item.registerAddress,
						functionCode: item.functionCode,
						dataType: item.dataType,
						deviceMappingId: item.id,
						extJson: thingModel.extJson
					}
								
					// 从extJson中恢复S7协议的额外字段
					if (item.extJson) {
						try {
							const extData = typeof item.extJson === 'string' ? JSON.parse(item.extJson) : item.extJson
							if (extData.area) result.area = extData.area
							if (extData.dbNumber) result.dbNumber = extData.dbNumber
							if (extData.dataTypePrefix) result.dataTypePrefix = extData.dataTypePrefix
							if (extData.offset != null) result.offset = extData.offset
							if (extData.bitIndex != null) result.bitIndex = extData.bitIndex
						} catch (e) {
							console.error('解析extJson失败', e)
						}
					}
								
					return result
				})
				return
			}

			// 2. 没有设备级配置，使用产品级（物模型）
			useDeviceLevelMapping.value = false
			const properties = await iotThingModelApi.iotThingModelGetProperties({
				productId: deviceData.value.productId,
				modelType: ModelType.PROPERTY
			})

			// 从extJson中读取寄存器地址、功能码和数据类型
			propertyList.value = (properties || []).map((item) => {
				let registerAddress = null
				let functionCode = null
				let dataType = null

				if (item.extJson) {
					try {
						const extJson = typeof item.extJson === 'string' ? JSON.parse(item.extJson) : item.extJson
						registerAddress = extJson.registerAddress
						functionCode = extJson.functionCode
						dataType = extJson.dataType
					} catch (e) {
						console.error('解析extJson失败', e)
					}
				}

				return {
					...item,
					registerAddress,
					functionCode,
					dataType
				}
			})
		} finally {
			mappingLoading.value = false
		}
	}

	// 切换映射模式
	const onMappingModeChange = async (useDeviceLevel) => {
		if (useDeviceLevel) {
			// 切换到设备级：从产品级克隆配置
			const properties = await iotThingModelApi.iotThingModelGetProperties({
				productId: deviceData.value.productId,
				modelType: ModelType.PROPERTY
			})
			
			// 从extJson中读取寄存器地址作为初始值
			propertyList.value = (properties || []).map((item) => {
				let registerAddress = null
				let functionCode = null
				let dataType = null
				
				if (item.extJson) {
					try {
						const extJson = typeof item.extJson === 'string' ? JSON.parse(item.extJson) : item.extJson
						registerAddress = extJson.registerAddress
						functionCode = extJson.functionCode
						dataType = extJson.dataType
					} catch (e) {
						console.error('解析extJson失败', e)
					}
				}
				
				return {
					...item,
					registerAddress,
					functionCode,
					dataType
				}
			})
			
			message.info('已切换到设备级配置，修改后请点击"保存设备级配置"')
		} else {
			// 切换到产品级：重新加载
			loadModbusMapping()
		}
	}

	// 保存设备级映射
	const saveDeviceLevelMapping = async () => {
		try {
			const protocolType = deviceData.value.protocolType?.toUpperCase()
			const isS7Protocol = protocolType === 'S7' || protocolType === 'TCP'
			
			// 构建设备级映射数据
			const mappings = propertyList.value
				.filter((item) => {
					// S7协议：检查 identifier 是否有效（不仅是属性标识符，应该是完整地址格式）
					if (isS7Protocol) {
						// S7地址格式：DB1.DBD0 或 MW100 等
						return item.identifier && (item.identifier.includes('DB') || item.identifier.includes('M') || item.identifier.includes('I') || item.identifier.includes('Q'))
					}
					// Modbus协议：检查 registerAddress
					return item.registerAddress != null
				})
				.map((item) => {
					const mapping = {
						thingModelId: item.id,
						identifier: item.identifier,
						registerAddress: item.registerAddress,
						functionCode: item.functionCode,
						dataType: item.dataType,
						enabled: true
					}
					
					// S7协议：将额外字段序列化到 extJson
					if (isS7Protocol) {
						const extData = {}
						if (item.area) extData.area = item.area
						if (item.dbNumber) extData.dbNumber = item.dbNumber
						if (item.dataTypePrefix) extData.dataTypePrefix = item.dataTypePrefix
						if (item.offset != null) extData.offset = item.offset
						if (item.bitIndex != null) extData.bitIndex = item.bitIndex
						
						if (Object.keys(extData).length > 0) {
							mapping.extJson = JSON.stringify(extData)
						}
					}
					
					return mapping
				})

			if (mappings.length === 0) {
				if (isS7Protocol) {
					message.warning('请至少配置一个寄存器映射，点击"构建"按钮配置S7地址')
				} else {
					message.warning('请至少配置一个寄存器映射')
				}
				return
			}

			await iotDeviceRegisterApi.iotDeviceRegisterBatchSave({
				deviceId: deviceData.value.id,
				mappings: mappings
			})

			message.success('设备级寄存器映射保存成功')
			loadModbusMapping()
		} catch (e) {
			message.error('保存失败: ' + e.message)
		}
	}

	// 清除设备级映射
	const deleteDeviceLevelMapping = async () => {
		try {
			await iotDeviceRegisterApi.iotDeviceRegisterDelete({
				deviceId: deviceData.value.id
			})

			message.success('设备级寄存器映射已清除')
			useDeviceLevelMapping.value = false
			loadModbusMapping()
		} catch (e) {
			message.error('清除失败: ' + e.message)
		}
	}

	// 注册SSE消息处理器
	const registerSSEHandler = () => {
		sseMessageHandler.value = (message) => {
			if (!open.value || !deviceData.value.id) return

			// 只处理当前设备的消息
			if (message.deviceId !== deviceData.value.id) return

			const now = new Date().toLocaleTimeString('zh-CN', {
				hour12: false,
				hour: '2-digit',
				minute: '2-digit',
				second: '2-digit',
				fractionalSecondDigits: 3
			})

			switch (message.type) {
				case SSEMessageType.DEVICE_STATUS:
					// 设备状态变化，更新基本信息
					if (message.status) {
						deviceData.value.deviceStatus = message.status
						if (message.ipAddress) {
							deviceData.value.ipAddress = message.ipAddress
						}
						// 设备上线时，更新最后在线时间
						if (message.status === 'ONLINE') {
							deviceData.value.lastOnlineTime = new Date(message.timestamp).toLocaleString('zh-CN', {
								year: 'numeric',
								month: '2-digit',
								day: '2-digit',
								hour: '2-digit',
								minute: '2-digit',
								second: '2-digit',
								hour12: false
							})
						}
						console.log('✅ 设备状态已更新:', message.status, '最后在线时间:', deviceData.value.lastOnlineTime)
					}
					break
				case SSEMessageType.DEVICE_DATA:
					// 设备数据上报,刷新实时趋势图表
					// 如果当前在实时趋势Tab,追加数据到图表
					if (activeTab.value === 'deviceData' && message.data) {
						addRealtimeChartData(message.data, message.timestamp)
					}
					// 同时更新实时数据和控制表单
					if (message.data) {
						// 使用Vue 3的响应式API强制触发更新
						const newDataMap = { ...realTimeDataMap.value }
						Object.keys(message.data).forEach((key) => {
							newDataMap[key] = {
								value: message.data[key],
								updateTime: new Date(message.timestamp).toLocaleString()
							}
						})
						realTimeDataMap.value = newDataMap
					}
					break
				case SSEMessageType.DEVICE_SHADOW:
					// 设备影子变化,直接使用SSE消息更新实时数据
					if (message.reported) {
						try {
							const reportedData = JSON.parse(message.reported)
							// 使用Vue 3的响应式API强制触发更新
							const newDataMap = { ...realTimeDataMap.value }
							Object.keys(reportedData).forEach((key) => {
								newDataMap[key] = {
									value: reportedData[key],
									updateTime: new Date(message.timestamp).toLocaleString()
								}
							})
							realTimeDataMap.value = newDataMap
						} catch (e) {
							console.error('❌ 解析设备影子数据失败:', e)
						}
					}
					break
				case SSEMessageType.DEVICE_EVENT:
					// 设备事件上报
					// 添加到事件列表（最多保存50条）
					if (message.eventType) {
						// 从物模型中查找事件名称
						const event = thingModelEvents.value.find((e) => e.identifier === message.eventType)
						const eventName = event ? event.name : message.eventType

						recentEvents.value.unshift({
							eventName: eventName,
							eventData: message.eventData ? JSON.stringify(message.eventData) : '-',
							timestamp: new Date(message.timestamp).toLocaleString('zh-CN', {
								year: 'numeric',
								month: '2-digit',
								day: '2-digit',
								hour: '2-digit',
								minute: '2-digit',
								second: '2-digit',
								hour12: false
							})
						})

						// 保持50条
						if (recentEvents.value.length > 50) {
							recentEvents.value = recentEvents.value.slice(0, 50)
						}
					}
					break
			}
		}
		// 存储到全局,供父组件调用
		window.__deviceDetailSSEHandler__ = sseMessageHandler.value
	}

	// 注销SSE消息处理器
	const unregisterSSEHandler = () => {
		sseMessageHandler.value = null
		if (window.__deviceDetailSSEHandler__) {
			delete window.__deviceDetailSSEHandler__
		}
	}

	// 监听Tab切换，切换到实时趋势Tab时清空数据并初始化图表
	watch(activeTab, async (newTab) => {
		if (newTab === 'deviceData') {
			// 清空之前的数据，从当前时刻开始记录
			realtimeChartData.value = []
			await nextTick()
			// 确保物模型已加载
			if (thingModelProperties.value.length === 0) {
				console.log('⚠️ 物模型未加载，等待加载完成..')
				await loadRealTimeData()
			}
			console.log('✅ 切换到实时趋势Tab，开始收集数据')
		}
	})

	// 组件卸载时注销监听
	onBeforeUnmount(() => {
		unregisterSSEHandler()
	})

	defineExpose({
		onOpen
	})
</script>

<style scoped>
	pre {
		margin: 0;
	}
</style>
