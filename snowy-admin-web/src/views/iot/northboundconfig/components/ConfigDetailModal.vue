<template>
	<a-modal v-model:open="visible" title="配置详情" width="800px" :footer="null" @cancel="handleClose">
		<a-descriptions bordered :column="2" size="small">
			<a-descriptions-item label="配置名称" :span="2">
				{{ config.name }}
			</a-descriptions-item>
			<a-descriptions-item label="推送类型">
				{{ $TOOL.dictTypeData('NORTHBOUND_PUSH_TYPE', config.pushType) }}
			</a-descriptions-item>
			<a-descriptions-item label="认证方式">
				{{ $TOOL.dictTypeData('NORTHBOUND_AUTH_TYPE', config.authType) }}
			</a-descriptions-item>
			<a-descriptions-item label="目标地址" :span="2">
				{{ config.targetUrl }}
			</a-descriptions-item>
			<a-descriptions-item label="目标Topic" :span="2" v-if="config.targetTopic">
				{{ config.targetTopic }}
			</a-descriptions-item>
			<a-descriptions-item label="QoS等级" v-if="config.pushType === 'MQTT'">
				{{ $TOOL.dictTypeData('MQTT_QOS_LEVEL', config.qos) }}
			</a-descriptions-item>
			<a-descriptions-item label="认证用户名" v-if="config.authUsername">
				{{ config.authUsername }}
			</a-descriptions-item>
			<a-descriptions-item label="认证密码" v-if="config.authPassword"> ****** (已加密) </a-descriptions-item>
			<a-descriptions-item label="认证Token" :span="2" v-if="config.authToken">
				<a-typography-paragraph :ellipsis="{ rows: 2, expandable: true }" style="margin: 0">
					{{ config.authToken }}
				</a-typography-paragraph>
			</a-descriptions-item>
			<a-descriptions-item label="自定义请求头" :span="2" v-if="config.customHeaders">
				<pre style="margin: 0; font-size: 12px; max-height: 150px; overflow: auto">{{
					formatJson(config.customHeaders)
				}}</pre>
			</a-descriptions-item>
			<a-descriptions-item label="数据过滤" :span="2" v-if="config.dataFilter">
				<pre style="margin: 0; font-size: 12px; max-height: 150px; overflow: auto">{{
					formatJson(config.dataFilter)
				}}</pre>
			</a-descriptions-item>
			<a-descriptions-item label="数据转换" :span="2" v-if="config.dataTransform">
				<pre style="margin: 0; font-size: 12px; max-height: 150px; overflow: auto">{{
					formatJson(config.dataTransform)
				}}</pre>
			</a-descriptions-item>
			<a-descriptions-item label="备注" :span="2" v-if="config.remark">
				{{ config.remark }}
			</a-descriptions-item>
		</a-descriptions>
	</a-modal>
</template>

<script setup>
	const visible = defineModel('open', { type: Boolean, default: false })
	const props = defineProps({
		config: {
			type: Object,
			default: () => ({})
		}
	})

	const formatJson = (jsonStr) => {
		if (!jsonStr) return ''
		try {
			const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
			return JSON.stringify(obj, null, 2)
		} catch {
			return jsonStr
		}
	}

	const handleClose = () => {
		visible.value = false
	}
</script>
