<template>
	<xn-form-container
		:title="formData.id ? '编辑北向推送配置' : '新增北向推送配置'"
		:width="1000"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-tabs v-model:activeKey="activeTab">
				<!-- 基础配置 -->
				<a-tab-pane key="basic" tab="基础配置">
					<a-row :gutter="16">
						<a-col :span="12">
							<a-form-item label="配置名称" name="name">
								<a-input v-model:value="formData.name" placeholder="请输入配置名称" allow-clear />
							</a-form-item>
						</a-col>
						<a-col :span="12">
							<a-form-item label="推送类型" name="pushType">
								<a-select
									v-model:value="formData.pushType"
									placeholder="请选择推送类型"
									:options="pushTypeOptions"
									@change="onPushTypeChange"
								/>
							</a-form-item>
						</a-col>

						<!-- Webhook配置 -->
						<a-col :span="24" v-if="formData.pushType === 'WEBHOOK'">
							<a-form-item label="目标地址" name="targetUrl">
								<a-input v-model:value="formData.targetUrl" placeholder="http://example.com/api/data" allow-clear />
							</a-form-item>
						</a-col>

						<!-- MQTT配置 -->
						<template v-if="formData.pushType === 'MQTT'">
							<a-col :span="16">
								<a-form-item label="Broker地址" name="targetUrl">
									<a-input v-model:value="formData.targetUrl" placeholder="示例: 192.168.1.100:1884" allow-clear />
									<div class="ant-form-item-extra" style="color: #ff4d4f; font-weight: 500">
										重要: 北向推送端口不能与设备MQTT服务端口(1883)相同,建议使用1884或其他端口
									</div>
								</a-form-item>
							</a-col>
							<a-col :span="8">
								<a-form-item label="QoS等级" name="qos">
									<a-select v-model:value="formData.qos" placeholder="请选择" :options="qosOptions" />
								</a-form-item>
							</a-col>
							<a-col :span="24">
								<a-form-item label="Topic模板" name="targetTopic">
									<a-input
										v-model:value="formData.targetTopic"
										placeholder="支持变量: {productId} {deviceKey} {deviceId}"
										allow-clear
									/>
									<div class="ant-form-item-extra">示例: northbound/{productId}/{deviceKey}</div>
								</a-form-item>
							</a-col>
						</template>

						<a-col :span="8">
							<a-form-item label="状态" name="enabled">
								<a-select v-model:value="formData.enabled" placeholder="请选择状态" :options="enabledOptions" />
							</a-form-item>
						</a-col>
						<a-col :span="16">
							<a-form-item label="推送触发时机" name="pushTrigger">
								<a-checkbox-group v-model:value="formData.pushTrigger" :options="pushTriggerOptions" />
							</a-form-item>
						</a-col>
						<a-col :span="8">
							<a-form-item label="重试次数" name="retryTimes">
								<a-input-number
									v-model:value="formData.retryTimes"
									placeholder="默认3"
									:min="0"
									:max="10"
									style="width: 100%"
								/>
							</a-form-item>
						</a-col>
						<a-col :span="8">
							<a-form-item label="超时时间(ms)" name="timeout">
								<a-input-number
									v-model:value="formData.timeout"
									placeholder="默认5000"
									:min="1000"
									:max="60000"
									style="width: 100%"
								/>
							</a-form-item>
						</a-col>

						<a-col :span="24" v-if="formData.id">
							<a-button type="primary" :loading="testLoading" @click="testConnection">
								<template #icon><ApiOutlined /></template>
								测试连接
							</a-button>
						</a-col>
					</a-row>
				</a-tab-pane>

				<!-- 认证配置 -->
				<a-tab-pane key="auth" tab="认证配置">
					<a-row :gutter="16">
						<a-col :span="24">
							<a-form-item label="认证方式" name="authType">
								<a-select
									v-model:value="formData.authType"
									placeholder="请选择认证方式"
									:options="authTypeOptions"
									@change="onAuthTypeChange"
								/>
							</a-form-item>
						</a-col>

						<template v-if="formData.authType === 'BASIC'">
							<a-col :span="12">
								<a-form-item label="用户名" name="authUsername">
									<a-input v-model:value="formData.authUsername" placeholder="请输入用户名" allow-clear />
								</a-form-item>
							</a-col>
							<a-col :span="12">
								<a-form-item label="密码" name="authPassword">
									<a-input-password v-model:value="formData.authPassword" placeholder="请输入密码" allow-clear />
								</a-form-item>
							</a-col>
						</template>

						<template v-if="formData.authType === 'TOKEN' || formData.authType === 'APIKEY'">
							<a-col :span="24">
								<a-form-item :label="formData.authType === 'TOKEN' ? 'Token' : 'API Key'" name="authToken">
									<a-textarea v-model:value="formData.authToken" placeholder="请输入" :rows="3" allow-clear />
								</a-form-item>
							</a-col>
						</template>

						<a-col :span="24" v-if="formData.pushType === 'WEBHOOK'">
							<a-form-item label="自定义请求头">
								<a-textarea
									v-model:value="formData.customHeaders"
									placeholder='JSON格式, 例如: {"Content-Type":"application/json"}'
									:rows="4"
									allow-clear
								/>
							</a-form-item>
						</a-col>
					</a-row>
				</a-tab-pane>

				<!-- 数据处理 -->
				<a-tab-pane key="data" tab="数据处理">
					<a-row :gutter="16">
						<a-col :span="24">
							<a-form-item>
								<template #label>
									<span>数据过滤条件</span>
									<a-tooltip title="点击查看示例">
										<QuestionCircleOutlined style="margin-left: 5px; cursor: pointer" @click="showFilterExample" />
									</a-tooltip>
								</template>
								<a-textarea
									v-model:value="formData.dataFilter"
									placeholder="JSON格式的过滤规则"
									:rows="8"
									allow-clear
								/>
							</a-form-item>
						</a-col>
						<a-col :span="24">
							<a-form-item>
								<template #label>
									<span>数据转换规则</span>
									<a-tooltip title="点击查看示例">
										<QuestionCircleOutlined style="margin-left: 5px; cursor: pointer" @click="showTransformExample" />
									</a-tooltip>
								</template>
								<a-textarea
									v-model:value="formData.dataTransform"
									placeholder="JSON格式的转换规则"
									:rows="8"
									allow-clear
								/>
							</a-form-item>
						</a-col>
					</a-row>
				</a-tab-pane>

				<!-- 其他配置 -->
				<a-tab-pane key="other" tab="其他">
					<a-row :gutter="16">
						<a-col :span="12">
							<a-form-item label="排序码" name="sortCode">
								<a-input-number v-model:value="formData.sortCode" placeholder="数字越小越靠前" style="width: 100%" />
							</a-form-item>
						</a-col>
						<a-col :span="24">
							<a-form-item label="备注" name="remark">
								<a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" allow-clear />
							</a-form-item>
						</a-col>
						<a-col :span="24">
							<a-form-item label="扩展信息" name="extJson">
								<a-textarea v-model:value="formData.extJson" placeholder="JSON格式" :rows="4" allow-clear />
							</a-form-item>
						</a-col>
					</a-row>
				</a-tab-pane>
			</a-tabs>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotNorthboundConfigForm">
	import tool from '@/utils/tool'
	import { Modal, message } from 'ant-design-vue'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotNorthboundConfigApi from '@/api/iot/iotNorthboundConfigApi'

	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	const activeTab = ref('basic')

	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const testLoading = ref(false)
	const pushTypeOptions = ref([])
	const authTypeOptions = ref([])
	const enabledOptions = ref([])
	const qosOptions = ref([])
	const pushTriggerOptions = ref([
		{ label: '属性上报', value: 'PROPERTY_REPORT' },
		{ label: '事件触发', value: 'EVENT' },
		{ label: '状态变化', value: 'STATUS_CHANGE' }
	])

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		activeTab.value = 'basic'
		if (record) {
			let recordData = cloneDeep(record)
			// 处理推送触发时机，将字符串转换为数组
			if (recordData.pushTrigger && typeof recordData.pushTrigger === 'string') {
				recordData.pushTrigger = recordData.pushTrigger.split(',')
			} else {
				recordData.pushTrigger = ['PROPERTY_REPORT', 'EVENT', 'STATUS_CHANGE']
			}
			formData.value = Object.assign({}, recordData)
		} else {
			// 新增时，默认全部触发
			formData.value = {
				pushTrigger: ['PROPERTY_REPORT', 'EVENT', 'STATUS_CHANGE']
			}
		}
		pushTypeOptions.value = tool.dictList('NORTHBOUND_PUSH_TYPE')
		authTypeOptions.value = tool.dictList('NORTHBOUND_AUTH_TYPE')
		enabledOptions.value = tool.dictList('NORTHBOUND_PUSH_STATUS')
		qosOptions.value = tool.dictList('MQTT_QOS_LEVEL')
	}

	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		open.value = false
	}

	// 推送类型变化
	const onPushTypeChange = (value) => {
		// 可以在这里添加类型切换逻辑
	}

	// 认证方式变化
	const onAuthTypeChange = (value) => {
		if (value === 'NONE') {
			formData.value.authUsername = ''
			formData.value.authPassword = ''
			formData.value.authToken = ''
		}
	}

	// 测试连接
	const testConnection = async () => {
		if (!formData.value.id) {
			message.warning('请先保存配置后再测试连接')
			return
		}

		testLoading.value = true
		try {
			const result = await iotNorthboundConfigApi.iotNorthboundConfigTestConnection({ id: formData.value.id })
			if (result.success) {
				message.success(result.message || '连接测试成功')
			} else {
				message.error(result.message || '连接测试失败')
			}
		} catch (e) {
			message.error('测试连接异常: ' + e.message)
		} finally {
			testLoading.value = false
		}
	}

	// 显示过滤示例
	const showFilterExample = () => {
		const example = {
			include: ['temperature', 'humidity'],
			exclude: ['deviceStatus'],
			conditions: [
				{ field: 'temperature', operator: '>', value: 25 },
				{ field: 'humidity', operator: '<=', value: 80 }
			]
		}

		Modal.info({
			title: '数据过滤示例',
			width: 600,
			content: `
				<pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; overflow: auto;">${JSON.stringify(example, null, 2)}</pre>
				<div style="margin-top: 12px;">
					<p><strong>说明：</strong></p>
					<ul>
						<li>include: 仅包含指定字段</li>
						<li>exclude: 排除指定字段</li>
						<li>conditions: 条件过滤，支持运算符: =, !=, >, >=, <, <=, contains</li>
					</ul>
				</div>
			`
		})
	}

	// 显示转换示例
	const showTransformExample = () => {
		const example = {
			fieldMapping: {
				temperature: 'temp',
				humidity: 'hum'
			},
			valueMapping: {
				deviceStatus: {
					ONLINE: '在线',
					OFFLINE: '离线'
				}
			},
			calculated: [
				{
					field: 'fahrenheit',
					expression: 'temperature * 1.8 + 32'
				}
			]
		}

		Modal.info({
			title: '数据转换示例',
			width: 600,
			content: `
				<pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; overflow: auto;">${JSON.stringify(example, null, 2)}</pre>
				<div style="margin-top: 12px;">
					<p><strong>说明：</strong></p>
					<ul>
						<li>fieldMapping: 字段名映射</li>
						<li>valueMapping: 字段值映射</li>
						<li>calculated: 计算新字段，支持基本算术运算</li>
					</ul>
				</div>
			`
		})
	}

	// 默认要校验的
	const formRules = {
		name: [required('请输入配置名称')],
		pushType: [required('请选择推送类型')],
		authType: [required('请选择认证方式')],
		enabled: [required('请选择状态')]
	}

	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				// 将pushTrigger数组转换为字符串
				if (Array.isArray(formDataParam.pushTrigger)) {
					formDataParam.pushTrigger = formDataParam.pushTrigger.join(',')
				}
				iotNorthboundConfigApi
					.iotNorthboundConfigSubmitForm(formDataParam, formDataParam.id)
					.then(() => {
						onClose()
						emit('successful')
					})
					.finally(() => {
						submitLoading.value = false
					})
			})
			.catch(() => {})
	}

	// 抛出函数
	defineExpose({
		onOpen
	})
</script>
