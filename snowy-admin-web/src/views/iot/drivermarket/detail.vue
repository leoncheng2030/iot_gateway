<template>
	<a-drawer
		:title="driverData.driverName || '驱动详情'"
		:width="500"
		:open="open"
		:destroy-on-close="true"
		@close="onClose"
	>
		<a-descriptions :column="1" bordered size="small">
			<a-descriptions-item label="驱动类型">{{ driverData.driverType }}</a-descriptions-item>
			<a-descriptions-item label="驱动名称">{{ driverData.driverName }}</a-descriptions-item>
			<a-descriptions-item label="版本号">{{ driverData.driverVersion || '1.0.0' }}</a-descriptions-item>
			<a-descriptions-item label="来源类型">
				<a-tag v-if="driverData.sourceType === 'BUILTIN'" color="blue">内置驱动</a-tag>
				<a-tag v-else color="green">上传驱动</a-tag>
			</a-descriptions-item>
			<a-descriptions-item label="安装状态">
				<a-badge v-if="driverData.installStatus === 'ENABLED'" status="success" text="已启用" />
				<a-badge v-else-if="driverData.installStatus === 'INSTALLED'" status="processing" text="已安装" />
				<a-badge v-else-if="driverData.installStatus === 'DISABLED'" status="warning" text="已禁用" />
				<a-badge v-else status="default" text="未安装" />
			</a-descriptions-item>
			<a-descriptions-item label="分类">
				<span v-if="driverData.category === 'INDUSTRIAL'">工业协议</span>
				<span v-else-if="driverData.category === 'NETWORK'">网络协议</span>
				<span v-else-if="driverData.category === 'GATEWAY'">网关驱动</span>
				<span v-else>其他</span>
			</a-descriptions-item>
			<a-descriptions-item label="供应商">{{ driverData.vendor || '-' }}</a-descriptions-item>
			<a-descriptions-item label="协议版本">{{ driverData.protocolVersion || '-' }}</a-descriptions-item>
			<a-descriptions-item label="支持热更新">
				<a-tag v-if="driverData.supportsHotUpdate" color="green">是</a-tag>
				<a-tag v-else color="default">否</a-tag>
			</a-descriptions-item>
			<a-descriptions-item label="安装次数">{{ driverData.downloadCount || 0 }}</a-descriptions-item>
			<a-descriptions-item label="安装时间">{{ driverData.installedTime || '-' }}</a-descriptions-item>
			<a-descriptions-item label="启用时间">{{ driverData.enabledTime || '-' }}</a-descriptions-item>
			<a-descriptions-item label="描述" :span="1">
				<div style="white-space: pre-wrap">{{ driverData.description || '暂无描述' }}</div>
			</a-descriptions-item>
		</a-descriptions>

		<!-- 配置字段 -->
		<a-divider>配置字段</a-divider>
		<a-empty v-if="!configFields || configFields.length === 0" description="暂无配置字段" />
		<a-table
			v-else
			:dataSource="configFields"
			:columns="configColumns"
			:pagination="false"
			size="small"
			bordered
		/>
	</a-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'

const open = ref(false)
const driverData = ref({})

const configColumns = [
	{ title: '字段', dataIndex: 'key', width: 100 },
	{ title: '名称', dataIndex: 'name', width: 100 },
	{ title: '类型', dataIndex: 'type', width: 80 },
	{ title: '必填', dataIndex: 'required', width: 60, customRender: ({ value }) => value ? '是' : '否' },
	{ title: '默认值', dataIndex: 'defaultValue' }
]

const configFields = computed(() => {
	if (!driverData.value.configFields) {
		return []
	}
	try {
		return JSON.parse(driverData.value.configFields)
	} catch (e) {
		return []
	}
})

const onOpen = (record) => {
	driverData.value = record || {}
	open.value = true
}

const onClose = () => {
	open.value = false
	driverData.value = {}
}

defineExpose({
	onOpen
})
</script>
