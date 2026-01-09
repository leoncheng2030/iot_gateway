<template>
	<a-descriptions bordered :column="2">
		<a-descriptions-item label="设备名称">{{ deviceData.deviceName }}</a-descriptions-item>
		<a-descriptions-item label="设备标识">{{ deviceData.deviceKey }}</a-descriptions-item>
		<a-descriptions-item label="设备密钥">
			<a-typography-text copyable>{{ deviceData.deviceSecret }}</a-typography-text>
		</a-descriptions-item>
		<a-descriptions-item label="产品ID">{{ deviceData.productId }}</a-descriptions-item>
		<a-descriptions-item label="设备状态">
			<a-badge
				:status="deviceData.deviceStatus === 'ONLINE' ? 'success' : 'default'"
				:text="$TOOL.dictTypeData('DEVICE_STATUS', deviceData.deviceStatus)"
			/>
		</a-descriptions-item>
		<a-descriptions-item label="网关设备ID">{{ deviceData.gatewayId || '-' }}</a-descriptions-item>
		<a-descriptions-item label="激活时间">{{ deviceData.activeTime || '-' }}</a-descriptions-item>
		<a-descriptions-item label="最后在线时间">{{ deviceData.lastOnlineTime || '-' }}</a-descriptions-item>
		<a-descriptions-item label="IP地址">{{ deviceData.ipAddress || '-' }}</a-descriptions-item>
		<a-descriptions-item label="固件版本">{{ deviceData.firmwareVersion || '-' }}</a-descriptions-item>
		<a-descriptions-item label="经度">{{ deviceData.longitude || '-' }}</a-descriptions-item>
		<a-descriptions-item label="纬度">{{ deviceData.latitude || '-' }}</a-descriptions-item>
		<a-descriptions-item label="位置描述" :span="2">{{ deviceData.location || '-' }}</a-descriptions-item>
		<a-descriptions-item label="标签" :span="2">{{ deviceData.tags || '-' }}</a-descriptions-item>
		<a-descriptions-item label="备注" :span="2">{{ deviceData.remark || '-' }}</a-descriptions-item>
		<a-descriptions-item label="创建时间">{{ deviceData.createTime }}</a-descriptions-item>
		<a-descriptions-item label="更新时间">{{ deviceData.updateTime }}</a-descriptions-item>
	</a-descriptions>
</template>

<script setup>
defineProps({
	deviceData: {
		type: Object,
		default: () => ({})
	}
})
</script>
