import { message } from 'ant-design-vue'

/**
 * 配置 LogicFlow Menu 插件
 */
export const configureContextMenu = (lf, addNodeAtPosition) => {
	if (!lf) {
		console.error('LogicFlow 实例不存在')
		return
	}

	if (!lf.extension || !lf.extension.menu) {
		console.error('LogicFlow Menu 插件未加载')
		return
	}

	console.log('开始配置右键菜单...')

	// 配置节点和画布的右键菜单
	lf.extension.menu.setMenuConfig({
		nodeMenu: [
			{
				text: '🗑️ 删除节点',
				className: 'lf-menu-item lf-menu-item-delete',
				callback(node) {
					console.log('删除节点', node)
					lf.deleteNode(node.id)
					message.success('节点已删除')
				}
			}
		],
		graphMenu: [
			{
				text: '▶️ 添加触发器',
				className: 'lf-menu-item lf-menu-item-trigger',
				callback(data) {
					console.log('点击添加触发器', data)
					const { x, y } = data
					addNodeAtPosition('trigger', { x, y })
				}
			},
			{
				text: '◆️ 添加条件判断',
				className: 'lf-menu-item lf-menu-item-condition',
				callback(data) {
					const { x, y } = data
					addNodeAtPosition('condition', { x, y })
				}
			},
			{
				text: '⚡ 添加执行动作',
				className: 'lf-menu-item lf-menu-item-action',
				callback(data) {
					const { x, y } = data
					addNodeAtPosition('action', { x, y })
				}
			},
			{
				text: '✅ 添加结束节点',
				className: 'lf-menu-item lf-menu-item-end',
				callback(data) {
					const { x, y } = data
					addNodeAtPosition('end', { x, y })
				}
			}
		]
	})

	console.log('右键菜单配置完成')
}
