<template>
	<div class="action-builder">
		<div v-for="(action, index) in actions" :key="index" class="action-item">
			<a-form layout="vertical">
				<a-row :gutter="12" align="middle">
					<!-- 动作类型 -->
					<a-col :span="4">
						<a-form-item label="动作类型" style="margin-bottom: 12px">
							<a-select v-model:value="action.type" @change="onActionTypeChange(index)">
								<a-select-option value="deviceCommand">设备指令</a-select-option>
								<a-select-option value="notification">通知</a-select-option>
								<a-select-option value="webhook">WebHook</a-select-option>
							</a-select>
						</a-form-item>
					</a-col>

					<!-- 设备指令简略信息 -->
					<template v-if="action.type === 'deviceCommand'">
						<a-col :span="5">
							<a-form-item label="目标设备" style="margin-bottom: 12px">
								<a-select
									v-model:value="action.deviceId"
									placeholder="选择设备"
									show-search
									@change="onDeviceChange(index)"
									:filter-option="filterOption"
								>
									<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
										{{ device.deviceName }}
									</a-select-option>
								</a-select>
							</a-form-item>
						</a-col>
						<a-col :span="5">
							<a-form-item label="服务指令" style="margin-bottom: 12px">
								<a-select
									v-model:value="action.command"
									placeholder="选择指令"
									@change="onCommandChange(index)"
									:loading="action._loading"
									:disabled="!action.deviceId"
								>
									<a-select-option
										v-for="service in action._serviceList"
										:key="service.identifier"
										:value="service.identifier"
									>
										{{ service.name }}
									</a-select-option>
								</a-select>
							</a-form-item>
						</a-col>
						<a-col :span="5">
							<a-form-item label="指令参数" style="margin-bottom: 12px">
								<a-tag v-if="action._inputParams && action._inputParams.length > 0" color="blue">
									{{ action._inputParams.length }} 个参数
								</a-tag>
								<span v-else style="color: #999; font-size: 12px">无参数</span>
							</a-form-item>
						</a-col>
					</template>

					<!-- 通知简略信息 -->
					<template v-else-if="action.type === 'notification'">
						<a-col :span="4">
							<a-form-item label="通知渠道" style="margin-bottom: 12px">
								<a-select v-model:value="action.channel" placeholder="选择渠道">
									<a-select-option value="sms">短信</a-select-option>
									<a-select-option value="email">邮件</a-select-option>
									<a-select-option value="dingtalk">钉钉</a-select-option>
									<a-select-option value="alert">系统告警</a-select-option>
								</a-select>
							</a-form-item>
						</a-col>
						<a-col :span="5">
							<a-form-item label="接收人" style="margin-bottom: 12px">
								<a-input v-model:value="action.target" placeholder="接收人" />
							</a-form-item>
						</a-col>
						<a-col :span="6">
							<a-form-item label="通知标题" style="margin-bottom: 12px">
								<a-input v-model:value="action.title" placeholder="通知标题" />
							</a-form-item>
						</a-col>
					</template>

					<!-- WebHook简略信息 -->
					<template v-else-if="action.type === 'webhook'">
						<a-col :span="3">
							<a-form-item label="请求方法" style="margin-bottom: 12px">
								<a-select v-model:value="action.method">
									<a-select-option value="POST">POST</a-select-option>
									<a-select-option value="GET">GET</a-select-option>
									<a-select-option value="PUT">PUT</a-select-option>
								</a-select>
							</a-form-item>
						</a-col>
						<a-col :span="12">
							<a-form-item label="WebHook URL" style="margin-bottom: 12px">
								<a-input v-model:value="action.url" placeholder="https://example.com/webhook" />
							</a-form-item>
						</a-col>
					</template>

					<!-- 操作按钮 -->
					<a-col :span="3" style="text-align: right">
						<a-form-item label=" " style="margin-bottom: 12px">
							<a-space size="small">
								<a-button
									v-if="hasConfig(action)"
									type="link"
									size="small"
									@click="openConfigModal(index)"
									style="padding: 0"
								>
									配置
								</a-button>
								<a-button type="text" danger size="small" @click="removeAction(index)" style="padding: 0">
									<DeleteOutlined />
								</a-button>
							</a-space>
						</a-form-item>
					</a-col>
				</a-row>
			</a-form>
		</div>

		<a-button type="dashed" block @click="addAction" style="margin-top: 8px"> <PlusOutlined /> 添加动作 </a-button>

		<!-- 配置弹窗 -->
		<a-modal
			v-model:open="configModalVisible"
			:title="getConfigTitle()"
			width="600px"
			@ok="handleConfigOk"
			@cancel="handleConfigCancel"
		>
			<a-form layout="vertical" v-if="currentConfigAction">
				<!-- 设备指令参数配置 -->
				<template v-if="currentConfigAction.type === 'deviceCommand' && currentConfigAction._inputParams">
					<a-form-item
						v-for="param in currentConfigAction._inputParams"
						:key="param.identifier"
						:label="param.identifier === 'value' ? getValueParamLabel(currentConfigAction) : param.name"
					>
						<!-- 特殊处理：value参数根据选中的属性类型动态渲染 -->
						<template v-if="param.identifier === 'value'">
							<!-- 布尔类型属性：下拉选择 -->
							<a-select
								v-if="getSelectedPropertyType(currentConfigAction) === 'bool'"
								v-model:value="currentConfigAction.params[param.identifier]"
								placeholder="请选择"
							>
								<a-select-option :value="true">{{
									getBoolLabelFromProperty(currentConfigAction, param, true)
								}}</a-select-option>
								<a-select-option :value="false">{{
									getBoolLabelFromProperty(currentConfigAction, param, false)
								}}</a-select-option>
							</a-select>
							<!-- 整数类型属性：数字输入框 -->
							<a-input-number
								v-else-if="getSelectedPropertyType(currentConfigAction) === 'int'"
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请输入' + param.name"
								style="width: 100%"
							/>
							<!-- 浮点数类型属性：数字输入框 -->
							<a-input-number
								v-else-if="['float', 'double'].includes(getSelectedPropertyType(currentConfigAction))"
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请输入' + param.name"
								:step="0.1"
								style="width: 100%"
							/>
							<!-- 枚举类型属性：下拉选择 -->
							<a-select
								v-else-if="getSelectedPropertyType(currentConfigAction) === 'enum'"
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请选择' + param.name"
							>
								<a-select-option
									v-for="(label, value) in getSelectedPropertyEnumOptions(currentConfigAction)"
									:key="value"
									:value="parseInt(value)"
								>
									{{ label }}
								</a-select-option>
							</a-select>
							<!-- 其他类型：文本输入框 -->
							<a-input
								v-else
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请输入' + param.name"
							/>
						</template>
						<!-- 普通参数：按服务参数定义的dataType渲染 -->
						<template v-else>
							<!-- 布尔类型 -->
							<a-select
								v-if="param.dataType === 'bool'"
								v-model:value="currentConfigAction.params[param.identifier]"
								placeholder="请选择"
							>
								<a-select-option :value="true">{{
									getBoolLabelFromProperty(currentConfigAction, param, true)
								}}</a-select-option>
								<a-select-option :value="false">{{
									getBoolLabelFromProperty(currentConfigAction, param, false)
								}}</a-select-option>
							</a-select>
							<!-- 枚举类型 -->
							<a-select
								v-else-if="param.dataType === 'enum'"
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请选择' + param.name"
							>
								<a-select-option
									v-for="(label, value) in getEnumOptions(param.specs)"
									:key="value"
									:value="parseInt(value)"
								>
									{{ label }}
								</a-select-option>
							</a-select>
							<!-- 可写属性：从可写属性中选择（通用） -->
							<a-select
								v-else-if="
									param.identifier === 'output' || param.identifier === 'property' || param.identifier === 'attribute'
								"
								v-model:value="currentConfigAction.params[param.identifier]"
								placeholder="选择可写属性"
							>
								<a-select-option
									v-for="prop in getWritableProperties(currentConfigAction)"
									:key="prop.identifier"
									:value="prop.identifier"
								>
									{{ prop.label }}
								</a-select-option>
							</a-select>
							<!-- 整数类型 -->
							<a-input-number
								v-else-if="param.dataType === 'int'"
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请输入' + param.name"
								style="width: 100%"
							/>
							<!-- 浮点数类型 -->
							<a-input-number
								v-else-if="param.dataType === 'float' || param.dataType === 'double'"
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请输入' + param.name"
								:step="0.1"
								style="width: 100%"
							/>
							<!-- 文本类型 -->
							<a-input
								v-else
								v-model:value="currentConfigAction.params[param.identifier]"
								:placeholder="'请输入' + param.name"
							/>
						</template>
					</a-form-item>
				</template>

				<!-- 通知配置 -->
				<template v-else-if="currentConfigAction.type === 'notification'">
					<a-form-item label="告警级别" v-if="currentConfigAction.channel === 'alert'">
						<a-select v-model:value="currentConfigAction.level">
							<a-select-option value="INFO">信息</a-select-option>
							<a-select-option value="WARNING">警告</a-select-option>
							<a-select-option value="ERROR">错误</a-select-option>
						</a-select>
					</a-form-item>
					<a-form-item label="通知内容">
						<a-textarea v-model:value="currentConfigAction.content" placeholder="请输入通知内容" :rows="4" />
					</a-form-item>
				</template>

				<!-- WebHook配置 -->
				<template v-else-if="currentConfigAction.type === 'webhook'">
					<a-form-item label="请求头 (JSON)">
						<a-textarea
							v-model:value="currentConfigAction.headers"
							placeholder='{"Authorization": "Bearer token"}'
							:rows="3"
						/>
					</a-form-item>
					<a-form-item label="请求体 (JSON)">
						<a-textarea v-model:value="currentConfigAction.body" placeholder='{"deviceId": "${deviceId}"}' :rows="3" />
					</a-form-item>
				</template>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="ActionBuilder">
	import { ref, onMounted } from 'vue'
	import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'

	const actions = ref([])
	const deviceList = ref([])
	const configModalVisible = ref(false)
	const currentConfigIndex = ref(-1)
	const currentConfigAction = ref(null)

	// 添加动作
	const addAction = () => {
		actions.value.push({
			type: 'deviceCommand',
			deviceId: '',
			command: '',
			params: {},
			_serviceList: [],
			_deviceProperties: [],
			_inputParams: [],
			_loading: false
		})
	}

	// 移除动作
	const removeAction = (index) => {
		actions.value.splice(index, 1)
	}

	// 判断是否需要显示配置按钮
	const hasConfig = (action) => {
		if (action.type === 'deviceCommand') {
			return action._inputParams && action._inputParams.length > 0
		}
		if (action.type === 'notification') {
			return true
		}
		if (action.type === 'webhook') {
			return true
		}
		return false
	}

	// 打开配置弹窗
	const openConfigModal = (index) => {
		currentConfigIndex.value = index
		currentConfigAction.value = actions.value[index]
		configModalVisible.value = true
	}

	// 获取配置弹窗标题
	const getConfigTitle = () => {
		if (!currentConfigAction.value) return '配置'
		if (currentConfigAction.value.type === 'deviceCommand') return '指令参数配置'
		if (currentConfigAction.value.type === 'notification') return '通知详细配置'
		if (currentConfigAction.value.type === 'webhook') return 'WebHook详细配置'
		return '配置'
	}

	// 配置确认
	const handleConfigOk = () => {
		configModalVisible.value = false
		currentConfigIndex.value = -1
		currentConfigAction.value = null
	}

	// 配置取消
	const handleConfigCancel = () => {
		configModalVisible.value = false
		currentConfigIndex.value = -1
		currentConfigAction.value = null
	}

	// 动作类型改变
	const onActionTypeChange = (index) => {
		const action = actions.value[index]
		// 清除旧的配置
		const type = action.type
		actions.value[index] = { type }

		// 设置默认值
		if (type === 'notification') {
			actions.value[index] = {
				type: 'notification',
				channel: 'alert',
				target: '',
				title: '',
				content: '',
				level: 'WARNING'
			}
		} else if (type === 'deviceCommand') {
			actions.value[index] = {
				type: 'deviceCommand',
				deviceId: '',
				command: '',
				params: {},
				_serviceList: [],
				_deviceProperties: [],
				_inputParams: [],
				_loading: false
			}
		} else if (type === 'webhook') {
			actions.value[index] = {
				type: 'webhook',
				url: '',
				method: 'POST',
				headers: '',
				body: ''
			}
		}
	}

	// 获取枚举选项
	const getEnumOptions = (specsStr) => {
		if (!specsStr) return {}
		try {
			return JSON.parse(specsStr)
		} catch (e) {
			return {}
		}
	}

	// 设备改变 - 加载该设备的物模型服务
	const onDeviceChange = async (index) => {
		const action = actions.value[index]
		if (!action.deviceId) return

		// 查找设备信息获取productId
		const device = deviceList.value.find((d) => d.id === action.deviceId)
		if (!device || !device.productId) return

		// 加载该产品的物模型服务列表
		action._loading = true
		try {
			// 加载服务列表
			const thingModels = await iotThingModelApi.iotThingModelGetProperties({
				productId: device.productId,
				modelType: 'SERVICE'
			})
			action._serviceList = thingModels || []

			// 加载属性列表（用于提取DO编号）
			const properties = await iotThingModelApi.iotThingModelGetProperties({
				productId: device.productId,
				modelType: 'PROPERTY'
			})
			action._deviceProperties = properties || []

			// 清空之前选择的指令
			action.command = ''
			action._inputParams = []
			action.params = {}
		} catch (e) {
			console.error('加载服务列表失败', e)
			action._serviceList = []
			action._deviceProperties = []
		} finally {
			action._loading = false
		}
	}

	// 指令改变 - 加载输入参数
	const onCommandChange = (index) => {
		const action = actions.value[index]
		if (!action.command || !action._serviceList) return

		// 查找选中的服务
		const service = action._serviceList.find((s) => s.identifier === action.command)
		if (!service || !service.extJson) {
			action._inputParams = []
			action.params = {}
			return
		}

		// 解析extJson获取输入参数
		try {
			const extData = JSON.parse(service.extJson)
			action._inputParams = extData.inputParams || []
			// 初始化参数默认值
			action.params = {}
			action._inputParams.forEach((param) => {
				// 布尔类型默认false
				if (param.dataType === 'bool') {
					action.params[param.identifier] = false
				}
			})
		} catch (e) {
			console.error('解析服务参数失败', e)
			action._inputParams = []
			action.params = {}
		}
	}

	// 获取选中属性的值类型
	const getSelectedPropertyType = (action) => {
		if (!action || !action.params || !action._deviceProperties) {
			return null
		}

		// 获取选中的identifier
		const selectedIdentifier = action.params.output || action.params.property || action.params.attribute

		if (!selectedIdentifier) {
			return null
		}

		// 查找对应的属性
		const property = action._deviceProperties.find((p) => p.identifier === selectedIdentifier)

		return property ? property.valueType : null
	}

	// 获取选中属性的枚举选项
	const getSelectedPropertyEnumOptions = (action) => {
		if (!action || !action.params || !action._deviceProperties) {
			return {}
		}

		// 获取选中的identifier
		const selectedIdentifier = action.params.output || action.params.property || action.params.attribute

		if (!selectedIdentifier) {
			return {}
		}

		// 查找对应的属性
		const property = action._deviceProperties.find((p) => p.identifier === selectedIdentifier)

		if (!property || !property.valueSpecs) {
			return {}
		}

		try {
			const specs = typeof property.valueSpecs === 'string' ? JSON.parse(property.valueSpecs) : property.valueSpecs
			return specs || {}
		} catch (e) {
			return {}
		}
	}

	// 获取布尔值的显示文本（从物模型属性配置中读取）
	const getBoolLabelFromProperty = (action, param, value) => {
		// 如果参数标识符是 'value'（开关状态），需要根据前面选中的可写属性来获取显示文本
		if (param.identifier === 'value' && action && action.params) {
			// 获取前面选择的identifier（"DO1", "DO2"等）
			const selectedIdentifier = action.params.output || action.params.property || action.params.attribute

			if (selectedIdentifier && action._deviceProperties) {
				// 根据identifier查找对应的属性
				const property = action._deviceProperties.find((p) => p.identifier === selectedIdentifier)

				if (property && property.valueSpecs) {
					try {
						const specs =
							typeof property.valueSpecs === 'string' ? JSON.parse(property.valueSpecs) : property.valueSpecs

						// 兼容两种格式：
						// 1. {"true":"接入","false":"未接入"}
						// 2. {"0":"关闭","1":"打开"}
						if (value) {
							return specs.true || specs['true'] || specs[1] || specs['1'] || '是'
						} else {
							return specs.false || specs['false'] || specs[0] || specs['0'] || '否'
						}
					} catch (e) {
						// 解析失败使用默认值
					}
				}
			}
		}

		// 如果没有找到属性配置，尝试从服务参数的specs读取（兼容旧逻辑）
		if (param && param.specs) {
			try {
				const specs = typeof param.specs === 'string' ? JSON.parse(param.specs) : param.specs
				if (value) {
					return specs.true || specs['true'] || specs[1] || specs['1'] || '是'
				} else {
					return specs.false || specs['false'] || specs[0] || specs['0'] || '否'
				}
			} catch (e) {
				// 解析失败
			}
		}

		// 默认值
		return value ? '是' : '否'
	}

	// 获取value参数的动态label
	const getValueParamLabel = (action) => {
		if (!action || !action.params || !action._deviceProperties) {
			return '属性值'
		}

		// 获取选中的identifier
		const selectedIdentifier = action.params.output || action.params.property || action.params.attribute

		if (!selectedIdentifier) {
			return '属性值'
		}

		// 查找对应的属性
		const property = action._deviceProperties.find((p) => p.identifier === selectedIdentifier)

		if (!property) {
			return '属性值'
		}

		// 返回属性名称
		return property.name ? `${property.name}的值` : '属性值'
	}

	// 获取可写的属性列表（accessMode包含W的属性）
	const getWritableProperties = (action) => {
		if (!action || !action.deviceId || !action._deviceProperties) {
			// 如果没有加载属性，返回空数组
			return []
		}

		// 筛选可写属性（accessMode = 'W' 或 'RW'）
		const writableProps = action._deviceProperties.filter((prop) => {
			return prop.accessMode && (prop.accessMode === 'W' || prop.accessMode === 'RW')
		})

		// 转换为下拉选项格式：value和label都使用identifier
		return writableProps.map((prop) => {
			return {
				identifier: prop.identifier,
				label: `${prop.name} (${prop.identifier})`
			}
		})
	}

	// 加载设备列表
	const loadDevices = () => {
		iotDeviceApi.iotDevicePage({ current: 1, size: 1000 }).then((data) => {
			deviceList.value = data.records || []
		})
	}

	// 过滤设备选项
	const filterOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 转换为JSON（过滤掉以_开头的辅助字段）
	const toJSON = () => {
		// 深度克隆并过滤掉辅助字段
		const cleanActions = actions.value.map((action) => {
			const cleanAction = {}
			for (const key in action) {
				// 只保留不以_开头的字段
				if (!key.startsWith('_')) {
					cleanAction[key] = action[key]
				}
			}
			return cleanAction
		})
		return JSON.stringify(cleanActions)
	}

	// 从JSON加载
	const fromJSON = async (json) => {
		if (json) {
			try {
				const data = typeof json === 'string' ? JSON.parse(json) : json
				const actionsData = Array.isArray(data) ? data : []

				// 确保设备列表已加载
				if (deviceList.value.length === 0) {
					await new Promise((resolve) => {
						iotDeviceApi.iotDevicePage({ current: 1, size: 1000 }).then((data) => {
							deviceList.value = data.records || []
							resolve()
						})
					})
				}

				// 为每个动作初始化辅助数据
				const loadPromises = actionsData.map(async (actionData) => {
					const action = {
						...actionData,
						_serviceList: [],
						_deviceProperties: [],
						_inputParams: [],
						_loading: false
					}

					// 如果是设备指令动作，加载设备的服务和属性
					if (action.type === 'deviceCommand' && action.deviceId) {
						const device = deviceList.value.find((d) => d.id === action.deviceId)
						if (device && device.productId) {
							// 加载服务列表
							const services = await iotThingModelApi.iotThingModelGetProperties({
								productId: device.productId,
								modelType: 'SERVICE'
							})
							action._serviceList = services || []

							// 加载属性列表
							const properties = await iotThingModelApi.iotThingModelGetProperties({
								productId: device.productId,
								modelType: 'PROPERTY'
							})
							action._deviceProperties = properties || []

							// 如果已经选择了服务，加载服务的输入参数
							if (action.command) {
								const service = action._serviceList.find((s) => s.identifier === action.command)
								if (service && service.extJson) {
									try {
										const extData = typeof service.extJson === 'string' ? JSON.parse(service.extJson) : service.extJson
										action._inputParams = extData.inputParams || []
									} catch (e) {
										// 解析失败
									}
								}
							}
						}
					}

					return action
				})

				// 等待所有动作的数据加载完成
				actions.value = await Promise.all(loadPromises)
			} catch (e) {
				console.error('解析执行动作失败', e)
				actions.value = []
			}
		}
	}

	// 验证
	const validate = () => {
		if (actions.value.length === 0) {
			return { valid: false, message: '请至少添加一个执行动作' }
		}

		for (let i = 0; i < actions.value.length; i++) {
			const action = actions.value[i]

			if (action.type === 'notification') {
				if (!action.channel) {
					return { valid: false, message: `动作${i + 1}: 请选择通知渠道` }
				}
				if (!action.content) {
					return { valid: false, message: `动作${i + 1}: 请输入通知内容` }
				}
			} else if (action.type === 'deviceCommand') {
				if (!action.deviceId) {
					return { valid: false, message: `动作${i + 1}: 请选择目标设备` }
				}
				if (!action.command) {
					return { valid: false, message: `动作${i + 1}: 请选择服务指令` }
				}
			} else if (action.type === 'webhook') {
				if (!action.url) {
					return { valid: false, message: `动作${i + 1}: 请输入WebHook URL` }
				}
				if (!action.method) {
					return { valid: false, message: `动作${i + 1}: 请选择请求方法` }
				}
				// 验证headers JSON格式（如果有）
				if (action.headers) {
					try {
						JSON.parse(action.headers)
					} catch (e) {
						return { valid: false, message: `动作${i + 1}: 请求头必须是有效的JSON格式` }
					}
				}
				// 验证body JSON格式（如果有）
				if (action.body) {
					try {
						JSON.parse(action.body)
					} catch (e) {
						return { valid: false, message: `动作${i + 1}: 请求体必须是有效的JSON格式` }
					}
				}
			}
		}

		return { valid: true }
	}

	onMounted(() => {
		loadDevices()
	})

	defineExpose({
		toJSON,
		fromJSON,
		validate,
		actions
	})
</script>

<style scoped>
	.action-builder {
		margin-bottom: 16px;
	}

	.action-item {
		margin-bottom: 12px;
		padding: 12px;
		background: #fafafa;
		border-radius: 4px;
		border: 1px solid #e8e8e8;
	}

	.action-item:last-child {
		margin-bottom: 0;
	}
</style>
