<template>
	<xn-form-container title="推送日志详情" :width="900" v-model:open="open" :destroy-on-close="true" @close="onClose">
		<a-descriptions :column="2" size="middle" bordered>
			<a-descriptions-item label="配置名称" :span="2">
				{{ formData.configName || '-' }}
			</a-descriptions-item>
			<a-descriptions-item label="设备标识">
				{{ formData.deviceKey || '-' }}
			</a-descriptions-item>
			<a-descriptions-item label="设备名称">
				{{ formData.deviceName || '-' }}
			</a-descriptions-item>
			<a-descriptions-item label="推送类型">
				<a-tag :color="formData.pushType === 'MQTT' ? 'blue' : 'orange'">
					{{ formData.pushType || '-' }}
				</a-tag>
			</a-descriptions-item>
			<a-descriptions-item label="推送状态">
				<a-tag :color="getStatusColor(formData.status)">
					{{ getStatusText(formData.status) }}
				</a-tag>
			</a-descriptions-item>
			<a-descriptions-item label="目标地址" :span="2">
				<a-typography-text copyable code>{{ formData.targetUrl || '-' }}</a-typography-text>
			</a-descriptions-item>
			<a-descriptions-item label="推送时间">
				{{ formData.pushTime || '-' }}
			</a-descriptions-item>
			<a-descriptions-item label="耗时(ms)">
				{{ formData.costTime || '-' }}
			</a-descriptions-item>
			<a-descriptions-item label="响应状态码">
				{{ formData.responseCode || '-' }}
			</a-descriptions-item>
			<a-descriptions-item label="重试次数">
				{{ formData.retryCount || 0 }}
			</a-descriptions-item>
			<a-descriptions-item label="错误信息" :span="2" v-if="formData.errorMessage">
				<span style="color: #ff4d4f">{{ formData.errorMessage }}</span>
			</a-descriptions-item>
		</a-descriptions>

		<a-divider orientation="left">推送数据</a-divider>
		<pre class="json-content">{{ formatJson(formData.payload) }}</pre>

		<a-divider orientation="left" v-if="formData.responseBody">响应内容</a-divider>
		<pre class="json-content" v-if="formData.responseBody">{{ formatJson(formData.responseBody) }}</pre>

		<template #footer>
			<a-button @click="onClose">关闭</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotNorthboundLogForm">
	import { cloneDeep } from 'lodash-es'
	// 抽屉状态
	const open = ref(false)
	// 表单数据
	const formData = ref({})

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		if (record) {
			const recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
		}
	}
	// 关闭抽屉
	const onClose = () => {
		formData.value = {}
		open.value = false
	}

	// 格式化JSON
	const formatJson = (jsonStr) => {
		if (!jsonStr) return '-'
		try {
			const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
			return JSON.stringify(obj, null, 2)
		} catch (e) {
			return jsonStr
		}
	}

	// 获取状态文本
	const getStatusText = (status) => {
		const statusMap = {
			SUCCESS: '成功',
			FAILED: '失败',
			RETRY: '重试'
		}
		return statusMap[status] || status || '-'
	}

	// 获取状态颜色
	const getStatusColor = (status) => {
		const colorMap = {
			SUCCESS: 'success',
			FAILED: 'error',
			RETRY: 'warning'
		}
		return colorMap[status] || 'default'
	}

	// 抛出函数
	defineExpose({
		onOpen
	})
</script>

<style scoped>
	.json-content {
		background: #f5f5f5;
		padding: 16px;
		border-radius: 4px;
		max-height: 400px;
		overflow: auto;
		margin: 0;
		font-size: 13px;
		line-height: 1.5;
	}
</style>
