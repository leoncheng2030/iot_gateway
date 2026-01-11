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
					<a-button type="primary" @click="checkWorkflowIntegrity">
						<template #icon><CheckCircleOutlined /></template>
						检查完整性
					</a-button>
					<a-divider type="vertical" />
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
	import { ref, onMounted, onUnmounted, nextTick, computed, watch, h } from 'vue'
	import {
		ClearOutlined,
		ZoomInOutlined,
		ZoomOutOutlined,
		FullscreenOutlined,
		CheckCircleOutlined
	} from '@ant-design/icons-vue'
	import { message, Modal } from 'ant-design-vue'
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

	// 递归向上查找设备触发器
	const findUpstreamDeviceTrigger = (nodeId, visited = new Set()) => {
		// 避免循环引用
		if (visited.has(nodeId)) return null
		visited.add(nodeId)

		const lf = getLogicFlowInstance()
		if (!lf) return null

		const graphData = lf.getGraphData()
		// 找到连接到当前节点的边
		const incomingEdges = graphData.edges.filter((edge) => edge.targetNodeId === nodeId)

		if (incomingEdges.length === 0) return null

		// 遍历上游节点
		for (const edge of incomingEdges) {
			const sourceNode = graphData.nodes.find((node) => node.id === edge.sourceNodeId)
			if (!sourceNode) continue

			console.log('查找上游触发器 - 检查节点:', sourceNode.id, '类型:', sourceNode.type)

			// 如果找到设备触发器，返回它的设备ID
			if (sourceNode.type === 'trigger' && sourceNode.properties?.triggerType === 'device') {
				const deviceId = sourceNode.properties?.deviceId || null
				console.log('找到设备触发器:', sourceNode.id, '设备ID:', deviceId)
				return deviceId
			}

			// 否则递归向上查找
			const result = findUpstreamDeviceTrigger(sourceNode.id, visited)
			if (result) return result
		}

		return null
	}

	// 计算属性：继承的设备ID（递归向上查找设备触发器）
	const inheritedDeviceId = computed(() => {
		if (selectedNode.value?.type !== 'condition') return ''

		const deviceId = findUpstreamDeviceTrigger(selectedNode.value.id) || ''
		console.log('计算inheritedDeviceId:', selectedNode.value.id, '→', deviceId)
		return deviceId
	})

	// 监听 selectedNode 变化，当选中条件节点时加载其设备属性
	watch(
		() => selectedNode.value,
		(newNode, oldNode) => {
			// 当选中条件节点时，加载该节点的设备属性
			if (newNode?.type === 'condition') {
				// 如果是继承模式，检查LogicFlow中是否有deviceId
				if (newNode.properties?.deviceSource === 'inherit') {
					const lf = getLogicFlowInstance()
					if (lf) {
						const nodeModel = lf.getNodeModelById(newNode.id)
						if (nodeModel) {
							const lfProps = nodeModel.getProperties()
							console.log('选中条件节点:', newNode.id, 'LogicFlow属性:', lfProps)

							// 如果LogicFlow中没有deviceId，尝试自动继承
							if (!lfProps.deviceId) {
								console.log('继承模式但LogicFlow中缺deviceId，尝试自动继承')
								checkAndInheritDevice()
								return // checkAndInheritDevice会调用loadDeviceProperties
							}
						}
					}
				}

				const deviceId =
					newNode.properties?.deviceSource === 'inherit' ? inheritedDeviceId.value : newNode.properties?.deviceId

				console.log('选中条件节点:', newNode.id, '设备ID:', deviceId, '设备来源:', newNode.properties?.deviceSource)

				if (deviceId) {
					// 加载该设备的属性列表
					loadDeviceProperties()
				} else {
					// 没有设备ID，清空属性列表
					currentDeviceProps.value = []
				}
			}
			// 如枟切换到非条件节点，清空属性列表
			else if (newNode?.type !== 'condition') {
				currentDeviceProps.value = []
			}
		},
		{ deep: true }
	)

	// 监听 inheritedDeviceId 变化，当继承的设备ID变化时，刷新当前选中的继承模式条件节点的属性
	watch(
		() => inheritedDeviceId.value,
		(newDeviceId, oldDeviceId) => {
			// 只有当前选中的是继承模式的条件节点，且设备ID真的变了，才重新加载属性
			if (
				selectedNode.value?.type === 'condition' &&
				selectedNode.value.properties?.deviceSource === 'inherit' &&
				newDeviceId &&
				newDeviceId !== oldDeviceId
			) {
				console.log('继承设备变化，重新加载属性:', newDeviceId)
				loadDeviceProperties()
			}
		}
	)

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
			const inheritedId = inheritedDeviceId.value
			console.log('检查自动继承 - inheritedId:', inheritedId)

			if (inheritedId) {
				const lf = getLogicFlowInstance()
				if (lf) {
					const nodeModel = lf.getNodeModelById(selectedNode.value.id)
					if (nodeModel) {
						const currentProps = nodeModel.getProperties()
						console.log('当前节点属性:', currentProps)

						// 如果没有deviceId，自动设置
						if (!currentProps.deviceId) {
							console.log('自动继承设备ID:', inheritedId)
							nodeModel.setProperties({
								...currentProps,
								deviceId: inheritedId
							})

							// 同步更新selectedNode
							selectedNode.value.properties.deviceId = inheritedId

							// 加载设备属性
							loadDeviceProperties()
							message.success('已自动继承上游触发器设备')
							updateNodeProperties()
						}
					}
				}
			}
		}
	}

	// 递归更新所有下游继承模式的简单条件节点
	const updateDownstreamInheritNodes = (lf, startNodeId, newDeviceId, visited = new Set()) => {
		// 避免循环引用
		if (visited.has(startNodeId)) return 0
		visited.add(startNodeId)

		const graphData = lf.getGraphData()
		const outgoingEdges = graphData.edges.filter((edge) => edge.sourceNodeId === startNodeId)
		let count = 0

		outgoingEdges.forEach((edge) => {
			const targetNode = graphData.nodes.find((node) => node.id === edge.targetNodeId)
			if (!targetNode) return

			console.log(
				'检查下游节点:',
				targetNode.id,
				'类型:',
				targetNode.type,
				'条件类型:',
				targetNode.properties?.conditionType,
				'设备来源:',
				targetNode.properties?.deviceSource
			)

			// 如果是简单条件且是继承模式,更新它
			if (
				targetNode.type === 'condition' &&
				targetNode.properties?.conditionType === 'simple' &&
				targetNode.properties?.deviceSource === 'inherit'
			) {
				console.log('更新简单条件节点:', targetNode.id)
				// 直接获取nodeModel并更新
				const nodeModel = lf.getNodeModelById(targetNode.id)
				if (nodeModel) {
					// 使用setProperties更新属性
					nodeModel.setProperties({
						...targetNode.properties,
						deviceId: newDeviceId,
						// 清空属性和阈值,因为设备变了
						property: undefined,
						operator: undefined,
						value: undefined
					})
					console.log('更新后的属性:', nodeModel.getProperties())
					count++

					// 检查当前选中的节点是否在被更新的节点中
					if (selectedNode.value?.id === targetNode.id) {
						console.log('当前选中的节点在更新列表中:', targetNode.id)
						return { updated: true, count }
					}
				}
			}
			// 如果是条件组,递归处理它的下游节点
			else if (targetNode.type === 'condition' && targetNode.properties?.conditionType === 'group') {
				console.log('递归处理条件组:', targetNode.id)
				const result = updateDownstreamInheritNodes(lf, targetNode.id, newDeviceId, visited)
				count += result
			}
			// 对于其他类型节点,也递归处理
			else {
				const result = updateDownstreamInheritNodes(lf, targetNode.id, newDeviceId, visited)
				count += result
			}
		})

		return count
	}

	// 处理触发器设备变化，更新下游所有继承模式的简单条件节点
	const handleTriggerDeviceChange = () => {
		console.log('=== 触发器设备变化开始 ===')
		if (selectedNode.value?.type !== 'trigger') {
			console.log('当前节点不是触发器，跳过')
			return
		}

		const lf = getLogicFlowInstance()
		if (!lf) {
			console.log('LogicFlow实例不存在')
			return
		}

		const newDeviceId = selectedNode.value.properties?.deviceId
		console.log('新设备ID:', newDeviceId)
		if (!newDeviceId) return

		// 只有设备触发器才需要更新下游节点
		if (selectedNode.value.properties?.triggerType !== 'device') {
			console.log('不是设备触发器，跳过')
			return
		}

		// 递归更新所有下游继承模式的简单条件节点
		const updatedCount = updateDownstreamInheritNodes(lf, selectedNode.value.id, newDeviceId)

		if (updatedCount > 0) {
			message.success(`已更新 ${updatedCount} 个继承模式的条件节点`)

			// 如果当前选中的是继承模式的简单条件，刷新它
			if (
				selectedNode.value?.type === 'condition' &&
				selectedNode.value.properties?.conditionType === 'simple' &&
				selectedNode.value.properties?.deviceSource === 'inherit'
			) {
				console.log('准备刷新当前选中节点')
				const nodeModel = lf.getNodeModelById(selectedNode.value.id)
				if (nodeModel) {
					console.log('从LogicFlow获取最新节点数据:', nodeModel.properties)
					selectedNode.value = {
						id: nodeModel.id,
						type: nodeModel.type,
						text: nodeModel.text?.value || '',
						properties: nodeModel.properties || {}
					}
					console.log('触发器设备变化，刷新当前选中的条件节点:', selectedNode.value.id)
				}
			}
		}

		console.log('=== 触发器设备变化结束 ===')
		// 如果当前选中的是触发器节点本身，也需要更新
		updateNodeProperties()
	}

	// 加载设备属性
	const loadDeviceProperties = async () => {
		console.log('=== 开始加载设备属性 ===')
		console.log('selectedNode:', selectedNode.value?.id, 'deviceSource:', selectedNode.value?.properties?.deviceSource)

		// 对于继承模式，从LogicFlow同步最新的deviceId
		let deviceId = selectedNode.value?.properties?.deviceId
		console.log('初始deviceId:', deviceId)

		if (selectedNode.value?.properties?.deviceSource === 'inherit') {
			const lf = getLogicFlowInstance()
			if (lf) {
				const nodeModel = lf.getNodeModelById(selectedNode.value.id)
				console.log('LogicFlow节点数据:', nodeModel?.properties)
				// LogicFlow使用MobX，需要使用getProperties()方法获取属性
				if (nodeModel && nodeModel.getProperties) {
					const props = nodeModel.getProperties()
					console.log('通过getProperties获取:', props)
					if (props?.deviceId) {
						deviceId = props.deviceId
						console.log('从LogicFlow同步设备ID:', deviceId)
					}
				} else if (nodeModel?.properties?.deviceId) {
					deviceId = nodeModel.properties.deviceId
					console.log('从LogicFlow同步设备ID:', deviceId)
				}
			}
		}

		console.log('最终使用的deviceId:', deviceId)
		if (!deviceId) {
			console.log('deviceId为空，清空属性列表')
			currentDeviceProps.value = []
			return
		}

		try {
			// 获取设备详情
			const device = deviceList.value.find((d) => d.id === deviceId)
			console.log('查找设备:', deviceId, '找到:', device)
			if (!device || !device.productId) {
				message.warning('设备信息不完整，无法获取属性列表')
				currentDeviceProps.value = []
				return
			}

			console.log('设备产品ID:', device.productId)
			// 根据产品ID获取物模型属性列表
			const properties = await iotThingModelApi.iotThingModelGetProperties({
				productId: device.productId,
				modelType: 'PROPERTY'
			})

			// 保存完整的属性对象，包含 dataType, dataSpecs 等信息
			currentDeviceProps.value = properties || []
			console.log('加载设备属性成功，数量:', currentDeviceProps.value.length, '属性列表:', currentDeviceProps.value)
		} catch (error) {
			console.error('加载设备属性失败:', error)
			message.error('加载设备属性失败')
			currentDeviceProps.value = []
		}

		console.log('=== 加载设备属性结束 ===')
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

	// 检查工作流完整性（纯检查逻辑，返回问题列表）
	const validateWorkflow = (graphData) => {
		const nodes = graphData?.nodes || []
		const edges = graphData?.edges || []
		const issues = []

		// 1. 检查是否有节点
		if (nodes.length === 0) {
			issues.push('画布为空，请添加节点')
			return issues
		}

		// 2. 检查是否有触发器
		const triggers = nodes.filter((node) => node.type === 'trigger')
		if (triggers.length === 0) {
			issues.push('缺少触发器节点')
		}

		// 3. 检查是否有动作
		const actions = nodes.filter((node) => node.type === 'action')
		if (actions.length === 0) {
			issues.push('缺少动作节点')
		}

		// 4. 检查触发器配置
		triggers.forEach((trigger) => {
			const props = trigger.properties || {}
			const nodeName = trigger.text?.value || '未命名触发器'

			if (!props.triggerType) {
				issues.push(`触发器「${nodeName}」未选择触发类型`)
			} else if (props.triggerType === 'device' && !props.deviceId) {
				issues.push(`触发器「${nodeName}」未选择设备`)
			}
		})

		// 5. 检查条件节点配置
		const conditions = nodes.filter((node) => node.type === 'condition')
		conditions.forEach((condition) => {
			const props = condition.properties || {}
			const nodeName = condition.text?.value || '未命名条件'

			if (!props.conditionType) {
				issues.push(`条件节点「${nodeName}」未选择条件类型`)
			} else if (props.conditionType === 'simple') {
				// 检查简单条件
				if (!props.property) {
					issues.push(`条件节点「${nodeName}」未选择属性`)
				}
				if (!props.operator) {
					issues.push(`条件节点「${nodeName}」未选择操作符`)
				}
				if (props.value === undefined || props.value === null || props.value === '') {
					issues.push(`条件节点「${nodeName}」未设置阈值`)
				}
				if (props.deviceSource === 'specify' && !props.deviceId) {
					issues.push(`条件节点「${nodeName}」未选择设备`)
				}
			} else if (props.conditionType === 'group') {
				// 检查条件组
				if (!props.logic) {
					issues.push(`条件组「${nodeName}」未选择逻辑关系`)
				}
				// 检查是否有子条件
				const childEdges = edges.filter((edge) => edge.sourceNodeId === condition.id)
				if (childEdges.length === 0) {
					issues.push(`条件组「${nodeName}」没有连接子条件`)
				}
			}
		})

		// 6. 检查动作节点配置
		actions.forEach((action) => {
			const props = action.properties || {}
			const nodeName = action.text?.value || '未命名动作'

			if (!props.actionType) {
				issues.push(`动作节点「${nodeName}」未选择动作类型`)
			} else if (props.actionType === 'deviceCommand') {
				if (!props.targetDeviceId) {
					issues.push(`动作节点「${nodeName}」未选择目标设备`)
				}
				if (!props.command) {
					issues.push(`动作节点「${nodeName}」未选择命令`)
				}
			}
		})

		// 7. 检查孤立节点（没有连线）
		nodes.forEach((node) => {
			const hasIncoming = edges.some((edge) => edge.targetNodeId === node.id)
			const hasOutgoing = edges.some((edge) => edge.sourceNodeId === node.id)
			const nodeName = node.text?.value || '未命名节点'

			// 触发器必须有连出
			if (node.type === 'trigger' && !hasOutgoing) {
				issues.push(`触发器「${nodeName}」没有连接到下游节点`)
			}

			// 动作节点必须有连入
			if (node.type === 'action' && !hasIncoming) {
				issues.push(`动作节点「${nodeName}」没有被任何节点连接`)
			}

			// 条件节点必须有连入和连出
			if (node.type === 'condition') {
				if (!hasIncoming) {
					issues.push(`条件节点「${nodeName}」没有被任何节点连接`)
				}
				// 简单条件必须有连出
				if (node.properties?.conditionType === 'simple' && !hasOutgoing) {
					issues.push(`条件节点「${nodeName}」没有连接到下游节点`)
				}
			}
		})

		return issues
	}

	// 检查流程完整性
	const checkWorkflowIntegrity = () => {
		const lf = getLogicFlowInstance()
		if (!lf) {
			message.warning('画布未初始化')
			return
		}

		const graphData = lf.getGraphData()
		const issues = validateWorkflow(graphData)

		// 显示检查结果
		if (issues.length === 0) {
			Modal.success({
				title: '流程完整性检查',
				content: '流程配置完整，没有发现问题'
			})
		} else {
			Modal.warning({
				title: `发现 ${issues.length} 个问题`,
				content: h(
					'div',
					{ style: { maxHeight: '400px', overflowY: 'auto' } },
					issues.map((issue, index) =>
						h('div', { style: { marginBottom: '8px', lineHeight: '1.6' } }, `${index + 1}. ${issue}`)
					)
				),
				width: 600
			})
		}
	}

	// 保存工作流
	const saveWorkflow = async () => {
		const data = getWorkflowData()
		if (!data) return

		if (!ruleRecord.value || !ruleRecord.value.id) {
			message.error('规则信息不存在，无法保存')
			return
		}

		// 检查完整性
		const issues = validateWorkflow(data)
		let integrityStatus = 'empty'
		let integrityIssues = 0

		if (data.nodes && data.nodes.length > 0) {
			if (issues.length === 0) {
				integrityStatus = 'valid'
				integrityIssues = 0
			} else {
				integrityStatus = 'invalid'
				integrityIssues = issues.length

				// 显示警告弹窗，询问是否继续保存
				const confirmed = await new Promise((resolve) => {
					Modal.confirm({
						title: `检测到 ${issues.length} 个配置问题`,
						content: h('div', {}, [
							h('div', { style: { marginBottom: '12px' } }, '以下配置不完整，可能导致规则无法正常执行：'),
							h(
								'div',
								{ style: { maxHeight: '200px', overflowY: 'auto' } },
								issues
									.slice(0, 5)
									.map((issue, index) => h('div', { style: { marginBottom: '4px' } }, `${index + 1}. ${issue}`))
							),
							issues.length > 5
								? h('div', { style: { marginTop: '8px', color: '#999' } }, `...还有 ${issues.length - 5} 个问题`)
								: null,
							h('div', { style: { marginTop: '12px', color: '#ff4d4f' } }, '是否仍然保存？')
						]),
						okText: '仍然保存',
						cancelText: '取消',
						width: 600,
						onOk() {
							resolve(true)
						},
						onCancel() {
							resolve(false)
						}
					})
				})

				if (!confirmed) {
					return
				}
			}
		}

		console.log('工作流数据:', data)
		console.log('工作流 JSON:', JSON.stringify(data, null, 2))

		try {
			// 保存工作流数据，需要携带必填字段和完整性信息
			const params = {
				id: ruleRecord.value.id,
				ruleName: ruleRecord.value.ruleName,
				ruleType: ruleRecord.value.ruleType,
				status: ruleRecord.value.status,
				workflowData: JSON.stringify(data),
				integrityStatus: integrityStatus,
				integrityIssues: integrityIssues
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
