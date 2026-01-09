<template>
	<xn-form-container
		:title="formData.id ? '编辑设备驱动配置表' : '增加设备驱动配置表'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="驱动名称：" name="driverName">
						<a-input v-model:value="formData.driverName" placeholder="请输入驱动名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="驱动类型：" name="driverType">
						<a-select
							v-model:value="formData.driverType"
							placeholder="请选择驱动类型"
							:options="driverTypeOptions"
							@change="onDriverTypeChange"
						/>
					</a-form-item>
				</a-col>
				<!-- 动态驱动配置区域 -->
				<template v-if="configFields.length > 0">
					<a-col
						v-for="field in configFields"
						:key="field.key"
						:xs="24"
						:sm="24"
						:md="field.span || 12"
						:lg="field.span || 12"
						:xl="field.span || 12"
					>
						<a-form-item :label="field.label">
							<!-- 数字输入 -->
							<a-input-number
								v-if="field.type === 'number'"
								v-model:value="configValues[field.key]"
								:placeholder="field.placeholder"
								:min="field.min"
								:max="field.max"
								style="width: 100%"
							/>
							<!-- 文本输入 -->
							<a-input
								v-else-if="field.type === 'text'"
								v-model:value="configValues[field.key]"
								:placeholder="field.placeholder"
								allow-clear
							/>
							<!-- 密码输入 -->
							<a-input-password
								v-else-if="field.type === 'password'"
								v-model:value="configValues[field.key]"
								:placeholder="field.placeholder"
								allow-clear
							/>
							<!-- 下拉选择 -->
							<a-select
								v-else-if="field.type === 'select'"
								v-model:value="configValues[field.key]"
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
								v-model:value="configValues[field.key]"
								:placeholder="field.placeholder"
								:auto-size="{ minRows: 3, maxRows: 6 }"
							/>
							<!-- 提示文本 -->
							<div v-if="field.tip" style="color: #999; font-size: 12px; margin-top: 4px">
								{{ field.tip }}
							</div>
						</a-form-item>
					</a-col>
				</template>
				<!-- TCP直连提示 -->
				<a-col v-else-if="formData.driverType === 'TCP_DIRECT'" :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
					<a-alert
						message="TCP直连驱动不需要预先配置,连接设备时再指定主机和端口"
						type="info"
						show-icon
						style="margin-bottom: 16px"
					/>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
					<a-form-item label="驱动描述：" name="description">
						<a-textarea
							v-model:value="formData.description"
							placeholder="请输入驱动描述"
							:auto-size="{ minRows: 3, maxRows: 5 }"
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
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

<script setup name="iotDeviceDriverForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotDeviceDriverApi from '@/api/iot/iotDeviceDriverApi'

	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const driverTypeOptions = ref([])
	const statusOptions = ref([])

	// 动态配置字段和值
	const configFields = ref([])
	const configValues = ref({})

	// 打开抽屉
	const onOpen = async (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)

			// 加载驱动配置模板
			if (recordData.driverType) {
				await loadConfigTemplate(recordData.driverType)
			}

			// 解析已有配置值
			if (recordData.configJson) {
				try {
					const config = JSON.parse(recordData.configJson)
					configValues.value = config
				} catch (e) {
					console.error('解析配置失败', e)
				}
			}
		}
		driverTypeOptions.value = tool.dictList('DEVICE_DRIVER_TYPE')
		statusOptions.value = tool.dictList('DRIVER_STATUS')
	}

	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		configFields.value = []
		configValues.value = {}
		open.value = false
	}

	// 驱动类型变化
	const onDriverTypeChange = async (driverType) => {
		if (!driverType) {
			configFields.value = []
			configValues.value = {}
			return
		}
		await loadConfigTemplate(driverType)
	}

	// 加载驱动配置模板
	const loadConfigTemplate = async (driverType) => {
		try {
			const fields = await iotDeviceDriverApi.iotDeviceDriverConfigTemplate(driverType)
			configFields.value = fields || []

			console.log('加载配置模板成功', driverType, fields)

			// 初始化默认值
			const defaultValues = {}
			fields.forEach((field) => {
				defaultValues[field.key] = field.defaultValue
			})
			configValues.value = defaultValues
			console.log('初始化默认值', defaultValues)
		} catch (e) {
			console.error('加载驱动配置模板失败', e)
			configFields.value = []
			configValues.value = {}
		}
	}
	// 默认要校验的
	const formRules = {
		driverName: [required('请输入驱动名称')],
		driverType: [required('请选择驱动类型')]
	}

	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)

				// 将配置值转为JSON
				formDataParam.configJson = JSON.stringify(configValues.value)

				iotDeviceDriverApi
					.iotDeviceDriverSubmitForm(formDataParam, formDataParam.id)
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
