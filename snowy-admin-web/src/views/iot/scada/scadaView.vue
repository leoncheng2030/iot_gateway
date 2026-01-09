<template>
	<div class="scada-view-container">
		<ScadaCanvas ref="scadaCanvasRef" :preview-mode="true" />
		<a-spin
			v-if="!canvasData"
			size="large"
			style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%)"
		/>
	</div>
</template>

<script setup>
	import { ref, onMounted } from 'vue'
	import { useRoute } from 'vue-router'
	import { ScadaCanvas } from '@nywqs/scada-engine'
	import '@nywqs/scada-engine/dist/scada-engine.css'
	import scadaApi from '@/api/iot/scadaApi'
	import { message } from 'ant-design-vue'

	const route = useRoute()
	const canvasData = ref(null)
	const scadaCanvasRef = ref()

	// 加载组态数据
	const loadScadaData = async () => {
		const scadaId = route.query.scadaId
		if (!scadaId) {
			message.error('缺少组态ID参数')
			return
		}

		try {
			const res = await scadaApi.scadaDetail({ id: scadaId })

			if (res.config) {
				canvasData.value = JSON.parse(res.config)

				// 等待下一帧再加载数据到预览组件
				setTimeout(() => {
					if (scadaCanvasRef.value && scadaCanvasRef.value.loadCanvasData) {
						scadaCanvasRef.value.loadCanvasData(canvasData.value)
					} else {
						console.error('⚠️ ScadaCanvas ref 或 loadCanvasData 方法不存在')
					}
				}, 100)
			} else {
				message.warning('该组态暂无配置数据')
			}
		} catch (error) {
			console.error('加载组态失败:', error)
			message.error('加载组态失败: ' + error.message)
		}
	}

	onMounted(async () => {
		loadScadaData()
	})

	// 暴露刷新方法
	defineExpose({
		refresh: loadScadaData
	})
</script>

<style scoped lang="less">
	.scada-view-container {
		width: 100vw;
		height: 100vh;
		overflow: hidden;
		background: #0f172a;
		position: relative;
		display: flex !important;
		flex-direction: column !important;
	}
</style>
