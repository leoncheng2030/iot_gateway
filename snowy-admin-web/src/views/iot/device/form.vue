<template>
	<xn-form-container
		:title="formData.id ? '编辑设备' : '增加设备'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="设备名称：" name="deviceName">
						<a-input v-model:value="formData.deviceName" placeholder="请输入设备名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="设备标识：" name="deviceKey">
						<a-input v-model:value="formData.deviceKey" placeholder="请输入设备标识" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="设备密钥：" name="deviceSecret">
						<a-input v-model:value="formData.deviceSecret" placeholder="请输入设备密钥" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="产品：" name="productId">
						<a-select v-model:value="formData.productId" placeholder="请选择产品" allow-clear @change="onProductChange">
							<a-select-option v-for="product in productList" :key="product.id" :value="product.id">
								{{ product.productName }} ({{ getProductTypeText(product.productType) }})
							</a-select-option>
						</a-select>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" v-if="showGatewaySelect">
					<a-form-item label="网关设备：" name="gatewayId">
						<a-select v-model:value="formData.gatewayId" placeholder="请选择网关设备" allow-clear>
							<a-select-option v-for="gateway in gatewayList" :key="gateway.id" :value="gateway.id">
								{{ gateway.deviceName }} ({{ gateway.deviceKey }})
							</a-select-option>
						</a-select>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="设备状态：" name="deviceStatus">
						<a-select
							v-model:value="formData.deviceStatus"
							placeholder="请选择设备状态"
							:options="deviceStatusOptions"
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="设备分组：" name="groupIds">
						<a-tree-select
							v-model:value="formData.groupIds"
							:dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
							placeholder="请选择设备分组（可选）"
							allow-clear
							multiple
							:tree-data="groupTreeData"
							:field-names="{
								children: 'children',
								label: 'name',
								value: 'id'
							}"
							tree-checkable
							:show-checked-strategy="'SHOW_PARENT'"
							tree-line
						/>
					</a-form-item>
				</a-col>
				<!-- 以下字段仅编辑时显示 -->
				<a-col v-if="formData.id" :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="激活时间：" name="activeTime">
						<a-date-picker
							v-model:value="formData.activeTime"
							value-format="YYYY-MM-DD HH:mm:ss"
							show-time
							placeholder="请选择激活时间"
							style="width: 100%"
							disabled
						/>
					</a-form-item>
				</a-col>
				<a-col v-if="formData.id" :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="最后在线时间：" name="lastOnlineTime">
						<a-date-picker
							v-model:value="formData.lastOnlineTime"
							value-format="YYYY-MM-DD HH:mm:ss"
							show-time
							placeholder="请选择最后在线时间"
							style="width: 100%"
							disabled
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="排序码：" name="sortCode">
						<a-input v-model:value="formData.sortCode" placeholder="请输入排序码" allow-clear />
					</a-form-item>
				</a-col>
			</a-row>

			<!-- 驱动配置 -->
			<a-divider orientation="left">驱动配置</a-divider>
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="选择驱动：" name="driverId">
						<a-select
							v-model:value="formData.driverId"
							placeholder="请选择驱动（可选）"
							allow-clear
							show-search
							:filter-option="filterDriverOption"
							@change="onDriverChange"
						>
							<a-select-option v-for="driver in driverList" :key="driver.id" :value="driver.id">
								{{ driver.driverName }} ({{ driver.driverType }})
							</a-select-option>
						</a-select>
						<div v-if="formData.driverId" style="color: #999; font-size: 12px; margin-top: 4px">
							轮询间隔: {{ driverDefaults.pollingInterval }}秒, 超时时间: {{ driverDefaults.timeout }}秒
						</div>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24" v-if="formData.driverId">
					<a-form-item label="设备级配置：">
						<!-- 动态根据驱动configJson生成表单 -->
						<template v-if="deviceConfigFields.length > 0">
							<!-- 必填字段 -->
							<div v-if="requiredFields.length > 0">
								<div style="font-weight: 500; margin-bottom: 8px">连接信息（必填）</div>
								<a-row :gutter="16">
									<a-col v-for="field in requiredFields" :key="field.key" :span="field.span || 12">
										<a-form-item :label="field.label" :required="true">
											<a-input-number
												v-if="field.type === 'number'"
												v-model:value="deviceConfigValues[field.key]"
												:placeholder="field.placeholder"
												style="width: 100%"
											/>
											<a-input
												v-else-if="field.type === 'text'"
												v-model:value="deviceConfigValues[field.key]"
												:placeholder="field.placeholder"
											/>
											<a-input-password
												v-else-if="field.type === 'password'"
												v-model:value="deviceConfigValues[field.key]"
												:placeholder="field.placeholder"
											/>
											<a-switch v-else-if="field.type === 'boolean'" v-model:checked="deviceConfigValues[field.key]" />
											<div v-if="field.tip" style="color: #999; font-size: 12px; margin-top: 4px">{{ field.tip }}</div>
										</a-form-item>
									</a-col>
								</a-row>
							</div>
							<!-- 可选字段 -->
							<div v-if="optionalFields.length > 0" style="margin-top: 16px">
								<div style="font-weight: 500; margin-bottom: 8px">可选配置（留空使用驱动默认值）</div>
								<a-row :gutter="16">
									<a-col v-for="field in optionalFields" :key="field.key" :span="field.span || 12">
										<a-form-item :label="field.label">
											<a-input-number
												v-if="field.type === 'number'"
												v-model:value="deviceConfigValues[field.key]"
												:placeholder="field.placeholder"
												style="width: 100%"
												allow-clear
											/>
											<a-input
												v-else-if="field.type === 'text'"
												v-model:value="deviceConfigValues[field.key]"
												:placeholder="field.placeholder"
												allow-clear
											/>
											<a-input-password
												v-else-if="field.type === 'password'"
												v-model:value="deviceConfigValues[field.key]"
												:placeholder="field.placeholder"
												allow-clear
											/>
											<a-switch v-else-if="field.type === 'boolean'" v-model:checked="deviceConfigValues[field.key]" />
											<div v-if="field.tip" style="color: #999; font-size: 12px; margin-top: 4px">{{ field.tip }}</div>
										</a-form-item>
									</a-col>
								</a-row>
							</div>
						</template>
						<a-alert v-else message="当前驱动无可覆盖配置" type="info" show-icon />
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

<script setup name="iotDeviceForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import { message } from 'ant-design-vue'
	import iotDeviceApi from '@/api/iot/iotDeviceApi'
	import iotProductApi from '@/api/iot/iotProductApi'
	import iotDeviceDriverApi from '@/api/iot/iotDeviceDriverApi'
	import iotDeviceDriverRelApi from '@/api/iot/iotDeviceDriverRelApi'
	import iotDeviceGroupApi from '@/api/iot/iotDeviceGroupApi'
	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const deviceStatusOptions = ref([])
	const productList = ref([])
	const gatewayList = ref([])
	const driverList = ref([])
	const groupTreeData = ref([])
	const selectedProduct = ref(null)
	const showGatewaySelect = ref(false)
	// 驱动默认配置
	const driverDefaults = ref({
		pollingInterval: 5,
		timeout: 3
	})
	// 动态表单字段配置（设备级）
	const deviceConfigFields = ref([])
	// 设备级配置值
	const deviceConfigValues = ref({})

	// 计算属性：必填字段
	const requiredFields = computed(() => {
		return deviceConfigFields.value.filter((field) => field.required === true)
	})

	// 计算属性：可选字段
	const optionalFields = computed(() => {
		return deviceConfigFields.value.filter((field) => field.required !== true)
	})

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		deviceStatusOptions.value = tool.dictList('DEVICE_STATUS')
		// 先加载产品列表，然后再设置表单数据
		loadProductList().then(() => {
			if (record) {
				let recordData = cloneDeep(record)
				formData.value = Object.assign({}, recordData)
				// 编辑时检查是否显示网关选择
				if (formData.value.productId) {
					checkShowGatewaySelect(formData.value.productId)
				}
				// 编辑时加载已绑定的驱动
				if (formData.value.id) {
					loadDeviceDriver(formData.value.id)
					loadDeviceGroups(formData.value.id)
				}
			} else {
				// 新增设备时，默认状态为INACTIVE（未激活）
				formData.value.deviceStatus = 'INACTIVE'
				formData.value.groupIds = []
			}
		})
		// 加载网关设备列表
		loadGatewayList()
		// 加载驱动列表
		loadDriverList().then(() => {
			// 驱动列表加载完成后，如果已选择驱动，解析配置
			if (formData.value.driverId) {
				onDriverChange(formData.value.driverId)
			}
		})
		// 加载分组树
		loadGroupTree()
	}
	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		open.value = false
	}
	// 默认要校验的
	const formRules = {
		deviceName: [required('请输入设备名称')],
		deviceKey: [required('请输入设备标识')],
		deviceSecret: [required('请输入设备密钥')],
		productId: [required('请选择产品')],
		deviceStatus: [required('请选择设备状态')]
	}
	// 加载产品列表
	const loadProductList = () => {
		return iotProductApi.iotProductPage({ current: 1, size: 1000 }).then((data) => {
			productList.value = data.records || []
		})
	}
	// 加载网关设备列表
	const loadGatewayList = () => {
		// 查询所有网关设备（可以根据产品类型筛选）
		iotDeviceApi.iotDevicePage({ current: 1, size: 1000 }).then((data) => {
			// 筛选出网关设备（根据产品的nodeType判断）
			gatewayList.value = (data.records || []).filter((device) => {
				// 这里需要根据实际情况判断，暂时返回所有设备
				return true
			})
		})
	}
	// 加载驱动列表
	const loadDriverList = () => {
		return iotDeviceDriverApi.iotDeviceDriverPage({ current: 1, size: 100 }).then((data) => {
			driverList.value = data.records || []
		})
	}
	// 加载分组树
	const loadGroupTree = () => {
		iotDeviceGroupApi.iotDeviceGroupTree().then((res) => {
			groupTreeData.value = res || []
		})
	}
	// 加载设备已关联的分组
	const loadDeviceGroups = (deviceId) => {
		if (!deviceId) {
			formData.value.groupIds = []
			return
		}
		// 查询设备所属的所有分组
		iotDeviceGroupApi.getGroupIdsByDeviceId({ deviceId }).then((groupIds) => {
			formData.value.groupIds = groupIds || []
		})
	}
	// 加载设备已绑定的驱动
	const loadDeviceDriver = (deviceId) => {
		iotDeviceDriverRelApi.iotDeviceDriverRelListByDeviceId({ deviceId }).then((list) => {
			if (list && list.length > 0) {
				// 只取第一个驱动（如果支持多驱动需要调整）
				const rel = list[0]
				formData.value.driverId = rel.driverId
				formData.value.deviceConfig = rel.deviceConfig || ''

				// 解析deviceConfig并填充表单字段
				if (rel.deviceConfig) {
					try {
						deviceConfigValues.value = JSON.parse(rel.deviceConfig)
					} catch (e) {
						deviceConfigValues.value = {}
					}
				}

				// 加载驱动配置并解析字段
				onDriverChange(rel.driverId)
			}
		})
	}
	// 驱动选择过滤
	const filterDriverOption = (input, option) => {
		return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
	}
	// 驱动变化
	const onDriverChange = async (driverId) => {
		if (!driverId) {
			// 清空时重置
			driverDefaults.value = { pollingInterval: 5, timeout: 3 }
			deviceConfigFields.value = []
			deviceConfigValues.value = {}
			formData.value.deviceConfig = ''
			return
		}

		// 查找选中的驱动
		const driver = driverList.value.find((d) => d.id === driverId)
		if (!driver) {
			console.warn('驱动未找到:', driverId)
			return
		}

		console.log('选中的驱动:', driver.driverName, 'driverType:', driver.driverType, 'configJson:', driver.configJson)

		// 提取驱动默认值
		if (driver.configJson) {
			try {
				const config = JSON.parse(driver.configJson)
				driverDefaults.value = config
			} catch (e) {
				console.error('解析驱动配置失败:', e)
				driverDefaults.value = { pollingInterval: 5, timeout: 3 }
			}
		}

		// 从后端获取驱动配置模板
		try {
			const template = await iotDeviceDriverApi.iotDeviceDriverConfigTemplate(driver.driverType)
			if (template && template.deviceFields && template.deviceFields.length > 0) {
				// 使用设备级字段，并更新placeholder为驱动默认值
				deviceConfigFields.value = template.deviceFields.map((field) => {
					// 对于可选字段，如果驱动有默认值，显示在placeholder中
					if (!field.required && driverDefaults.value[field.key]) {
						return {
							...field,
							placeholder: `留空使用驱动默认(${driverDefaults.value[field.key]})`
						}
					}
					return field
				})
				console.log('生成的设备级字段:', deviceConfigFields.value)

				// 打印驱动级字段（仅用于显示提示）
				if (template.driverFields && template.driverFields.length > 0) {
					console.log('驱动级字段（默认值）:', template.driverFields)
				}
			} else {
				deviceConfigFields.value = []
			}
		} catch (e) {
			console.error('获取驱动配置模板失败:', e)
			deviceConfigFields.value = []
		}
	}

	// 解析驱动配置生成表单字段
	const parseDriverConfigFields = (config) => {
		const fields = []

		// 遍历配置中的所有字段，自动生成表单
		for (const key in config) {
			const value = config[key]
			const valueType = typeof value

			// 根据值类型生成不同的表单控件
			if (valueType === 'number') {
				fields.push({
					key: key,
					label: `${key}:`,
					type: 'number',
					placeholder: `留空使用驱动默认(${value})`,
					defaultValue: value,
					span: 12
				})
			} else if (valueType === 'string') {
				fields.push({
					key: key,
					label: `${key}:`,
					type: 'text',
					placeholder: `留空使用驱动默认(${value})`,
					defaultValue: value,
					span: 12
				})
			} else if (valueType === 'boolean') {
				fields.push({
					key: key,
					label: `${key}:`,
					type: 'boolean',
					defaultValue: value,
					span: 12
				})
			}
			// 忽略对象和数组类型
		}

		return fields
	}
	// 产品变化时
	const onProductChange = (productId) => {
		checkShowGatewaySelect(productId)
	}
	// 检查是否显示网关选择
	const checkShowGatewaySelect = (productId) => {
		if (!productId) {
			showGatewaySelect.value = false
			formData.value.gatewayId = ''
			return
		}
		selectedProduct.value = productList.value.find((p) => p.id === productId)
		// 如果是网关子设备（SUBSET），显示网关选择
		if (selectedProduct.value && selectedProduct.value.productType === 'SUBSET') {
			showGatewaySelect.value = true
		} else {
			showGatewaySelect.value = false
			formData.value.gatewayId = ''
		}
	}
	// 产品类型文本转换
	const getProductTypeText = (productType) => {
		const typeMap = {
			DEVICE: '直连设备',
			GATEWAY: '网关设备',
			SUBSET: '子设备'
		}
		return typeMap[productType] || productType
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				// 构建deviceConfig JSON
				const deviceConfig = {}
				// 只保存非空的设备级配置
				for (const key in deviceConfigValues.value) {
					if (deviceConfigValues.value[key] !== null && deviceConfigValues.value[key] !== undefined) {
						deviceConfig[key] = deviceConfigValues.value[key]
					}
				}
				// 如果有配置则转为JSON字符串
				if (Object.keys(deviceConfig).length > 0) {
					formData.value.deviceConfig = JSON.stringify(deviceConfig)
				} else {
					formData.value.deviceConfig = ''
				}

				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)

				// 保存设备
				iotDeviceApi
					.iotDeviceSubmitForm(formDataParam, formDataParam.id)
					.then((result) => {
						const deviceId = formDataParam.id || result

						// 处理驱动绑定
						const promises = []
						if (formDataParam.driverId) {
							// 绑定驱动（新增或编辑都重新绑定）
							promises.push(
								iotDeviceDriverRelApi.iotDeviceDriverRelBindDriver({
									deviceId: deviceId,
									driverId: formDataParam.driverId,
									deviceConfig: formDataParam.deviceConfig
								})
							)
						}
						// 处理分组关联
						if (formDataParam.groupIds && formDataParam.groupIds.length > 0) {
							// 同步设备分组
							promises.push(
								iotDeviceGroupApi.syncDeviceGroups({
									deviceId: deviceId,
									groupIds: formDataParam.groupIds
								})
							)
						} else if (formDataParam.id) {
							// 编辑时如果清空分组，需要移除所有关联
							promises.push(
								iotDeviceGroupApi.syncDeviceGroups({
									deviceId: deviceId,
									groupIds: []
								})
							)
						}

						if (promises.length > 0) {
							return Promise.all(promises)
								.then(() => {
									onClose()
									emit('successful')
								})
								.catch(() => {
									// 即使绑定失败，设备也已创建成功
									onClose()
									emit('successful')
								})
						} else {
							onClose()
							emit('successful')
						}
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
