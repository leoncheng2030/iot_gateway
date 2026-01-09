<template>
	<xn-form-container
		:title="formData.id ? '编辑产品' : '增加产品'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
					<a-form-item label="产品名称：" name="productName">
						<a-input v-model:value="formData.productName" placeholder="请输入产品名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="产品标识：" name="productKey">
						<a-input v-model:value="formData.productKey" placeholder="请输入产品标识" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="产品类型：" name="productType">
						<a-select v-model:value="formData.productType" placeholder="请选择产品类型" :options="productTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="接入协议：" name="protocolType">
						<a-select
							v-model:value="formData.protocolType"
							placeholder="请选择接入协议"
							:options="protocolTypeOptions"
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="数据格式：" name="dataFormat">
						<a-select v-model:value="formData.dataFormat" placeholder="请选择数据格式" :options="dataFormatOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
					<a-form-item label="产品描述：" name="productDesc">
						<a-textarea v-model:value="formData.productDesc" placeholder="请输入产品描述" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="状态：" name="status">
						<a-select v-model:value="formData.status" placeholder="请选择状态" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="排序码：" name="sortCode">
						<a-input v-model:value="formData.sortCode" placeholder="请输入排序码" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="扩展信息：" name="extJson">
						<a-input v-model:value="formData.extJson" placeholder="请输入扩展信息" allow-clear />
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

<script setup name="iotProductForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotProductApi from '@/api/iot/iotProductApi'
	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const productTypeOptions = ref([])
	const protocolTypeOptions = ref([])
	const dataFormatOptions = ref([])
	const statusOptions = ref([])

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
		}
		productTypeOptions.value = tool.dictList('PRODUCT_TYPE')
		protocolTypeOptions.value = tool.dictList('PROTOCOL_TYPE')
		dataFormatOptions.value = tool.dictList('DATA_FORMAT')
		statusOptions.value = tool.dictList('COMMON_STATUS')
	}
	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		open.value = false
	}
	// 默认要校验的
	const formRules = {
		productName: [required('产品名称')],
		productKey: [required('产品标识')],
		productType: [required('产品类型')],
		protocolType: [required('接入协议')],
		dataFormat: [required('数据格式')],
		status: [required('状态')]
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotProductApi
					.iotProductSubmitForm(formDataParam, formDataParam.id)
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
