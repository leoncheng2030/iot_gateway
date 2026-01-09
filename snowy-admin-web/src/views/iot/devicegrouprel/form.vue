<template>
	<xn-form-container
		:title="formData.id ? '编辑设备分组关联表' : '增加设备分组关联表'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-form-item label="分组ID：" name="groupId">
				<a-input v-model:value="formData.groupId" placeholder="请输入分组ID" allow-clear />
			</a-form-item>
			<a-form-item label="设备ID：" name="deviceId">
				<a-input v-model:value="formData.deviceId" placeholder="请输入设备ID" allow-clear />
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotDeviceGroupRelForm">
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotDeviceGroupRelApi from '@/api/iot/iotDeviceGroupRelApi'
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
		groupId: [required('请输入分组ID')],
		deviceId: [required('请输入设备ID')],
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotDeviceGroupRelApi
					.iotDeviceGroupRelSubmitForm(formDataParam, formDataParam.id)
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
