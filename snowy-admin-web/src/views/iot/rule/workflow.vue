<template>
	<a-modal
		v-model:open="visible"
		title="规则编排"
		width="90%"
		centered
		:body-style="{ padding: '0', height: '80vh' }"
		@ok="saveWorkflow"
		@cancel="handleCancel"
	>
		<div class="workflow-container">
			<!-- 工具栏 -->
			<div class="workflow-toolbar">
				<a-space>
					<a-button @click="clearWorkflow">
						<template #icon><ClearOutlined /></template>
						清空画布
					</a-button>
					<a-divider type="vertical" />
					<a-button @click="zoomIn">
						<template #icon><ZoomInOutlined /></template>
					</a-button>
					<a-button @click="zoomOut">
						<template #icon><ZoomOutOutlined /></template>
					</a-button>
					<a-button @click="fitView">
						<template #icon><FullscreenOutlined /></template>
						适应画布
					</a-button>
				</a-space>
			</div>

			<!-- 工作区域 -->
			<div class="workflow-content" @click="hideContextMenu">
				<!-- 画布区域 -->
				<div class="canvas-wrapper">
					<div ref="canvasRef" class="logic-flow-canvas"></div>
				</div>

				<!-- 右侧属性面板 -->
				<div class="property-panel" v-if="selectedNode">
					<div class="panel-title">节点属性</div>
					<div class="property-content">
						<a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
							<a-form-item label="节点名称">
								<a-input v-model:value="nodeDisplayName" @change="updateNodeText" />
							</a-form-item>
						
							<!-- 触发器节点配置 -->
							<TriggerConfig
								v-if="selectedNode.type === 'trigger'"
								:properties="selectedNode.properties"
								:device-list="deviceList"
								@change="updateNodeProperties"
								@device-change="handleTriggerDeviceChange"
							/>
						
							<!-- 条件节点配置 -->
							<ConditionConfig
								v-if="selectedNode.type === 'condition'"
								:properties="selectedNode.properties"
								:available-devices="deviceList"
								:device-props="currentDeviceProps"
								:inherited-device-id="inheritedDeviceId"
								@change="updateNodeProperties"
								@device-change="loadDeviceProperties"
								@type-change="handleConditionTypeChange"
							/>
						
							<!-- 动作节点配置 -->
							<ActionConfig
								v-if="selectedNode.type === 'action'"
								:properties="selectedNode.properties"
								:device-list="deviceList"
								@change="updateNodeProperties"
							/>
						</a-form>
					</div>
				</div>
			</div>
		</div>
	</a-modal>
</template>

<script setup name="iotRuleWorkflow">
	import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
	import {
		ClearOutlined,
		ZoomInOutlined,
		ZoomOutOutlined,
		FullscreenOutlined
	} from '@ant-design/icons-vue'
	import { message } from 'ant-design-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'
	import iotRuleApi from '@/api/iot/iotRuleApi'
	import { useWorkflow } from './workflow/useWorkflow'
	import TriggerConfig from './workflow/components/TriggerConfig.vue'
	import ConditionConfig from './workflow/components/ConditionConfig.vue'
	import ActionConfig from './workflow/components/ActionConfig.vue'
	// 导入 LogicFlow 样式
	import '@logicflow/core/lib/style/index.css'
	import '@logicflow/extension/lib/style/index.css'

	// 弹窗状态
	const visible = ref(false)
	const ruleRecord = ref(null)
	const emit = defineEmits(['successful'])

	// 画布引用
	const canvasRef = ref(null)

	// 设备列表
	const deviceList = ref([])
	const currentDeviceProps = ref([]) // 完整的属性对象列表

	// 使用工作流 hook
	const {
		selectedNode,
		nodeDisplayName,
		initLogicFlow,
		updateNodeText,
		updateNodeProperties,
		zoomIn,
		zoomOut,
		fitView,
		clearWorkflow,
		saveWorkflow: getWorkflowData,
		loadWorkflowData,
		resizeCanvas,
		destroy,
		getLogicFlowInstance
	} = useWorkflow(canvasRef)

	// 获取上游触发器节点的设备
	const getUpstreamTriggerDevice = () => {
		if (!selectedNode.value) return null

		const lf = getLogicFlowInstance()
		if (!lf) return null

		// 获取所有连线
		const graphData = lf.getGraphData()
		// 找到连接到当前节点的边
		const incomingEdges = graphData.edges.filter((edge) => edge.targetNodeId === selectedNode.value.id)

		if (incomingEdges.length === 0) return null

		// 遍历上游节点，找到第一个触发器节点
		for (const edge of incomingEdges) {
			const sourceNode = graphData.nodes.find((node) => node.id === edge.sourceNodeId)
			if (sourceNode && sourceNode.type === 'trigger') {
				// 返回触发器节点配置的设备ID
				return sourceNode.properties?.deviceId || null
			}
		}

		return null
	}

	// 计算属性：继承的设备ID（只有设备事件触发器才返回）
	const inheritedDeviceId = computed(() => {
		if (selectedNode.value?.type !== 'condition') return ''
		
		const lf = getLogicFlowInstance()
		if (!lf) return ''

		// 获取所有连线
		const graphData = lf.getGraphData()
		// 找到连接到当前节点的边
		const incomingEdges = graphData.edges.filter((edge) => edge.targetNodeId === selectedNode.value.id)

		if (incomingEdges.length === 0) return ''

		// 遍历上游节点，找到第一个设备触发器节点
		for (const edge of incomingEdges) {
			const sourceNode = graphData.nodes.find((node) => node.id === edge.sourceNodeId)
			if (
				sourceNode &&
				sourceNode.type === 'trigger' &&
				sourceNode.properties?.triggerType === 'device' // 只有设备触发器才返回
			) {
				return sourceNode.properties?.deviceId || ''
			}
		}

		return ''
	})

	// 监听条件类型变化，初始化 deviceSource
	const handleConditionTypeChange = () => {
		if (selectedNode.value?.type === 'condition' && selectedNode.value.properties?.conditionType === 'simple') {
			// 初始化 deviceSource：根据是否有上游设备触发器决定默认值
			if (!selectedNode.value.properties.deviceSource) {
				if (inheritedDeviceId.value) {
					// 有上游设备触发器，默认继承
					selectedNode.value.properties.deviceSource = 'inherit'
					selectedNode.value.properties.deviceId = inheritedDeviceId.value
					loadDeviceProperties()
					message.success('已自动继承上游触发器设备')
				} else {
					// 没有上游设备触发器，默认指定
					selectedNode.value.properties.deviceSource = 'specify'
				}
			}
		}
		updateNodeProperties()
	}

	// 检查并自动继承设备（用于连线创建后）
	const checkAndInheritDevice = () => {
		if (
			selectedNode.value?.type === 'condition' &&
			selectedNode.value.properties?.conditionType === 'simple' &&
			selectedNode.value.properties?.deviceSource === 'inherit'
		) {
			if (inheritedDeviceId.value && !selectedNode.value.properties.deviceId) {
				// 自动设置为继承的设备
				selectedNode.value.properties.deviceId = inheritedDeviceId.value
				// 加载设备属性
				loadDeviceProperties()
				message.success('已自动继承上游触发器设备')
				updateNodeProperties()
			}
		}
	}

	// 处理触发器设备变化，更新下游所有继承模式的简单条件节点
	const handleTriggerDeviceChange = () => {
		if (selectedNode.value?.type !== 'trigger') return

		const lf = getLogicFlowInstance()
		if (!lf) return

		const newDeviceId = selectedNode.value.properties?.deviceId
		if (!newDeviceId) return

		// 只有设备触发器才需要更新下游节点
		if (selectedNode.value.properties?.triggerType !== 'device') return

		// 获取所有节点和连线
		const graphData = lf.getGraphData()
		const triggerId = selectedNode.value.id

		// 找到所有从该触发器出发的连线
		const outgoingEdges = graphData.edges.filter((edge) => edge.sourceNodeId === triggerId)

		// 遍历所有直接连接的下游节点
		let updatedCount = 0
		outgoingEdges.forEach((edge) => {
			const targetNode = graphData.nodes.find((node) => node.id === edge.targetNodeId)
			if (
				targetNode &&
				targetNode.type === 'condition' &&
				targetNode.properties?.conditionType === 'simple' &&
				targetNode.properties?.deviceSource === 'inherit' // 只更新继承模式的节点
			) {
				// 更新条件节点的设备为新的触发器设备
				lf.setProperties(targetNode.id, {
					...targetNode.properties,
					deviceId: newDeviceId,
					// 清空属性和阈值，因为设备变了
					property: undefined,
					operator: undefined,
					value: undefined
				})
				updatedCount++
			}
		})

		if (updatedCount > 0) {
			message.success(`已更新 ${updatedCount} 个继承模式的条件节点`)
		}

		// 如果当前选中的是触发器节点本身，也需要更新
		updateNodeProperties()
	}

	// 加载设备属性
	const loadDeviceProperties = async () => {
		if (!selectedNode.value?.properties?.deviceId) {
			currentDeviceProps.value = []
			return
		}

		try {
			// 获取设备详情
			const device = deviceList.value.find((d) => d.id === selectedNode.value.properties.deviceId)
			if (!device || !device.productId) {
				message.warning('设备信息不完整，无法获取属性列表')
				currentDeviceProps.value = []
				return
			}

			// 根据产品ID获取物模型属性列表
			const properties = await iotThingModelApi.iotThingModelGetProperties({
				productId: device.productId,
				modelType: 'PROPERTY'
			})

			// 保存完整的属性对象，包含 dataType, dataSpecs 等信息
			currentDeviceProps.value = properties || []
			console.log('加载设备属性:', currentDeviceProps.value)
		} catch (error) {
			console.error('加载设备属性失败:', error)
			message.error('加载设备属性失败')
			currentDeviceProps.value = []
		}

		updateNodeProperties()
	}

	// 隐藏右键菜单
	const hideContextMenu = () => {
		// LogicFlow Menu 插件会自动处理
	}

	// 打开弹窗
	const onOpen = (record) => {
		visible.value = true
		ruleRecord.value = record
		nextTick(() => {
			setTimeout(() => {
				if (!canvasRef.value) return

				// 初始化 LogicFlow，传入回调函数
				initLogicFlow({
					onEdgeAdd: (edge) => {
						// 连线创建后，如果目标节点是当前选中的条件节点，尝试继承设备
						if (selectedNode.value?.id === edge.targetNodeId) {
							setTimeout(() => {
								checkAndInheritDevice()
							}, 100)
						}
					}
				})
				loadDeviceList()

				// 加载工作流数据
				if (record && record.workflowData) {
					loadWorkflowData(record.workflowData)
				}

				// 再次调整画布尺寸确保占满
				setTimeout(() => {
					resizeCanvas()
				}, 100)
			}, 100)
		})
	}

	// 关闭弹窗
	const handleCancel = () => {
		visible.value = false
		ruleRecord.value = null
		destroy()
	}

	// 保存工作流
	const saveWorkflow = async () => {
		const data = getWorkflowData()
		if (!data) return

		if (!ruleRecord.value || !ruleRecord.value.id) {
			message.error('规则信息不存在，无法保存')
			return
		}

		console.log('工作流数据:', data)
		console.log('工作流 JSON:', JSON.stringify(data, null, 2))

		try {
			// 保存工作流数据，需要携带必填字段
			const params = {
				id: ruleRecord.value.id,
				ruleName: ruleRecord.value.ruleName,
				ruleType: ruleRecord.value.ruleType,
				status: ruleRecord.value.status,
				workflowData: JSON.stringify(data)
			}
			console.log('保存参数:', params)
				
			await iotRuleApi.iotRuleSubmitForm(params, true)
			message.success('规则保存成功')
			visible.value = false
			emit('successful')
		} catch (error) {
			console.error('保存工作流失败:', error)
			message.error('保存失败，请重试')
		}
	}

	// 加载设备列表
	const loadDeviceList = () => {
		iotDeviceApi.iotDevicePage({ current: 1, size: 1000 }).then((res) => {
			deviceList.value = res.records || []
		})
	}

	// 键盘事件处理
	const handleKeyDown = (e) => {
		// Delete 键删除选中节点
		if (e.key === 'Delete' && selectedNode.value) {
			// TODO: 调用删除节点方法
		}
	}

	onMounted(() => {
		document.addEventListener('keydown', handleKeyDown)
		// 监听窗口大小变化
		window.addEventListener('resize', resizeCanvas)
	})

	onUnmounted(() => {
		document.removeEventListener('keydown', handleKeyDown)
		window.removeEventListener('resize', resizeCanvas)
		destroy()
	})

	defineExpose({
		onOpen
	})
</script>

<style scoped lang="less">
	.workflow-container {
		height: 100%;
		display: flex;
		flex-direction: column;
	}

	.workflow-toolbar {
		padding: 12px 16px;
		background: #fafafa;
		border-bottom: 1px solid #e8e8e8;
	}

	.workflow-content {
		flex: 1;
		display: flex;
		overflow: hidden;
		position: relative;
	}

	.canvas-wrapper {
		flex: 1;
		position: relative;
		overflow: hidden;
	}

	.logic-flow-canvas {
		width: 100%;
		height: 100%;
		background: #f5f5f5;
	}

	/* 自定义 LogicFlow Menu 样式 */
	:deep(.lf-menu) {
		background: #ffffff;
		border-radius: 6px;
		box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
		min-width: 180px;
		padding: 4px 0;
	}

	:deep(.lf-menu-item) {
		padding: 8px 16px;
		cursor: pointer;
		transition: all 0.2s;
		font-size: 14px;
		color: rgba(0, 0, 0, 0.85);
		position: relative;
		display: flex;
		align-items: center;

		&:hover {
			background-color: #f5f5f5;
		}

		&.lf-menu-item-delete {
			color: #ff4d4f;

			&:hover {
				background-color: #fff1f0 !important;
			}
		}

		&.lf-menu-item-trigger {
			color: #52c41a;
		}

		&.lf-menu-item-condition {
			color: #1890ff;
		}

		&.lf-menu-item-action {
			color: #fa8c16;
		}

		&.lf-menu-item-end {
			color: #8c8c8c;
		}
	}

	.property-panel {
		width: 320px;
		background: #fafafa;
		border-left: 1px solid #e8e8e8;
		overflow-y: auto;
		padding: 16px 12px;

		.panel-title {
			font-size: 14px;
			font-weight: 600;
			margin-bottom: 12px;
			padding: 8px 12px;
			color: rgba(0, 0, 0, 0.85);
		}
	}

	.property-content {
		padding: 12px;
		background: #fff;
		border-radius: 4px;
	}

	/* 美化滚动条 */
	.node-panel::-webkit-scrollbar,
	.property-panel::-webkit-scrollbar {
		width: 6px;
	}

	.node-panel::-webkit-scrollbar-thumb,
	.property-panel::-webkit-scrollbar-thumb {
		background: #bfbfbf;
		border-radius: 3px;
	}

	.node-panel::-webkit-scrollbar-thumb:hover,
	.property-panel::-webkit-scrollbar-thumb:hover {
		background: #999;
	}
</style>
