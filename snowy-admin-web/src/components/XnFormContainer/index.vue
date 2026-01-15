<template>
	<a-modal
		v-if="isModal"
		:open="visible"
		@cancel="cancel"
		v-bind="$attrs"
		:footer="slotKeys.includes('footer') ? undefined : null"
	>
		<template v-for="slotKey in slotKeys" #[slotKey]>
			<slot :name="slotKey" />
		</template>
	</a-modal>
	<a-drawer
		v-else
		:open="visible"
		v-bind="$attrs"
		@close="cancel"
		:footer-style="{ textAlign: 'right' }"
		:width="drawerWidth"
	>
		<template v-for="slotKey in slotKeys" #[slotKey]>
			<slot :name="slotKey" />
		</template>
	</a-drawer>
</template>

<script setup>
	import { useSlots, computed, useAttrs, onMounted, onUnmounted, ref } from 'vue'
	import { globalStore } from '@/store'
	import { debounce } from 'lodash-es'

	const slots = useSlots()
	const attrs = useAttrs()
	const store = globalStore()
	const props = defineProps({
		visible: {
			type: Boolean,
			default: false,
			required: false
		}
	})

	const FormContainerTypeEnum = {
		DRAWER: 'drawer',
		MODAL: 'modal'
	}

	const formStyle = computed(() => store.formStyle)
	const slotKeys = computed(() => Object.keys(slots))
	const isModal = computed(() => FormContainerTypeEnum.MODAL === formStyle.value)

	// 响应式抽屉宽度 - 使用防抖处理窗口大小变化
	const isSmallScreen = ref(window.innerWidth <= 768)
	const drawerWidth = computed(() => isSmallScreen.value ? '100%' : attrs.width)

	const emit = defineEmits(['close'])
	const cancel = () => emit('close')

	// 防抖处理窗口大小变化，避免频繁更新
	const handleResize = debounce(() => {
		isSmallScreen.value = window.innerWidth <= 768
	}, 250)

	onMounted(() => {
		window.addEventListener('resize', handleResize)
	})

	onUnmounted(() => {
		window.removeEventListener('resize', handleResize)
	})
</script>
<script>
	// 声明额外的选项
	export default {
		inheritAttrs: false
	}
</script>

<style scoped>
	/* 确保小屏幕下抽屉不会有额外的边距或滚动条 */
	@media (max-width: 576px) {
		:deep(.ant-drawer-content-wrapper) {
			width: 100% !important;
			max-width: 100% !important;
		}
	}
</style>
