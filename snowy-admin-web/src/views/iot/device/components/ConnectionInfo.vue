<template>
	<div>
		<!-- MQTT设备连接信息 -->
		<template v-if="!isModbusDevice">
			<a-descriptions bordered :column="1">
				<a-descriptions-item label="协议类型">
					<a-tag color="blue">MQTT</a-tag>
				</a-descriptions-item>
				<a-descriptions-item label="MQTT Broker地址">
					<a-typography-text copyable>tcp://localhost:1883</a-typography-text>
				</a-descriptions-item>
				<a-descriptions-item label="Client ID">
					<a-typography-text copyable>{{ deviceData.deviceKey }}</a-typography-text>
				</a-descriptions-item>
				<a-descriptions-item label="用户名">
					<a-typography-text copyable>{{ deviceData.deviceKey }}</a-typography-text>
				</a-descriptions-item>
				<a-descriptions-item label="密码">
					<a-typography-text copyable>{{ deviceData.deviceSecret }}</a-typography-text>
				</a-descriptions-item>
			</a-descriptions>

			<a-divider orientation="left">Topic说明</a-divider>
			<a-table :columns="topicColumns" :data-source="topicList" :pagination="false" size="small">
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'topic'">
						<a-typography-text copyable code>{{ record.topic }}</a-typography-text>
					</template>
				</template>
			</a-table>
		</template>

		<!-- Modbus TCP设备连接信息 -->
		<template v-else>
			<a-descriptions bordered :column="1">
				<a-descriptions-item label="协议类型">
					<a-tag color="green">Modbus TCP</a-tag>
				</a-descriptions-item>
				<a-descriptions-item label="设备IP地址">
					<a-typography-text copyable>{{ modbusConfig.ip || '-' }}</a-typography-text>
				</a-descriptions-item>
				<a-descriptions-item label="通信端口">
					<a-tag color="blue">{{ modbusConfig.port || 502 }}</a-tag>
				</a-descriptions-item>
				<a-descriptions-item label="Modbus从站地址">
					<a-tag color="orange">{{ modbusConfig.slaveAddress || '-' }}</a-tag>
				</a-descriptions-item>
				<a-descriptions-item label="连接状态">
					<a-badge
						:status="deviceData.deviceStatus === 'ONLINE' ? 'success' : 'default'"
						:text="deviceData.deviceStatus === 'ONLINE' ? '已连接' : '未连接'"
					/>
				</a-descriptions-item>
			</a-descriptions>

			<a-divider orientation="left">轮询配置</a-divider>
			<a-alert
				message="平台作为Modbus主站，定时主动轮询读取设备寄存器数据"
				type="info"
				show-icon
				style="margin-bottom: 16px"
			/>
			<a-descriptions bordered :column="2" size="small">
				<a-descriptions-item label="轮询间隔">每 5 秒</a-descriptions-item>
				<a-descriptions-item label="超时时间">3 秒</a-descriptions-item>
				<a-descriptions-item label="支持功能码" :span="2">
					0x01(读线圈)、0x03(读保持寄存器)、0x05(写单个线圈)、0x06(写单个寄存器)、0x0F(写多个线圈)、0x10(写多个寄存器)
				</a-descriptions-item>
			</a-descriptions>

			<a-divider orientation="left">使用说明</a-divider>
			<a-steps direction="vertical" size="small" :current="3">
				<a-step title="配置物模型" description="在产品管理中定义设备的属性、事件和服务" />
				<a-step title="配置寄存器映射" description="在'Modbus寄存器映射'Tab中配置寄存器地址与物模型属性的对应关系" />
				<a-step title="启动轮询" description="配置完成后，平台将自动按配置的功能码和地址轮询读取设备数据" />
				<a-step title="实时监控" description="在'实时数据'Tab中查看设备上报的最新数据" />
			</a-steps>
		</template>
	</div>
</template>

<script setup>
	import { computed, ref, watch } from 'vue'
	import iotDeviceDriverRelApi from '@/api/iot/iotDeviceDriverRelApi'
	import iotDeviceDriverApi from '@/api/iot/iotDeviceDriverApi'

	const props = defineProps({
		deviceData: {
			type: Object,
			default: () => ({})
		}
	})

	// 设备驱动类型和配置
	const deviceDriverType = ref(null)
	const deviceDriverConfig = ref(null)
	const driverLoading = ref(false)

	// Modbus配置（从deviceConfig解析）
	const modbusConfig = computed(() => {
		if (!deviceDriverConfig.value) {
			return {}
		}
		try {
			return JSON.parse(deviceDriverConfig.value)
		} catch (e) {
			console.error('解析Modbus配置失败:', e)
			return {}
		}
	})

	// 北向推送配置
	const northboundLoading = ref(false)
	const northboundConfigs = ref([])

	// 加载北向推送配置
	const loadNorthboundConfigs = async () => {
		if (!props.deviceData.id) return

		northboundLoading.value = true
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
			northboundLoading.value = false
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

	// Topic列表
	const topicColumns = [
		{ title: 'Topic', dataIndex: 'topic', width: 400 },
		{ title: '说明', dataIndex: 'description' }
	]

	const topicList = computed(() => {
		const productKey = props.deviceData.productId || 'PRODUCT_KEY'
		const deviceKey = props.deviceData.deviceKey || 'DEVICE_KEY'
		return [
			{
				topic: `/${productKey}/${deviceKey}/property/post`,
				description: '设备属性上报'
			},
			{
				topic: `/${productKey}/${deviceKey}/property/set`,
				description: '设置设备属性'
			},
			{
				topic: `/${productKey}/${deviceKey}/event/post`,
				description: '设备事件上报'
			},
			{
				topic: `/${productKey}/${deviceKey}/service/call`,
				description: '调用设备服务'
			}
		]
	})

	// 加载设备驱动类型
	const loadDeviceDriver = async () => {
		if (!props.deviceData.id) return

		driverLoading.value = true
		try {
			// 查询设备关联的驱动
			const driverRels = await iotDeviceDriverRelApi.iotDeviceDriverRelListByDeviceId({
				deviceId: props.deviceData.id
			})

			if (driverRels && driverRels.length > 0) {
				// 保存设备配置
				deviceDriverConfig.value = driverRels[0].deviceConfig
				// 查询驱动详情
				const driverDetail = await iotDeviceDriverApi.iotDeviceDriverDetail({
					id: driverRels[0].driverId
				})
				deviceDriverType.value = driverDetail.driverType
			} else {
				deviceDriverType.value = null
				deviceDriverConfig.value = null
			}
		} catch (error) {
			console.error('加载设备驱动失败:', error)
			deviceDriverType.value = null
		} finally {
			driverLoading.value = false
		}
	}

	// 判断是否为Modbus设备
	const isModbusDevice = computed(() => {
		return deviceDriverType.value === 'MODBUS_TCP'
	})

	// 监听deviceData变化，加载驱动信息
	watch(
		() => props.deviceData.id,
		(newId) => {
			if (newId) {
				loadDeviceDriver()
			}
		},
		{ immediate: true }
	)
</script>
