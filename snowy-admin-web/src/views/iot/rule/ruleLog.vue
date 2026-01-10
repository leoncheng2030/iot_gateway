<template>
	<a-drawer
		:title="'规则日志 - ' + ruleRecord.ruleName"
		:width="1200"
		:open="visible"
		:body-style="{ paddingBottom: '80px' }"
		@close="onClose"
	>
		<a-card :bordered="false">
			<a-form ref="searchFormRef" :model="searchFormState">
				<a-row :gutter="10">
					<a-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
						<a-form-item label="执行状态" name="executeResult">
							<a-select
								v-model:value="searchFormState.executeResult"
								placeholder="请选择执行状态"
								:options="statusOptions"
								allowClear
							/>
						</a-form-item>
					</a-col>
					<a-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
						<a-form-item>
							<a-space>
								<a-button type="primary" @click="tableRef.refresh(true)">
									<template #icon><SearchOutlined /></template>
									查询
								</a-button>
								<a-button @click="reset">
									<template #icon><redo-outlined /></template>
									重置
								</a-button>
							</a-space>
						</a-form-item>
					</a-col>
				</a-row>
			</a-form>
			<s-table
				ref="tableRef"
				:columns="columns"
				:data="loadData"
				bordered
				:row-key="(record) => record.id"
				:tool-config="toolConfig"
				:scroll="{ x: 'max-content' }"
			>
				<template #bodyCell="{ column, record }">
					<template v-if="column.dataIndex === 'executeResult'">
						<a-badge
							:status="record.executeResult === 'SUCCESS' ? 'success' : record.executeResult === 'FAILED' ? 'error' : 'default'"
							:text="record.executeResult"
						/>
					</template>
					<template v-if="column.dataIndex === 'triggerData'">
						<a-button v-if="record.triggerData" type="link" size="small" @click="viewTriggerData(record.triggerData)">
							查看
						</a-button>
						<span v-else>-</span>
					</template>
					<template v-if="column.dataIndex === 'errorMsg'">
						<a-button v-if="record.errorMsg" type="link" size="small" danger @click="viewErrorMsg(record.errorMsg)">
							查看错误
						</a-button>
						<span v-else>-</span>
					</template>
				</template>
			</s-table>
		</a-card>
		
		<!-- 触发数据弹窗 -->
		<a-modal
			v-model:open="triggerDataVisible"
			title="触发数据"
			:width="600"
			:footer="null"
		>
			<a-descriptions bordered :column="1" size="small">
				<a-descriptions-item v-for="(value, key) in parsedTriggerData" :key="key" :label="key">
					{{ value }}
				</a-descriptions-item>
			</a-descriptions>
		</a-modal>
		
		<!-- 错误信息弹窗 -->
		<a-modal
			v-model:open="errorMsgVisible"
			title="错误信息"
			:width="700"
			:footer="null"
		>
			<a-alert type="error" :message="currentErrorMsg" style="white-space: pre-wrap; word-break: break-all;" />
		</a-modal>
	</a-drawer>
</template>

<script setup name="ruleLog">
	import { cloneDeep } from 'lodash-es'
	import iotRuleLogApi from '@/api/iot/iotRuleLogApi'

	const visible = ref(false)
	const ruleRecord = ref({})
	const searchFormState = ref({})
	const searchFormRef = ref()
	const tableRef = ref()
	
	// 触发数据弹窗
	const triggerDataVisible = ref(false)
	const parsedTriggerData = ref({})
	
	// 错误信息弹窗
	const errorMsgVisible = ref(false)
	const currentErrorMsg = ref('')

	const toolConfig = { refresh: true, height: true, columnSetting: true, striped: false }

	const columns = [
		{
			title: '执行时间',
			dataIndex: 'executeTime',
			width: 180
		},
		{
			title: '执行状态',
			dataIndex: 'executeResult',
			width: 100,
			align: 'center'
		},
		{
			title: '触发数据',
			dataIndex: 'triggerData',
			width: 100,
			align: 'center'
		},
		{
			title: '错误信息',
			dataIndex: 'errorMsg',
			width: 100,
			align: 'center'
		}
	]

	const statusOptions = [
		{ label: '成功', value: 'SUCCESS' },
		{ label: '失败', value: 'FAILED' }
	]

	const loadData = (parameter) => {
		if (!ruleRecord.value.id) {
			return Promise.resolve({ data: [] })
		}

		const searchFormParam = cloneDeep(searchFormState.value)
		searchFormParam.ruleId = ruleRecord.value.id

		return iotRuleLogApi.iotRuleLogPage(Object.assign(parameter, searchFormParam)).then((data) => {
			return data
		})
	}

	// 重置
	const reset = () => {
		searchFormRef.value.resetFields()
		tableRef.value.refresh(true)
	}

	// 打开抽屉
	const onOpen = (record) => {
		ruleRecord.value = record
		visible.value = true
		// 延迟一下刷新表格，确保 DOM 已渲染
		nextTick(() => {
			tableRef.value?.refresh(true)
		})
	}

	// 关闭抽屉
	const onClose = () => {
		visible.value = false
		ruleRecord.value = {}
		searchFormState.value = {}
	}
	
	// 查看触发数据
	const viewTriggerData = (triggerData) => {
		try {
			parsedTriggerData.value = JSON.parse(triggerData)
		} catch (e) {
			// 如果不是 JSON 格式，直接显示原始数据
			parsedTriggerData.value = { '原始数据': triggerData }
		}
		triggerDataVisible.value = true
	}
	
	// 查看错误信息
	const viewErrorMsg = (errorMsg) => {
		currentErrorMsg.value = errorMsg
		errorMsgVisible.value = true
	}

	// 暴露方法给父组件
	defineExpose({
		onOpen
	})
</script>
