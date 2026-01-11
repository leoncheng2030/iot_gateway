<template>
	<div>
		<!-- 动态协议连接信息 -->
		<a-spin :spinning="driverLoading">
			<a-descriptions bordered :column="1">
				<a-descriptions-item label="协议类型">
					<a-tag :color="getProtocolColor(props.deviceData.protocolType)">
						{{ getProtocolName(props.deviceData.protocolType) }}
					</a-tag>
				</a-descriptions-item>

				<!-- 根据协议类型动态显示连接信息 -->
				<template v-if="connectionFields.length > 0">
					<a-descriptions-item v-for="field in connectionFields" :key="field.key" :label="field.label">
						<a-typography-text v-if="field.copyable" copyable>{{ field.value }}</a-typography-text>
						<a-tag v-else-if="field.tag" :color="field.tagColor || 'blue'">{{ field.value }}</a-tag>
						<a-badge v-else-if="field.badge" :status="field.badgeStatus" :text="field.value" />
						<span v-else>{{ field.value }}</span>
					</a-descriptions-item>
				</template>
				<template v-else>
					<a-descriptions-item label="连接信息">
						<a-empty description="该协议暂无连接信息" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
					</a-descriptions-item>
				</template>
			</a-descriptions>

			<!-- MQTT Topic说明 -->
			<template v-if="isMqttProtocol">
				<a-divider orientation="left">Topic说明</a-divider>
				<a-table :columns="topicColumns" :data-source="topicList" :pagination="false" size="small">
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'topic'">
							<a-typography-text copyable code>{{ record.topic }}</a-typography-text>
						</template>
					</template>
				</a-table>
			</template>

			<!-- Modbus轮询配置 -->
			<template v-if="isModbusProtocol">
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
		</a-spin>
	</div>
</template>

<script setup>
	import { computed, ref, watch } from 'vue'
	import { Empty } from 'ant-design-vue'
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

	// 协议类型显示名称映射
	const protocolNameMap = {
		S7: 'S7协议',
		TCP: 'S7协议',
		MODBUS_TCP: 'Modbus TCP',
		MODBUS_RTU: 'Modbus RTU',
		MQTT: 'MQTT',
		HTTP: 'HTTP',
		OPCUA: 'OPC UA'
	}

	// 协议类型颜色映射
	const protocolColorMap = {
		S7: 'purple',
		TCP: 'purple',
		MODBUS_TCP: 'green',
		MODBUS_RTU: 'green',
		MQTT: 'blue',
		HTTP: 'orange',
		OPCUA: 'cyan'
	}

	// 获取协议显示名称
	const getProtocolName = (protocolType) => {
		return protocolNameMap[protocolType] || protocolType || '未知协议'
	}

	// 获取协议颜色
	const getProtocolColor = (protocolType) => {
		return protocolColorMap[protocolType] || 'default'
	}

	// 解析配置（兼容JSON字符串和对象）
	const parseConfig = (config) => {
		if (!config) return {}
		try {
			return typeof config === 'string' ? JSON.parse(config) : config
		} catch (e) {
			console.error('解析配置失败:', e)
			return {}
		}
	}

	// 动态连接字段（根据协议类型生成）
	const connectionFields = computed(() => {
		const protocol = props.deviceData.protocolType?.toUpperCase()
		const config = parseConfig(deviceDriverConfig.value)
		const fields = []

		// S7协议
		if (protocol === 'S7' || protocol === 'TCP') {
			// 优先使用 host，其次是 ip
			const hostValue = config.host || config.ip || '-'
			fields.push(
				{ key: 'host', label: 'PLC地址', value: hostValue, copyable: true },
				{ key: 'port', label: '端口', value: config.port || 102, tag: true, tagColor: 'blue' },
				{ key: 'rack', label: '机架号(Rack)', value: config.rack ?? 0, tag: true, tagColor: 'orange' },
				{ key: 'slot', label: '插槽号(Slot)', value: config.slot ?? 1, tag: true, tagColor: 'orange' },
				{ key: 'plcType', label: 'PLC类型', value: config.plcType || 'S7-200', tag: true, tagColor: 'purple' },
				{
					key: 'status',
					label: '连接状态',
					value: props.deviceData.deviceStatus === 'ONLINE' ? '已连接' : '未连接',
					badge: true,
					badgeStatus: props.deviceData.deviceStatus === 'ONLINE' ? 'success' : 'default'
				}
			)
		}
		// Modbus TCP
		else if (protocol === 'MODBUS_TCP') {
			// 优先使用 host，其次是 ip
			const hostValue = config.host || config.ip || '-'
			fields.push(
				{ key: 'host', label: '设备主机地址', value: hostValue, copyable: true },
				{ key: 'port', label: '通信端口', value: config.port || 502, tag: true, tagColor: 'blue' },
				{
					key: 'slaveAddress',
					label: 'Modbus从站地址',
					value: config.slaveAddress || config.slaveId || '-',
					tag: true,
					tagColor: 'orange'
				},
				{
					key: 'status',
					label: '连接状态',
					value: props.deviceData.deviceStatus === 'ONLINE' ? '已连接' : '未连接',
					badge: true,
					badgeStatus: props.deviceData.deviceStatus === 'ONLINE' ? 'success' : 'default'
				}
			)
		}
		// MQTT
		else if (protocol === 'MQTT') {
			fields.push(
				{ key: 'broker', label: 'MQTT Broker地址', value: 'tcp://localhost:1883', copyable: true },
				{ key: 'clientId', label: 'Client ID', value: props.deviceData.deviceKey, copyable: true },
				{ key: 'username', label: '用户名', value: props.deviceData.deviceKey, copyable: true },
				{ key: 'password', label: '密码', value: props.deviceData.deviceSecret, copyable: true }
			)
		}

		return fields
	})

	// 判断是否为MQTT协议
	const isMqttProtocol = computed(() => {
		return props.deviceData.protocolType?.toUpperCase() === 'MQTT'
	})

	// 判断是否为Modbus协议
	const isModbusProtocol = computed(() => {
		const protocol = props.deviceData.protocolType?.toUpperCase()
		return protocol === 'MODBUS_TCP' || protocol === 'MODBUS_RTU'
	})

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
