<template>
	<div>
		<a-form-item label="条件类型">
			<a-radio-group v-model:value="properties.conditionType" @change="handleTypeChange">
				<a-radio value="simple">简单条件</a-radio>
				<a-radio value="group">条件组</a-radio>
			</a-radio-group>
		</a-form-item>

		<template v-if="properties.conditionType === 'simple'">
			<a-form-item label="设备来源">
				<a-radio-group v-model:value="properties.deviceSource" @change="handleDeviceSourceChange">
					<a-radio value="inherit">继承</a-radio>
					<a-radio value="specify">指定</a-radio>
				</a-radio-group>
			</a-form-item>
		
			<a-form-item label="设备">
				<!-- 继承模式：显示继承的设备名称，只读 -->
				<a-input
					v-if="properties.deviceSource === 'inherit'"
					:value="inheritedDeviceName"
					readonly
					disabled
					placeholder="自动继承触发器设备"
				>
					<template #prefix>
						<span style="color: #52c41a">🔗</span>
					</template>
				</a-input>
				<!-- 指定模式：下拉选择设备 -->
				<a-select
					v-else
					v-model:value="properties.deviceId"
					show-search
					placeholder="请选择设备"
					@change="handleDeviceChange"
				>
					<a-select-option v-for="device in availableDevices" :key="device.id" :value="device.id">
						{{ device.deviceName }}
					</a-select-option>
				</a-select>
			</a-form-item>
		
			<!-- 提示信息 -->
			<a-alert
				v-if="properties.deviceSource === 'inherit' && !inheritedDeviceName"
				message="请先连接到设备触发器节点"
				type="warning"
				show-icon
				style="margin-bottom: 16px"
			/>
			<a-alert
				v-else-if="properties.deviceSource === 'inherit' && inheritedDeviceName"
				message="设备已继承自上游触发器节点"
				type="info"
				show-icon
				style="margin-bottom: 16px"
			/>

			<a-form-item label="属性">
				<a-select v-model:value="properties.property" @change="handlePropertyChange">
					<a-select-option v-for="prop in deviceProps" :key="prop.identifier" :value="prop.identifier">
						{{ prop.name || prop.identifier }}
					</a-select-option>
				</a-select>
			</a-form-item>

			<a-form-item label="操作符">
				<a-select v-model:value="properties.operator" @change="handleChange">
					<template v-if="currentPropertyType === 'bool'">
						<a-select-option value="==">等于</a-select-option>
						<a-select-option value="!=">不等于</a-select-option>
					</template>
					<template v-else-if="currentPropertyType === 'string' || currentPropertyType === 'enum'">
						<a-select-option value="==">等于</a-select-option>
						<a-select-option value="!=">不等于</a-select-option>
					</template>
					<template v-else>
						<a-select-option value=">">大于</a-select-option>
						<a-select-option value=">=">大于等于</a-select-option>
						<a-select-option value="<">小于</a-select-option>
						<a-select-option value="<=">小于等于</a-select-option>
						<a-select-option value="==">等于</a-select-option>
						<a-select-option value="!=">不等于</a-select-option>
					</template>
				</a-select>
			</a-form-item>

			<a-form-item label="阈值">
				<!-- 布尔类型 -->
				<a-select v-if="currentPropertyType === 'bool'" v-model:value="properties.value" @change="handleChange">
					<template v-if="currentPropertyEnums.length > 0">
						<a-select-option v-for="item in currentPropertyEnums" :key="item.value" :value="item.value">
							{{ item.name }}
						</a-select-option>
					</template>
					<template v-else>
						<a-select-option value="true">true</a-select-option>
						<a-select-option value="false">false</a-select-option>
					</template>
				</a-select>
				<!-- 枚举类型 -->
				<a-select
					v-else-if="currentPropertyType === 'enum'"
					v-model:value="properties.value"
					@change="handleChange"
				>
					<a-select-option v-for="item in currentPropertyEnums" :key="item.value" :value="item.value">
						{{ item.name || item.value }}
					</a-select-option>
				</a-select>
				<!-- 数值类型 -->
				<a-input-number
					v-else-if="isNumericType(currentPropertyType)"
					v-model:value="properties.value"
					:precision="currentPropertyType === 'int' ? 0 : 2"
					style="width: 100%"
					@change="handleChange"
				/>
				<!-- 字符串类型 -->
				<a-input v-else v-model:value="properties.value" @change="handleChange" />
			</a-form-item>
		</template>

		<template v-else>
			<a-form-item label="逻辑关系">
				<a-radio-group v-model:value="properties.logic" @change="handleChange">
					<a-radio value="AND">且(AND)</a-radio>
					<a-radio value="OR">或(OR)</a-radio>
				</a-radio-group>
			</a-form-item>
			<a-alert message="条件组需要连接多个子条件节点" type="info" show-icon style="margin-top: 8px" />
		</template>
	</div>
</template>

<script setup>
import { computed, watch, onMounted } from 'vue'

const props = defineProps({
	properties: {
		type: Object,
		required: true
	},
	availableDevices: {
		type: Array,
		default: () => []
	},
	deviceProps: {
		type: Array,
		default: () => []
	},
	// 继承的设备ID（来自上游触发器）
	inheritedDeviceId: {
		type: String,
		default: ''
	}
})

const emit = defineEmits(['change', 'deviceChange', 'typeChange'])

// 初始化设备来源默认值
const initDeviceSource = () => {
	// 只在简单条件模式下，且 deviceSource 未设置时初始化
	if (props.properties.conditionType === 'simple' && !props.properties.deviceSource) {
		if (props.inheritedDeviceId) {
			// 如果上一个节点是设备触发器，默认继承
			props.properties.deviceSource = 'inherit'
			props.properties.deviceId = props.inheritedDeviceId
			// 触发设备变化事件，加载设备属性
			emit('deviceChange')
		} else {
			// 否则默认指定
			props.properties.deviceSource = 'specify'
		}
		emit('change')
	}
}

// 初始化阈值类型（修复布尔值类型问题）
const initValueType = () => {
	// 如果 value 存在且是布尔类型，转换为字符串
	if (props.properties.value !== undefined && typeof props.properties.value === 'boolean') {
		props.properties.value = String(props.properties.value)
	}
}

// 监听属性变化，修复阈值类型
watch(
	() => props.properties.value,
	(newValue) => {
		// 如果新值是布尔类型，转换为字符串
		if (typeof newValue === 'boolean') {
			props.properties.value = String(newValue)
		}
	},
	{ immediate: true }
)

// 监听 inheritedDeviceId 变化，自动更新设备来源
watch(
	() => props.inheritedDeviceId,
	(newValue, oldValue) => {
		// 只在简单条件模式下处理
		if (props.properties.conditionType !== 'simple') return
		
		// 如果当前是继承模式，更新继承的设备
		if (props.properties.deviceSource === 'inherit') {
			if (newValue) {
				props.properties.deviceId = newValue
				emit('deviceChange')
				emit('change')
			} else {
				// 如果继承的设备消失了，清空设备
				props.properties.deviceId = undefined
				emit('change')
			}
		}
		// 如果当前是指定模式，但从无设备触发器变为有设备触发器，且用户还没选择设备
		else if (props.properties.deviceSource === 'specify') {
			if (newValue && !oldValue && !props.properties.deviceId) {
				// 自动切换为继承模式
				props.properties.deviceSource = 'inherit'
				props.properties.deviceId = newValue
				emit('deviceChange')
				emit('change')
			}
		}
	},
	{ immediate: false }
)

// 组件挂载时初始化
onMounted(() => {
	initDeviceSource()
	initValueType() // 修复布尔值类型
	
	// 如果已经有设备ID和属性,主动加载设备属性数据
	// 检查是否需要加载设备属性：有属性配置但设备属性列表为空
	const hasDeviceId = props.properties.deviceSource === 'inherit' 
		? props.inheritedDeviceId 
		: props.properties.deviceId
			
	if (hasDeviceId && props.properties.property && props.deviceProps.length === 0) {
		// 触发设备变化事件,加载设备属性
		setTimeout(() => {
			emit('deviceChange')
		}, 100) // 延迟一点执行,确保父组件已准备好
	}
})

// 继承的设备名称
const inheritedDeviceName = computed(() => {
	if (!props.inheritedDeviceId) return ''
	const device = props.availableDevices.find((d) => d.id === props.inheritedDeviceId)
	return device ? device.deviceName : ''
})

// 当前选中的属性对象
const currentProperty = computed(() => {
	if (!props.properties.property) return null
	return props.deviceProps.find((p) => p.identifier === props.properties.property)
})

// 当前属性的数据类型
const currentPropertyType = computed(() => {
	return currentProperty.value?.valueType || 'string'
})

// 当前属性的枚举值列表（包括布尔类型的 valueSpecs）
const currentPropertyEnums = computed(() => {
	const specs = currentProperty.value?.valueSpecs
	if (!specs) return []
	try {
		// valueSpecs 可能是字符串或对象
		const specsObj = typeof specs === 'string' ? JSON.parse(specs) : specs
		
		// 如果是布尔类型的 valueSpecs，转换为枚举格式
		if (currentPropertyType.value === 'bool') {
			if (specsObj.true !== undefined && specsObj.false !== undefined) {
				return [
					{ value: 'true', name: specsObj.true },
					{ value: 'false', name: specsObj.false }
				]
			}
		}
		// 如果是枚举类型
		if (currentPropertyType.value === 'enum' && specsObj.enumList) {
			return specsObj.enumList
		}
		return []
	} catch (e) {
		console.error('解析枚举值失败:', e)
		return []
	}
})

// 判断是否为数值类型
const isNumericType = (type) => {
	return ['int', 'float', 'double', 'long'].includes(type)
}

const handleChange = () => {
	emit('change')
}

const handleTypeChange = () => {
	emit('typeChange')
}

const handleDeviceChange = () => {
	emit('deviceChange')
}

// 处理设备来源变化
const handleDeviceSourceChange = () => {
	if (props.properties.deviceSource === 'inherit') {
		// 切换为继承模式
		if (props.inheritedDeviceId) {
			// 自动设置为继承的设备
			props.properties.deviceId = props.inheritedDeviceId
			// 清空属性配置
			props.properties.property = undefined
			props.properties.operator = undefined
			props.properties.value = undefined
			// 触发设备变化事件，加载设备属性
			emit('deviceChange')
		} else {
			// 没有上游触发器，清空设备
			props.properties.deviceId = undefined
		}
	} else {
		// 切换为指定模式，清空所有配置
		props.properties.deviceId = undefined
		props.properties.property = undefined
		props.properties.operator = undefined
		props.properties.value = undefined
	}
	emit('change')
}

// 属性变化时，重置阈值
const handlePropertyChange = () => {
	// 根据新属性类型重置阈值
	if (currentPropertyType.value === 'bool') {
		const enums = currentPropertyEnums.value
		props.properties.value = enums.length > 0 ? enums[0].value : 'false'
	} else if (currentPropertyType.value === 'enum') {
		const enums = currentPropertyEnums.value
		props.properties.value = enums.length > 0 ? enums[0].value : ''
	} else if (isNumericType(currentPropertyType.value)) {
		props.properties.value = 0
	} else {
		props.properties.value = ''
	}
	// 重置操作符为默认值
	if (currentPropertyType.value === 'bool' || currentPropertyType.value === 'string' || currentPropertyType.value === 'enum') {
		props.properties.operator = '=='
	} else {
		props.properties.operator = '>'
	}
	emit('change')
}
</script>
