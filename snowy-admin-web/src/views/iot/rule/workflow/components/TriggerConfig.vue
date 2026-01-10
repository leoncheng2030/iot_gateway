<template>
	<div>
		<a-form-item label="触发类型">
			<a-select v-model:value="properties.triggerType" @change="handleChange">
				<a-select-option value="device">设备事件</a-select-option>
				<a-select-option value="timer">定时触发</a-select-option>
			</a-select>
		</a-form-item>

		<a-form-item label="设备" v-if="properties.triggerType === 'device'">
			<a-select
				v-model:value="properties.deviceId"
				show-search
				placeholder="选择设备"
				@change="handleDeviceChange"
			>
				<a-select-option v-for="device in deviceList" :key="device.id" :value="device.id">
					{{ device.deviceName }}
				</a-select-option>
			</a-select>
		</a-form-item>

		<a-form-item label="Cron表达式" v-if="properties.triggerType === 'timer'">
			<xn-cron v-model:value="properties.cronExpression" @update:value="handleChange" />
		</a-form-item>
	</div>
</template>

<script setup>
import XnCron from '@/components/Cron/index.vue'
const props = defineProps({
	properties: {
		type: Object,
		required: true
	},
	deviceList: {
		type: Array,
		default: () => []
	}
})

const emit = defineEmits(['change', 'deviceChange'])

const handleChange = () => {
	emit('change')
}

const handleDeviceChange = () => {
	emit('change')
	emit('deviceChange')
}
</script>
