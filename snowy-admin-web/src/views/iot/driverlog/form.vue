<template>
	<xn-form-container
		:title="formData.id ? '编辑运行日志' : '增加运行日志'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-form-item label="驱动ID：" name="driverId">
				<a-input v-model:value="formData.driverId" placeholder="请输入驱动ID" allow-clear />
			</a-form-item>
			<a-form-item label="驱动名称：" name="driverName">
				<a-input v-model:value="formData.driverName" placeholder="请输入驱动名称" allow-clear />
			</a-form-item>
			<a-form-item label="日志类型：" name="logType">
				<a-select v-model:value="formData.logType" placeholder="请选择日志类型" :options="logTypeOptions" />
			</a-form-item>
			<a-form-item label="日志内容：" name="logContent">
				<a-input v-model:value="formData.logContent" placeholder="请输入日志内容" allow-clear />
			</a-form-item>
			<a-form-item label="关联设备标识：" name="deviceKey">
				<a-input v-model:value="formData.deviceKey" placeholder="请输入关联设备标识" allow-clear />
			</a-form-item>
			<a-form-item label="错误信息：" name="errorMsg">
				<a-input v-model:value="formData.errorMsg" placeholder="请输入错误信息" allow-clear />
			</a-form-item>
			<a-form-item label="扩展信息：" name="extJson">
				<a-input v-model:value="formData.extJson" placeholder="请输入扩展信息" allow-clear />
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotDriverLogForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotDriverLogApi from '@/api/iot/iotDriverLogApi'
	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const logTypeOptions = ref([])

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
		}
		logTypeOptions.value = tool.dictList('DRIVER_LOG_TYPE')
	}
	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		open.value = false
	}
	// 默认要校验的
	const formRules = {
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotDriverLogApi
					.iotDriverLogSubmitForm(formDataParam, formDataParam.id)
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
