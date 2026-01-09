<template>
	<div>
		<a-card size="small" style="margin-bottom: 16px">
			<a-space>
				<a-tag color="blue">设备当前状态</a-tag>
				<a-alert
					v-if="deviceData.deviceStatus !== 'ONLINE'"
					message="设备离线，无法控制"
					type="warning"
					show-icon
					size="small"
				/>
			</a-space>
		</a-card>

		<a-spin :spinning="realTimeDataLoading">
			<a-empty v-if="!hasSetOutputService" description="该设备不支持控制功能" />
			<div v-else>
				<!-- 8路输出控制面板 -->
				<a-card size="small" title="输出控制">
					<a-row :gutter="[16, 16]">
						<a-col :span="6" v-for="outputNum in availableOutputs" :key="'output-' + outputNum">
							<a-card size="small" hoverable>
								<div style="text-align: center">
									<div style="font-size: 14px; font-weight: 500; margin-bottom: 12px">DO{{ outputNum }}</div>
									<a-switch
										v-model:checked="outputStates[outputNum]"
										:disabled="deviceData.deviceStatus !== 'ONLINE'"
										:loading="outputLoading[outputNum]"
										@change="(checked) => handleOutputChange(outputNum, checked)"
										checked-children="开"
										un-checked-children="关"
									/>
									<div style="margin-top: 8px; font-size: 12px; color: #999">
										{{ outputStates[outputNum] ? '已打开' : '已关闭' }}
									</div>
								</div>
							</a-card>
						</a-col>
					</a-row>
					<template #extra v-if="hasBatchOutputService || hasToggleOutputService">
						<a-space>
							<a-button
								v-if="hasBatchOutputService"
								size="small"
								:disabled="deviceData.deviceStatus !== 'ONLINE'"
								:loading="batchLoading"
								@click="handleBatchControl(true)"
							>
								全部打开
							</a-button>
							<a-button
								v-if="hasBatchOutputService"
								size="small"
								:disabled="deviceData.deviceStatus !== 'ONLINE'"
								:loading="batchLoading"
								@click="handleBatchControl(false)"
							>
								全部关闭
							</a-button>
							<a-button
								v-if="hasToggleOutputService"
								size="small"
								:disabled="deviceData.deviceStatus !== 'ONLINE'"
								:loading="batchLoading"
								@click="handleToggleOutputs"
							>
								反转状态
							</a-button>
						</a-space>
					</template>
				</a-card>

				<!-- 其他服务 -->
				<div v-if="otherServices && otherServices.length > 0" style="margin-top: 24px">
					<a-card size="small" title="其他服务">
						<a-space wrap>
							<a-button
								v-for="service in otherServices"
								:key="service.identifier"
								:disabled="deviceData.deviceStatus !== 'ONLINE'"
								@click="showServiceModal(service)"
							>
								{{ service.name }}
							</a-button>
						</a-space>
					</a-card>
				</div>
			</div>
		</a-spin>

		<!-- 服务调用弹窗 -->
		<a-modal
			v-model:open="serviceModalVisible"
			:title="'调用服务: ' + (currentService?.name || '')"
			@ok="handleServiceInvoke"
			@cancel="serviceModalVisible = false"
		>
			<a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
				<a-form-item v-for="param in serviceInputParams" :key="param.identifier" :label="param.name">
					<a-input-number
						v-if="param.dataType === ServiceParamDataType.INT || param.dataType === ServiceParamDataType.FLOAT"
						v-model:value="serviceParams[param.identifier]"
						:precision="param.dataType === ServiceParamDataType.FLOAT ? 2 : 0"
						style="width: 100%"
					/>
					<a-switch
						v-else-if="param.dataType === ServiceParamDataType.BOOL"
						v-model:checked="serviceParams[param.identifier]"
					/>
					<a-input v-else v-model:value="serviceParams[param.identifier]" />
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup>
	import { ref, computed, watch } from 'vue'
	import { message } from 'ant-design-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import { ModelType, ServiceParamDataType } from '@/utils/iotConstants'

	const props = defineProps({
		deviceData: {
			type: Object,
			default: () => ({})
		},
		thingModelData: {
			type: Array,
			default: () => []
		},
		realTimeDataLoading: {
			type: Boolean,
			default: false
		},
		realTimeDataMap: {
			type: Object,
			default: () => ({})
		}
	})

	// 服务调用相关
	const serviceModalVisible = ref(false)
	const currentService = ref(null)
	const serviceParams = ref({})
	const serviceInputParams = ref({})

	// 输出控制相关(8路IO模块)
	const outputStates = ref({}) // 输出状态 {1: true, 2: false, ...}
	const outputLoading = ref({}) // 各路输出加载状态
	const batchLoading = ref(false) // 批量操作加载状态

	// 从物模型数据中筛选服务
	const thingModelServices = computed(() => {
		return props.thingModelData.filter((item) => item.modelType === ModelType.SERVICE)
	})

	// 是否有setOutput服务(用于显示8路输出控制面板)
	const hasSetOutputService = computed(() => {
		return thingModelServices.value.some((s) => s.identifier === 'setOutput')
	})

	// 是否有setBatchOutputs服务(用于显示全部打开/关闭按钮)
	const hasBatchOutputService = computed(() => {
		return thingModelServices.value.some((s) => s.identifier === 'setBatchOutputs')
	})

	// 是否有toggleOutputs服务(用于显示反转按钮)
	const hasToggleOutputService = computed(() => {
		return thingModelServices.value.some((s) => s.identifier === 'toggleOutputs')
	})

	// 获取所有DO属性的编号(从物模型属性中提取)
	const availableOutputs = computed(() => {
		const outputs = []
		const properties = props.thingModelData.filter((item) => item.modelType === ModelType.PROPERTY)
		properties.forEach((p) => {
			const match = p.identifier.match(/^DO(\d+)$/)
			if (match) {
				outputs.push(parseInt(match[1]))
			}
		})
		return outputs.sort((a, b) => a - b)
	})

	// 其他服务(排除setOutput、setBatchOutputs和toggleOutputs)
	const otherServices = computed(() => {
		return thingModelServices.value.filter(
			(s) => s.identifier !== 'setOutput' && s.identifier !== 'setBatchOutputs' && s.identifier !== 'toggleOutputs'
		)
	})

	// 初始化输出状态(从DO属性中读取)
	const initOutputStates = () => {
		const states = {}
		// 根据实际的DO属性数量初始化
		availableOutputs.value.forEach((i) => {
			const identifier = `DO${i}`
			// 从实时数据中获取值
			const realTimeValue = props.realTimeDataMap[identifier]
			states[i] = realTimeValue?.value === true || realTimeValue?.value === 1
		})
		outputStates.value = states
	}

	// 监听实时数据变化
	watch(
		() => props.realTimeDataMap,
		() => {
			initOutputStates()
		},
		{ deep: true, immediate: true }
	)

	// 显示服务调用弹窗
	const showServiceModal = (service) => {
		currentService.value = service

		// 解析服务输入参数(从extJson中获取)
		let inputParams = []
		if (service.extJson) {
			try {
				const extData = typeof service.extJson === 'string' ? JSON.parse(service.extJson) : service.extJson
				inputParams = extData.inputParams || []
			} catch (e) {
				console.error('解析extJson失败:', e)
			}
		}
		// 兼容旧版本:如果extJson中没有,尝试从inputParams字段读取
		if (inputParams.length === 0 && service.inputParams) {
			try {
				const params = typeof service.inputParams === 'string' ? JSON.parse(service.inputParams) : service.inputParams
				inputParams = params || []
			} catch (e) {
				console.error('解析inputParams失败:', e)
			}
		}

		// 初始化参数默认值
		const defaultParams = {}
		inputParams.forEach((param) => {
			// 根据数据类型设置默认值
			if (param.dataType === 'bool') {
				defaultParams[param.identifier] = false
			} else if (param.dataType === 'int' || param.dataType === 'float') {
				// 解析specs获取最小值或默认值
				let defaultValue = 1 // 默认最小值为1
				if (param.specs) {
					try {
						const specs = typeof param.specs === 'string' ? JSON.parse(param.specs) : param.specs
						if (specs.min !== undefined) {
							defaultValue = specs.min
						}
					} catch (e) {
						// 解析失败使用默认值
					}
				}
				defaultParams[param.identifier] = defaultValue
			} else if (param.dataType === 'string') {
				defaultParams[param.identifier] = ''
			} else if (param.dataType === 'array') {
				defaultParams[param.identifier] = []
			} else {
				defaultParams[param.identifier] = null
			}
		})

		serviceParams.value = defaultParams
		serviceInputParams.value = inputParams
		serviceModalVisible.value = true
	}

	// 执行服务调用
	const handleServiceInvoke = () => {
		if (props.deviceData.deviceStatus !== 'ONLINE') {
			message.warning('设备离线，无法调用服务')
			return
		}

		if (!currentService.value) {
			message.error('服务信息丢失')
			return
		}

		// 调用服务
		iotDeviceApi
			.iotDeviceInvokeService({
				deviceId: props.deviceData.id,
				serviceId: currentService.value.identifier,
				params: serviceParams.value
			})
			.then(() => {
				message.success(`服务调用成功: ${currentService.value.name}`)
				serviceModalVisible.value = false
			})
			.catch((err) => {
				message.error('服务调用失败: ' + (err.message || '未知错误'))
			})
	}

	// 单路输出控制
	const handleOutputChange = (outputNum, checked) => {
		if (props.deviceData.deviceStatus !== 'ONLINE') {
			message.warning('设备离线，无法控制')
			// 恢复状态
			outputStates.value[outputNum] = !checked
			return
		}

		outputLoading.value[outputNum] = true

		iotDeviceApi
			.iotDeviceInvokeService({
				deviceId: props.deviceData.id,
				serviceId: 'setOutput',
				params: {
					output: outputNum,
					value: checked
				}
			})
			.then(() => {
				message.success(`DO${outputNum} ${checked ? '已打开' : '已关闭'}`)
			})
			.catch((err) => {
				message.error(`控制失败: ${err.message || '未知错误'}`)
				// 恢复状态
				outputStates.value[outputNum] = !checked
			})
			.finally(() => {
				outputLoading.value[outputNum] = false
			})
	}

	// 批量控制(全部打开/关闭)
	const handleBatchControl = (turnOn) => {
		if (props.deviceData.deviceStatus !== 'ONLINE') {
			message.warning('设备离线，无法控制')
			return
		}

		batchLoading.value = true

		iotDeviceApi
			.iotDeviceInvokeService({
				deviceId: props.deviceData.id,
				serviceId: 'setBatchOutputs',
				params: {
					outputs: availableOutputs.value,
					value: turnOn
				}
			})
			.then(() => {
				message.success(`已${turnOn ? '全部打开' : '全部关闭'}`)
				// 更新所有输出状态
				availableOutputs.value.forEach((i) => {
					outputStates.value[i] = turnOn
				})
			})
			.catch((err) => {
				message.error(`批量控制失败: ${err.message || '未知错误'}`)
			})
			.finally(() => {
				batchLoading.value = false
			})
	}

	// 反转输出状态
	const handleToggleOutputs = () => {
		if (props.deviceData.deviceStatus !== 'ONLINE') {
			message.warning('设备离线，无法控制')
			return
		}

		batchLoading.value = true

		iotDeviceApi
			.iotDeviceInvokeService({
				deviceId: props.deviceData.id,
				serviceId: 'toggleOutputs',
				params: {}
			})
			.then(() => {
				message.success('已反转所有输出状态')
				// 反转本地状态
				availableOutputs.value.forEach((i) => {
					outputStates.value[i] = !outputStates.value[i]
				})
			})
			.catch((err) => {
				message.error(`反转失败: ${err.message || '未知错误'}`)
			})
			.finally(() => {
				batchLoading.value = false
			})
	}
</script>
