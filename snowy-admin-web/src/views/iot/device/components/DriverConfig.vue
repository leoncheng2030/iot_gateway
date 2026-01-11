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
			:title="isEditMode ? '编辑设备级配置' : '绑定驱动'"
			:width="800"
			@ok="handleBindOk"
			@cancel="handleBindCancel"
		>
			<a-spin :spinning="templateLoading">
				<a-form :model="bindForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
					<a-form-item label="选择驱动" required>
						<a-select
							v-model:value="bindForm.driverId"
							placeholder="请选择驱动"
							show-search
							:disabled="isEditMode"
							:filter-option="filterDriverOption"
							@change="onDriverChange"
						>
							<a-select-option v-for="driver in availableDrivers" :key="driver.id" :value="driver.id">
								{{ driver.driverName }} ({{ driver.driverType }})
							</a-select-option>
						</a-select>
					</a-form-item>

					<!-- 动态渲染设备级配置字段 -->
					<template v-if="deviceConfigFields.length > 0">
						<a-divider orientation="left">设备级配置</a-divider>
						<a-form-item
							v-for="field in deviceConfigFields"
							:key="field.key"
							:label="field.label"
							:required="field.required"
						>
							<!-- 数字输入 -->
							<a-input-number
								v-if="field.type === 'number'"
								v-model:value="deviceConfigValues[field.key]"
								:placeholder="field.placeholder"
								:min="field.min"
								:max="field.max"
								style="width: 100%"
							/>
							<!-- 文本输入 -->
							<a-input
								v-else-if="field.type === 'text'"
								v-model:value="deviceConfigValues[field.key]"
								:placeholder="field.placeholder"
								allow-clear
							/>
							<!-- 密码输入 -->
							<a-input-password
								v-else-if="field.type === 'password'"
								v-model:value="deviceConfigValues[field.key]"
								:placeholder="field.placeholder"
								allow-clear
							/>
							<!-- 下拉选择 -->
							<a-select
								v-else-if="field.type === 'select'"
								v-model:value="deviceConfigValues[field.key]"
								:placeholder="field.placeholder"
								allow-clear
							>
								<a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
									{{ opt.label }}
								</a-select-option>
							</a-select>
							<!-- 多行文本 -->
							<a-textarea
								v-else-if="field.type === 'textarea'"
								v-model:value="deviceConfigValues[field.key]"
								:placeholder="field.placeholder"
								:auto-size="{ minRows: 3, maxRows: 6 }"
							/>
							<!-- 提示文本 -->
							<div v-if="field.tip" style="color: #999; font-size: 12px; margin-top: 4px">
								{{ field.tip }}
							</div>
						</a-form-item>
					</template>
					<a-alert v-else message="该驱动没有设备级配置字段" type="info" show-icon />
				</a-form>
			</a-spin>
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
	const templateLoading = ref(false)
	const driverList = ref([])
	const availableDrivers = ref([])
	const bindModalVisible = ref(false)
	const isEditMode = ref(false) // 是否编辑模式
	const bindForm = ref({
		driverId: undefined,
		deviceConfig: ''
	})

	// 动态配置字段和值
	const deviceConfigFields = ref([])
	const deviceConfigValues = ref({})

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
		isEditMode.value = false
		bindForm.value = {
			driverId: undefined,
			deviceConfig: ''
		}
		deviceConfigFields.value = []
		deviceConfigValues.value = {}
		bindModalVisible.value = true
		loadAvailableDrivers()
	}

	// 驱动选择过滤
	const filterDriverOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}

	// 驱动选择变化
	const onDriverChange = async (driverId) => {
		if (!driverId) {
			deviceConfigFields.value = []
			deviceConfigValues.value = {}
			return
		}

		// 获取选中驱动的类型
		const selectedDriver = availableDrivers.value.find((d) => d.id === driverId)
		if (!selectedDriver) {
			return
		}

		// 加载设备级配置模板
		await loadDeviceConfigTemplate(selectedDriver.driverType)
	}

	// 加载设备级配置模板
	const loadDeviceConfigTemplate = async (driverType) => {
		templateLoading.value = true
		try {
			const template = await iotDeviceDriverApi.iotDeviceDriverConfigTemplate(driverType)
			// 后端返回 {driverFields: [], deviceFields: []}，设备配置只使用设备级字段
			const fields = template.deviceFields || []
			deviceConfigFields.value = fields

			console.log('加载设备级配置模板成功', driverType, fields)

			// 初始化默认值（如果不是编辑模式）
			if (!isEditMode.value) {
				const defaultValues = {}
				fields.forEach((field) => {
					defaultValues[field.key] = field.defaultValue
				})
				deviceConfigValues.value = defaultValues
				console.log('初始化默认值', defaultValues)
			}
		} catch (e) {
			console.error('加载设备级配置模板失败', e)
			deviceConfigFields.value = []
			if (!isEditMode.value) {
				deviceConfigValues.value = {}
			}
		} finally {
			templateLoading.value = false
		}
	}

	// 绑定驱动确认
	const handleBindOk = async () => {
		if (!bindForm.value.driverId) {
			message.warning('请选择驱动')
			return
		}

		// 验证必填字段
		for (const field of deviceConfigFields.value) {
			if (field.required && !deviceConfigValues.value[field.key]) {
				message.warning(`请填写${field.label}`)
				return
			}
		}

		// 将配置值转为JSON字符串
		const deviceConfig =
			Object.keys(deviceConfigValues.value).length > 0 ? JSON.stringify(deviceConfigValues.value) : ''

		try {
			await iotDeviceDriverRelApi.iotDeviceDriverRelBindDriver({
				deviceId: props.deviceData.id,
				driverId: bindForm.value.driverId,
				deviceConfig: deviceConfig
			})
			message.success(isEditMode.value ? '配置更新成功' : '绑定成功')
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
	const editConfig = async (record) => {
		isEditMode.value = true
		bindForm.value = {
			driverId: record.driverId,
			deviceConfig: record.deviceConfig || ''
		}

		// 加载可用驱动列表（用于显示驱动名称）
		await loadAvailableDrivers()

		// 加载驱动类型
		const driverDetail = await iotDeviceDriverApi.iotDeviceDriverDetail({ id: record.driverId })
		await loadDeviceConfigTemplate(driverDetail.driverType)

		// 解析已有配置值
		if (record.deviceConfig) {
			try {
				const config = JSON.parse(record.deviceConfig)
				deviceConfigValues.value = config
			} catch (e) {
				console.error('解析配置失败', e)
				deviceConfigValues.value = {}
			}
		} else {
			// 初始化默认值
			const defaultValues = {}
			deviceConfigFields.value.forEach((field) => {
				defaultValues[field.key] = field.defaultValue
			})
			deviceConfigValues.value = defaultValues
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
