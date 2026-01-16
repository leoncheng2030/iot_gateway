<template>
	<a-modal
		title="上传驱动"
		:open="open"
		:confirm-loading="uploading"
		@ok="handleUpload"
		@cancel="onClose"
		:width="500"
	>
		<a-upload-dragger
			v-model:file-list="fileList"
			name="file"
			:multiple="false"
			:before-upload="beforeUpload"
			:show-upload-list="true"
			accept=".jar"
		>
			<p class="ant-upload-drag-icon">
				<inbox-outlined />
			</p>
			<p class="ant-upload-text">点击或拖拽 JAR 文件到此区域上传</p>
			<p class="ant-upload-hint">仅支持 .jar 格式的驱动包文件</p>
		</a-upload-dragger>

		<a-alert
			v-if="uploadResult"
			:type="uploadResult.success ? 'success' : 'error'"
			:message="uploadResult.message"
			show-icon
			style="margin-top: 16px"
		/>

		<a-divider />

		<div class="upload-tips">
			<h4>驱动开发说明：</h4>
			<ul>
				<li>驱动 JAR 包需要实现 <code>DriverProvider</code> 接口</li>
				<li>在 <code>META-INF/services</code> 下配置 SPI 文件</li>
				<li>上传后驱动将自动识别并添加到市场</li>
				<li>同类型驱动不允许重复上传</li>
			</ul>
		</div>
	</a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import iotDriverMarketApi from '@/api/iot/iotDriverMarketApi'

const emit = defineEmits(['success'])

const open = ref(false)
const uploading = ref(false)
const fileList = ref([])
const uploadResult = ref(null)

const beforeUpload = (file) => {
	const isJar = file.name.toLowerCase().endsWith('.jar')
	if (!isJar) {
		message.error('只能上传 JAR 格式文件!')
		return false
	}
	const isLt50M = file.size / 1024 / 1024 < 50
	if (!isLt50M) {
		message.error('文件大小不能超过 50MB!')
		return false
	}
	fileList.value = [file]
	return false
}

const handleUpload = async () => {
	if (fileList.value.length === 0) {
		message.warning('请选择要上传的驱动文件')
		return
	}

	const formData = new FormData()
	formData.append('file', fileList.value[0])

	uploading.value = true
	uploadResult.value = null

	try {
		await iotDriverMarketApi.iotDriverMarketUpload(formData)
		uploadResult.value = { success: true, message: '驱动上传成功' }
		message.success('上传成功')
		emit('success')
		setTimeout(() => {
			onClose()
		}, 1500)
	} catch (e) {
		uploadResult.value = { success: false, message: e.msg || '上传失败' }
	} finally {
		uploading.value = false
	}
}

const onOpen = () => {
	open.value = true
	fileList.value = []
	uploadResult.value = null
}

const onClose = () => {
	open.value = false
	fileList.value = []
	uploadResult.value = null
}

defineExpose({
	onOpen
})
</script>

<style scoped>
.upload-tips {
	color: #666;
	font-size: 13px;
}
.upload-tips h4 {
	margin-bottom: 8px;
	color: #333;
}
.upload-tips ul {
	padding-left: 20px;
	margin: 0;
}
.upload-tips li {
	margin-bottom: 4px;
}
.upload-tips code {
	background: #f5f5f5;
	padding: 2px 4px;
	border-radius: 3px;
	font-size: 12px;
}
</style>
