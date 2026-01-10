<template>
	<div>
		<a-card size="small" style="margin-bottom: 16px">
			<a-space>
				<a-tag color="blue">设备当前状态</a-tag>
				<a-button size="small" @click="$emit('refresh')">
					<template #icon><ReloadOutlined /></template>
					刷新
				</a-button>
				<a-alert v-if="deviceData.deviceStatus !== 'ONLINE'" message="设备离线" type="warning" show-icon size="small" />
			</a-space>
		</a-card>

		<a-spin :spinning="realTimeDataLoading">
			<a-empty v-if="!thingModelProperties || thingModelProperties.length === 0" description="该产品暂无属性定义" />
			<div v-else>
				<!-- 属性区域 -->
				<a-card size="small" title="属性">
					<a-row :gutter="[16, 16]">
						<a-col :span="6" v-for="property in thingModelProperties" :key="property.identifier">
							<a-card size="small" :title="property.name" hoverable>
								<template #extra>
									<a-tag :color="getRealTimeValueColor(property.identifier)" size="small">
										{{
											formatRealTimeValue(
												property.identifier,
												property.valueType,
												property.valueSpecs,
												property.accessMode
											)
										}}
									</a-tag>
								</template>
								<a-descriptions size="small" :column="1">
									<a-descriptions-item label="标识符">{{ property.identifier }}</a-descriptions-item>
									<a-descriptions-item label="读写类型">
										<a-tag
											size="small"
											:color="property.accessMode === 'R' ? 'cyan' : property.accessMode === 'W' ? 'purple' : 'blue'"
										>
											{{ property.accessMode === 'R' ? '只读' : property.accessMode === 'W' ? '只写' : '读写' }}
										</a-tag>
									</a-descriptions-item>
									<a-descriptions-item label="单位">
										{{ getValueUnit(property.identifier, property.valueSpecs) || '-' }}
									</a-descriptions-item>
									<a-descriptions-item v-if="realTimeDataMap[property.identifier]" label="更新时间">
										{{ realTimeDataMap[property.identifier].updateTime }}
									</a-descriptions-item>
								</a-descriptions>
							</a-card>
						</a-col>
					</a-row>
				</a-card>
			</div>
		</a-spin>
	</div>
</template>

<script setup>
	import { ReloadOutlined } from '@ant-design/icons-vue'
	import { ValueType, AccessMode, isNumericType } from '@/utils/iotConstants'

	const props = defineProps({
		deviceData: {
			type: Object,
			default: () => ({})
		},
		thingModelProperties: {
			type: Array,
			default: () => []
		},
		registerMappingList: {
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

	defineEmits(['refresh'])

	// 格式化实时值
	const formatRealTimeValue = (identifier, valueType, valueSpecs, accessMode) => {
		console.log('🔍 formatRealTimeValue被调用 - identifier:', identifier)
		console.log('🔍 props.realTimeDataMap:', props.realTimeDataMap)
		console.log('🔍 props.realTimeDataMap[identifier]:', props.realTimeDataMap[identifier])
		
		const dataItem = props.realTimeDataMap[identifier]
		if (!dataItem) {
			console.warn('⚠️ 暂无数据 - identifier:', identifier)
			return '暂无数据'
		}

		const value = dataItem.value
		console.log('✅ 找到数据 - identifier:', identifier, ', value:', value, ', valueType:', valueType)

		// 根据值类型格式化
		if (valueType === ValueType.BOOL) {
			// 布尔类型：优先使用valueSpecs中的自定义文本
			if (valueSpecs) {
				try {
					const specs = JSON.parse(valueSpecs)
					// 支持自定义布尔值文本，例如：{"true": "运行", "false": "停止"} 或 {"true": "开", "false": "关"}
					if (specs.true !== undefined || specs.false !== undefined) {
						return value ? specs.true || 'true' : specs.false || 'false'
					}
				} catch (e) {
					// JSON解析失败，使用默认值
				}
			}
			// 没有配置valueSpecs时，使用默认值：只读属性显示运行/停止，可写属性显示开/关
			return accessMode === AccessMode.READ ? (value ? '运行' : '停止') : value ? '开' : '关'
		} else if (valueType === ValueType.ENUM) {
			// 枚举类型
			if (valueSpecs) {
				try {
					const specs = JSON.parse(valueSpecs)
					return specs[value] || value
				} catch (e) {
					return value
				}
			}
			return value
		} else if (isNumericType(valueType)) {
			// 数值类型
			if (valueType === ValueType.INT) {
				return parseInt(value)
			} else {
				return parseFloat(value).toFixed(2)
			}
		} else {
			// 其他类型直接返回
			return value
		}
	}

	// 获取单位
	const getValueUnit = (identifier, valueSpecs) => {
		if (!valueSpecs) return ''
		try {
			const specs = JSON.parse(valueSpecs)
			return specs.unit || ''
		} catch (e) {
			return ''
		}
	}

	// 获取实时值颜色
	const getRealTimeValueColor = (identifier) => {
		const dataItem = props.realTimeDataMap[identifier]
		if (!dataItem) {
			return 'default'
		}
		// 布尔型：运行绿色，停止红色
		const value = dataItem.value
		if (typeof value === 'boolean') {
			return value ? 'green' : 'red'
		} else if (typeof value === 'number') {
			// 数值：1为true（绿色），0为false（红色）
			return value !== 0 ? 'green' : 'red'
		}
		return 'green'
	}
</script>
