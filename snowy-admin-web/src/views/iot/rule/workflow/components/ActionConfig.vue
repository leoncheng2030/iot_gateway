<template>
	<div>
		<a-form-item label="动作类型">
			<a-select v-model:value="properties.actionType" @change="handleActionTypeChange">
				<a-select-option value="deviceCommand">设备指令</a-select-option>
				<a-select-option value="notification">发送通知</a-select-option>
				<a-select-option value="webhook">Webhook</a-select-option>
			</a-select>
		</a-form-item>

		<!-- 设备指令 -->
		<template v-if="properties.actionType === 'deviceCommand'">
			<a-form-item label="目标设备">
				<a-select
					v-model:value="properties.targetDeviceId"
					show-search
					placeholder="请选择目标设备"
					@change="handleDeviceChange"
				>
					<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
						{{ device.deviceName }}
					</a-select-option>
				</a-select>
			</a-form-item>

			<a-form-item label="服务指令">
				<a-select
					v-model:value="properties.command"
					placeholder="请选择服务指令"
					:loading="serviceLoading"
					:disabled="!properties.targetDeviceId"
					@change="handleCommandChange"
				>
					<a-select-option v-for="service in serviceList" :key="service.identifier" :value="service.identifier">
						{{ service.name || service.identifier }}
					</a-select-option>
				</a-select>
			</a-form-item>

			<!-- 动态参数配置 -->
			<template v-if="currentInputParams && currentInputParams.length > 0">
				<a-form-item
					v-for="param in currentInputParams"
					:key="param.identifier"
					:label="param.name || param.identifier"
				>
					<!-- 可写属性选择（output, property, attribute） -->
					<a-select
						v-if="param.identifier === 'output' || param.identifier === 'property' || param.identifier === 'attribute'"
						v-model:value="paramValues[param.identifier]"
						placeholder="选择可写属性"
						@change="handleParamChange"
					>
						<a-select-option
							v-for="prop in getWritableProperties()"
							:key="prop.identifier"
							:value="prop.identifier"
						>
							{{ prop.label }}
						</a-select-option>
					</a-select>
					<!-- 布尔类型 -->
					<a-select
						v-else-if="param.dataType === 'bool'"
						v-model:value="paramValues[param.identifier]"
						@change="handleParamChange"
					>
						<a-select-option value="true">是</a-select-option>
						<a-select-option value="false">否</a-select-option>
					</a-select>
					<!-- 枚举类型 -->
					<a-select
						v-else-if="param.dataType === 'enum'"
						v-model:value="paramValues[param.identifier]"
						@change="handleParamChange"
					>
						<a-select-option
							v-for="option in getEnumOptions(param.specs)"
							:key="option.value"
							:value="option.value"
						>
							{{ option.name }}
						</a-select-option>
					</a-select>
					<!-- 数值类型 -->
					<a-input-number
						v-else-if="['int', 'float', 'double', 'long'].includes(param.dataType)"
						v-model:value="paramValues[param.identifier]"
						:precision="param.dataType === 'int' || param.dataType === 'long' ? 0 : 2"
						style="width: 100%"
						:placeholder="'请输入' + (param.name || param.identifier)"
						@change="handleParamChange"
					/>
					<!-- 字符串类型 -->
					<a-input
						v-else
						v-model:value="paramValues[param.identifier]"
						:placeholder="'请输入' + (param.name || param.identifier)"
						@change="handleParamChange"
					/>
				</a-form-item>
			</template>
			
			<!-- 无参数提示 -->
			<a-alert
				v-else-if="properties.command && currentInputParams && currentInputParams.length === 0"
				message="该指令无需配置参数"
				type="info"
				show-icon
				style="margin-bottom: 16px"
			/>
		</template>

		<!-- 发送通知 -->
		<template v-if="properties.actionType === 'notification'">
			<a-form-item label="通知渠道">
				<a-select v-model:value="properties.channel" @change="handleChange">
					<a-select-option value="sms">短信</a-select-option>
					<a-select-option value="email">邮件</a-select-option>
					<a-select-option value="dingtalk">钉钉</a-select-option>
					<a-select-option value="alert">告警</a-select-option>
				</a-select>
			</a-form-item>

			<a-form-item label="标题">
				<a-input v-model:value="properties.title" @change="handleChange" />
			</a-form-item>

			<a-form-item label="内容">
				<a-textarea
					v-model:value="properties.content"
					:rows="3"
					@change="handleChange"
				/>
			</a-form-item>

			<a-form-item label="接收人">
				<a-input
					v-model:value="properties.target"
					placeholder="多个用逗号分隔"
					@change="handleChange"
				/>
			</a-form-item>
		</template>

		<!-- Webhook -->
		<template v-if="properties.actionType === 'webhook'">
			<a-form-item label="URL">
				<a-input
					v-model:value="properties.url"
					placeholder="https://example.com/webhook"
					@change="handleChange"
				/>
			</a-form-item>

			<a-form-item label="请求方法">
				<a-select v-model:value="properties.method" @change="handleChange">
					<a-select-option value="GET">GET</a-select-option>
					<a-select-option value="POST">POST</a-select-option>
				</a-select>
			</a-form-item>

			<a-form-item label="请求体">
				<a-textarea
					v-model:value="properties.body"
					placeholder='{"data": "value"}'
					:rows="3"
					@change="handleChange"
				/>
			</a-form-item>
		</template>
	</div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import iotThingModelApi from '@/api/iot/iotThingModelApi'

const props = defineProps({
	properties: {
		type: Object,
		required: true
	},
	deviceList: {
		type: Array,
		default: () => []
	}
})

const emit = defineEmits(['change'])

// 服务列表加载状态
const serviceLoading = ref(false)
// 服务列表
const serviceList = ref([])
// 当前选中服务的输入参数
const currentInputParams = ref([])
// 设备属性列表（用于选择可写属性）
const deviceProperties = ref([])
// 参数值对象
const paramValues = reactive({})

// 初始化参数值
const initParamValues = () => {
	if (!props.properties.params) {
		return
	}
	
	try {
		const params = typeof props.properties.params === 'string' 
			? JSON.parse(props.properties.params) 
			: props.properties.params
		
		if (params && typeof params === 'object') {
			// 清空旧值
			Object.keys(paramValues).forEach(key => delete paramValues[key])
			// 赋值新值，并进行类型转换
			Object.keys(params).forEach(key => {
				const value = params[key]
				// 如果是布尔值，转换为字符串（兼容旧数据）
				if (typeof value === 'boolean') {
					paramValues[key] = String(value)
				} else {
					paramValues[key] = value
				}
			})
		}
	} catch (e) {
		console.error('解析参数失败:', e)
	}
}

// 监听 properties.params 变化，同步到 paramValues
watch(
	() => props.properties.params,
	() => {
		initParamValues()
	},
	{ immediate: true }
)

const handleChange = () => {
	emit('change')
}

const handleActionTypeChange = () => {
	// 切换动作类型时清空配置
	props.properties.targetDeviceId = undefined
	props.properties.command = undefined
	props.properties.params = undefined
	serviceList.value = []
	currentInputParams.value = []
	Object.keys(paramValues).forEach(key => delete paramValues[key])
	emit('change')
}

// 处理设备变化
const handleDeviceChange = async () => {
	const device = props.deviceList.find(d => d.id === props.properties.targetDeviceId)
	
	if (!device || !device.productId) {
		serviceList.value = []
		currentInputParams.value = []
		return
	}

	// 保存当前的指令和参数（用于后续恢复）
	const savedCommand = props.properties.command
	const savedParams = props.properties.params

	// 加载服务列表
	serviceLoading.value = true
	try {
		const services = await iotThingModelApi.iotThingModelGetProperties({
			productId: device.productId,
			modelType: 'SERVICE'
		})
		serviceList.value = services || []
		
		if (!services || services.length === 0) {
			console.warn('该产品没有配置服务类型的物模型')
		}
		
		// 同时加载设备属性列表（用于选择输出端口等可写属性）
		const properties = await iotThingModelApi.iotThingModelGetProperties({
			productId: device.productId,
			modelType: 'PROPERTY'
		})
		deviceProperties.value = properties || []
		
		// 如果有保存的指令，恢复它
		if (savedCommand && services && services.find(s => s.identifier === savedCommand)) {
			props.properties.command = savedCommand
			props.properties.params = savedParams
			// 触发指令变化，加载参数定义
			handleCommandChange()
		} else {
			// 没有保存的指令或指令不存在，清空
			props.properties.command = undefined
			props.properties.params = undefined
			currentInputParams.value = []
			Object.keys(paramValues).forEach(key => delete paramValues[key])
		}
	} catch (error) {
		console.error('加载服务列表失败:', error)
		serviceList.value = []
		deviceProperties.value = []
	} finally {
		serviceLoading.value = false
	}

	emit('change')
}

// 处理指令变化
const handleCommandChange = () => {
	if (!props.properties.command) {
		currentInputParams.value = []
		props.properties.params = undefined
		Object.keys(paramValues).forEach(key => delete paramValues[key])
		return
	}

	// 查找选中的服务
	const service = serviceList.value.find(s => s.identifier === props.properties.command)
	
	if (!service || !service.extJson) {
		console.warn('服务不存在或没有 extJson')
		currentInputParams.value = []
		props.properties.params = undefined
		Object.keys(paramValues).forEach(key => delete paramValues[key])
		emit('change')
		return
	}

	// 解析服务的输入参数
	try {
		const extData = JSON.parse(service.extJson)
		currentInputParams.value = extData.inputParams || []
		
		// 如果已经有保存的参数，恢复它们
		if (props.properties.params) {
			try {
				const savedParams = typeof props.properties.params === 'string'
					? JSON.parse(props.properties.params)
					: props.properties.params
				
				if (savedParams && typeof savedParams === 'object') {
					// 清空旧值
					Object.keys(paramValues).forEach(key => delete paramValues[key])
					// 恢复保存的值，并进行类型转换
					Object.keys(savedParams).forEach(key => {
						const value = savedParams[key]
						// 如果是布尔值，转换为字符串（兼容旧数据）
						if (typeof value === 'boolean') {
							paramValues[key] = String(value)
						} else {
							paramValues[key] = value
						}
					})
					emit('change') // 发送变化事件
					return // 直接返回，不要用默认值覆盖
				}
			} catch (e) {
				console.error('恢复参数失败:', e)
			}
		}
		
		// 如果没有保存的参数，初始化默认值
		Object.keys(paramValues).forEach(key => delete paramValues[key])
		currentInputParams.value.forEach(param => {
			if (param.dataType === 'bool') {
				paramValues[param.identifier] = 'false' // 使用字符串而不是布尔值
			} else if (param.dataType === 'int' || param.dataType === 'float' || param.dataType === 'double' || param.dataType === 'long') {
				paramValues[param.identifier] = 0
			} else {
				paramValues[param.identifier] = ''
			}
		})
		
		// 同步到 properties.params
		props.properties.params = JSON.stringify(paramValues)
	} catch (error) {
		console.error('解析服务参数失败:', error)
		currentInputParams.value = []
	}

	emit('change')
}

// 处理参数变化
const handleParamChange = () => {
	// 同步 paramValues 到 properties.params（JSON字符串）
	props.properties.params = JSON.stringify(paramValues)
	emit('change')
}

// 解析枚举选项
const getEnumOptions = (specsStr) => {
	if (!specsStr) return []
	try {
		const specs = typeof specsStr === 'string' ? JSON.parse(specsStr) : specsStr
		// 支持两种格式：
		// 1. { enumList: [{value: 0, name: '关闭'}, {value: 1, name: '打开'}] }
		// 2. { "0": "关闭", "1": "打开" }
		if (specs.enumList && Array.isArray(specs.enumList)) {
			return specs.enumList
		} else {
			// 转换为数组格式
			return Object.entries(specs).map(([value, name]) => ({
				value: isNaN(value) ? value : Number(value),
				name
			}))
		}
	} catch (e) {
		console.error('解析枚举选项失败:', e)
		return []
	}
}

// 获取可写属性列表（用于 output, property, attribute 等参数）
const getWritableProperties = () => {
	if (!deviceProperties.value || deviceProperties.value.length === 0) {
		return []
	}
	
	// 筛选可写属性（accessMode = 'W' 或 'RW'）
	const writableProps = deviceProperties.value.filter(prop => {
		return prop.accessMode && (prop.accessMode === 'W' || prop.accessMode === 'RW')
	})
	
	// 转换为下拉选项格式
	return writableProps.map(prop => ({
		identifier: prop.identifier,
		label: `${prop.name} (${prop.identifier})`,
		valueType: prop.valueType // 保留 valueType 用于 value 参数的控件渲染
	}))
}

// 组件挂载时，如果已经有配置，初始化加载服务列表
onMounted(() => {
	// 如果已经有设备和指令，加载服务列表和参数
	if (props.properties.targetDeviceId && props.properties.command) {
		handleDeviceChange()
	}
})
</script>
