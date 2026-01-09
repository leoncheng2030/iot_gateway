<template>
	<xn-form-container
		:title="formData.id ? '编辑北向推送设备关联表' : '增加北向推送设备关联表'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-form-item label="推送配置ID：" name="configId">
				<a-input v-model:value="formData.configId" placeholder="请输入推送配置ID" allow-clear />
			</a-form-item>
			<a-form-item label="设备ID(为NULL表示推送所有设备)：" name="deviceId">
				<a-input v-model:value="formData.deviceId" placeholder="请输入设备ID(为NULL表示推送所有设备)" allow-clear />
			</a-form-item>
			<a-form-item label="产品ID(为NULL表示不限产品)：" name="productId">
				<a-input v-model:value="formData.productId" placeholder="请输入产品ID(为NULL表示不限产品)" allow-clear />
			</a-form-item>
			<a-form-item label="设备分组ID：" name="deviceGroupId">
				<a-input v-model:value="formData.deviceGroupId" placeholder="请输入设备分组ID" allow-clear />
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotNorthboundDeviceRelForm">
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotNorthboundDeviceRelApi from '@/api/iot/iotNorthboundDeviceRelApi'
	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
		}
	}
	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		open.value = false
	}
	// 默认要校验的
	const formRules = {
		configId: [required('请输入推送配置ID')],
		deviceId: [required('请输入设备ID(为NULL表示推送所有设备)')],
		productId: [required('请输入产品ID(为NULL表示不限产品)')],
		deviceGroupId: [required('请输入设备分组ID')],
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotNorthboundDeviceRelApi
					.iotNorthboundDeviceRelSubmitForm(formDataParam, formDataParam.id)
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
