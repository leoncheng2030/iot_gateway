<template>
	<div class="driver-config-container">
		<a-spin :spinning="loading">
			<a-empty v-if="driverList.length === 0 && !loading" description="暂无绑定驱动">
				<a-button type="primary" @click="showBindModal">绑定驱动</a-button>
			</a-empty>

			<div v-else>
				<div style="margin-bottom: 16px">
					<a-button type="primary" @click="showBindModal">绑定驱动</a-button>
				</div>

				<a-table :columns="columns" :data-source="driverList" :pagination="false" row-key="id">
					<template #bodyCell="{ column, record }">
						<template v-if="column.dataIndex === 'driverName'">
							<a @click="viewDriverDetail(record)">{{ record.driverName }}</a>
						</template>

						<template v-if="column.dataIndex === 'driverType'">
							<a-tag>{{ record.driverTypeLabel }}</a-tag>
						</template>

						<template v-if="column.dataIndex === 'deviceConfig'">
							<a-tooltip v-if="record.deviceConfig" placement="top">
								<template #title>
									<pre>{{ formatJson(record.deviceConfig) }}</pre>
								</template>
								<a-button type="link" size="small">查看配置</a-button>
							</a-tooltip>
							<span v-else>-</span>
						</template>

						<template v-if="column.dataIndex === 'action'">
							<a-space>
								<a @click="editConfig(record)">配置</a>
								<a-divider type="vertical" />
								<a-popconfirm title="确定要解绑此驱动吗？" @confirm="unbind(record)">
									<a style="color: #ff4d4f">解绑</a>
								</a-popconfirm>
							</a-space>
						</template>
					</template>
				</a-table>
			</div>
		</a-spin>

		<!-- 绑定驱动弹窗 -->
		<a-modal
			v-model:open="bindModalVisible"
			title="绑定驱动"
			:width="800"
			@ok="handleBindOk"
			@cancel="handleBindCancel"
		>
			<a-form :model="bindForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
				<a-form-item label="选择驱动" required>
					<a-select
						v-model:value="bindForm.driverId"
						placeholder="请选择驱动"
						show-search
						:filter-option="filterDriverOption"
						@change="onDriverChange"
					>
						<a-select-option v-for="driver in availableDrivers" :key="driver.id" :value="driver.id">
							{{ driver.driverName }} ({{ driver.driverType }})
						</a-select-option>
					</a-select>
				</a-form-item>

				<a-form-item label="设备级配置" help="可选，覆盖驱动全局配置">
					<a-textarea
						v-model:value="bindForm.deviceConfig"
						:rows="8"
						placeholder='例: {"ip":"192.168.1.100","port":502}'
					/>
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="DriverConfig">
	import { ref, onMounted } from 'vue'
	import { message } from 'ant-design-vue'
	import iotDeviceDriverRelApi from '@/api/iot/iotDeviceDriverRelApi'
	import iotDeviceDriverApi from '@/api/iot/iotDeviceDriverApi'

	const props = defineProps({
		deviceData: {
			type: Object,
			required: true
		}
	})

	const loading = ref(false)
	const driverList = ref([])
	const availableDrivers = ref([])
	const bindModalVisible = ref(false)
	const bindForm = ref({
		driverId: undefined,
		deviceConfig: ''
	})

	const columns = [
		{
			title: '驱动名称',
			dataIndex: 'driverName',
			key: 'driverName',
			width: 200
		},
		{
			title: '驱动类型',
			dataIndex: 'driverType',
			key: 'driverType',
			width: 150
		},
		{
			title: '设备级配置',
			dataIndex: 'deviceConfig',
			key: 'deviceConfig',
			width: 150
		},
		{
			title: '操作',
			dataIndex: 'action',
			key: 'action',
			width: 150,
			fixed: 'right'
		}
	]

	// 加载设备绑定的驱动列表
	const loadDriverList = async () => {
		loading.value = true
		try {
			const res = await iotDeviceDriverRelApi.iotDeviceDriverRelListByDeviceId({
				deviceId: props.deviceData.id
			})
			driverList.value = res || []

			// 加载驱动详情（获取驱动名称等信息）
			for (const rel of driverList.value) {
				const driverDetail = await iotDeviceDriverApi.iotDeviceDriverDetail({
					id: rel.driverId
				})
				rel.driverName = driverDetail.driverName
				rel.driverType = driverDetail.driverType
				rel.driverTypeLabel = driverDetail.driverType
			}
		} catch (error) {
			message.error('加载驱动列表失败')
		} finally {
			loading.value = false
		}
	}

	// 加载可用驱动列表
	const loadAvailableDrivers = async () => {
		try {
			const res = await iotDeviceDriverApi.iotDeviceDriverPage({
				current: 1,
				size: 100
			})
			availableDrivers.value = res.records || []
		} catch (error) {
			message.error('加载可用驱动失败')
		}
	}

	// 显示绑定弹窗
	const showBindModal = () => {
		bindForm.value = {
			driverId: undefined,
			deviceConfig: ''
		}
		bindModalVisible.value = true
		loadAvailableDrivers()
	}

	// 驱动选择过滤
	const filterDriverOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 驱动选择变化
	const onDriverChange = () => {
		// 可以在这里加载选中驱动的默认配置
	}

	// 绑定驱动确认
	const handleBindOk = async () => {
		if (!bindForm.value.driverId) {
			message.warning('请选择驱动')
			return
		}

		// 验证JSON格式
		if (bindForm.value.deviceConfig) {
			try {
				JSON.parse(bindForm.value.deviceConfig)
			} catch (e) {
				message.error('设备级配置必须是有效的JSON格式')
				return
			}
		}

		try {
			await iotDeviceDriverRelApi.iotDeviceDriverRelBindDriver({
				deviceId: props.deviceData.id,
				driverId: bindForm.value.driverId,
				deviceConfig: bindForm.value.deviceConfig
			})
			message.success('绑定成功')
			bindModalVisible.value = false
			loadDriverList()
		} catch (error) {
			// 错误由API统一处理
		}
	}

	// 取消绑定
	const handleBindCancel = () => {
		bindModalVisible.value = false
	}

	// 解绑驱动
	const unbind = async (record) => {
		try {
			await iotDeviceDriverRelApi.iotDeviceDriverRelUnbindDriver({
				deviceId: props.deviceData.id,
				driverId: record.driverId
			})
			message.success('解绑成功')
			loadDriverList()
		} catch (error) {
			// 错误由API统一处理
		}
	}

	// 编辑配置
	const editConfig = (record) => {
		bindForm.value = {
			driverId: record.driverId,
			deviceConfig: record.deviceConfig || ''
		}
		bindModalVisible.value = true
	}

	// 查看驱动详情
	const viewDriverDetail = (record) => {
		message.info('查看驱动详情功能待实现')
	}

	// 格式化JSON
	const formatJson = (jsonStr) => {
		try {
			return JSON.stringify(JSON.parse(jsonStr), null, 2)
		} catch (e) {
			return jsonStr
		}
	}

	onMounted(() => {
		loadDriverList()
	})
</script>

<style scoped lang="less">
	.driver-config-container {
		padding: 16px;
	}
</style>
