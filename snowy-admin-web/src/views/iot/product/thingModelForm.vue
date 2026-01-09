<template>
	<a-modal
		:title="formData.id ? '编辑功能' : '添加功能'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@ok="onSubmit"
		@cancel="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="功能类型" name="modelType">
						<a-select
							v-model:value="formData.modelType"
							placeholder="请选择功能类型"
							@change="onModelTypeChange"
							:options="modelTypeOptions"
						/>
					</a-form-item>
				</a-col>
				<!-- 服务类型:显示模板选择 -->
				<a-col :span="12" v-if="formData.modelType === ModelType.SERVICE">
					<a-form-item label="服务模板">
						<a-select
							v-model:value="selectedServiceTemplate"
							placeholder="选择服务模板(可选)"
							@change="onServiceTemplateChange"
							allow-clear
							:options="serviceTemplateOptions"
						/>
					</a-form-item>
				</a-col>
				<!-- 非服务类型或自定义服务:显示标识符输入框 -->
				<a-col :span="12" v-if="formData.modelType !== ModelType.SERVICE || selectedServiceTemplate === 'custom'">
					<a-form-item label="标识符" name="identifier">
						<a-input v-model:value="formData.identifier" placeholder="如: temperature" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="功能名称" name="name">
						<a-input v-model:value="formData.name" placeholder="如: 温度" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12" v-if="formData.modelType !== ModelType.SERVICE">
					<a-form-item label="值类型" name="valueType">
						<a-select v-model:value="formData.valueType" placeholder="请选择值类型" :options="valueTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12" v-if="formData.modelType === ModelType.SERVICE">
					<a-form-item label="调用方式" name="callType">
						<a-select v-model:value="formData.callType" placeholder="请选择调用方式" :options="callTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12" v-if="formData.modelType === ModelType.PROPERTY">
					<a-form-item label="读写类型" name="accessMode">
						<a-select v-model:value="formData.accessMode" placeholder="请选择读写类型" :options="accessModeOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="是否必须" name="required">
						<a-switch v-model:checked="formData.required" />
					</a-form-item>
				</a-col>
				<a-col :span="24" v-if="formData.modelType !== ModelType.SERVICE">
					<a-divider orientation="left">值定义配置</a-divider>

					<!-- 布尔类型配置 -->
					<template v-if="formData.valueType === ValueType.BOOL">
						<a-row :gutter="16">
							<a-col :span="12">
								<a-form-item label="true时显示">
									<a-input v-model:value="valueSpecsData.true" placeholder="如:运行、开、启动" allow-clear />
								</a-form-item>
							</a-col>
							<a-col :span="12">
								<a-form-item label="false时显示">
									<a-input v-model:value="valueSpecsData.false" placeholder="如:停止、关、关闭" allow-clear />
								</a-form-item>
							</a-col>
						</a-row>
					</template>

					<!-- 数值类型配置 -->
					<template v-else-if="isNumericType(formData.valueType)">
						<a-row :gutter="16">
							<a-col :span="6">
								<a-form-item label="最小值">
									<a-input-number v-model:value="valueSpecsData.min" placeholder="-100" style="width: 100%" />
								</a-form-item>
							</a-col>
							<a-col :span="6">
								<a-form-item label="最大值">
									<a-input-number v-model:value="valueSpecsData.max" placeholder="100" style="width: 100%" />
								</a-form-item>
							</a-col>
							<a-col :span="6">
								<a-form-item label="步长">
									<a-input-number v-model:value="valueSpecsData.step" placeholder="1" :step="0.1" style="width: 100%" />
								</a-form-item>
							</a-col>
							<a-col :span="6">
								<a-form-item label="单位">
									<a-input v-model:value="valueSpecsData.unit" placeholder="如:℃、%、V" allow-clear />
								</a-form-item>
							</a-col>
						</a-row>
					</template>

					<!-- 其他类型使用JSON配置 -->
					<template v-else>
						<a-form-item label="值定义(JSON格式)" name="valueSpecs">
							<a-textarea v-model:value="formData.valueSpecs" placeholder='{"0":"关","1":"开"}' :rows="4" allow-clear />
							<div style="color: #999; font-size: 12px; margin-top: 4px">
								枚举类型示例: {"0":"关","1":"开"}<br />
								结构体示例: {"params":[{"identifier":"temp","name":"温度","valueType":"float"}]}
							</div>
						</a-form-item>
					</template>
				</a-col>
				<!-- 服务输入参数配置 -->
				<a-col :span="24" v-if="formData.modelType === ModelType.SERVICE">
					<a-form-item label="输入参数">
						<a-button
							v-if="selectedServiceTemplate === 'custom'"
							type="dashed"
							block
							@click="addInputParam"
							style="margin-bottom: 8px"
						>
							<template #icon><PlusOutlined /></template>
							添加参数
						</a-button>
						<div v-for="(param, index) in inputParams" :key="index" style="margin-bottom: 8px">
							<a-card size="small">
								<template #extra>
									<a-button
										v-if="selectedServiceTemplate === 'custom'"
										type="link"
										danger
										size="small"
										@click="removeInputParam(index)"
									>
										删除
									</a-button>
									<a-tag v-else color="blue" size="small">模板参数</a-tag>
								</template>
								<a-row :gutter="8">
									<a-col :span="8">
										<a-input
											v-model:value="param.identifier"
											placeholder="参数标识符"
											size="small"
											:disabled="selectedServiceTemplate !== 'custom'"
										/>
									</a-col>
									<a-col :span="8">
										<a-input
											v-model:value="param.name"
											placeholder="参数名称"
											size="small"
											:disabled="selectedServiceTemplate !== 'custom'"
										/>
									</a-col>
									<a-col :span="8">
										<a-select
											v-model:value="param.dataType"
											placeholder="数据类型"
											size="small"
											:disabled="selectedServiceTemplate !== 'custom'"
											:options="dataTypeOptions"
										/>
									</a-col>
								</a-row>
							</a-card>
						</div>
						<a-empty v-if="inputParams.length === 0" description="暂无输入参数" :image="simpleImage" />
					</a-form-item>
				</a-col>
				<!-- 服务输出参数配置 -->
				<a-col :span="24" v-if="formData.modelType === ModelType.SERVICE">
					<a-form-item label="输出参数">
						<a-button
							v-if="selectedServiceTemplate === 'custom'"
							type="dashed"
							block
							@click="addOutputParam"
							style="margin-bottom: 8px"
						>
							<template #icon><PlusOutlined /></template>
							添加输出参数
						</a-button>
						<div v-for="(param, index) in outputParams" :key="index" style="margin-bottom: 8px">
							<a-card size="small">
								<template #extra>
									<a-button
										v-if="selectedServiceTemplate === 'custom'"
										type="link"
										danger
										size="small"
										@click="removeOutputParam(index)"
									>
										删除
									</a-button>
									<a-tag v-else color="green" size="small">模板参数</a-tag>
								</template>
								<a-row :gutter="8">
									<a-col :span="8">
										<a-input
											v-model:value="param.identifier"
											placeholder="参数标识符"
											size="small"
											:disabled="selectedServiceTemplate !== 'custom'"
										/>
									</a-col>
									<a-col :span="8">
										<a-input
											v-model:value="param.name"
											placeholder="参数名称"
											size="small"
											:disabled="selectedServiceTemplate !== 'custom'"
										/>
									</a-col>
									<a-col :span="8">
										<a-select
											v-model:value="param.dataType"
											placeholder="数据类型"
											size="small"
											:disabled="selectedServiceTemplate !== 'custom'"
											:options="dataTypeOptions"
										/>
									</a-col>
								</a-row>
							</a-card>
						</div>
						<a-empty v-if="outputParams.length === 0" description="暂无输出参数" :image="simpleImage" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="功能描述" name="description">
						<a-textarea v-model:value="formData.description" placeholder="请输入功能描述" :rows="2" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="排序码" name="sortCode">
						<a-input-number v-model:value="formData.sortCode" placeholder="请输入排序码" style="width: 100%" />
					</a-form-item>
				</a-col>
			</a-row>
		</a-form>
	</a-modal>
</template>

<script setup name="thingModelForm">
	import { ref } from 'vue'
	import { cloneDeep } from 'lodash-es'
	import { PlusOutlined } from '@ant-design/icons-vue'
	import { Empty } from 'ant-design-vue'
	import tool from '@/utils/tool'
	import { required } from '@/utils/formRules'
	import iotThingModelApi from '@/api/iot/iotThingModelApi'
	import {
		ModelType,
		ValueType,
		ServiceParamDataType,
		ServiceParamDataTypeLabels,
		isNumericType
	} from '@/utils/iotConstants'

	const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	const formData = ref({})
	const submitLoading = ref(false)

	// 字典数据
	const modelTypeOptions = tool.dictList('MODEL_TYPE')
	const valueTypeOptions = tool.dictList('VALUE_TYPE')
	const accessModeOptions = tool.dictList('ACCESS_MODE')
	const callTypeOptions = [
		{ label: '异步', value: 'ASYNC' },
		{ label: '同步', value: 'SYNC' }
	]
	// 数据类型选项(使用常量)
	const dataTypeOptions = Object.keys(ServiceParamDataTypeLabels).map((key) => ({
		label: ServiceParamDataTypeLabels[key],
		value: key
	}))

	// 服务输入参数
	const inputParams = ref([])

	// 服务输出参数
	const outputParams = ref([])

	// 服务模板选择
	const selectedServiceTemplate = ref()

	// 值定义配置数据(用于布尔和数值类型)
	const valueSpecsData = ref({})

	// 服务模板选项
	const serviceTemplateOptions = [
		{ label: '设置输出', value: 'setOutput' },
		{ label: '批量控制输出', value: 'setBatchOutputs' },
		{ label: '反转输出状态', value: 'toggleOutputs' },
		{ label: '自定义服务', value: 'custom' }
	]

	// 服务模板定义
	const serviceTemplates = {
		setOutput: {
			identifier: 'setOutput',
			name: '设置输出',
			callType: 'ASYNC',
			description: '设置单个输出的开关状态',
			inputParams: [
				{
					identifier: 'output',
					name: '输出编号',
					dataType: 'int',
					required: true,
					specs: JSON.stringify({ min: 1, max: 8, step: 1, unit: '' })
				},
				{
					identifier: 'value',
					name: '开关状态',
					dataType: 'bool',
					required: true,
					specs: JSON.stringify({ 0: '关闭', 1: '打开' })
				}
			],
			outputParams: [
				{
					identifier: 'code',
					name: '返回码',
					dataType: 'int',
					specs: JSON.stringify({ 200: '成功', 500: '失败' })
				},
				{
					identifier: 'message',
					name: '返回消息',
					dataType: 'string'
				}
			]
		},
		setBatchOutputs: {
			identifier: 'setBatchOutputs',
			name: '批量控制输出',
			callType: 'ASYNC',
			description: '批量设置多个输出的开关状态',
			inputParams: [
				{
					identifier: 'outputs',
					name: '输出编号列表',
					dataType: 'array',
					required: true,
					specs: JSON.stringify({ itemType: 'int', minLength: 1, maxLength: 8 })
				},
				{
					identifier: 'value',
					name: '开关状态',
					dataType: 'bool',
					required: true,
					specs: JSON.stringify({ 0: '关闭', 1: '打开' })
				}
			],
			outputParams: [
				{
					identifier: 'code',
					name: '返回码',
					dataType: 'int'
				},
				{
					identifier: 'message',
					name: '返回消息',
					dataType: 'string'
				}
			]
		},
		toggleOutputs: {
			identifier: 'toggleOutputs',
			name: '反转输出状态',
			callType: 'ASYNC',
			description: '反转所有输出的当前状态',
			inputParams: [],
			outputParams: [
				{
					identifier: 'code',
					name: '返回码',
					dataType: 'int'
				},
				{
					identifier: 'message',
					name: '返回消息',
					dataType: 'string'
				}
			]
		}
	}

	// 打开表单
	const onOpen = (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)

			console.log('编辑记录:', record)
			console.log('extJson:', record.extJson)

			// 解析valueSpecs(用于布尔和数值类型)
			if (record.valueSpecs) {
				try {
					valueSpecsData.value = JSON.parse(record.valueSpecs)
				} catch (e) {
					valueSpecsData.value = {}
				}
			} else {
				valueSpecsData.value = {}
			}

			// 解析服务输入参数
			if (record.modelType === ModelType.SERVICE) {
				if (record.extJson) {
					try {
						const extData = typeof record.extJson === 'string' ? JSON.parse(record.extJson) : record.extJson
						console.log('解析extData:', extData)
						if (extData.inputParams) {
							inputParams.value = cloneDeep(extData.inputParams)
							console.log('输入参数:', inputParams.value)
						}
						if (extData.outputParams) {
							outputParams.value = cloneDeep(extData.outputParams)
							console.log('输出参数:', outputParams.value)
						}
						if (extData.callType) {
							formData.value.callType = extData.callType
						}
					} catch (e) {
						console.error('解析extJson失败', e)
					}

					// 编辑时判断是否为预定义服务
					if (serviceTemplates[record.identifier]) {
						selectedServiceTemplate.value = record.identifier
					} else {
						selectedServiceTemplate.value = 'custom'
					}
				} else {
					// 没有extJson,判断是否是预定义服务,使用模板填充
					console.warn('extJson为空,尝试使用模板')
					if (serviceTemplates[record.identifier]) {
						selectedServiceTemplate.value = record.identifier
						const template = serviceTemplates[record.identifier]
						inputParams.value = cloneDeep(template.inputParams)
						outputParams.value = cloneDeep(template.outputParams)
						formData.value.callType = template.callType
					} else {
						selectedServiceTemplate.value = 'custom'
					}
				}
			}
		}
		// 设置默认值
		if (!formData.value.accessMode && formData.value.modelType === ModelType.PROPERTY) {
			formData.value.accessMode = 'RW'
		}
		if (formData.value.callType === undefined && formData.value.modelType === ModelType.SERVICE) {
			formData.value.callType = 'ASYNC'
		}
		if (formData.value.required === undefined) {
			formData.value.required = false
		}
	}

	// 关闭表单
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		inputParams.value = []
		outputParams.value = []
		valueSpecsData.value = {}
		selectedServiceTemplate.value = undefined
		open.value = false
	}

	// 功能类型改变
	const onModelTypeChange = () => {
		if (formData.value.modelType === ModelType.SERVICE) {
			// 切换到服务类型,清空不相关字段
			formData.value.valueType = undefined
			formData.value.accessMode = undefined
			formData.value.valueSpecs = undefined
			if (!formData.value.callType) {
				formData.value.callType = 'ASYNC'
			}
			// 重置服务模板选择
			selectedServiceTemplate.value = undefined
		} else {
			// 切换到属性/事件,清空服务相关字段
			formData.value.callType = undefined
			inputParams.value = []
			outputParams.value = []
			selectedServiceTemplate.value = undefined
		}
	}

	// 服务模板改变
	const onServiceTemplateChange = (value) => {
		if (!value || value === 'custom') {
			// 自定义服务,清空表单
			formData.value.identifier = ''
			formData.value.name = ''
			formData.value.description = ''
			formData.value.callType = 'ASYNC'
			inputParams.value = []
			outputParams.value = []
			return
		}

		// 应用模板
		const template = serviceTemplates[value]
		if (template) {
			formData.value.identifier = template.identifier
			formData.value.name = template.name
			formData.value.description = template.description
			formData.value.callType = template.callType
			inputParams.value = cloneDeep(template.inputParams)
			outputParams.value = cloneDeep(template.outputParams)
		}
	}

	// 添加输入参数
	const addInputParam = () => {
		inputParams.value.push({
			identifier: '',
			name: '',
			dataType: 'int'
		})
	}

	// 删除输入参数
	const removeInputParam = (index) => {
		inputParams.value.splice(index, 1)
	}

	// 添加输出参数
	const addOutputParam = () => {
		outputParams.value.push({
			identifier: '',
			name: '',
			dataType: 'int'
		})
	}

	// 删除输出参数
	const removeOutputParam = (index) => {
		outputParams.value.splice(index, 1)
	}

	// 表单规则
	const formRules = {
		modelType: [required('请选择功能类型')],
		identifier: [required('请输入标识符')],
		name: [required('请输入功能名称')],
		valueType: [
			{
				validator: (rule, value) => {
					// 服务类型不需要valueType
					if (formData.value.modelType === ModelType.SERVICE) {
						return Promise.resolve()
					}
					// 属性/事件类型必须选择
					if (!value) {
						return Promise.reject('请选择值类型')
					}
					return Promise.resolve()
				},
				trigger: 'change'
			}
		],
		accessMode: [
			{
				validator: (rule, value) => {
					if (formData.value.modelType === ModelType.PROPERTY && !value) {
						return Promise.reject('请选择读写类型')
					}
					return Promise.resolve()
				},
				trigger: 'change'
			}
		],
		valueSpecs: [
			{
				validator: (rule, value) => {
					if (!value) return Promise.resolve()
					try {
						JSON.parse(value)
						return Promise.resolve()
					} catch (e) {
						return Promise.reject('请输入正确的JSON格式')
					}
				},
				trigger: 'blur'
			}
		]
	}

	// 提交表单
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)

				// 服务类型:将输入参数、输出参数和调用方式存入extJson
				if (formDataParam.modelType === ModelType.SERVICE) {
					const extData = {
						callType: formDataParam.callType,
						inputParams: inputParams.value.filter((p) => p.identifier && p.name && p.dataType),
						outputParams: outputParams.value.filter((p) => p.identifier && p.name && p.dataType)
					}
					formDataParam.extJson = JSON.stringify(extData)
					// 服务类型设置一个默认valueType(后端校验需要)
					formDataParam.valueType = 'object'
					// 清除不相关字段
					delete formDataParam.accessMode
					delete formDataParam.valueSpecs
					delete formDataParam.callType
				} else {
					// 属性/事件类型:清除服务相关字段
					delete formDataParam.callType
				}

				// 将valueSpecsData转换为valueSpecs JSON字符串(仅对布尔和数值类型)
				if (formDataParam.modelType !== ModelType.SERVICE) {
					if (formDataParam.valueType === ValueType.BOOL) {
						// 布尔类型:从 valueSpecsData 提取 true/false 配置
						const specs = {}
						if (valueSpecsData.value.true) specs.true = valueSpecsData.value.true
						if (valueSpecsData.value.false) specs.false = valueSpecsData.value.false
						formDataParam.valueSpecs = JSON.stringify(specs)
					} else if (isNumericType(formDataParam.valueType)) {
						// 数值类型:从 valueSpecsData 提取 min/max/step/unit
						const specs = {}
						if (valueSpecsData.value.min !== undefined && valueSpecsData.value.min !== null)
							specs.min = valueSpecsData.value.min
						if (valueSpecsData.value.max !== undefined && valueSpecsData.value.max !== null)
							specs.max = valueSpecsData.value.max
						if (valueSpecsData.value.step !== undefined && valueSpecsData.value.step !== null)
							specs.step = valueSpecsData.value.step
						if (valueSpecsData.value.unit) specs.unit = valueSpecsData.value.unit
						formDataParam.valueSpecs = JSON.stringify(specs)
					}
					// 其他类型保持原样(使用JSON文本框)
				}

				iotThingModelApi
					.iotThingModelSubmitForm(formDataParam, formDataParam.id)
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

	defineExpose({
		onOpen
	})
</script>
