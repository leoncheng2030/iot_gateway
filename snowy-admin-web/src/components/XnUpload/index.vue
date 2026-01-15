<template>
	<div>
		<a-upload
			v-if="props.uploadMode === 'file'"
			v-model:file-list="fileList"
			name="file"
			:action="action"
			:headers="headers"
			:maxCount="props.uploadNumber"
			:progress="progress"
			@change="handleChange"
			:showUploadList="props.showUploadList"
			:accept="accept"
			:disabled="props.disabled"
		>
			<a-button>
				<upload-outlined />
				{{ props.uploadText }}
			</a-button>
		</a-upload>

		<a-upload
			v-if="props.uploadMode === 'video'"
			v-model:file-list="fileList"
			name="file"
			:action="action"
			:headers="headers"
			:maxCount="props.uploadNumber"
			:before-upload="beforeUpload"
			list-type="picture-card"
			@change="handleChange"
			@preview="handlePreview"
			:progress="progress"
			:showUploadList="props.showUploadList"
			:accept="accept"
			:disabled="props.disabled"
		>
			<div class="clearfix" v-if="fileList.length < props.uploadNumber">
				<plus-outlined />
				<div style="margin-top: 8px">{{ props.uploadText }}</div>
			</div>
		</a-upload>

		<a-upload
			v-if="props.uploadMode === 'image'"
			v-model:file-list="fileList"
			name="file"
			:action="action"
			:headers="headers"
			:maxCount="props.uploadNumber"
			:before-upload="beforeUpload"
			list-type="picture-card"
			@change="handleChange"
			@preview="handlePreview"
			:progress="progress"
			:showUploadList="props.showUploadList"
			:accept="accept"
			:disabled="props.disabled"
		>
			<div class="clearfix" v-if="fileList.length < props.uploadNumber">
				<plus-outlined />
				<div style="margin-top: 8px">{{ props.uploadText }}</div>
			</div>
		</a-upload>

		<!-- 文件预览 -->
		<a-modal :open="previewVisible" :title="previewTitle" :footer="null" :destroyOnClose="true" @cancel="handleCancel">
			<template v-if="props.uploadMode === 'image'">
				<img alt="example" style="width: 100%" :src="previewObj" />
			</template>
			<template v-else-if="props.uploadMode === 'video'">
				<video width="100%" controls type="video" id="video">
					<source :src="previewObj" type="video/mp4" />
					<object :data="previewObj" width="100%">
						<embed :src="previewObj" width="100%" />
					</object>
					您的浏览器不支持video标签哦，请联系管理员
				</video>
			</template>
			<template v-else> 暂不支持该文件预览 </template>
		</a-modal>

		<a-upload-dragger
			v-if="props.uploadMode === 'drag'"
			v-model:fileList="fileList"
			name="file"
			:multiple="true"
			:action="action"
			:headers="headers"
			:maxCount="props.uploadNumber"
			@change="handleChange"
			:progress="progress"
			:showUploadList="props.showUploadList"
			:accept="accept"
			:disabled="props.disabled"
		>
			<p class="ant-upload-drag-icon">
				<inbox-outlined />
			</p>
			<p class="ant-upload-text">
				{{ props.uploadText }}
			</p>
			<p class="ant-upload-hint"></p>
		</a-upload-dragger>
		<slot name="explain"></slot>
	</div>
</template>

<script setup name="uploadIndex">
	import tool from '@/utils/tool'
	import sysConfig from '@/config/index'
	import { convertUrl } from '@/utils/apiAdaptive'
	import { message, Upload } from 'ant-design-vue'
	import { cloneDeep } from 'lodash-es'
	const fileList = ref([])
	const emit = defineEmits(['update:value', 'onChange', 'onSuccessful'])
	const previewVisible = ref(false)
	const previewTitle = ref('')
	const previewObj = ref('')
	const headers = ref({
		token: tool.data.get('TOKEN')
	})
	const accept = ref('')
	const props = defineProps({
		// 上传返回id
		uploadReturnIdApi: {
			type: String,
			default: convertUrl('/dev/file/uploadLocalReturnId'),
			required: false
		},
		// 上传返回url
		uploadDynamicReturnUrlApi: {
			type: String,
			default: convertUrl('/dev/file/uploadDynamicReturnUrl'),
			required: false
		},
		// 当上传接口为id的情况下，配置下载接口
		uploadIdDownloadUrl: {
			type: String,
			default: convertUrl('/dev/file/download?id='),
			required: false
		},
		// 上传样式或图片方式 file || video || drag || image
		uploadMode: {
			type: String,
			default: 'file',
			required: false
		},
		// 上传数量
		uploadNumber: {
			type: Number,
			default: 1,
			required: false
		},
		// 上传按钮文字
		uploadText: {
			type: String,
			default: '上传',
			required: false
		},
		// 上传返回id或url
		uploadResultType: {
			type: String,
			default: 'url',
			required: false
		},
		// 上传返回分类 数组或字符串逗号隔离 interval | array
		uploadResultCategory: {
			type: String,
			default: 'interval',
			required: false
		},
		// 跟antdv官方一样，是否显示文件列表
		showUploadList: {
			type: Boolean,
			default: true,
			required: false
		},
		// 跟antdv官方一样，接受上传的文件类型
		accept: {
			type: String,
			default: '',
			required: false
		},
		// 是否是完整的结果（就是文件上传返回什么，该组件返回什么，uploadResultCategory必须为array）
		completeResult: {
			type: Boolean,
			default: false,
			required: false
		},
		// 父组件传来的参数
		value: {
			type: [String, Array],
			default: undefined,
			required: false
		},
		// 组件禁用状态
		disabled: {
			type: Boolean,
			default: false,
			required: false
		},
	})
	const action =
		props.uploadResultType === 'id'
			? sysConfig.API_URL + props.uploadReturnIdApi
			: sysConfig.API_URL + props.uploadDynamicReturnUrlApi

	// 构造文件对象
	const buildFileObject = (url, id) => {
		return {
			data: url ? url : sysConfig.API_URL + props.uploadIdDownloadUrl + id,
			name: url ? url : id,
			url: url ? url : sysConfig.API_URL + props.uploadIdDownloadUrl + id,
			status: 'done',
			response: {
				data: url ? url : id,
				code: 200
			}
		}
	}
	// 处理回显 - 使用策略模式优化嵌套条件
	const echoStrategies = {
		interval_id: (newVal) => {
			newVal.split(',').forEach((id) => {
				const file = buildFileObject(undefined, id)
				fileList.value.push(file)
				fileList.value.reverse()
			})
		},
		interval_url: (newVal) => {
			newVal.split(',').forEach((url) => {
				const file = buildFileObject(url)
				fileList.value.push(file)
				fileList.value.reverse()
			})
		},
		array_id_complete: (newVal) => {
			let newResult = cloneDeep(newVal)
			newResult.forEach((e) => {
				delete e.thumbUrl // 去掉base64的thumbUrl
				e.url = sysConfig.API_URL + props.uploadIdDownloadUrl + e.response.data
			})
			fileList.value = newResult
		},
		array_url_complete: (newVal) => {
			let newResult = cloneDeep(newVal)
			newResult.forEach((e) => {
				delete e.thumbUrl
				e.url = e.response.data
			})
			fileList.value = newResult
		},
		array_id: (newVal) => {
			newVal.forEach((id) => {
				fileList.value.push(buildFileObject(undefined, id))
			})
		},
		array_url: (newVal) => {
			newVal.forEach((url) => {
				fileList.value.push(buildFileObject(url))
			})
		}
	}

	const echo = (newVal) => {
		// 根据配置组合key获取对应策略
		const key = `${props.uploadResultCategory}_${props.uploadResultType}${props.completeResult ? '_complete' : ''}`
		const strategy = echoStrategies[key]
		strategy?.(newVal)
	}
	// 监听参数
	watch(
		() => props.value,
		(newVal) => {
			if (props.value && newVal) {
				fileList.value = []
				echo(newVal)
			}
		},
		{ immediate: true, deep: true }
	)
	// 监听上传类型，从而设置对应的文件类型
	watch(
		() => props.uploadMode,
		(newVal) => {
			// 简化逻辑：优先使用传入的accept，否则根据mode自动设置
			if (props.accept) {
				accept.value = props.accept
			} else {
				accept.value = newVal === 'image' ? 'image/*' : ''
			}
		},
		{ immediate: true, deep: true }
	)
	// 进度条
	const progress = {
		strokeWidth: 5,
		format: (percent) => parseFloat(percent.toFixed(2)) + '%'
	}

	// 文件类型验证配置
	const fileTypeValidators = {
		image: (file) => ({
			isValid: file.type.startsWith('image/'),
			message: '只能上传图片类型文件'
		}),
		video: (file) => ({
			isValid: file.type.startsWith('video/'),
			message: '只能上传视频类型文件'
		})
	}

	// 统一的文件验证逻辑
	const beforeUpload = (file) => {
		const validator = fileTypeValidators[props.uploadMode]
		if (!validator) return true

		const { isValid, message: msg } = validator(file)
		if (!isValid) {
			message.warning(msg)
		}
		return isValid || Upload.LIST_IGNORE
	}
	// 预览资源
	const handlePreview = async (file) => {
		previewVisible.value = true
		previewTitle.value = file.name
		// 如果返回的是id
		if (props.uploadResultType === 'id') {
			previewObj.value = sysConfig.API_URL + props.uploadIdDownloadUrl + file.response.data
		} else {
			previewObj.value = file.response.data
		}
	}
	// 关闭预览窗口
	const handleCancel = () => {
		previewVisible.value = false
		previewTitle.value = ''
		previewObj.value = ''
	}

	// 获取上传结果值
	const getUploadResult = (resultData) => {
		if (props.uploadResultCategory === 'interval') {
			const values = resultData.map(data => data.response.data)
			return values.join(',')
		} else if (props.uploadResultCategory === 'array') {
			if (props.completeResult) {
				// 移除thumbUrl并保留完整对象
				return resultData.map((e) => {
					const item = cloneDeep(e)
					delete item.thumbUrl
					return item
				})
			} else {
				return resultData.map(data => data.response.data)
			}
		}
	}

	// 发送上传事件
	const emitUploadResult = (resultValue) => {
		emit('update:value', resultValue)
		emit('onSuccessful', resultValue)
		emit('onChange', resultValue)
	}

	// 上传事件
	const handleChange = (uploads) => {
		const file = uploads.file
		if (file && (file.status === 'done' || file.status === 'removed') && file.response?.code === 200) {
			const resultValue = getUploadResult(uploads.fileList)
			if (resultValue) {
				emitUploadResult(resultValue)
				return
			}
		}
		emitUploadResult(undefined)
		emit('onChange', undefined)
	}
	// 通过DOM获取上传的文件
	const uploadFileList = () => {
		if (fileList.value) {
			const result = []
			// 只返回这些就够用了，其他基本用不到
			fileList.value.forEach((item) => {
				const obj = {
					name: item.name,
					type: item.type,
					size: item.size,
					url: item.response.data
				}
				result.push(obj)
			})
			return result
		} else {
			return []
		}
	}
	// 抛出这个获取文件列表的方法
	defineExpose({
		uploadFileList
	})
</script>
