<template>
	<xn-form-container
		:title="formData.id ? '编辑设备分组' : '增加设备分组'"
		:width="700"
		v-model:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
			<a-row :gutter="16">
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="上级分组:" name="parentId">
						<a-tree-select
							v-model:value="formData.parentId"
							:dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
							placeholder="请选择上级分组"
							allow-clear
							:tree-data="treeData"
							:field-names="{
								children: 'children',
								label: 'name',
								value: 'id'
							}"
							tree-line
						/>
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="分组名称:" name="groupName">
						<a-input v-model:value="formData.groupName" placeholder="请输入分组名称" allow-clear />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="分组类型:" name="groupType">
						<a-select v-model:value="formData.groupType" placeholder="请选择分组类型" :options="groupTypeOptions" />
					</a-form-item>
				</a-col>
				<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
					<a-form-item label="排序码:" name="sortCode">
						<a-input-number v-model:value="formData.sortCode" placeholder="请输入排序码" style="width: 100%" :min="0" />
					</a-form-item>
				</a-col>
				<a-col :span="24">
					<a-form-item label="备注:" name="remark">
						<a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" allow-clear />
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

<script setup name="iotDeviceGroupForm">
	import tool from '@/utils/tool'
	import { cloneDeep } from 'lodash-es'
	import { required } from '@/utils/formRules'
	import iotDeviceGroupApi from '@/api/iot/iotDeviceGroupApi'

	// 抽屉状态
	const open = ref(false)
	const emit = defineEmits({ successful: null })
	const formRef = ref()
	// 表单数据
	const formData = ref({})
	const submitLoading = ref(false)
	const groupTypeOptions = ref([])
	const treeData = ref([])

	// 打开抽屉
	const onOpen = (record, parentId) => {
		open.value = true
		if (record) {
			let recordData = cloneDeep(record)
			formData.value = Object.assign({}, recordData)
		} else if (parentId) {
			formData.value.parentId = parentId
		}
		groupTypeOptions.value = tool.dictList('DEVICE_GROUP_TYPE')
		// 获取分组树并加入顶级
		iotDeviceGroupApi.iotDeviceGroupTree().then((res) => {
			treeData.value = [
				{
					id: '0',
					parentId: '-1',
					name: '顶级',
					children: res
				}
			]
		})
	}
	// 关闭抽屉
	const onClose = () => {
		formRef.value.resetFields()
		formData.value = {}
		open.value = false
	}
	// 默认要校验的
	const formRules = {
		parentId: [required('请选择上级分组')],
		groupName: [required('请输入分组名称')],
		groupType: [required('请选择分组类型')],
		sortCode: [required('请输入排序码')]
	}
	// 验证并提交数据
	const onSubmit = () => {
		formRef.value
			.validate()
			.then(() => {
				submitLoading.value = true
				const formDataParam = cloneDeep(formData.value)
				iotDeviceGroupApi
					.iotDeviceGroupSubmitForm(formDataParam, formDataParam.id)
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
