<template>
	<a @click="handleClick" style="display: block; cursor: pointer">
		<div v-if="stats">
			<div style="color: #1890ff">今日: {{ stats.totalCount || 0 }}</div>
			<div style="font-size: 12px; color: #52c41a">成功: {{ stats.successCount || 0 }}</div>
			<div v-if="stats.failedCount > 0" style="font-size: 12px; color: #ff4d4f">失败: {{ stats.failedCount }}</div>
		</div>
		<span v-else style="color: #999">暂无数据</span>
	</a>
</template>

<script setup>
	defineProps({
		stats: {
			type: Object,
			default: null
		}
	})

	const emit = defineEmits(['click'])

	const handleClick = () => {
		emit('click')
	}
</script>
