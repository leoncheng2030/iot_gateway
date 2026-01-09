<template>
	<a-drawer
		:title="'规则详情 - ' + ruleData.ruleName"
		:width="1200"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-tabs v-model:activeKey="activeTab" @change="onTabChange">
			<!-- 基本信息 -->
			<a-tab-pane key="basic" tab="基本信息">
				<a-descriptions bordered :column="2">
					<a-descriptions-item label="规则名称">{{ ruleData.ruleName }}</a-descriptions-item>
					<a-descriptions-item label="规则类型">
						{{ $TOOL.dictTypeData('RULE_TYPE', ruleData.ruleType) }}
					</a-descriptions-item>
					<a-descriptions-item label="状态">
						<a-badge
							:status="ruleData.status === 'ENABLE' ? 'success' : 'default'"
							:text="$TOOL.dictTypeData('COMMON_STATUS', ruleData.status)"
						/>
					</a-descriptions-item>
					<a-descriptions-item label="排序码">{{ ruleData.sortCode || '-' }}</a-descriptions-item>
					<a-descriptions-item label="规则描述" :span="2">{{ ruleData.ruleDesc || '-' }}</a-descriptions-item>
					<a-descriptions-item label="创建时间">{{ ruleData.createTime }}</a-descriptions-item>
					<a-descriptions-item label="更新时间">{{ ruleData.updateTime }}</a-descriptions-item>
				</a-descriptions>
			</a-tab-pane>

			<!-- 规则配置 -->
			<a-tab-pane key="config" tab="规则配置">
				<!-- 触发条件 -->
				<a-card size="small" title="触发条件" style="margin-bottom: 16px">
					<ConditionBuilder ref="conditionBuilderRef" />
				</a-card>

				<!-- 执行动作 -->
				<a-card size="small" title="执行动作" style="margin-bottom: 16px">
					<ActionBuilder ref="actionBuilderRef" />
				</a-card>

				<!-- 保存按钮 -->
				<div style="text-align: right">
					<a-button type="primary" @click="saveRuleConfig" :loading="saveConfigLoading">保存配置</a-button>
				</div>
			</a-tab-pane>

			<!-- 查看配置 -->
			<a-tab-pane key="view" tab="查看配置">
				<a-card size="small" title="触发条件" style="margin-bottom: 16px">
					<div v-if="triggerCondition" style="padding: 12px; background: #f5f5f5; border-radius: 4px">
						<a-descriptions :column="1" size="small">
							<a-descriptions-item label="设备ID">
								{{ triggerCondition.deviceId || '-' }}
							</a-descriptions-item>
							<a-descriptions-item label="属性">
								{{ triggerCondition.property || '-' }}
							</a-descriptions-item>
							<a-descriptions-item label="运算符">
								<a-tag color="blue">{{ triggerCondition.operator || '-' }}</a-tag>
							</a-descriptions-item>
							<a-descriptions-item label="阈值">
								<a-tag color="orange">{{ triggerCondition.value }}</a-tag>
							</a-descriptions-item>
						</a-descriptions>
						<a-divider style="margin: 12px 0" />
						<div style="font-family: Monaco, monospace; font-size: 12px; color: #666">
							<div style="color: #999; margin-bottom: 4px">JSON格式：</div>
							<pre style="margin: 0">{{ JSON.stringify(triggerCondition, null, 2) }}</pre>
						</div>
					</div>
					<a-empty v-else description="暂无触发条件配置" :image="simpleImage" />
				</a-card>

				<a-card size="small" title="执行动作">
					<a-timeline v-if="actions && actions.length > 0" style="margin-top: 16px">
						<a-timeline-item v-for="(action, index) in actions" :key="index">
							<template #dot>
								<span
									style="
										display: inline-block;
										width: 24px;
										height: 24px;
										line-height: 24px;
										text-align: center;
										background: #1890ff;
										color: white;
										border-radius: 50%;
										font-size: 12px;
									"
								>
									{{ index + 1 }}
								</span>
							</template>
							<a-card size="small" :title="getActionTitle(action.type)" style="margin-bottom: 12px">
								<!-- 设备指令动作 -->
								<template v-if="action.type === 'deviceCommand'">
									<a-descriptions :column="1" size="small">
										<a-descriptions-item label="目标设备ID">
											{{ action.deviceId }}
										</a-descriptions-item>
										<a-descriptions-item label="指令名称">
											<a-tag color="blue">{{ action.command }}</a-tag>
										</a-descriptions-item>
										<a-descriptions-item label="指令参数">
											<pre style="margin: 0; font-size: 12px">{{ JSON.stringify(action.params, null, 2) }}</pre>
										</a-descriptions-item>
									</a-descriptions>
								</template>

								<!-- 通知动作 -->
								<template v-else-if="action.type === 'notification'">
									<a-descriptions :column="1" size="small">
										<a-descriptions-item label="通知渠道">
											<a-tag :color="getChannelColor(action.channel)">
												{{ getChannelName(action.channel) }}
											</a-tag>
										</a-descriptions-item>
										<a-descriptions-item label="通知标题">
											{{ action.title }}
										</a-descriptions-item>
										<a-descriptions-item label="通知内容">
											{{ action.content }}
										</a-descriptions-item>
										<a-descriptions-item label="告警级别">
											<a-tag :color="getLevelColor(action.level)">
												{{ action.level || '-' }}
											</a-tag>
										</a-descriptions-item>
									</a-descriptions>
								</template>

								<!-- Webhook动作 -->
								<template v-else-if="action.type === 'webhook'">
									<a-descriptions :column="1" size="small">
										<a-descriptions-item label="请求URL">
											<a-typography-text copyable>{{ action.url }}</a-typography-text>
										</a-descriptions-item>
										<a-descriptions-item label="请求方法">
											<a-tag>{{ action.method || 'POST' }}</a-tag>
										</a-descriptions-item>
										<a-descriptions-item label="请求体">
											<pre style="margin: 0; font-size: 12px">{{ JSON.stringify(action.body, null, 2) }}</pre>
										</a-descriptions-item>
									</a-descriptions>
								</template>
							</a-card>
						</a-timeline-item>
					</a-timeline>
					<a-empty v-else description="暂无执行动作配置" :image="simpleImage" />
				</a-card>
			</a-tab-pane>

			<!-- 执行日志 -->
			<a-tab-pane key="executeLog" tab="执行日志">
				<a-card size="small" style="margin-bottom: 16px">
					<a-row :gutter="16">
						<a-col :span="8">
							<a-statistic title="总执行次数" :value="logStatistics.total" />
						</a-col>
						<a-col :span="8">
							<a-statistic title="成功次数" :value="logStatistics.success" :value-style="{ color: '#52c41a' }" />
						</a-col>
						<a-col :span="8">
							<a-statistic title="失败次数" :value="logStatistics.failed" :value-style="{ color: '#f5222d' }" />
						</a-col>
					</a-row>
				</a-card>

				<a-table
					:columns="executeLogColumns"
					:data-source="executeLogList"
					:pagination="executeLogPagination"
					:loading="executeLogLoading"
					size="middle"
					@change="onExecuteLogTableChange"
				>
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'executeResult'">
							<a-tag v-if="record.executeResult === 'SUCCESS'" color="success">
								{{ $TOOL.dictTypeData('EXECUTE_RESULT', record.executeResult) }}
							</a-tag>
							<a-tag v-else color="error">
								{{ $TOOL.dictTypeData('EXECUTE_RESULT', record.executeResult) }}
							</a-tag>
						</template>
						<template v-if="column.dataIndex === 'triggerData'">
							<a-button size="small" @click="showTriggerDataModal(record)">查看</a-button>
						</template>
						<template v-if="column.dataIndex === 'errorMsg'">
							<a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
								<a-typography-text
									type="danger"
									:ellipsis="{ tooltip: true }"
									style="max-width: 200px"
									:content="record.errorMsg"
								/>
							</a-tooltip>
							<span v-else style="color: #999">-</span>
						</template>
					</template>
				</a-table>
			</a-tab-pane>
		</a-tabs>
	</a-drawer>

	<!-- 触发数据详情弹窗 -->
	<a-modal v-model:open="triggerDataModalVisible" title="触发数据详情" width="600px" :footer="null">
		<pre style="max-height: 400px; overflow: auto; background: #f5f5f5; padding: 12px; border-radius: 4px">{{
			currentTriggerData
		}}</pre>
	</a-modal>
</template>

<script setup name="ruleDetail">
	import { ref, computed, nextTick } from 'vue'
	import { Empty } from 'ant-design-vue'
	import tool from '@/utils/tool'
	import iotRuleApi from '@/api/iot/iotRuleApi'
	import iotRuleLogApi from '@/api/iot/iotRuleLogApi'
	import ConditionBuilder from './components/ConditionBuilder.vue'
	import ActionBuilder from './components/ActionBuilder.vue'

	const open = ref(false)
	const activeTab = ref('basic')
	const ruleData = ref({})
	const executeLogList = ref([])
	const executeLogLoading = ref(false)
	const executeLogPagination = ref({
		current: 1,
		pageSize: 10,
		total: 0,
		showSizeChanger: true,
		showQuickJumper: true,
		showTotal: (total) => `共 ${total} 条`
	})

	const triggerDataModalVisible = ref(false)
	const currentTriggerData = ref('')
	const conditionBuilderRef = ref()
	const actionBuilderRef = ref()
	const saveConfigLoading = ref(false)

	const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

	// 解析触发条件
	const triggerCondition = computed(() => {
		if (!ruleData.value.triggerCondition) return null
		try {
			return JSON.parse(ruleData.value.triggerCondition)
		} catch (e) {
			return null
		}
	})

	// 解析执行动作
	const actions = computed(() => {
		if (!ruleData.value.actions) return []
		try {
			return JSON.parse(ruleData.value.actions)
		} catch (e) {
			return []
		}
	})

	// 日志统计
	const logStatistics = computed(() => {
		const total = executeLogList.value.length > 0 ? executeLogPagination.value.total : 0
		const success = executeLogList.value.filter((log) => log.executeResult === 'SUCCESS').length
		const failed = total - success
		return { total, success, failed }
	})

	// 执行日志列表列定义
	const executeLogColumns = [
		{
			title: '执行时间',
			dataIndex: 'executeTime',
			width: 180
		},
		{
			title: '执行结果',
			dataIndex: 'executeResult',
			width: 100
		},
		{
			title: '触发数据',
			dataIndex: 'triggerData',
			width: 100
		},
		{
			title: '错误信息',
			dataIndex: 'errorMsg'
		}
	]

	// 打开详情抽屉
	const onOpen = (record) => {
		open.value = true
		ruleData.value = record
		activeTab.value = 'basic'
		// 加载执行日志
		loadExecuteLog()
	}

	// Tab切换事件
	const onTabChange = (key) => {
		// 切换到规则配置Tab时，加载数据到组件
		if (key === 'config') {
			// 使用nextTick确保组件已渲染
			nextTick(() => {
				if (conditionBuilderRef.value && ruleData.value.triggerCondition) {
					conditionBuilderRef.value.fromJSON(ruleData.value.triggerCondition)
				}
				if (actionBuilderRef.value && ruleData.value.actions) {
					actionBuilderRef.value.fromJSON(ruleData.value.actions)
				}
			})
		}
	}

	// 关闭详情抽屉
	const onClose = () => {
		open.value = false
		ruleData.value = {}
		executeLogList.value = []
		executeLogPagination.value.current = 1
	}

	// 加载执行日志
	const loadExecuteLog = () => {
		executeLogLoading.value = true
		const params = {
			ruleId: ruleData.value.id,
			current: executeLogPagination.value.current,
			size: executeLogPagination.value.pageSize
		}
		iotRuleLogApi
			.iotRuleLogPage(params)
			.then((res) => {
				executeLogList.value = res.records
				executeLogPagination.value.total = res.total
			})
			.finally(() => {
				executeLogLoading.value = false
			})
	}

	// 执行日志表格变化
	const onExecuteLogTableChange = (pagination) => {
		executeLogPagination.value.current = pagination.current
		executeLogPagination.value.pageSize = pagination.pageSize
		loadExecuteLog()
	}

	// 显示触发数据弹窗
	const showTriggerDataModal = (record) => {
		currentTriggerData.value = record.triggerData
			? JSON.stringify(JSON.parse(record.triggerData), null, 2)
			: '无触发数据'
		triggerDataModalVisible.value = true
	}

	// 保存规则配置（触发条件 + 执行动作）
	const saveRuleConfig = () => {
		// 验证触发条件
		const conditionValid = conditionBuilderRef.value.validate()
		if (!conditionValid.valid) {
			return tool.message.warning(conditionValid.message)
		}

		// 验证执行动作
		const actionValid = actionBuilderRef.value.validate()
		if (!actionValid.valid) {
			return tool.message.warning(actionValid.message)
		}

		saveConfigLoading.value = true
		const triggerCondition = conditionBuilderRef.value.toJSON()
		const actions = actionBuilderRef.value.toJSON()

		iotRuleApi
			.iotRuleSubmitForm(
				{
					id: ruleData.value.id,
					ruleName: ruleData.value.ruleName,
					ruleType: ruleData.value.ruleType,
					status: ruleData.value.status,
					triggerCondition,
					actions
				},
				!!ruleData.value.id // 如果id存在则为true，否则为false
			)
			.then((res) => {
				tool.message.success('保存成功')
				ruleData.value.triggerCondition = triggerCondition
				ruleData.value.actions = actions
			})
			.finally(() => {
				saveConfigLoading.value = false
			})
	}

	// 获取动作类型标题
	const getActionTitle = (type) => {
		const typeMap = {
			deviceCommand: '设备指令',
			notification: '消息通知',
			webhook: 'Webhook回调'
		}
		return typeMap[type] || type
	}

	// 获取通知渠道名称
	const getChannelName = (channel) => {
		const channelMap = {
			alert: '自动告警',
			sms: '短信通知',
			email: '邮件通知',
			dingtalk: '钉钉通知'
		}
		return channelMap[channel] || channel
	}

	// 获取通知渠道颜色
	const getChannelColor = (channel) => {
		const colorMap = {
			alert: 'red',
			sms: 'blue',
			email: 'green',
			dingtalk: 'cyan'
		}
		return colorMap[channel] || 'default'
	}

	// 获取告警级别颜色
	const getLevelColor = (level) => {
		const colorMap = {
			CRITICAL: 'red',
			WARNING: 'orange',
			INFO: 'blue'
		}
		return colorMap[level] || 'default'
	}

	// 抛出函数
	defineExpose({
		onOpen
	})
</script>

<style scoped>
	pre {
		white-space: pre-wrap;
		word-wrap: break-word;
	}
</style>
