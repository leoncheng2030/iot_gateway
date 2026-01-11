<template>
	<div class="register-mapping-config">
		<!-- 模式切换区域（仅设备级显示） -->
		<a-card v-if="showModeSwitch" size="small" style="margin-bottom: 16px">
			<a-space>
				<a-switch v-model:checked="useMappingModeLocal" @change="onMappingModeChange" />
				<span>使用设备级配置（当前：{{ useMappingModeLocal ? '设备级' : '产品级' }}）</span>
				<a-button v-if="useMappingModeLocal" type="primary" size="small" @click="handleSave"> 保存设备级配置 </a-button>
			</a-space>
		</a-card>

		<!-- 产品级操作区域 -->
		<a-card v-else size="small" style="margin-bottom: 16px">
			<a-space>
				<a-button type="primary" @click="handleSave" :loading="saving">
					<template #icon>
						<SaveOutlined />
					</template>
					保存映射配置
				</a-button>
				<a-button @click="handleRefresh">
					<template #icon>
						<ReloadOutlined />
					</template>
					刷新
				</a-button>
				<a-typography-text type="secondary">
					{{ tip }}
				</a-typography-text>
			</a-space>
		</a-card>

		<a-table
			:columns="columns"
			:data-source="sortedMappingList"
			:pagination="pagination"
			:loading="loading"
			size="middle"
		>
			<template #bodyCell="{ column, record }">
				<!-- 属性标识符列：只读显示 -->
				<template v-if="column.dataIndex === 'identifier'">
					<a-tag color="blue">{{ record.identifier }}</a-tag>
				</template>

				<!-- 地址列：根据协议模板动态渲染 -->
				<template v-if="column.dataIndex === 'registerAddress'">
					<!-- BUILDER 模式：显示文本输入 + 构建按钮 -->
					<template v-if="addressTemplate?.inputMode === 'BUILDER'">
						<a-input
							v-model:value="record.displayAddress"
							style="width: 180px"
							:placeholder="addressTemplate.formatDescription || '地址'"
							readonly
							:disabled="showModeSwitch && !useMappingModeLocal"
						>
							<template #addonAfter>
								<a v-if="!showModeSwitch || useMappingModeLocal" @click="openAddressBuilder(record)">构建</a>
								<span v-else style="color: #999">构建</span>
							</template>
						</a-input>
					</template>
					<!-- SIMPLE 模式：显示数字输入 -->
					<template v-else>
						<a-input-number
							v-model:value="record.registerAddress"
							:min="0"
							:max="65535"
							style="width: 100px"
							placeholder="地址"
							:disabled="showModeSwitch && !useMappingModeLocal"
						/>
					</template>
				</template>

				<!-- 动态渲染其他字段（根据模板定义） -->
				<template v-for="field in addressTemplate?.fields || []" :key="field.name">
					<!-- 跳过 registerAddress 字段，因为已经在上面处理过了 -->
					<template v-if="column.dataIndex === field.name && field.name !== 'registerAddress'">
						<!-- SELECT 类型 -->
						<template v-if="field.type === 'SELECT'">
							<a-select
								v-model:value="record[field.name]"
								style="width: 170px"
								:options="formatFieldOptions(field.options)"
								:placeholder="field.label"
								:disabled="showModeSwitch && !useMappingModeLocal"
							/>
						</template>
						<!-- NUMBER 类型 -->
						<template v-else-if="field.type === 'NUMBER'">
							<a-input-number
								v-model:value="record[field.name]"
								:min="field.min"
								:max="field.max"
								style="width: 100px"
								:placeholder="field.label"
								:disabled="showModeSwitch && !useMappingModeLocal"
							/>
						</template>
						<!-- TEXT 类型 -->
						<template v-else>
							<a-input
								v-model:value="record[field.name]"
								style="width: 150px"
								:placeholder="field.label"
								:disabled="showModeSwitch && !useMappingModeLocal"
							/>
						</template>
					</template>
				</template>
				<template v-if="column.dataIndex === 'enabled'">
					<a-switch v-model:checked="record.enabled" :disabled="showModeSwitch && !useMappingModeLocal" />
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a v-if="!showModeSwitch || useMappingModeLocal" @click="openAdvancedConfig(record)">配置</a>
					<span v-else style="color: #999">配置</span>
				</template>
			</template>
		</a-table>

		<!-- 通用地址构建器弹窗 -->
		<a-modal
			v-model:open="addressBuilderVisible"
			:title="addressTemplate?.templateName || '地址配置'"
			width="550px"
			@ok="saveAddress"
			@cancel="addressBuilderVisible = false"
		>
			<a-alert
				:message="addressTemplate?.templateName"
				:description="addressTemplate?.formatDescription"
				type="info"
				show-icon
				style="margin-bottom: 16px"
			/>

			<!-- 动态表单字段 -->
			<a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
				<template v-for="field in addressTemplate?.fields" :key="field.name">
					<a-form-item v-if="shouldShowField(field)" :label="field.label">
						<!-- SELECT 类型 -->
						<a-select
							v-if="field.type === 'SELECT'"
							v-model:value="builderFormData[field.name]"
							:options="formatFieldOptions(field.options)"
							style="width: 100%"
						/>
						<!-- NUMBER 类型 -->
						<a-input-number
							v-else-if="field.type === 'NUMBER'"
							v-model:value="builderFormData[field.name]"
							:min="field.min"
							:max="field.max"
							style="width: 100%"
						/>
						<!-- TEXT 类型 -->
						<a-input v-else v-model:value="builderFormData[field.name]" style="width: 100%" />
						<a-typography-text v-if="field.description" type="secondary">
							{{ field.description }}
						</a-typography-text>
					</a-form-item>
				</template>

				<!-- 地址预览 -->
				<a-form-item v-if="addressTemplate?.inputMode === 'BUILDER'" label="生成地址">
					<a-input :value="addressPreview" readonly>
						<template #prefix>
							<span style="color: #1890ff">🔍</span>
						</template>
					</a-input>
					<a-typography-text type="success" style="font-weight: bold"> ✓ 该地址将被保存 </a-typography-text>
				</a-form-item>
			</a-form>

			<!-- 地址示例 -->
			<a-divider v-if="addressTemplate?.examples?.length" orientation="left" style="margin-top: 16px">
				常用地址示例
			</a-divider>
			<a-space v-if="addressTemplate?.examples?.length" direction="vertical" style="width: 100%">
				<a-typography-text v-for="(example, index) in addressTemplate.examples" :key="index">
					{{ example }}
				</a-typography-text>
			</a-space>
		</a-modal>

		<!-- 高级配置弹窗 -->
		<a-modal
			v-model:open="advancedConfigVisible"
			:title="`高级配置 - ${currentConfigItem.name}`"
			width="550px"
			@ok="saveAdvancedConfig"
			@cancel="advancedConfigVisible = false"
		>
			<a-descriptions bordered :column="1" size="small" style="margin-bottom: 16px">
				<a-descriptions-item label="属性标识符">
					<a-tag>{{ currentConfigItem.identifier }}</a-tag>
				</a-descriptions-item>
				<a-descriptions-item label="属性名称">{{ currentConfigItem.name }}</a-descriptions-item>
				<a-descriptions-item label="属性描述">{{ currentConfigItem.description || '-' }}</a-descriptions-item>
			</a-descriptions>

			<a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
				<!-- 数据类型显示（自动从物模型获取） -->
				<a-form-item label="数据类型">
					<a-input :value="currentConfigItem.dataType || '未配置'" readonly style="width: 100%" />
					<a-typography-text type="secondary"> 自动从物模型属性的valueType字段获取，不需要手动配置 </a-typography-text>
				</a-form-item>

				<!-- 布尔类型配置 -->
				<template v-if="currentConfigItem.dataType === 'bool'">
					<a-form-item label="位索引">
						<a-input-number
							v-model:value="currentConfigItem.bitIndex"
							:min="0"
							:max="15"
							style="width: 100%"
							placeholder="位索引 (0-15)"
						/>
						<a-typography-text type="secondary">指定读取的位 (0-15)</a-typography-text>
					</a-form-item>
				</template>

				<!-- 非布尔类型配置 -->
				<template v-else>
					<a-form-item label="缩放系数">
						<a-input-number
							v-model:value="currentConfigItem.scaleFactor"
							:min="0"
							:step="0.1"
							:precision="2"
							style="width: 100%"
							placeholder="1.0"
						/>
						<a-typography-text type="secondary">用于单位转换,例如原始值×100</a-typography-text>
					</a-form-item>

					<a-form-item label="偏移量">
						<a-input-number
							v-model:value="currentConfigItem.offset"
							:step="0.1"
							:precision="2"
							style="width: 100%"
							placeholder="0.0"
						/>
						<a-typography-text type="secondary">计算公式:最终值 = 原始值 × 缩放系数 + 偏移量</a-typography-text>
					</a-form-item>
				</template>

				<a-form-item label="字节序">
					<a-select
						v-model:value="currentConfigItem.byteOrder"
						style="width: 100%"
						:options="byteOrderOptions"
						placeholder="选择字节序"
					/>
					<a-typography-text type="secondary">数据字节序,默认为大端序 (BIG_ENDIAN)</a-typography-text>
				</a-form-item>

				<a-form-item label="备注">
					<a-textarea v-model:value="currentConfigItem.remark" :rows="3" placeholder="请输入备注信息" />
				</a-form-item>
			</a-form>
		</a-modal>
	</div>
</template>

<script setup name="RegisterMappingConfig">
	import { ref, computed, watch, onMounted } from 'vue'
	import { SaveOutlined, ReloadOutlined } from '@ant-design/icons-vue'
	import { message } from 'ant-design-vue'
	import iotProtocolApi from '@/api/iot/iotProtocolApi'

	const props = defineProps({
		// 映射列表数据
		mappingList: {
			type: Array,
			default: () => []
		},
		// 加载状态
		loading: {
			type: Boolean,
			default: false
		},
		// 保存状态
		saving: {
			type: Boolean,
			default: false
		},
		// 提示文本
		tip: {
			type: String,
			default: '配置寄存器映射规则'
		},
		// 是否显示模式切换（设备级使用）
		showModeSwitch: {
			type: Boolean,
			default: false
		},
		// 是否使用设备级映射
		useDeviceMapping: {
			type: Boolean,
			default: false
		},
		// 协议类型（用于动态适配界面）
		protocolType: {
			type: String,
			default: 'MODBUS_TCP'
		}
	})

	const emit = defineEmits(['save', 'refresh', 'modeChange', 'update:mappingList', 'update:useDeviceMapping'])

	// ========== 协议配置模板相关 ==========
	const addressTemplate = ref(null) // 地址配置模板
	const templateLoading = ref(false)

	// 加载协议地址配置模板
	const loadAddressTemplate = async () => {
		if (!props.protocolType) {
			console.warn('协议类型为空，跳过加载配置模板')
			return
		}

		templateLoading.value = true
		try {
			const response = await iotProtocolApi.iotProtocolAddressTemplate(props.protocolType)

			// 检查响应格式，处理 CommonResult 包装
			if (response && response.data) {
				addressTemplate.value = response.data
			} else if (response) {
				// 直接返回数据（无包装）
				addressTemplate.value = response
			}
		} catch (error) {
			console.error('加载地址配置模板失败:', error)
			console.error('错误详情:', error.response || error.message)
			message.warning(`协议 ${props.protocolType} 不支持地址配置或加载失败`)
		} finally {
			templateLoading.value = false
		}
	}

	// 监听协议类型变化，重新加载模板
	watch(
		() => props.protocolType,
		(newType) => {
			if (newType) {
				loadAddressTemplate()
			}
		},
		{ immediate: true }
	)

	// 设备级/产品级模式切换
	const useMappingModeLocal = ref(props.useDeviceMapping)

	// 监听模式切换prop变化
	watch(
		() => props.useDeviceMapping,
		(newVal) => {
			useMappingModeLocal.value = newVal
		}
	)

	// 模式切换事件
	const onMappingModeChange = () => {
		emit('update:useDeviceMapping', useMappingModeLocal.value)
		emit('modeChange', useMappingModeLocal.value)
	}

	// 分页配置
	const pagination = ref({
		current: 1,
		pageSize: 20,
		total: 0,
		showSizeChanger: true,
		showQuickJumper: true,
		pageSizeOptions: ['10', '20', '50', '100'],
		showTotal: (total) => `共 ${total} 条`
	})

	// 监听映射列表变化,更新分页总数
	watch(
		() => props.mappingList,
		(newList) => {
			pagination.value.total = newList?.length || 0
		},
		{ immediate: true, deep: true }
	)

	// 高级配置弹窗
	const advancedConfigVisible = ref(false)
	const currentConfigItem = ref({})

	// 注：已移除 isS7Protocol，不再需要协议特定的判断逻辑

	// 动态表格列定义（根据模板生成）
	const columns = computed(() => {
		const baseColumns = [
			{ title: '属性名称', dataIndex: 'name', width: 200 },
			{ title: '属性标识符', dataIndex: 'identifier', width: 150 },
			{ title: '地址', dataIndex: 'registerAddress', width: 200 }
		]

		// 根据模板添加配置字段列
		if (addressTemplate.value?.fields) {
			addressTemplate.value.fields.forEach((field) => {
				// 跳过在地址列中已处理的字段
				if (field.name !== 'registerAddress') {
					baseColumns.push({
						title: field.label,
						dataIndex: field.name,
						width: 180
					})
				}
			})
		}

		baseColumns.push(
			{ title: '启用', dataIndex: 'enabled', width: 80, align: 'center' },
			{ title: '操作', dataIndex: 'action', width: 90, align: 'center' }
		)

		return baseColumns
	})

	// 格式化字段选项（适配 Ant Design Vue 的 Select 组件）
	const formatFieldOptions = (options) => {
		if (!options) return []
		return options.map((opt) => ({
			label: opt.label,
			value: opt.value
		}))
	}

	// ========== 通用地址构建器 ==========
	const addressBuilderVisible = ref(false)
	const currentRecord = ref({})
	const builderFormData = ref({})

	// 打开地址构建器
	const openAddressBuilder = (record) => {
		currentRecord.value = record

		// 初始化表单数据（优先使用record中已保存的值，如果不存在才使用默认值）
		builderFormData.value = {}
		addressTemplate.value?.fields?.forEach((field) => {
			// 优先使用record中的值，即使是0、false等falsy值也应该保留
			if (record[field.name] !== undefined && record[field.name] !== null) {
				builderFormData.value[field.name] = record[field.name]
			} else {
				builderFormData.value[field.name] = field.defaultValue
			}
		})

		addressBuilderVisible.value = true
	}

	// 判断字段是否应该显示（根据 showWhen 条件）
	const shouldShowField = (field) => {
		if (!field.showWhen) return true

		// 解析 showWhen 条件（如 "area=DB"）
		const [fieldName, expectedValue] = field.showWhen.split('=')
		return builderFormData.value[fieldName] === expectedValue
	}

	// 生成地址预览（通用化：根据 addressTemplate 提供的 formatter 或规则）
	const addressPreview = computed(() => {
		if (!addressTemplate.value) {
			return ''
		}

		// 如果模板提供了自定义格式化函数（未来扩展）
		if (addressTemplate.value.formatter) {
			return addressTemplate.value.formatter(builderFormData.value)
		}

		// 默认地址生成逻辑：根据协议类型动态生成
		const protocolType = props.protocolType?.toUpperCase()

		// S7协议的默认地址生成（保持兼容）
		if (protocolType === 'S7' || protocolType === 'TCP') {
			const { area, dbNumber, dataTypePrefix, offset, bitIndex } = builderFormData.value
			let address = ''

			if (area === 'DB') {
				address = `DB${dbNumber || 1}.DB${dataTypePrefix || 'W'}${offset || 0}`
			} else {
				address = `${area || 'M'}${dataTypePrefix || 'W'}${offset || 0}`
			}

			if (dataTypePrefix === 'X' && bitIndex != null) {
				address += `.${bitIndex}`
			}

			return address
		}

		// BACNET等其他协议：根据字段值自动拼接
		// 例如: BACNET 的 "ANALOG_INPUT:1" 格式
		if (protocolType === 'BACNET') {
			const { objectType, instance } = builderFormData.value
			return objectType && instance != null ? `${objectType}:${instance}` : ''
		}

		// 通用默认：返回空（对于SIMPLE模式的协议不需要预览）
		return ''
	})

	// 保存地址
	const saveAddress = () => {
		// 更新当前记录的字段值
		Object.keys(builderFormData.value).forEach((key) => {
			currentRecord.value[key] = builderFormData.value[key]
		})

		// 更新 identifier 字段（用于 BUILDER 模式）
		// 注意：这里仅用于显示，不应覆盖物模型属性标识符
		if (addressTemplate.value?.inputMode === 'BUILDER') {
			// 如果当前记录没有 originalIdentifier，先保存原始标识符
			if (!currentRecord.value.originalIdentifier) {
				currentRecord.value.originalIdentifier = currentRecord.value.identifier
			}
			// 将构建的地址保存为显示用的标识符
			// 但在保存时会恢复 originalIdentifier
			currentRecord.value.displayAddress = addressPreview.value
		}

		// 同时更新 registerAddress 字段，用于后端保存
		currentRecord.value.registerAddress = addressPreview.value

		addressBuilderVisible.value = false
		message.success('地址配置已更新')
	}

	// Modbus功能码选项
	const functionCodeOptions = [
		{ label: '0x01 - 读线圈', value: '0x01' },
		{ label: '0x02 - 读离散输入', value: '0x02' },
		{ label: '0x03 - 读保持寄存器', value: '0x03' },
		{ label: '0x04 - 读输入寄存器', value: '0x04' },
		{ label: '0x05 - 写单个线圈', value: '0x05' },
		{ label: '0x06 - 写单个寄存器', value: '0x06' },
		{ label: '0x0F - 写多个线圈', value: '0x0F' },
		{ label: '0x10 - 写多个寄存器', value: '0x10' }
	]

	// 字节序选项
	const byteOrderOptions = [
		{ label: '大端序 (BIG_ENDIAN)', value: 'BIG_ENDIAN' },
		{ label: '小端序 (LITTLE_ENDIAN)', value: 'LITTLE_ENDIAN' }
	]

	// 功能码排序优先级
	const functionCodeOrder = {
		'0x02': 1,
		'0x03': 2,
		'0x01': 3,
		'0x04': 4,
		'0x05': 5,
		'0x06': 6,
		'0x0F': 7,
		'0x10': 8
	}

	// 排序后的映射列表
	const sortedMappingList = computed(() => {
		if (!props.mappingList || props.mappingList.length === 0) {
			return []
		}

		return [...props.mappingList].sort((a, b) => {
			const orderA = functionCodeOrder[a.functionCode] || 999
			const orderB = functionCodeOrder[b.functionCode] || 999

			if (orderA !== orderB) {
				return orderA - orderB
			}

			const addrA = a.registerAddress ?? 999999
			const addrB = b.registerAddress ?? 999999
			return addrA - addrB
		})
	})

	// 打开高级配置
	const openAdvancedConfig = (record) => {
		currentConfigItem.value = { ...record }
		advancedConfigVisible.value = true
	}

	// 保存高级配置
	const saveAdvancedConfig = () => {
		const index = props.mappingList.findIndex((item) => item.identifier === currentConfigItem.value.identifier)
		if (index !== -1) {
			const newList = [...props.mappingList]
			newList[index] = { ...currentConfigItem.value }
			emit('update:mappingList', newList)
		}
		advancedConfigVisible.value = false
	}

	// 保存映射配置
	const handleSave = () => {
		emit('save')
	}

	// 刷新
	const handleRefresh = () => {
		emit('refresh')
	}
</script>

<style scoped>
	.register-mapping-config {
		width: 100%;
	}
</style>
