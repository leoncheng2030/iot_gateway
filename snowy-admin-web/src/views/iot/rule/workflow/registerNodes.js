/**
 * 注册自定义 LogicFlow 节点
 */
export const registerCustomNodes = (lf) => {
	// 触发器节点
	lf.register('trigger', ({ RectNode, RectNodeModel }) => {
		class TriggerNode extends RectNode {}
		class TriggerModel extends RectNodeModel {
			initNodeData(data) {
				super.initNodeData(data)
				this.width = 120
				this.height = 60
				// 确保 text为字符串
				if (data.text && typeof data.text === 'object') {
					this.text.value = data.text.value || '触发器'
				}
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
				// 确保 text为字符串
				if (data.text && typeof data.text === 'object') {
					this.text.value = data.text.value || '条件判断'
				}
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
				// 确保 text为字符串
				if (data.text && typeof data.text === 'object') {
					this.text.value = data.text.value || '执行动作'
				}
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

	// 结束节点
	lf.register('end', ({ CircleNode, CircleNodeModel }) => {
		class EndNode extends CircleNode {}
		class EndModel extends CircleNodeModel {
			initNodeData(data) {
				super.initNodeData(data)
				this.r = 30
				// 确保 text为字符串
				if (data.text && typeof data.text === 'object') {
					this.text.value = data.text.value || '结束'
				}
			}
			getNodeStyle() {
				const style = super.getNodeStyle()
				style.fill = '#F5F5F5'
				style.stroke = '#8C8C8C'
				style.strokeWidth = 3
				return style
			}
		}
		return {
			view: EndNode,
			model: EndModel
		}
	})
}
