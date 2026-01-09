<template>
	<div class="device-bind-container">
		<!-- 添加绑定按钮 -->
		<a-button type="primary" block size="large" @click="onAddBind" class="add-bind-btn">添加绑定</a-button>

		<!-- 绑定列表 -->
		<div v-for="(bind_item, index) in bind_devices" :key="index" class="bind-item">
			<div class="bind-header" @click="toggleExpand(index)">
				<span class="bind-title">绑定{{ index + 1 }}</span>
				<div class="bind-actions">
					<delete-outlined class="delete-icon" @click.stop="onRemoveBind(index)" />
					<right-outlined :class="['expand-icon', { expanded: bind_item.expanded }]" />
				</div>
			</div>

			<!-- 展开内容 -->
			<div v-show="bind_item.expanded" class="bind-content">
				<div class="bind-field">
					<div class="field-label">设备点位</div>
					<a-select
						v-model:value="bind_item.device_id"
						placeholder="选择设备点位"
						size="small"
						show-search
						:filter-option="filterDevice"
						@change="handleDeviceChange(bind_item)"
						@focus="loadDeviceList"
						class="field-input"
					>
						<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
							{{ device.deviceName }}
						</a-select-option>
					</a-select>
				</div>

				<div class="bind-field">
					<div class="field-label">图元属性</div>
					<a-select v-model:value="bind_item.node_prop" placeholder="选择属性" size="small" class="field-input">
						<a-select-option v-for="prop in getNodeProps()" :key="prop.value" :value="prop.value">
							{{ prop.label }}
						</a-select-option>
					</a-select>
				</div>

				<div class="bind-field">
					<div class="field-label">设备属性</div>
					<a-select
						v-model:value="bind_item.device_prop"
						placeholder="选择属性"
						size="small"
						show-search
						:disabled="!bind_item.device_id"
						class="field-input"
					>
						<a-select-option v-for="prop in getDeviceProps(bind_item.device_id)" :key="prop.value" :value="prop.value">
							{{ prop.label }}
						</a-select-option>
					</a-select>
				</div>

				<!-- 配置事件绑定按钮 -->
				<a-button type="text" size="small" @click="openMappingModal(bind_item, index)" class="edit-mapping-btn">
					{{
						bind_item.mapping?.eventName
							? '✓ 已配置事件: ' + getEventLabel(bind_item.mapping.eventName)
							: '配置事件绑定'
					}}
				</a-button>
			</div>
		</div>

		<!-- 保存按钮 -->
		<a-button type="primary" block size="small" @click="onSave" class="save-btn" v-if="bind_devices.length > 0">
			保存配置
		</a-button>

		<!-- 映射规则弹窗 -->
		<a-modal
			v-model:open="mappingModalVisible"
			title="配置设备绑定"
			width="500px"
			@ok="saveMappingRule"
			@cancel="closeMappingModal"
		>
			<a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
				<a-alert
					message="说明"
					description="设备绑定只负责关联设备数据和组件事件。数据转换、条件判断等逻辑应在设计器中配置组件事件的动作。例如：当温度>=60时执行'设置属性 type=danger'动作。"
					type="info"
					show-icon
					style="margin-bottom: 16px"
				/>

				<a-form-item label="事件选择" required>
					<a-select v-model:value="currentMapping.eventName" placeholder="选择组件已配置的事件">
						<a-select-option
							v-for="event in getComponentEvents()"
							:key="event.value"
							:value="event.value"
							:disabled="event.disabled"
						>
							{{ event.label }}
						</a-select-option>
					</a-select>
					<div style="font-size: 12px; color: #999; margin-top: 4px">
						选择组件在设计器中配置的事件ID，设备数据更新时会触发该事件
					</div>
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup>
	import { ref } from 'vue'
	import { message } from 'ant-design-vue'
	import { DeleteOutlined, RightOutlined } from '@ant-design/icons-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotDeviceShadowApi from '@/api/iot/iotDeviceShadowApi'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'
	import { ModelType } from '@/utils/iotConstants'

	const props = defineProps({
		handItemInfo: {
			type: Object,
			required: true
		}
	})

	const deviceList = ref([])
	const devicePropsMap = ref(new Map())
	const bind_devices = ref([])

	// 映射规则弹窗相关
	const mappingModalVisible = ref(false)
	const currentBindingIndex = ref(-1)
	const currentMapping = ref({
		eventName: '' // 事件名称
	})

	// 初始化时加载绑定数据
	const loadBindings = async () => {
		// 先加载设备列表，用于显示设备名称
		await loadDeviceList()

		// 从 handItemInfo 中读取绑定数据
		if (props.handItemInfo.deviceBindings && Array.isArray(props.handItemInfo.deviceBindings)) {
			// 添加 expanded 属性
			bind_devices.value = props.handItemInfo.deviceBindings.map((item) => ({
				...item,
				expanded: false // 默认折叠
			}))

			// 预加载所有设备的属性列表，用于反显
			const deviceIds = [...new Set(bind_devices.value.map((b) => b.device_id))]
			for (const deviceId of deviceIds) {
				if (deviceId && !devicePropsMap.value.has(deviceId)) {
					try {
						await loadDeviceProperties(deviceId)
					} catch (error) {
						console.error(`预加载设备 ${deviceId} 属性失败:`, error)
					}
				}
			}
		} else {
			bind_devices.value = []
		}
	}

	// 加载设备属性（提取为独立函数，供多处复用）
	const loadDeviceProperties = async (deviceId) => {
		if (!deviceId || devicePropsMap.value.has(deviceId)) return

		try {
			// 1. 获取设备详情（获取 productId）
			const deviceDetail = await iotDeviceApi.iotDeviceDetail({ id: deviceId })
			const props = []

			// 2. 从物模型获取属性定义
			if (deviceDetail.productId) {
				const thingModelProps = await iotThingModelApi.iotThingModelGetProperties({
					productId: deviceDetail.productId,
					modelType: ModelType.PROPERTY
				})

				// 3. 从设备影子获取当前值（证明这些属性是有数据的）
				const shadowData = await iotDeviceShadowApi.iotDeviceShadowPage({
					current: 1,
					size: 1,
					deviceId: deviceId
				})

				let reportedKeys = []
				if (shadowData.records && shadowData.records.length > 0) {
					const shadow = shadowData.records[0]
					if (shadow.reported) {
						try {
							const reportedData = JSON.parse(shadow.reported)
							reportedKeys = Object.keys(reportedData)
						} catch (error) {
							console.error('解析设备影子数据失败:', error)
						}
					}
				}

				// 4. 使用物模型属性，但优先显示有影子数据的
				if (thingModelProps && thingModelProps.length > 0) {
					// 先添加有实时数据的属性
					thingModelProps.forEach((prop) => {
						if (reportedKeys.includes(prop.identifier)) {
							props.push({
								label: `${prop.name} (${prop.identifier})`,
								value: prop.identifier
							})
						}
					})

					// 再添加没有实时数据的属性
					thingModelProps.forEach((prop) => {
						if (!reportedKeys.includes(prop.identifier)) {
							props.push({
								label: `${prop.name} (${prop.identifier}) [无数据]`,
								value: prop.identifier
							})
						}
					})
				}
			}

			// 5. 如果没有物模型属性，使用默认属性
			if (props.length === 0) {
				props.push(
					{ label: 'temperature（温度）', value: 'temperature' },
					{ label: 'humidity（湿度）', value: 'humidity' },
					{ label: 'pressure（压力）', value: 'pressure' },
					{ label: 'voltage（电压）', value: 'voltage' },
					{ label: 'current（电流）', value: 'current' },
					{ label: 'power（功率）', value: 'power' },
					{ label: 'status（状态）', value: 'status' },
					{ label: 'value（数值）', value: 'value' }
				)
			}

			devicePropsMap.value.set(deviceId, props)
		} catch (error) {
			console.error('加载设备属性失败:', error)
			throw error
		}
	}

	// 设备变化时加载属性
	const handleDeviceChange = async (bindItem) => {
		bindItem.device_prop = ''
		if (bindItem.device_id) {
			try {
				await loadDeviceProperties(bindItem.device_id)
			} catch (error) {
				message.error('加载设备属性失败')
			}
		}
	}
	// 获取设备属性列表
	const getDeviceProps = (deviceId) => {
		if (!deviceId) return []
		return devicePropsMap.value.get(deviceId) || []
	}

	// 获取图形属性列表
	const getNodeProps = () => {
		const propList = []
		if (props.handItemInfo?.props) {
			for (const key in props.handItemInfo.props) {
				const prop = props.handItemInfo.props[key]
				propList.push({
					label: prop.title || key,
					value: `props.${key}.val`
				})
			}
		}
		return propList
	}

	// 获取组件事件列表
	const getComponentEvents = () => {
		const eventList = []

		console.log('组件配置:', props.handItemInfo)

		// 从组件的 events 中获取
		if (props.handItemInfo?.events && Array.isArray(props.handItemInfo.events)) {
			console.log('组件事件配置:', props.handItemInfo.events)
			props.handItemInfo.events.forEach((event) => {
				// 优先使用用户自定义的事件名称（maotu 0.6.5+ 支持）
				const eventName = event.name || event.id || '未命名事件'
				const eventType = event.type || ''

				// 显示格式：如果用户输入了name，显示"事件名称 [触发方式]"，否则显示"事件ID [触发方式]"
				const eventLabel = eventType ? `${eventName} [${eventType}]` : eventName

				eventList.push({
					label: eventLabel,
					value: event.id // 使用事件ID作为值（确保唯一性）
				})
			})
		}

		// 如果没有配置事件，提示用户
		if (eventList.length === 0) {
			console.log('⚠️ 组件没有配置事件，请先在设计器中为组件配置事件')
			eventList.push({
				label: '⚠️ 请先在设计器中为组件配置事件',
				value: '',
				disabled: true
			})
		}

		console.log('可用事件列表:', eventList)
		return eventList
	}

	// 获取事件标签（用于显示）
	const getEventLabel = (eventValue) => {
		const events = getComponentEvents()
		const event = events.find((e) => e.value === eventValue)
		return event ? event.label : eventValue
	}

	// 设备筛选
	const filterDevice = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 新增绑定
	const onAddBind = () => {
		bind_devices.value.push({
			device_id: '',
			device_prop: '',
			node_prop: '',
			expanded: true // 默认展开
		})
	}

	// 切换展开/折叠
	const toggleExpand = (index) => {
		bind_devices.value[index].expanded = !bind_devices.value[index].expanded
	}

	// 打开映射规则弹窗
	const openMappingModal = (bindItem, index) => {
		currentBindingIndex.value = index
		// 加载当前绑定的事件配置
		if (bindItem.mapping && bindItem.mapping.eventName) {
			currentMapping.value = {
				eventName: bindItem.mapping.eventName
			}
		} else {
			// 重置为默认值
			currentMapping.value = {
				eventName: ''
			}
		}
		mappingModalVisible.value = true
	}

	// 保存映射规则
	const saveMappingRule = () => {
		// 验证事件ID
		if (!currentMapping.value.eventName) {
			message.error('请选择事件')
			return
		}

		// 保存到绑定项
		bind_devices.value[currentBindingIndex.value].mapping = {
			eventName: currentMapping.value.eventName
		}

		// 调试日志
		console.log('💾 映射规则已保存到绑定项:', {
			index: currentBindingIndex.value,
			mapping: bind_devices.value[currentBindingIndex.value].mapping
		})

		// 🎯 直接保存到 handItemInfo，无需再点击"保存配置"
		const cleanedBindings = bind_devices.value.map((item) => ({
			device_id: item.device_id,
			device_prop: item.device_prop,
			node_prop: item.node_prop,
			mapping: item.mapping || null
		}))

		// eslint-disable-next-line vue/no-mutating-props
		props.handItemInfo.deviceBindings = cleanedBindings
		console.log('✅ 已自动更新 handItemInfo.deviceBindings')

		message.success('映射规则保存成功')
		closeMappingModal()
	}

	// 关闭映射规则弹窗
	const closeMappingModal = () => {
		mappingModalVisible.value = false
		currentBindingIndex.value = -1
	}

	// 删除绑定
	const onRemoveBind = (index) => {
		bind_devices.value.splice(index, 1)

		// 🎯 同步删除到 handItemInfo
		const cleanedBindings = bind_devices.value.map((item) => ({
			device_id: item.device_id,
			device_prop: item.device_prop,
			node_prop: item.node_prop,
			mapping: item.mapping || null
		}))
		// eslint-disable-next-line vue/no-mutating-props
		props.handItemInfo.deviceBindings = cleanedBindings
	}

	// 保存绑定
	const onSave = () => {
		// 验证表单
		const hasEmpty = bind_devices.value.some((item) => !item.device_id || !item.device_prop || !item.node_prop)

		if (hasEmpty) {
			message.warning('请完整填写所有绑定信息')
			return
		}

		// 过滤掉 UI 状态（expanded），保留映射规则
		const cleanedBindings = bind_devices.value.map((item) => ({
			device_id: item.device_id,
			device_prop: item.device_prop,
			node_prop: item.node_prop,
			mapping: item.mapping || null // 保存映射规则
		}))

		console.log('💾 保存设备绑定配置:', cleanedBindings)

		// 直接修改 handItemInfo（这是 maotu 内部状态的引用，不是 Vue props）
		// eslint-disable-next-line vue/no-mutating-props
		props.handItemInfo.deviceBindings = cleanedBindings

		console.log('✅ 已更新 handItemInfo.deviceBindings')

		message.success('保存成功')
	}

	// 加载设备列表
	const loadDeviceList = async () => {
		if (deviceList.value.length > 0) return
		try {
			const data = await iotDeviceApi.iotDevicePage({ pageSize: 1000 })
			deviceList.value = data.records || []
		} catch (error) {
			console.error('加载设备列表失败:', error)
		}
	}

	// 初始化加载
	loadBindings()
</script>

<style scoped lang="less">
	.device-bind-container {
		padding: 16px;
		background: #1e1e1e;

		.add-bind-btn {
			height: 48px;
			margin-bottom: 16px;
			background: #5a8cef;
			border: none;
			font-size: 15px;
			font-weight: 500;
			border-radius: 6px;

			&:hover {
				background: #6a9cff;
			}
		}

		.bind-item {
			margin-bottom: 8px;
			background: #2a2a2a;
			border-radius: 4px;
			overflow: hidden;

			.bind-header {
				display: flex;
				align-items: center;
				justify-content: space-between;
				padding: 12px 16px;
				cursor: pointer;
				transition: background 0.2s;

				&:hover {
					background: #333;
				}

				.bind-title {
					color: #fff;
					font-size: 14px;
				}

				.bind-actions {
					display: flex;
					align-items: center;
					gap: 12px;

					.delete-icon {
						color: #999;
						font-size: 16px;
						cursor: pointer;
						transition: color 0.2s;

						&:hover {
							color: #ff4d4f;
						}
					}

					.expand-icon {
						color: #999;
						font-size: 12px;
						transition: transform 0.2s;

						&.expanded {
							transform: rotate(90deg);
						}
					}
				}
			}

			.bind-content {
				padding: 0 16px 16px 16px;

				.bind-field {
					margin-bottom: 12px;

					.field-label {
						color: #999;
						font-size: 12px;
						margin-bottom: 6px;
					}

					.field-input {
						width: 100%;
					}
				}

				.edit-mapping-btn {
					color: #5a8cef;
					padding: 4px 8px;
					height: auto;
					font-size: 12px;
				}
			}
		}

		.save-btn {
			margin-top: 16px;
			background: #52c41a;
			border: none;
			height: 36px;
			font-weight: 500;

			&:hover {
				background: #73d13d;
			}
		}
	}
</style>
