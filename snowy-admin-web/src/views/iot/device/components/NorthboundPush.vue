<template>
	<div>
		<a-alert
			message="北向推送说明"
			description="北向推送用于将设备数据实时转发到外部系统(如MQTT Broker、HTTP服务等)。配置后,当设备上报数据、触发事件或状态变化时,平台将自动推送到目标地址。"
			type="info"
			show-icon
			style="margin-bottom: 16px"
		/>

		<a-spin :spinning="loading">
			<a-empty v-if="northboundConfigs.length === 0" description="该设备未配置北向推送">
				<template #extra>
					<a-button type="link" @click="goToNorthboundConfig">前往北向配置管理</a-button>
				</template>
			</a-empty>

			<template v-else>
				<a-space direction="vertical" style="width: 100%" :size="16">
					<a-card
						v-for="config in northboundConfigs"
						:key="config.id"
						size="small"
						:title="config.name"
						:bordered="true"
					>
						<template #extra>
							<a-space>
								<a-tag :color="config.enabled === 'ENABLE' ? 'success' : 'default'">
									{{ config.enabled === 'ENABLE' ? '已启用' : '已禁用' }}
								</a-tag>
								<a-button type="link" size="small" @click="goToNorthboundConfigDetail(config.id)"> 编辑配置 </a-button>
							</a-space>
						</template>

						<a-descriptions :column="2" size="small" bordered>
							<a-descriptions-item label="推送类型">
								<a-tag :color="config.pushType === 'MQTT' ? 'blue' : 'orange'">{{ config.pushType }}</a-tag>
							</a-descriptions-item>
							<a-descriptions-item label="QoS等级" v-if="config.pushType === 'MQTT'">
								{{ config.qos }}
							</a-descriptions-item>

							<a-descriptions-item label="目标地址" :span="2">
								<a-typography-text copyable code>{{ config.targetUrl }}</a-typography-text>
							</a-descriptions-item>

							<a-descriptions-item label="Topic模板" :span="2" v-if="config.pushType === 'MQTT'">
								<a-typography-text copyable code>{{ config.targetTopic }}</a-typography-text>
							</a-descriptions-item>

							<a-descriptions-item label="实际Topic" :span="2" v-if="config.pushType === 'MQTT'">
								<a-typography-text copyable code style="color: #52c41a">
									{{ buildActualTopic(config.targetTopic) }}
								</a-typography-text>
							</a-descriptions-item>

							<a-descriptions-item label="触发时机" :span="2">
								<a-space>
									<a-tag v-if="config.pushTrigger && config.pushTrigger.includes('PROPERTY_REPORT')" color="blue">
										属性上报
									</a-tag>
									<a-tag v-if="config.pushTrigger && config.pushTrigger.includes('EVENT')" color="orange">
										事件触发
									</a-tag>
									<a-tag v-if="config.pushTrigger && config.pushTrigger.includes('STATUS_CHANGE')" color="purple">
										状态变化
									</a-tag>
								</a-space>
							</a-descriptions-item>

							<a-descriptions-item label="重试次数">{{ config.retryTimes }}</a-descriptions-item>
							<a-descriptions-item label="超时时间">{{ config.timeout }}ms</a-descriptions-item>

							<a-descriptions-item label="备注" :span="2" v-if="config.remark">
								{{ config.remark }}
							</a-descriptions-item>
						</a-descriptions>
					</a-card>
				</a-space>
			</template>
		</a-spin>
	</div>
</template>

<script setup>
	import { ref, watch } from 'vue'
	import { useRouter } from 'vue-router'
	import iotNorthboundDeviceRelApi from '@/api/iot/iotNorthboundDeviceRelApi'
	import iotNorthboundConfigApi from '@/api/iot/iotNorthboundConfigApi'

	const router = useRouter()

	const props = defineProps({
		deviceData: {
			type: Object,
			default: () => ({})
		}
	})

	const loading = ref(false)
	const northboundConfigs = ref([])

	// 加载北向推送配置
	const loadNorthboundConfigs = async () => {
		if (!props.deviceData.id) return

		loading.value = true
		try {
			// 查询设备关联的推送配置
			const relRes = await iotNorthboundDeviceRelApi.iotNorthboundDeviceRelPage({
				current: 1,
				size: 1000,
				deviceId: props.deviceData.id
			})

			if (relRes.records && relRes.records.length > 0) {
				// 获取配置ID列表
				const configIds = relRes.records.map((item) => item.configId)

				// 查询配置详情
				const configs = []
				for (const configId of configIds) {
					try {
						const config = await iotNorthboundConfigApi.iotNorthboundConfigDetail({ id: configId })
						configs.push(config)
					} catch (e) {
						console.error('加载配置失败:', e)
					}
				}
				northboundConfigs.value = configs
			} else {
				northboundConfigs.value = []
			}
		} catch (error) {
			console.error('加载北向推送配置失败:', error)
			northboundConfigs.value = []
		} finally {
			loading.value = false
		}
	}

	// 构建实际Topic
	const buildActualTopic = (topicTemplate) => {
		if (!topicTemplate) return ''
		return topicTemplate
			.replace('{productId}', props.deviceData.productId || 'PRODUCT_ID')
			.replace('{deviceKey}', props.deviceData.deviceKey || 'DEVICE_KEY')
			.replace('{deviceId}', props.deviceData.id || 'DEVICE_ID')
	}

	// 跳转到北向配置管理
	const goToNorthboundConfig = () => {
		router.push('/iot/northboundconfig')
	}

	// 跳转到北向配置详情
	const goToNorthboundConfigDetail = (configId) => {
		router.push(`/iot/northboundconfig?id=${configId}`)
	}

	// 监听deviceData变化，加载配置
	watch(
		() => props.deviceData.id,
		(newId) => {
			if (newId) {
				loadNorthboundConfigs()
			}
		},
		{ immediate: true }
	)
</script>
