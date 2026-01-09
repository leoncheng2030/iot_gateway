<template>
	<div class="scada-design-container">
		<ScadaCanvas 
			ref="scadaRef" 
			:auth-code="testAuthCode"
			:custom-footer="customFooter"
			:on-save="handleSave"
			@preview="handlePreview"
		/>
	</div>
</template>

<script setup>
	import { ref, onMounted, onBeforeUnmount } from 'vue'
	import { useRoute, useRouter } from 'vue-router'
	import { ScadaCanvas } from '@nywqs/scada-engine'
	import '@nywqs/scada-engine/dist/scada-engine.css'
	import { message } from 'ant-design-vue'
	import scadaApi from '@/api/iot/scadaApi'

	const route = useRoute()
	const router = useRouter()
	const scadaRef = ref()
	
	const currentScadaId = ref(route.query.scadaId)
	const scadaInfo = ref({ name: '' })
	
	// 授权码（leoncheng | 有效期：2099-12-31）
	const testAuthCode = ref('53616c7465645f5f8df6cf694df1e55b6c73d798f89afb350873b606846a38112be2efac043802e6bbff66e720eb006d')
	
	// 自定义 Footer 配置
	const customFooter = ref({
		copyright: '© 2025 leoncheng',
		license: '仅供学习研究使用，禁止商业用途',
		contact: '📧 nywqs@outlook.com | 📱 18637762001'
	})
	
	// 自动保存定时器
	let autoSaveTimer = null

	

	// 加载组态配置
	const loadScadaConfig = async () => {
		if (!currentScadaId.value) {
			console.log('新建组态模式，使用默认配置')
			return
		}

		try {
			const res = await scadaApi.scadaDetail({ id: currentScadaId.value })
			scadaInfo.value.name = res.name
			
			if (res.config) {
				const configData = JSON.parse(res.config)
				console.log('加载的组态配置:', configData)
				
				// 加载到 SCADA Engine
				const success = scadaRef.value?.loadCanvasData(configData)
				if (success) {
					message.success('组态加载成功')
				} else {
					message.error('组态加载失败')
				}
			}
		} catch (error) {
			console.error('加载组态配置失败:', error)
			message.error('加载组态配置失败: ' + error.message)
		}
	}

	// 保存组态
	const handleSave = async () => {
		try {
			const canvasData = scadaRef.value?.getCanvasData()
			if (!canvasData) {
				message.error('获取画布数据失败')
				return
			}

			console.log('准备保存的数据:', canvasData)

			const data = {
				name: scadaInfo.value.name || '未命名组态',
				config: JSON.stringify(canvasData)
			}

			if (currentScadaId.value) {
				data.id = currentScadaId.value
			}

			const apiMethod = currentScadaId.value ? scadaApi.scadaEdit : scadaApi.scadaAdd
			const res = await apiMethod(data)
			
			// 如果是新建，保存后更新ID
			if (!currentScadaId.value && res.id) {
				currentScadaId.value = res.id
			}
			
			message.success('保存成功')
		} catch (error) {
			console.error('保存失败:', error)
			message.error('保存失败: ' + error.message)
		}
	}

	// 自动保存
	const startAutoSave = () => {
		// 每5分钟自动保存一次
		autoSaveTimer = setInterval(() => {
			if (currentScadaId.value) {
				handleSave()
				console.log('自动保存完成')
			}
		}, 5 * 60 * 1000)
	}

	// 停止自动保存
	const stopAutoSave = () => {
		if (autoSaveTimer) {
			clearInterval(autoSaveTimer)
			autoSaveTimer = null
		}
	}

	// 预览
	const handlePreview = () => {
		console.log('🔍 handlePreview 被调用')
		console.log('currentScadaId.value:', currentScadaId.value)
		if (currentScadaId.value) {
			// 先保存再预览
			console.log('✅ 有 scadaId，开始保存...')
			handleSave().then(() => {
				// 使用完整路径而不是 hash 路由
				const url = `/iot/scada/view?scadaId=${currentScadaId.value}`
				console.log('📍 即将打开预览页面:', url)
				window.open(url, '_blank')
			})
		} else {
			console.warn('⚠️ 没有 scadaId，提示保存')
			message.warning('请先保存组态后再预览')
		}
	}

	// 返回列表
	const handleReturn = () => {
		router.push('/iot/scada')
	}

	// 监听键盘快捷键
	const handleKeyDown = (e) => {
		// Ctrl+S 保存
		if (e.ctrlKey && e.key === 's') {
			e.preventDefault()
			handleSave()
		}
	}

	onMounted(async () => {
		// 加载组态配置
		await loadScadaConfig()
		
		// 启动自动保存
		startAutoSave()
		
		// 添加键盘监听
		document.addEventListener('keydown', handleKeyDown)
	})

	onBeforeUnmount(() => {
		// 停止自动保存
		stopAutoSave()
		
		// 移除键盘监听
		document.removeEventListener('keydown', handleKeyDown)
		
	})

	// 暴露方法给外部调用（如果需要）
	defineExpose({
		save: handleSave,
		preview: handlePreview,
		return: handleReturn,
		getScadaRef: () => scadaRef.value
	})
</script>

<style scoped lang="less">
	.scada-design-container {
		width: 100vw;
		height: 100vh;
		overflow: hidden;
		background: #0f172a;
	}
</style>
