<template>
	<xn-form-container
		:title="formData.id ? '编辑北向推送统计表' : '增加北向推送统计表'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-form-item label="推送配置ID：" name="configId">
				<a-input v-model:value="formData.configId" placeholder="请输入推送配置ID" allow-clear />
			</a-form-item>
			<a-form-item label="统计日期：" name="statDate">
				<a-date-picker v-model:value="formData.statDate" value-format="YYYY-MM-DD HH:mm:ss" show-time placeholder="请选择统计日期" style="width: 100%" />
			</a-form-item>
			<a-form-item label="总推送次数：" name="totalCount">
				<a-input v-model:value="formData.totalCount" placeholder="请输入总推送次数" allow-clear />
			</a-form-item>
			<a-form-item label="成功次数：" name="successCount">
				<a-input v-model:value="formData.successCount" placeholder="请输入成功次数" allow-clear />
			</a-form-item>
			<a-form-item label="失败次数：" name="failedCount">
				<a-input v-model:value="formData.failedCount" placeholder="请输入失败次数" allow-clear />
			</a-form-item>
			<a-form-item label="平均耗时(毫秒)：" name="avgCostTime">
				<a-input v-model:value="formData.avgCostTime" placeholder="请输入平均耗时(毫秒)" allow-clear />
			</a-form-item>
			<a-form-item label="最大耗时(毫秒)：" name="maxCostTime">
				<a-input v-model:value="formData.maxCostTime" placeholder="请输入最大耗时(毫秒)" allow-clear />
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotNorthboundStatisticsForm">
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotNorthboundStatisticsApi from '@/api/iot/iotNorthboundStatisticsApi'
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
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotNorthboundStatisticsApi
					.iotNorthboundStatisticsSubmitForm(formDataParam, formDataParam.id)
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
