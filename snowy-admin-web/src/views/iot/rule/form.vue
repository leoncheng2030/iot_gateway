<template>
	<xn-form-container
		:title="formData.id ? '编辑规则' : '新增规则'"
		:width="600"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :span="12">
					<a-form-item label="规则名称" name="ruleName">
						<a-input v-model:value="formData.ruleName" placeholder="请输入规则名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="规则类型" name="ruleType">
						<a-select v-model:value="formData.ruleType" placeholder="请选择" :options="ruleTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="规则描述" name="ruleDesc">
						<a-textarea v-model:value="formData.ruleDesc" placeholder="请输入规则描述" :rows="3" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="状态" name="status">
						<a-select v-model:value="formData.status" placeholder="请选择" :options="statusOptions" />
					</a-form-item>
				</a-col>
				<a-col :span="12">
					<a-form-item label="排序码" name="sortCode">
						<a-input-number v-model:value="formData.sortCode" placeholder="排序码" style="width: 100%" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-alert message="提示:保存后请在规则编排中配置工作流" type="info" show-icon />
				</a-col>
			</a-row>
		</a-form>
		<template #footer>
			<a-button style="margin-right: 8px" @click="onClose">关闭</a-button>
			<a-button type="primary" @click="onSubmit" :loading="submitLoading">保存</a-button>
		</template>
	</xn-form-container>
</template>

<script setup name="iotRuleForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotRuleApi from '@/api/iot/iotRuleApi'
	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const ruleTypeOptions = ref([])
	const statusOptions = ref([])

	// 打开抽屉
	const onOpen = (record) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
		}
		ruleTypeOptions.value = tool.dictList('RULE_TYPE')
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
		ruleName: [required('请输入规则名称')],
		ruleType: [required('请选择规则类型')],
		status: [required('请选择状态')]
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)

				// 新增时初始化空的工作流数据
				if (!formDataParam.id && !formDataParam.workflowData) {
					formDataParam.workflowData = JSON.stringify({ nodes: [], edges: [] })
				}

				iotRuleApi
					.iotRuleSubmitForm(formDataParam, formDataParam.id)
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
