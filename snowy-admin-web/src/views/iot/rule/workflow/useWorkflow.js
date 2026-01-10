import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import LogicFlow from '@logicflow/core'
import { Menu } from '@logicflow/extension'
import { registerCustomNodes } from './registerNodes'
import { configureContextMenu } from './menuConfig'
import { nodeTypes, initNodeCounters } from './nodeConfig'

// 注册 LogicFlow Menu 插件
LogicFlow.use(Menu)

/**
 * 工作流逻辑封装
 */
export const useWorkflow = (canvasRef) => {
	// LogicFlow 实例
	let lf = null

	// 选中的节点
	const selectedNode = ref(null)

	// 节点计数器
	const nodeCounters = ref(initNodeCounters())

	// 计算属性：节点显示名称
	const nodeDisplayName = computed({
		get() {
			if (!selectedNode.value) return ''
			if (typeof selectedNode.value.text === 'string') {
				return selectedNode.value.text
			}
			if (typeof selectedNode.value.text === 'object' && selectedNode.value.text.value) {
				return selectedNode.value.text.value
			}
			return ''
		},
		set(newValue) {
			if (selectedNode.value) {
				if (typeof selectedNode.value.text === 'object') {
					selectedNode.value.text.value = newValue
				} else {
					selectedNode.value.text = newValue
				}
			}
		}
	})

	// 在指定位置添加节点
	const addNodeAtPosition = (nodeType, position) => {
		const nodeConfig = nodeTypes.find((n) => n.type === nodeType)
		if (!nodeConfig) return

		// 处理不同的位置格式
		let x, y
		if (position.domOverlayPosition) {
			// LogicFlow position 对象
			x = position.domOverlayPosition.x
			y = position.domOverlayPosition.y
		} else {
			// 简单的 {x, y} 对象
			x = position.x
			y = position.y
		}

		// 生成节点名称
		nodeCounters.value[nodeType]++
		const nodeName = `${nodeConfig.label}${nodeCounters.value[nodeType]}`

		console.log('添加节点:', nodeType, '名称:', nodeName, 'typeof:', typeof nodeName)

		const newNode = lf.addNode({
			type: nodeType,
			x,
			y,
			text: {
				value: nodeName,
				x: x,
				y: y
			},
			properties: {}
		})

		message.success(`已添加${nodeName}`)
	}

	// 初始化 LogicFlow
	const initLogicFlow = (callbacks = {}) => {
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
					color: '#ababab',
					thickness: 1
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

		console.log('LogicFlow 实例创建完成', lf)
		console.log('extension 对象:', lf.extension)
		console.log('menu 对象:', lf.extension?.menu)

		// 注册自定义节点
		registerCustomNodes(lf)

		// 渲染画布 - 传入空数据以确保画布正确渲染
		lf.render({})

		console.log('画布渲染完成')

		// 配置右键菜单（在 render 之后）
		configureContextMenu(lf, addNodeAtPosition)

		// 监听节点添加事件，只选中新添加的节点
		lf.on('node:add', ({ data }) => {
			lf.clearSelectElements()
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
			// 从 LogicFlow 中获取最新的节点数据（包含 properties）
			const nodeModel = lf.getNodeModelById(data.id)
			if (nodeModel) {
				selectedNode.value = {
					id: nodeModel.id,
					type: nodeModel.type,
					text: nodeModel.text?.value || '',
					properties: nodeModel.properties || {}
				}
			} else {
				selectedNode.value = data
			}
			if (!selectedNode.value.properties) {
				selectedNode.value.properties = {}
			}
		})

		lf.on('node:delete', () => {
			selectedNode.value = null
		})

		// 监听连线创建成功
		lf.on('edge:add', ({ data }) => {
			// 如果有回调函数，调用它
			if (callbacks.onEdgeAdd) {
				callbacks.onEdgeAdd(data)
			}
		})

		// 监听连线创建,验证连接规则
		lf.on('connection:not-allowed', (data) => {
			message.warning('该连接不符合规则')
		})
	}

	// 更新节点文本
	const updateNodeText = () => {
		if (!selectedNode.value) return

		lf.setProperties(selectedNode.value.id, {
			...selectedNode.value.properties
		})

		// 使用 nodeDisplayName 的值更新文本
		lf.updateText(selectedNode.value.id, nodeDisplayName.value)
	}

	// 更新节点属性
	const updateNodeProperties = () => {
		if (!selectedNode.value) {
			return
		}
		lf.setProperties(selectedNode.value.id, selectedNode.value.properties)
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
		nodeCounters.value = initNodeCounters()
		message.success('画布已清空')
	}

	// 保存工作流
	const saveWorkflow = () => {
		const data = lf.getGraphData()
		console.log('规则数据:', data)

		// 验证流程
		if (!data.nodes || data.nodes.length === 0) {
			message.warning('请先添加节点')
			return false
		}

		// 检查是否有触发器
		const hasTrigger = data.nodes.some((node) => node.type === 'trigger')
		if (!hasTrigger) {
			message.warning('规则必须包含至少一个触发器节点')
			return false
		}

		return data
	}

	// 加载工作流数据
	const loadWorkflowData = (workflowData) => {
		if (!workflowData) {
			nodeCounters.value = initNodeCounters()
			return
		}

		try {
			const graphData = JSON.parse(workflowData)
			lf.renderRawData(graphData)

			// 统计已有节点，初始化计数器
			const typeCounts = initNodeCounters()
			graphData.nodes?.forEach((node) => {
				if (typeCounts.hasOwnProperty(node.type)) {
					// 从节点文本中提取编号
					if (node.text && typeof node.text === 'string') {
						const match = node.text.match(/\d+$/)
						if (match) {
							const num = parseInt(match[0])
							if (num > typeCounts[node.type]) {
								typeCounts[node.type] = num
							}
						}
					}
				}
			})
			nodeCounters.value = typeCounts
		} catch (e) {
			console.error('加载工作流数据失败', e)
		}
	}

	// 调整画布大小
	const resizeCanvas = () => {
		const canvas = canvasRef.value
		if (canvas && lf) {
			const width = canvas.offsetWidth || 800
			const height = canvas.offsetHeight || 600
			lf.resize(width, height)
		}
	}

	// 清理资源
	const destroy = () => {
		if (lf) {
			lf.destroy()
			lf = null
		}
		selectedNode.value = null
		nodeCounters.value = initNodeCounters()
	}

	// 获取 LogicFlow 实例
	const getLogicFlowInstance = () => lf

	return {
		// 状态
		selectedNode,
		nodeDisplayName,

		// 方法
		initLogicFlow,
		updateNodeText,
		updateNodeProperties,
		zoomIn,
		zoomOut,
		fitView,
		clearWorkflow,
		saveWorkflow,
		loadWorkflowData,
		resizeCanvas,
		destroy,
		getLogicFlowInstance
	}
}
