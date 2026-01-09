<template>
	<a-modal
		v-model:open="visible"
		title="规则编排"
		width="90%"
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

				<!-- 右键菜单 -->
				<div
					v-if="contextMenu.visible"
					class="context-menu"
					:style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
				>
					<!-- 连线中添加节点 -->
					<template v-if="contextMenu.isConnecting">
						<div class="menu-header">连接到新节点</div>
						<div class="menu-item" @click.stop="addNode('trigger')">
							<PlayCircleOutline class="menu-icon" />
							触发器
						</div>
						<div class="menu-item" @click.stop="addNode('condition')">
							<GitCompareOutline class="menu-icon" />
							条件判断
						</div>
						<div class="menu-item" @click.stop="addNode('action')">
							<FlashOutline class="menu-icon" />
							执行动作
						</div>
					</template>
					<!-- 节点右键菜单 -->
					<template v-else-if="contextMenu.node">
						<div class="menu-item" @click.stop="deleteNode">
							<DeleteOutlined class="menu-icon" />
							删除节点
						</div>
					</template>
					<!-- 画布空白处添加节点 -->
					<template v-else>
						<div class="menu-item" @click.stop="addNode('trigger')">
							<PlayCircleOutline class="menu-icon" />
							添加触发器
						</div>
						<div class="menu-item" @click.stop="addNode('condition')">
							<GitCompareOutline class="menu-icon" />
							添加条件判断
						</div>
						<div class="menu-item" @click.stop="addNode('action')">
							<FlashOutline class="menu-icon" />
							添加执行动作
						</div>
					</template>
				</div>

				<!-- 右侧属性面板 -->
				<div class="property-panel" v-if="selectedNode">
					<div class="panel-title">节点属性</div>
					<div class="property-content">
						<a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
							<a-form-item label="节点名称">
								<a-input v-model:value="selectedNode.text" @change="updateNodeText" />
							</a-form-item>

							<!-- 触发器节点配置 -->
							<template v-if="selectedNode.type === 'trigger'">
								<a-form-item label="触发类型">
									<a-select v-model:value="selectedNode.properties.triggerType" @change="updateNodeProperties">
										<a-select-option value="device">设备事件</a-select-option>
										<a-select-option value="timer">定时触发</a-select-option>
									</a-select>
								</a-form-item>

								<a-form-item label="设备" v-if="selectedNode.properties.triggerType === 'device'">
									<a-select
										v-model:value="selectedNode.properties.deviceId"
										show-search
										placeholder="选择设备"
										@change="updateNodeProperties"
									>
										<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
											{{ device.deviceName }}
										</a-select-option>
									</a-select>
								</a-form-item>

								<a-form-item label="Cron表达式" v-if="selectedNode.properties.triggerType === 'timer'">
									<a-input
										v-model:value="selectedNode.properties.cronExpression"
										placeholder="0 0 * * * ?"
										@change="updateNodeProperties"
									/>
								</a-form-item>
							</template>

							<!-- 条件节点配置 -->
							<template v-if="selectedNode.type === 'condition'">
								<a-form-item label="条件类型">
									<a-radio-group v-model:value="selectedNode.properties.conditionType" @change="updateNodeProperties">
										<a-radio value="simple">简单条件</a-radio>
										<a-radio value="group">条件组</a-radio>
									</a-radio-group>
								</a-form-item>

								<template v-if="selectedNode.properties.conditionType === 'simple'">
									<a-form-item label="设备">
										<a-select
											v-model:value="selectedNode.properties.deviceId"
											show-search
											@change="loadDeviceProperties"
											:disabled="availableDevices.length === 1"
										>
											<a-select-option v-for="device in availableDevices" :key="device.id" :value="device.id">
												{{ device.deviceName }}
											</a-select-option>
										</a-select>
									</a-form-item>

									<a-form-item label="属性">
										<a-select v-model:value="selectedNode.properties.property" @change="updateNodeProperties">
											<a-select-option v-for="prop in currentDeviceProps" :key="prop" :value="prop">
												{{ prop }}
											</a-select-option>
										</a-select>
									</a-form-item>

									<a-form-item label="操作符">
										<a-select v-model:value="selectedNode.properties.operator" @change="updateNodeProperties">
											<a-select-option value=">">大于</a-select-option>
											<a-select-option value=">=">大于等于</a-select-option>
											<a-select-option value="<">小于</a-select-option>
											<a-select-option value="<=">小于等于</a-select-option>
											<a-select-option value="==">等于</a-select-option>
											<a-select-option value="!=">不等于</a-select-option>
										</a-select>
									</a-form-item>

									<a-form-item label="阈值">
										<a-input
											v-model:value="selectedNode.properties.value"
											type="number"
											@change="updateNodeProperties"
										/>
									</a-form-item>
								</template>

								<template v-else>
									<a-form-item label="逻辑关系">
										<a-radio-group v-model:value="selectedNode.properties.logic" @change="updateNodeProperties">
											<a-radio value="AND">且(AND)</a-radio>
											<a-radio value="OR">或(OR)</a-radio>
										</a-radio-group>
									</a-form-item>
									<a-alert message="条件组需要连接多个子条件节点" type="info" show-icon style="margin-top: 8px" />
								</template>
							</template>

							<!-- 动作节点配置 -->
							<template v-if="selectedNode.type === 'action'">
								<a-form-item label="动作类型">
									<a-select v-model:value="selectedNode.properties.actionType" @change="updateNodeProperties">
										<a-select-option value="deviceCommand">设备指令</a-select-option>
										<a-select-option value="notification">发送通知</a-select-option>
										<a-select-option value="webhook">Webhook</a-select-option>
									</a-select>
								</a-form-item>

								<template v-if="selectedNode.properties.actionType === 'deviceCommand'">
									<a-form-item label="目标设备">
										<a-select
											v-model:value="selectedNode.properties.targetDeviceId"
											show-search
											@change="updateNodeProperties"
										>
											<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
												{{ device.deviceName }}
											</a-select-option>
										</a-select>
									</a-form-item>

									<a-form-item label="指令">
										<a-input
											v-model:value="selectedNode.properties.command"
											placeholder="setOutput"
											@change="updateNodeProperties"
										/>
									</a-form-item>

									<a-form-item label="参数">
										<a-textarea
											v-model:value="selectedNode.properties.params"
											placeholder='{"output": "DO1", "value": true}'
											:rows="3"
											@change="updateNodeProperties"
										/>
									</a-form-item>
								</template>

								<template v-if="selectedNode.properties.actionType === 'notification'">
									<a-form-item label="通知渠道">
										<a-select v-model:value="selectedNode.properties.channel" @change="updateNodeProperties">
											<a-select-option value="sms">短信</a-select-option>
											<a-select-option value="email">邮件</a-select-option>
											<a-select-option value="dingtalk">钉钉</a-select-option>
											<a-select-option value="alert">告警</a-select-option>
										</a-select>
									</a-form-item>

									<a-form-item label="标题">
										<a-input v-model:value="selectedNode.properties.title" @change="updateNodeProperties" />
									</a-form-item>

									<a-form-item label="内容">
										<a-textarea
											v-model:value="selectedNode.properties.content"
											:rows="3"
											@change="updateNodeProperties"
										/>
									</a-form-item>

									<a-form-item label="接收人">
										<a-input
											v-model:value="selectedNode.properties.target"
											placeholder="多个用逗号分隔"
											@change="updateNodeProperties"
										/>
									</a-form-item>
								</template>

								<template v-if="selectedNode.properties.actionType === 'webhook'">
									<a-form-item label="URL">
										<a-input
											v-model:value="selectedNode.properties.url"
											placeholder="https://example.com/webhook"
											@change="updateNodeProperties"
										/>
									</a-form-item>

									<a-form-item label="请求方法">
										<a-select v-model:value="selectedNode.properties.method" @change="updateNodeProperties">
											<a-select-option value="GET">GET</a-select-option>
											<a-select-option value="POST">POST</a-select-option>
										</a-select>
									</a-form-item>

									<a-form-item label="请求体">
										<a-textarea
											v-model:value="selectedNode.properties.body"
											placeholder='{"data": "value"}'
											:rows="3"
											@change="updateNodeProperties"
										/>
									</a-form-item>
								</template>
							</template>
						</a-form>
					</div>
				</div>
			</div>
		</div>
	</a-modal>
</template>

<script setup name="iotRuleWorkflow">
	import { ref, onMounted, onUnmounted, reactive, nextTick, computed } from 'vue'
	import LogicFlow from '@logicflow/core'
	import '@logicflow/core/dist/index.css'
	import {
		SaveOutlined,
		ClearOutlined,
		ZoomInOutlined,
		ZoomOutOutlined,
		FullscreenOutlined,
		DeleteOutlined
	} from '@ant-design/icons-vue'
	import { PlayCircleOutline, GitCompareOutline, FlashOutline } from '@vicons/ionicons5'
	import { message } from 'ant-design-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'

	// 弹窗状态
	const visible = ref(false)
	const ruleRecord = ref(null)
	const emit = defineEmits(['successful'])

	// LogicFlow 实例
	let lf = null
	const canvasRef = ref(null)

	// 选中的节点
	const selectedNode = ref(null)

	// 设备列表
	const deviceList = ref([])
	const currentDeviceProps = ref([])

	// 获取上游触发器节点的设备
	const getUpstreamTriggerDevice = () => {
		if (!lf || !selectedNode.value) return null

		// 获取所有连线
		const graphData = lf.getGraphData()
		const edges = graphData.edges || []

		// 查找指向当前节点的连线
		const incomingEdges = edges.filter((edge) => edge.targetNodeId === selectedNode.value.id)

		if (incomingEdges.length === 0) return null

		// 获取源节点
		const sourceNodeId = incomingEdges[0].sourceNodeId
		const nodes = graphData.nodes || []
		const sourceNode = nodes.find((node) => node.id === sourceNodeId)

		if (!sourceNode) return null

		// 如果源节点是触发器且有设备配置，返回设备ID
		if (
			sourceNode.type === 'trigger' &&
			sourceNode.properties?.triggerType === 'device' &&
			sourceNode.properties?.deviceId
		) {
			return sourceNode.properties.deviceId
		}

		return null
	}

	// 计算属性：条件节点可用的设备列表
	const availableDevices = computed(() => {
		if (!selectedNode.value || selectedNode.value.type !== 'condition') {
			return deviceList.value
		}

		const upstreamDeviceId = getUpstreamTriggerDevice()
		if (upstreamDeviceId) {
			// 如果有上游触发器设备，只显示该设备
			return deviceList.value.filter((device) => device.id === upstreamDeviceId)
		}

		// 否则显示所有设备
		return deviceList.value
	})

	// 节点类型定义
	const nodeTypes = [
		{
			type: 'trigger',
			label: '触发器',
			icon: PlayCircleOutline,
			color: '#52C41A'
		},
		{
			type: 'condition',
			label: '条件判断',
			icon: GitCompareOutline,
			color: '#1890FF'
		},
		{
			type: 'action',
			label: '执行动作',
			icon: FlashOutline,
			color: '#FA8C16'
		}
	]

	// 初始化 LogicFlow
	const initLogicFlow = () => {
		const canvas = canvasRef.value
		if (!canvas) {
			console.error('画布容器未找到')
			return
		}

		// 获取画布容器尺寸，如果为0则使用默认值
		const width = canvas.offsetWidth || 800
		const height = canvas.offsetHeight || 600

		console.log('初始化画布尺寸:', width, height)

		lf = new LogicFlow({
			container: canvas,
			width,
			height,
			grid: {
				size: 10,
				visible: true,
				type: 'dot',
				config: {
					color: '#e5e5e5'
				}
			},
			keyboard: {
				enabled: true
			},
			multipleSelectKey: '',
			style: {
				rect: {
					rx: 4,
					ry: 4,
					strokeWidth: 2
				},
				circle: {
					r: 40,
					strokeWidth: 2
				},
				nodeText: {
					fontSize: 14
				},
				edgeText: {
					fontSize: 12
				}
			},
			edgeStyle: {
				stroke: '#52C41A',
				strokeWidth: 2
			}
		})

		// 注册自定义节点
		registerCustomNodes()

		// 渲染画布
		lf.render()

		// 监听画布右键点击
		lf.on('blank:contextmenu', ({ e, position }) => {
			e.preventDefault()
			showContextMenu(position.domOverlayPosition.x, position.domOverlayPosition.y, null)
		})

		// 监听节点右键点击
		lf.on('node:contextmenu', ({ e, data, position }) => {
			e.preventDefault()
			showContextMenu(position.domOverlayPosition.x, position.domOverlayPosition.y, data)
		})

		// 监听连线拖拽中的右键点击（未连接到目标节点）
		lf.on('edge:adjust', ({ e }) => {
			if (e && e.button === 2) {
				// 右键点击
				e.preventDefault()
			}
		})

		// 监听连线开始（鼠标按下连接点）
		lf.on('anchor:dragstart', ({ data, nodeModel }) => {
			isDrawingEdge = true
			edgeSourceNode = { id: nodeModel.id, type: nodeModel.type, properties: nodeModel.properties }
		})

		// 监听连线结束
		lf.on('anchor:drop', () => {
			isDrawingEdge = false
			edgeSourceNode = null
		})

		// 监听画布点击（取消连线）
		lf.on('blank:click', () => {
			if (isDrawingEdge) {
				// 如果正在连线中点击画布，重置状态
				isDrawingEdge = false
				edgeSourceNode = null
			}
			selectedNode.value = null
		})

		// 监听连接未完成事件
		lf.on('connection:not-allowed', ({ e, msg }) => {
			if (e && e.button === 2) {
				// 如果是右键点击，显示菜单而不是警告
				e.preventDefault()
				const rect = canvasRef.value.getBoundingClientRect()
				showContextMenu(e.clientX - rect.left, e.clientY - rect.top, null, true)
			} else if (msg) {
				message.warning(msg || '该连接不符合规则')
			}
			// 重置连线状态
			isDrawingEdge = false
			edgeSourceNode = null
		})

		// 监听节点添加事件，只选中新添加的节点
		lf.on('node:add', ({ data }) => {
			// 取消所有选中
			lf.clearSelectElements()
			// 只选中新添加的节点
			setTimeout(() => {
				lf.selectElementById(data.id, true)
				selectedNode.value = data
				if (!selectedNode.value.properties) {
					selectedNode.value.properties = {}
				}
			}, 0)
		})

		// 监听事件
		lf.on('node:click', ({ data }) => {
			selectedNode.value = data
			if (!selectedNode.value.properties) {
				selectedNode.value.properties = {}
			}
		})

		lf.on('node:delete', () => {
			selectedNode.value = null
		})

		// 监听连线创建
		lf.on('edge:add', ({ data }) => {
			// 如果目标节点是条件节点，自动继承上游触发器的设备
			const graphData = lf.getGraphData()
			const targetNode = graphData.nodes.find((node) => node.id === data.targetNodeId)
			const sourceNode = graphData.nodes.find((node) => node.id === data.sourceNodeId)

			if (
				targetNode &&
				targetNode.type === 'condition' &&
				sourceNode &&
				sourceNode.type === 'trigger' &&
				sourceNode.properties?.triggerType === 'device' &&
				sourceNode.properties?.deviceId
			) {
				// 自动设置条件节点的设备ID
				const updatedProperties = {
					...(targetNode.properties || {}),
					deviceId: sourceNode.properties.deviceId
				}
				lf.setProperties(targetNode.id, updatedProperties)

				// 如果当前选中的是该节点，更新选中节点的数据
				if (selectedNode.value && selectedNode.value.id === targetNode.id) {
					selectedNode.value.properties = updatedProperties
					// 加载设备属性
					loadDeviceProperties()
				}
			}
		})

		// 监听连线删除
		lf.on('edge:delete', () => {
			// 如果当前选中的是条件节点，刷新设备列表
			if (selectedNode.value && selectedNode.value.type === 'condition') {
				// 触发重新计算
				const temp = selectedNode.value
				selectedNode.value = null
				setTimeout(() => {
					selectedNode.value = temp
				}, 0)
			}
		})

		// 监听连线创建,验证连接规则
		lf.on('connection:not-allowed', (data) => {
			message.warning('该连接不符合规则')
		})
	}

	// 注册自定义节点
	const registerCustomNodes = () => {
		// 触发器节点
		lf.register('trigger', ({ RectNode, RectNodeModel }) => {
			class TriggerNode extends RectNode {}
			class TriggerModel extends RectNodeModel {
				initNodeData(data) {
					super.initNodeData(data)
					this.width = 120
					this.height = 60
				}
				getNodeStyle() {
					const style = super.getNodeStyle()
					style.fill = '#F6FFED'
					style.stroke = '#52C41A'
					style.strokeWidth = 2
					return style
				}
			}
			return {
				view: TriggerNode,
				model: TriggerModel
			}
		})

		// 条件节点
		lf.register('condition', ({ DiamondNode, DiamondNodeModel }) => {
			class ConditionNode extends DiamondNode {}
			class ConditionModel extends DiamondNodeModel {
				initNodeData(data) {
					super.initNodeData(data)
					this.rx = 50
					this.ry = 30
				}
				getNodeStyle() {
					const style = super.getNodeStyle()
					style.fill = '#E6F7FF'
					style.stroke = '#1890FF'
					style.strokeWidth = 2
					return style
				}
			}
			return {
				view: ConditionNode,
				model: ConditionModel
			}
		})

		// 动作节点
		lf.register('action', ({ RectNode, RectNodeModel }) => {
			class ActionNode extends RectNode {}
			class ActionModel extends RectNodeModel {
				initNodeData(data) {
					super.initNodeData(data)
					this.width = 120
					this.height = 60
				}
				getNodeStyle() {
					const style = super.getNodeStyle()
					style.fill = '#FFF7E6'
					style.stroke = '#FA8C16'
					style.strokeWidth = 2
					return style
				}
			}
			return {
				view: ActionNode,
				model: ActionModel
			}
		})
	}

	// 右键菜单状态
	const contextMenu = reactive({
		visible: false,
		x: 0,
		y: 0,
		node: null,
		isConnecting: false, // 是否正在连线
		sourceNode: null // 连线的源节点
	})

	// 记录连线状态
	let isDrawingEdge = false
	let edgeSourceNode = null

	// 显示右键菜单
	const showContextMenu = (x, y, node, isConnecting = false) => {
		contextMenu.x = x
		contextMenu.y = y
		contextMenu.node = node
		contextMenu.isConnecting = isConnecting || isDrawingEdge
		if (contextMenu.isConnecting) {
			contextMenu.sourceNode = edgeSourceNode || selectedNode.value
		}
		contextMenu.visible = true
	}

	// 隐藏右键菜单
	const hideContextMenu = () => {
		contextMenu.visible = false
		contextMenu.isConnecting = false
		contextMenu.sourceNode = null
	}

	// 添加节点
	const addNode = (nodeType) => {
		const nodeConfig = nodeTypes.find((n) => n.type === nodeType)
		if (!nodeConfig) return

		const newNode = lf.addNode({
			type: nodeType,
			x: contextMenu.x,
			y: contextMenu.y,
			text: nodeConfig.label,
			properties: {}
		})

		// 如果是在连线中添加的节点，自动创建连线
		if (contextMenu.isConnecting && contextMenu.sourceNode) {
			setTimeout(() => {
				lf.addEdge({
					sourceNodeId: contextMenu.sourceNode.id,
					targetNodeId: newNode.id,
					type: 'polyline'
				})
			}, 100)
		}

		hideContextMenu()
	}

	// 删除节点
	const deleteNode = () => {
		if (contextMenu.node) {
			lf.deleteNode(contextMenu.node.id)
			hideContextMenu()
		}
	}

	// 更新节点文本
	const updateNodeText = () => {
		lf.setProperties(selectedNode.value.id, {
			...selectedNode.value.properties
		})
		lf.updateText(selectedNode.value.id, selectedNode.value.text)
	}

	// 更新节点属性
	const updateNodeProperties = () => {
		lf.setProperties(selectedNode.value.id, selectedNode.value.properties)
	}

	// 加载设备属性
	const loadDeviceProperties = () => {
		// 这里应该调用 API 获取设备的属性列表
		// 暂时使用模拟数据
		currentDeviceProps.value = ['temperature', 'humidity', 'pressure', 'DO1', 'DO2', 'DO3']
		updateNodeProperties()
	}

	// 画布操作
	const zoomIn = () => {
		lf.zoom(true)
	}

	const zoomOut = () => {
		lf.zoom(false)
	}

	const fitView = () => {
		lf.resetZoom()
		lf.resetTranslate()
	}

	const clearWorkflow = () => {
		lf.clearData()
		selectedNode.value = null
		message.success('画布已清空')
	}

	// 打开弹窗
	const onOpen = (record) => {
		visible.value = true
		ruleRecord.value = record
		nextTick(() => {
			// 等待 DOM 完全渲染后再初始化
			setTimeout(() => {
				if (!lf) {
					initLogicFlow()
					loadDeviceList()
				} else {
					// 如果已经初始化，重新调整画布大小
					const canvas = canvasRef.value
					if (canvas) {
						const width = canvas.offsetWidth || 800
						const height = canvas.offsetHeight || 600
						lf.resize(width, height)
					}
				}
				// 如果规则有保存的工作流数据，加载它
				if (record && record.workflowData) {
					try {
						const graphData = JSON.parse(record.workflowData)
						lf.renderRawData(graphData)
					} catch (e) {
						console.error('加载工作流数据失败', e)
					}
				}
			}, 100)
		})
	}

	// 关闭弹窗
	const handleCancel = () => {
		visible.value = false
		ruleRecord.value = null
		selectedNode.value = null
		if (lf) {
			lf.clearData()
		}
	}

	// 保存工作流
	const saveWorkflow = () => {
		const data = lf.getGraphData()
		console.log('规则数据:', data)

		// 验证流程
		if (!data.nodes || data.nodes.length === 0) {
			message.warning('请先添加节点')
			return
		}

		// 检查是否有触发器
		const hasTrigger = data.nodes.some((node) => node.type === 'trigger')
		if (!hasTrigger) {
			message.warning('规则必须包含至少一个触发器节点')
			return
		}

		// 这里可以调用API保存工作流数据到规则记录
		// iotRuleApi.iotRuleSubmitForm({ id: ruleRecord.value.id, workflowData: JSON.stringify(data) }, true)
		message.success('规则保存成功')
		visible.value = false
		emit('successful')
	}

	// 加载设备列表
	const loadDeviceList = () => {
		iotDeviceApi.iotDevicePage({ current: 1, size: 1000 }).then((res) => {
			deviceList.value = res.records || []
		})
	}

	onMounted(() => {
		// 监听键盘事件
		document.addEventListener('keydown', handleKeyDown)
	})

	onUnmounted(() => {
		document.removeEventListener('keydown', handleKeyDown)
		if (lf) {
			lf.destroy()
		}
	})

	defineExpose({
		onOpen
	})

	const handleKeyDown = (e) => {
		// Delete 键删除选中节点
		if (e.key === 'Delete' && selectedNode.value) {
			lf.deleteNode(selectedNode.value.id)
		}
	}
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
		background: #fff;
	}

	/* 右键菜单样式 */
	.context-menu {
		position: fixed;
		z-index: 9999;
		background: #fff;
		border: 1px solid #e8e8e8;
		border-radius: 4px;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
		min-width: 160px;
		padding: 4px 0;

		.menu-header {
			padding: 8px 16px;
			font-size: 12px;
			color: #8c8c8c;
			border-bottom: 1px solid #f0f0f0;
			margin-bottom: 4px;
		}

		.menu-item {
			display: flex;
			align-items: center;
			gap: 8px;
			padding: 8px 16px;
			cursor: pointer;
			transition: all 0.3s;
			user-select: none;
			font-size: 14px;

			&:hover {
				background: #f5f5f5;
			}

			.menu-icon {
				font-size: 16px;
				color: #1890ff;
			}
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
