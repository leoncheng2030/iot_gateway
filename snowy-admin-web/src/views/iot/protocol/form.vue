<template>
	<xn-form-container
		:title="formData.id ? '编辑协议配置' : '增加协议配置'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
					<a-form-item label="协议名称：" name="protocolName">
						<a-input v-model:value="formData.protocolName" placeholder="请输入协议名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="协议类型：" name="protocolType">
						<a-select
							v-model:value="formData.protocolType"
							placeholder="请选择协议类型"
							:options="protocolTypeOptions"
							@change="handleProtocolTypeChange"
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="协议端口：" name="protocolPort">
						<a-input v-model:value="formData.protocolPort" placeholder="请输入协议端口" allow-clear />
					</a-form-item>
				</a-col>
				<!-- MQTT协议配置 -->
				<template v-if="formData.protocolType === 'MQTT'">
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="启用WebSocket支持：">
							<a-switch v-model:checked="mqttConfig.enableWebSocket" />
							<span style="margin-left: 8px; color: #999; font-size: 12px">开启后支持ws://协议连接</span>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" v-if="mqttConfig.enableWebSocket">
						<a-form-item label="WebSocket路径：">
							<a-input v-model:value="mqttConfig.wsPath" placeholder="默认/mqtt" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="心跳超时(秒)：">
							<a-input-number
								v-model:value="mqttConfig.keepAlive"
								placeholder="默认60秒"
								:min="10"
								:max="300"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="消息最大长度(字节)：">
							<a-input-number
								v-model:value="mqttConfig.maxMessageSize"
								placeholder="默认8192字节"
								:min="1024"
								:max="65536"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
				</template>

				<!-- WebSocket协议配置 -->
				<template v-if="formData.protocolType === 'WEBSOCKET'">
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="访问路径：">
							<a-input v-model:value="wsConfig.path" placeholder="默认/iot/websocket" allow-clear />
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="心跳超时(秒)：">
							<a-input-number
								v-model:value="wsConfig.heartbeatTimeout"
								placeholder="默认120秒"
								:min="30"
								:max="600"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
				</template>
				<!-- TCP协议配置 -->
				<template v-if="formData.protocolType === 'TCP'">
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="最大帧长度(字节)：">
							<a-input-number
								v-model:value="tcpConfig.maxFrameLength"
								placeholder="默认1024字节"
								:min="512"
								:max="65536"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="读超时(秒)：">
							<a-input-number
								v-model:value="tcpConfig.readTimeout"
								placeholder="默认60秒"
								:min="10"
								:max="300"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="写超时(秒)：">
							<a-input-number
								v-model:value="tcpConfig.writeTimeout"
								placeholder="默认60秒"
								:min="10"
								:max="300"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="空闲超时(秒)：">
							<a-input-number
								v-model:value="tcpConfig.idleTimeout"
								placeholder="默认120秒"
								:min="30"
								:max="600"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
				</template>
				<!-- MODBUS_TCP协议配置 -->
				<template v-if="formData.protocolType === 'MODBUS_TCP'">
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="读超时(秒)：">
							<a-input-number
								v-model:value="modbusConfig.readTimeout"
								placeholder="默认60秒"
								:min="10"
								:max="300"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="写超时(秒)：">
							<a-input-number
								v-model:value="modbusConfig.writeTimeout"
								placeholder="默认60秒"
								:min="10"
								:max="300"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
						<a-form-item label="空闲超时(秒)：">
							<a-input-number
								v-model:value="modbusConfig.idleTimeout"
								placeholder="默认120秒"
								:min="30"
								:max="600"
								style="width: 100%"
							/>
						</a-form-item>
					</a-col>
				</template>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="状态：" name="status">
						<a-select v-model:value="formData.status" placeholder="请选择状态" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
					<a-form-item label="备注：" name="remark">
						<a-textarea v-model:value="formData.remark" placeholder="请输入备注" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="排序码：" name="sortCode">
						<a-input v-model:value="formData.sortCode" placeholder="请输入排序码" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotProtocolForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotProtocolApi from '@/api/iot/iotProtocolApi'
	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const protocolTypeOptions = ref([])
	const statusOptions = ref([])

	// 协议配置
	const mqttConfig = ref({
		keepAlive: 60,
		maxMessageSize: 8192,
		enableWebSocket: false,
		wsPath: '/mqtt'
	})
	const wsConfig = ref({
		path: '/iot/websocket',
		heartbeatTimeout: 120
	})
	const tcpConfig = ref({
		maxFrameLength: 1024,
		readTimeout: 60,
		writeTimeout: 60,
		idleTimeout: 120
	})
	const modbusConfig = ref({
		readTimeout: 60,
		writeTimeout: 60,
		idleTimeout: 120
	})

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
			// 解析配置JSON
			if (recordData.configJson) {
				try {
					const config = JSON.parse(recordData.configJson)
					if (recordData.protocolType === 'MQTT') {
						mqttConfig.value = Object.assign(
							{},
							{
								keepAlive: 60,
								maxMessageSize: 8192,
								enableWebSocket: false,
								wsPath: '/mqtt'
							},
							config
						)
					} else if (recordData.protocolType === 'WEBSOCKET') {
						wsConfig.value = Object.assign({}, { path: '/iot/websocket', heartbeatTimeout: 120 }, config)
					} else if (recordData.protocolType === 'TCP') {
						tcpConfig.value = Object.assign(
							{},
							{ maxFrameLength: 1024, readTimeout: 60, writeTimeout: 60, idleTimeout: 120 },
							config
						)
					} else if (recordData.protocolType === 'MODBUS_TCP') {
						modbusConfig.value = Object.assign({}, { readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }, config)
					}
				} catch (e) {
					console.warn('协议配置解析失败', e)
				}
			}
		} else {
			// 重置配置
			mqttConfig.value = { keepAlive: 60, maxMessageSize: 8192, enableWebSocket: false, wsPath: '/mqtt' }
			wsConfig.value = { path: '/iot/websocket', heartbeatTimeout: 120 }
			tcpConfig.value = { maxFrameLength: 1024, readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }
			modbusConfig.value = { readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }
		}
		protocolTypeOptions.value = tool.dictList('PROTOCOL_TYPE')
		statusOptions.value = tool.dictList('COMMON_STATUS')
	}

	// 协议类型变更
	const handleProtocolTypeChange = () => {
		// 切换协议类型时重置配置
		mqttConfig.value = { keepAlive: 60, maxMessageSize: 8192, enableWebSocket: false, wsPath: '/mqtt' }
		wsConfig.value = { path: '/iot/websocket', heartbeatTimeout: 120 }
		tcpConfig.value = { maxFrameLength: 1024, readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }
		modbusConfig.value = { readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }
	}
	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		mqttConfig.value = { keepAlive: 60, maxMessageSize: 8192, enableWebSocket: false, wsPath: '/mqtt' }
		wsConfig.value = { path: '/iot/websocket', heartbeatTimeout: 120 }
		tcpConfig.value = { maxFrameLength: 1024, readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }
		modbusConfig.value = { readTimeout: 60, writeTimeout: 60, idleTimeout: 120 }
		open.value = false
	}
	// 默认要校验的
	const formRules = {
		protocolName: [required('请输入协议名称')],
		protocolType: [required('请输入协议类型')],
		protocolPort: [required('请输入协议端口')],
		status: [required('请输入状态')]
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)

				// 根据协议类型生成配置JSON
				if (formDataParam.protocolType === 'MQTT') {
					formDataParam.configJson = JSON.stringify(mqttConfig.value)
				} else if (formDataParam.protocolType === 'WEBSOCKET') {
					formDataParam.configJson = JSON.stringify(wsConfig.value)
				} else if (formDataParam.protocolType === 'TCP') {
					formDataParam.configJson = JSON.stringify(tcpConfig.value)
				} else if (formDataParam.protocolType === 'MODBUS_TCP') {
					formDataParam.configJson = JSON.stringify(modbusConfig.value)
				} else {
					formDataParam.configJson = '{}'
				}

				iotProtocolApi
					.iotProtocolSubmitForm(formDataParam, formDataParam.id)
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
