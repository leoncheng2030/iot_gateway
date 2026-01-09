<template>
	<a-modal
		v-model:open="visible"
		title="设备关联管理"
		:width="1000"
		:mask-closable="false"
		:destroy-on-close="true"
		@ok="handleOk"
		@cancel="handleClose"
	>
		<a-row :gutter="16">
			<a-col :span="12">
				<a-card size="small" title="未关联设备" :loading="unrelatedLoading">
					<a-space direction="vertical" style="width: 100%">
						<a-input-search
							v-model:value="unrelatedSearchKey"
							placeholder="搜索设备名称或设备Key"
							@search="loadUnrelatedDevices"
						/>
						<a-table
							:columns="deviceColumns"
							:data-source="unrelatedDevices"
							:pagination="false"
							:row-selection="{
								selectedRowKeys: selectedUnrelatedKeys,
								onChange: onUnrelatedSelect
							}"
							:scroll="{ y: 400 }"
							size="small"
							row-key="id"
						/>
						<a-button type="primary" block @click="addDevices" :disabled="selectedUnrelatedKeys.length === 0">
							<template #icon><arrow-right-outlined /></template>
							添加到分组
						</a-button>
					</a-space>
				</a-card>
			</a-col>
			<a-col :span="12">
				<a-card size="small" title="已关联设备" :loading="relatedLoading">
					<a-space direction="vertical" style="width: 100%">
						<a-input-search
							v-model:value="relatedSearchKey"
							placeholder="搜索设备名称或设备Key"
							@search="loadRelatedDevices"
						/>
						<a-table
							:columns="deviceColumns"
							:data-source="relatedDevices"
							:pagination="false"
							:row-selection="{
								selectedRowKeys: selectedRelatedKeys,
								onChange: onRelatedSelect
							}"
							:scroll="{ y: 400 }"
							size="small"
							row-key="id"
						/>
						<a-button danger block @click="removeDevices" :disabled="selectedRelatedKeys.length === 0">
							<template #icon><arrow-left-outlined /></template>
							从分组移除
						</a-button>
					</a-space>
				</a-card>
			</a-col>
		</a-row>
	</a-modal>
</template>

<script setup name="deviceRelate">
	import { message } from 'ant-design-vue'
	import iotDeviceGroupApi from '@/api/iot/iotDeviceGroupApi'

	const visible = ref(false)
	const groupId = ref('')
	const groupName = ref('')
	const unrelatedLoading = ref(false)
	const relatedLoading = ref(false)
	const unrelatedSearchKey = ref('')
	const relatedSearchKey = ref('')
	const unrelatedDevices = ref([])
	const relatedDevices = ref([])
	const selectedUnrelatedKeys = ref([])
	const selectedRelatedKeys = ref([])
	const relatedDeviceIds = ref([])

	const deviceColumns = [
		{
			title: '设备名称',
			dataIndex: 'deviceName',
			ellipsis: true
		},
		{
			title: '设备Key',
			dataIndex: 'deviceKey',
			ellipsis: true
		}
	]

	// 打开弹窗
	const onOpen = (record) => {
		visible.value = true
		groupId.value = record.id
		groupName.value = record.groupName
		selectedUnrelatedKeys.value = []
		selectedRelatedKeys.value = []
		// 后端已处理过滤逻辑，可以并发加载
		loadRelatedDevices()
		loadUnrelatedDevices()
	}

	// 加载已关联设备
	const loadRelatedDevices = () => {
		relatedLoading.value = true
		// 调用后端API获取已关联设备
		return iotDeviceGroupApi
			.getRelatedDevices({
				groupId: groupId.value,
				searchKey: relatedSearchKey.value,
				pageSize: 1000
			})
			.then((res) => {
				relatedDevices.value = res.records || []
				// 保存已关联设备ID列表，用于前端显示
				relatedDeviceIds.value = relatedDevices.value.map((device) => device.id)
			})
			.finally(() => {
				relatedLoading.value = false
			})
	}

	// 加载未关联设备
	const loadUnrelatedDevices = () => {
		unrelatedLoading.value = true
		// 调用后端API获取未被任何分组关联的设备
		iotDeviceGroupApi
			.getUnrelatedDevices({
				searchKey: unrelatedSearchKey.value,
				pageSize: 1000
			})
			.then((res) => {
				unrelatedDevices.value = res.records || []
			})
			.finally(() => {
				unrelatedLoading.value = false
			})
	}

	// 未关联设备选择
	const onUnrelatedSelect = (selectedKeys) => {
		selectedUnrelatedKeys.value = selectedKeys
	}

	// 已关联设备选择
	const onRelatedSelect = (selectedKeys) => {
		selectedRelatedKeys.value = selectedKeys
	}

	// 添加设备到分组
	const addDevices = () => {
		if (selectedUnrelatedKeys.value.length === 0) {
			message.warning('请选择要添加的设备')
			return
		}
		iotDeviceGroupApi
			.batchRelateDevices({
				groupId: groupId.value,
				deviceIds: selectedUnrelatedKeys.value
			})
			.then(() => {
				message.success('设备添加成功')
				selectedUnrelatedKeys.value = []
				// 刷新两个列表
				loadRelatedDevices()
				loadUnrelatedDevices()
			})
	}

	// 从分组移除设备
	const removeDevices = () => {
		if (selectedRelatedKeys.value.length === 0) {
			message.warning('请选择要移除的设备')
			return
		}
		iotDeviceGroupApi
			.batchRemoveDevices({
				groupId: groupId.value,
				deviceIds: selectedRelatedKeys.value
			})
			.then(() => {
				message.success('设备移除成功')
				selectedRelatedKeys.value = []
				// 刷新两个列表
				loadRelatedDevices()
				loadUnrelatedDevices()
			})
	}

	// 确定
	const handleOk = () => {
		visible.value = false
	}

	// 关闭
	const handleClose = () => {
		visible.value = false
	}

	defineExpose({
		onOpen
	})
</script>
