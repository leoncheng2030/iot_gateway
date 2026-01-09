<template>
	<div class="condition-builder">
		<div v-for="(condition, index) in conditions" :key="index" class="condition-item">
			<a-form layout="vertical">
				<a-row :gutter="12">
					<a-col :span="5">
						<a-form-item label="触发设备" style="margin-bottom: 12px">
							<a-select
								v-model:value="condition.deviceId"
								placeholder="请选择设备"
								show-search
								:filter-option="filterOption"
								allow-clear
								@change="onDeviceChange(index)"
							>
								<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
									{{ device.deviceName }} ({{ device.deviceKey }})
								</a-select-option>
							</a-select>
						</a-form-item>
					</a-col>
					<a-col :span="5">
						<a-form-item label="属性名称" style="margin-bottom: 12px">
							<a-select
								v-model:value="condition.property"
								placeholder="请选择属性"
								show-search
								:filter-option="filterPropertyOption"
								allow-clear
								:disabled="!condition.deviceId"
								@change="onPropertyChange(index)"
							>
								<a-select-option
									v-for="prop in getPropertyList(condition.deviceId)"
									:key="prop.identifier"
									:value="prop.identifier"
								>
									{{ prop.name }} ({{ prop.identifier }})
								</a-select-option>
							</a-select>
						</a-form-item>
					</a-col>
					<a-col :span="4">
						<a-form-item label="比较操作" style="margin-bottom: 12px">
							<a-select v-model:value="condition.operator" :disabled="!condition.property">
								<a-select-option v-for="op in getAvailableOperators(index)" :key="op.value" :value="op.value">
									{{ op.label }}
								</a-select-option>
							</a-select>
						</a-form-item>
					</a-col>
					<a-col :span="5">
						<a-form-item label="比较值" style="margin-bottom: 12px">
							<template v-if="getCurrentProperty(index)">
								<!-- 布尔类型 -->
								<a-select
									v-if="getCurrentPropertyType(index) === 'bool'"
									v-model:value="condition.value"
									placeholder="请选择"
								>
									<a-select-option :value="true">{{ getBoolLabel(index, true) }}</a-select-option>
									<a-select-option :value="false">{{ getBoolLabel(index, false) }}</a-select-option>
								</a-select>
								<!-- 枚举类型 -->
								<a-select
									v-else-if="getCurrentPropertyType(index) === 'enum'"
									v-model:value="condition.value"
									placeholder="请选择"
								>
									<a-select-option v-for="(label, value) in getEnumOptions(index)" :key="value" :value="value">
										{{ label }}
									</a-select-option>
								</a-select>
								<!-- 整数类型 -->
								<a-input-number
									v-else-if="getCurrentPropertyType(index) === 'int'"
									v-model:value="condition.value"
									placeholder="请输入"
									style="width: 100%"
								/>
								<!-- 浮点数类型 -->
								<a-input-number
									v-else-if="getCurrentPropertyType(index) === 'float' || getCurrentPropertyType(index) === 'double'"
									v-model:value="condition.value"
									placeholder="请输入"
									:precision="2"
									style="width: 100%"
								/>
								<!-- 文本类型 -->
								<a-input v-else v-model:value="condition.value" placeholder="请输入" />
							</template>
							<a-input v-else v-model:value="condition.value" placeholder="请先选择属性" disabled />
						</a-form-item>
					</a-col>
					<a-col :span="3">
						<a-form-item label="逻辑关系" style="margin-bottom: 12px" v-if="index < conditions.length - 1">
							<a-select v-model:value="condition.logic" style="width: 100%">
								<a-select-option value="AND">且</a-select-option>
								<a-select-option value="OR">或</a-select-option>
							</a-select>
						</a-form-item>
						<a-form-item label=" " style="margin-bottom: 12px" v-else>
							<span style="color: #999">-</span>
						</a-form-item>
					</a-col>
					<a-col :span="2">
						<a-form-item label=" " style="margin-bottom: 12px">
							<a-button type="text" danger :disabled="conditions.length === 1" @click="removeCondition(index)">
								<DeleteOutlined />
							</a-button>
						</a-form-item>
					</a-col>
				</a-row>
			</a-form>
		</div>
		<a-button type="dashed" block @click="addCondition" style="margin-top: 8px"> <PlusOutlined /> 添加条件 </a-button>
	</div>
</template>

<script setup name="ConditionBuilder">
	import { ref, onMounted } from 'vue'
	import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'

	const conditions = ref([
		{
			deviceId: '',
			property: '',
			operator: '==',
			value: null,
			logic: 'AND' // 与下一个条件的逻辑关系
		}
	])

	const deviceList = ref([])
	const devicePropertiesMap = ref({}) // 存储每个设备的属性列表

	// 获取指定设备的属性列表
	const getPropertyList = (deviceId) => {
		return devicePropertiesMap.value[deviceId] || []
	}

	// 获取当前条件的属性对象
	const getCurrentProperty = (index) => {
		const condition = conditions.value[index]
		if (!condition.deviceId || !condition.property) return null
		const properties = getPropertyList(condition.deviceId)
		return properties.find((p) => p.identifier === condition.property)
	}

	// 获取当前条件的属性类型
	const getCurrentPropertyType = (index) => {
		const property = getCurrentProperty(index)
		return property?.valueType || ''
	}

	// 获取当前条件的枚举选项
	const getEnumOptions = (index) => {
		const property = getCurrentProperty(index)
		if (!property || property.valueType !== 'enum' || !property.valueSpecs) {
			return {}
		}
		try {
			return JSON.parse(property.valueSpecs)
		} catch (e) {
			return {}
		}
	}

	// 根据属性类型返回可用的操作符
	const getAvailableOperators = (index) => {
		const type = getCurrentPropertyType(index)

		// 数值类型: int, float, double
		if (['int', 'float', 'double'].includes(type)) {
			return [
				{ value: '>', label: '大于 (>)' },
				{ value: '>=', label: '大于等于 (>=)' },
				{ value: '<', label: '小于 (<)' },
				{ value: '<=', label: '小于等于 (<=)' },
				{ value: '==', label: '等于 (==)' },
				{ value: '!=', label: '不等于 (!=)' }
			]
		}

		// 布尔类型
		if (type === 'bool') {
			return [
				{ value: '==', label: '等于 (==)' },
				{ value: '!=', label: '不等于 (!=)' }
			]
		}

		// 枚举类型
		if (type === 'enum') {
			return [
				{ value: '==', label: '等于 (==)' },
				{ value: '!=', label: '不等于 (!=)' }
			]
		}

		// 文本类型
		if (type === 'text') {
			return [
				{ value: '==', label: '等于 (==)' },
				{ value: '!=', label: '不等于 (!=)' },
				{ value: 'contains', label: '包含' },
				{ value: 'not_contains', label: '不包含' },
				{ value: 'starts_with', label: '开头是' },
				{ value: 'ends_with', label: '结尾是' }
			]
		}

		// 默认：等于/不等于
		return [
			{ value: '==', label: '等于 (==)' },
			{ value: '!=', label: '不等于 (!=)' }
		]
	}

	// 加载设备列表
	const loadDevices = () => {
		return iotDeviceApi.iotDevicePage({ current: 1, size: 1000 }).then((data) => {
			deviceList.value = data.records || []
		})
	}

	// 设备改变时加载属性列表
	const onDeviceChange = (index) => {
		const condition = conditions.value[index]
		if (!condition.deviceId) {
			condition.property = ''
			condition.operator = '=='
			condition.value = null
			return
		}

		// 如果已经加载过该设备的属性，直接返回
		if (devicePropertiesMap.value[condition.deviceId]) {
			return
		}

		// 查找设备对应的产品ID
		const device = deviceList.value.find((d) => d.id === condition.deviceId)
		if (device && device.productId) {
			// 加载该产品的属性列表
			iotThingModelApi
				.iotThingModelGetProperties({ productId: device.productId, modelType: 'PROPERTY' })
				.then((data) => {
					devicePropertiesMap.value[condition.deviceId] = data || []
				})
				.catch(() => {
					devicePropertiesMap.value[condition.deviceId] = []
				})
		}
	}

	// 属性改变时
	const onPropertyChange = (index) => {
		const condition = conditions.value[index]
		if (!condition.property) {
			condition.operator = '=='
			condition.value = null
			return
		}

		// 查找选中的属性
		const prop = getCurrentProperty(index)
		if (prop) {
			// 根据类型设置默认操作符和值
			const type = prop.valueType
			if (['int', 'float', 'double'].includes(type)) {
				condition.operator = '>'
				condition.value = 0
			} else if (type === 'bool') {
				condition.operator = '=='
				condition.value = true
			} else if (type === 'enum') {
				condition.operator = '=='
				// 设置为第一个枚举值
				try {
					const enumOptions = JSON.parse(prop.valueSpecs || '{}')
					const firstKey = Object.keys(enumOptions)[0]
					condition.value = firstKey || ''
				} catch (e) {
					condition.value = ''
				}
			} else {
				condition.operator = '=='
				condition.value = ''
			}
		}
	}

	// 添加条件
	const addCondition = () => {
		conditions.value.push({
			deviceId: '',
			property: '',
			operator: '==',
			value: null,
			logic: 'AND'
		})
	}

	// 删除条件
	const removeCondition = (index) => {
		if (conditions.value.length > 1) {
			conditions.value.splice(index, 1)
		}
	}

	// 过滤设备选项
	const filterOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 过滤属性选项
	const filterPropertyOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 获取布尔值的显示文本
	const getBoolLabel = (index, value) => {
		const property = getCurrentProperty(index)
		if (!property || !property.valueSpecs) {
			// 没有自定义配置时使用默认值
			return value ? '是' : '否'
		}

		try {
			const specs = JSON.parse(property.valueSpecs)
			if (value) {
				return specs.true || '是'
			} else {
				return specs.false || '否'
			}
		} catch (e) {
			return value ? '是' : '否'
		}
	}

	// 转换为JSON
	const toJSON = () => {
		return JSON.stringify(conditions.value)
	}

	// 从JSON加载
	const fromJSON = async (json) => {
		if (json) {
			try {
				const data = typeof json === 'string' ? JSON.parse(json) : json

				// 兼容旧数据格式（单个对象）和新数据格式（数组）
				let conditionsData = []
				if (Array.isArray(data)) {
					conditionsData = data
				} else if (data && typeof data === 'object') {
					// 单个条件对象，转换为数组
					conditionsData = [data]
				}

				if (conditionsData.length > 0) {
					// 确保设备列表已加载
					if (deviceList.value.length === 0) {
						await loadDevices()
					}

					conditions.value = conditionsData

					// 加载每个设备的属性列表
					const loadPromises = conditionsData.map((cond) => {
						if (cond.deviceId && !devicePropertiesMap.value[cond.deviceId]) {
							const device = deviceList.value.find((d) => d.id === cond.deviceId)
							if (device && device.productId) {
								return iotThingModelApi
									.iotThingModelGetProperties({ productId: device.productId, modelType: 'PROPERTY' })
									.then((properties) => {
										devicePropertiesMap.value[cond.deviceId] = properties || []
									})
							}
						}
						return Promise.resolve()
					})

					// 等待所有属性列表加载完成
					await Promise.all(loadPromises)
				}
			} catch (e) {
				console.error('解析触发条件失败', e)
			}
		}
	}

	// 验证
	const validate = () => {
		// 至少需要一个条件
		if (conditions.value.length === 0) {
			return { valid: false, message: '请至少添加一个触发条件' }
		}

		for (let i = 0; i < conditions.value.length; i++) {
			const cond = conditions.value[i]
			if (!cond.deviceId) {
				return { valid: false, message: `条件${i + 1}: 请选择触发设备` }
			}
			if (!cond.property) {
				return { valid: false, message: `条件${i + 1}: 请选择属性名称` }
			}
			if (!cond.operator) {
				return { valid: false, message: `条件${i + 1}: 请选择比较操作` }
			}
			if (cond.value === null || cond.value === undefined || cond.value === '') {
				return { valid: false, message: `条件${i + 1}: 请输入比较值` }
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
		conditions
	})
</script>

<style scoped>
	.condition-builder {
		margin-bottom: 12px;
	}

	.condition-item {
		margin-bottom: 8px;
		padding-bottom: 8px;
		border-bottom: 1px dashed #e8e8e8;
	}

	.condition-item:last-child {
		border-bottom: none;
	}
</style>
