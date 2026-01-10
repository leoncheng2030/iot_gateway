/**
 * 工作流节点类型配置
 */
export const nodeTypes = [
	{
		type: 'trigger',
		label: '触发器',
		color: '#52C41A'
	},
	{
		type: 'condition',
		label: '条件判断',
		color: '#1890FF'
	},
	{
		type: 'action',
		label: '执行动作',
		color: '#FA8C16'
	},
	{
		type: 'end',
		label: '结束',
		color: '#8C8C8C'
	}
]

/**
 * 初始化节点计数器
 */
export const initNodeCounters = () => ({
	trigger: 0,
	condition: 0,
	action: 0,
	end: 0
})
