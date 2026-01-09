<template>
	<xn-form-container
		:title="formData.id ? '编辑网关拓扑' : '增加网关拓扑'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="网关设备ID：" name="gatewayId">
						<a-input v-model:value="formData.gatewayId" placeholder="请输入网关设备ID" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="子设备ID：" name="subDeviceId">
						<a-input v-model:value="formData.subDeviceId" placeholder="请输入子设备ID" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="绑定时间：" name="bindTime">
						<a-date-picker v-model:value="formData.bindTime" value-format="YYYY-MM-DD HH:mm:ss" show-time placeholder="请选择绑定时间" style="width: 100%" />
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

<script setup name="iotGatewayTopoForm">
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotGatewayTopoApi from '@/api/iot/iotGatewayTopoApi'
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
		gatewayId: [required('请输入网关设备ID')],
		subDeviceId: [required('请输入子设备ID')],
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotGatewayTopoApi
					.iotGatewayTopoSubmitForm(formDataParam, formDataParam.id)
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
