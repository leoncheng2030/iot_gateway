<template>
	<a-modal
		v-model:open="visible"
		:title="isBatch ? '批量配置北向推送' : '配置北向推送'"
		:width="700"
		@ok="handleOk"
		@cancel="handleCancel"
	>
		<a-spin :spinning="loading">
			<a-form ref="formRef" :model="formData" layout="vertical">
				<a-alert
					v-if="isBatch"
					:message="`已选择 ${deviceIds.length} 个设备`"
					type="info"
					show-icon
					style="margin-bottom: 16px"
				/>
				<a-alert v-else :message="`设备: ${deviceName}`" type="info" show-icon style="margin-bottom: 16px" />

				<a-form-item label="选择推送配置">
					<a-transfer
						v-model:target-keys="targetKeys"
						:data-source="dataSource"
						:titles="['可用配置', '已绑定配置']"
						:render="(item) => item.title"
						:list-style="{
							width: '300px',
							height: '400px'
						}"
						show-search
						:filter-option="filterOption"
					/>
				</a-form-item>

				<a-alert
					message="说明"
					description="将设备绑定到推送配置后，该设备的数据将自动推送到配置的目标系统。可以同时绑定多个推送配置。"
					type="info"
					show-icon
				/>
			</a-form>
		</a-spin>
	</a-modal>
</template>

<script setup name="NorthboundConfigModal">
	import { ref, reactive } from 'vue'
	import { message } from 'ant-design-vue'
	import iotNorthboundConfigApi from '@/api/iot/iotNorthboundConfigApi'
	import iotNorthboundDeviceRelApi from '@/api/iot/iotNorthboundDeviceRelApi'

	const visible = ref(false)
	const loading = ref(false)
	const formRef = ref()
	const isBatch = ref(false)
	const deviceIds = ref([])
	const deviceName = ref('')

	const formData = reactive({})
	const dataSource = ref([])
	const targetKeys = ref([])

	const emit = defineEmits(['success'])

	// 打开弹窗
	const onOpen = async (records) => {
		visible.value = true
		loading.value = true

		if (Array.isArray(records)) {
			// 批量配置
			isBatch.value = true
			deviceIds.value = records.map((r) => r.id)
		} else {
			// 单个设备配置
			isBatch.value = false
			deviceIds.value = [records.id]
			deviceName.value = records.deviceName
		}

		try {
			// 加载所有推送配置
			await loadConfigs()
			// 加载已绑定的配置
			await loadBoundConfigs()
		} finally {
			loading.value = false
		}
	}

	// 加载所有推送配置
	const loadConfigs = async () => {
		try {
			const res = await iotNorthboundConfigApi.iotNorthboundConfigPage({
				current: 1,
				size: 1000,
				enabled: 'ENABLED'
			})
			dataSource.value = (res.records || []).map((item) => ({
				key: item.id,
				title: `${item.name} (${item.pushType})`,
				description: item.remark || ''
			}))
		} catch (e) {
			message.error('加载推送配置失败: ' + e.message)
		}
	}

	// 加载已绑定的配置
	const loadBoundConfigs = async () => {
		try {
			if (isBatch.value) {
				// 批量时不加载已绑定配置，需要用户手动选择
				targetKeys.value = []
			} else {
				// 单个设备时加载已绑定的配置
				const res = await iotNorthboundDeviceRelApi.iotNorthboundDeviceRelPage({
					current: 1,
					size: 1000,
					deviceId: deviceIds.value[0]
				})
				targetKeys.value = (res.records || []).map((item) => item.configId)
			}
		} catch (e) {
			message.error('加载已绑定配置失败: ' + e.message)
		}
	}

	// 过滤选项
	const filterOption = (inputValue, item) => {
		return item.title.toLowerCase().includes(inputValue.toLowerCase())
	}

	// 确定
	const handleOk = async () => {
		loading.value = true
		try {
			// 构建关联数据
			const relations = []
			deviceIds.value.forEach((deviceId) => {
				targetKeys.value.forEach((configId) => {
					relations.push({
						deviceId: deviceId,
						configId: configId
					})
				})
			})

			// 提交绑定
			await iotNorthboundDeviceRelApi.iotNorthboundDeviceRelBind({
				deviceIds: deviceIds.value,
				configIds: targetKeys.value
			})

			message.success('配置成功')
			visible.value = false
			emit('success')
		} catch (e) {
			message.error('配置失败: ' + e.message)
		} finally {
			loading.value = false
		}
	}

	// 取消
	const handleCancel = () => {
		visible.value = false
		targetKeys.value = []
		deviceIds.value = []
		deviceName.value = ''
	}

	defineExpose({
		onOpen
	})
</script>

<style scoped>
	:deep(.ant-transfer-list) {
		flex: 1;
	}
</style>
