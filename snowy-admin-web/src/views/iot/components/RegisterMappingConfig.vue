<template>
	<div class="register-mapping-config">
		<!-- 模式切换区域（仅设备级显示） -->
		<a-card v-if="showModeSwitch" size="small" style="margin-bottom: 16px">
			<a-space>
				<a-switch v-model:checked="useMappingModeLocal" @change="onMappingModeChange" />
				<span>使用设备级配置（当前：{{ useMappingModeLocal ? '设备级' : '产品级' }}）</span>
				<a-button v-if="useMappingModeLocal" type="primary" size="small" @click="handleSave"> 保存设备级配置 </a-button>
				<a-button v-if="useMappingModeLocal" danger size="small" @click="handleDelete"> 清除设备级配置 </a-button>
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
				<template v-if="column.dataIndex === 'registerAddress'">
					<a-input-number
						v-model:value="record.registerAddress"
						:min="0"
						:max="65535"
						style="width: 90px"
						placeholder="地址"
					/>
				</template>
				<template v-if="column.dataIndex === 'functionCode'">
					<a-select
						v-model:value="record.functionCode"
						style="width: 170px"
						:options="functionCodeOptions"
						placeholder="选择功能码"
					/>
				</template>
				<template v-if="column.dataIndex === 'dataType'">
					<a-select
						v-model:value="record.dataType"
						style="width: 150px"
						:options="dataTypeOptions"
						placeholder="数据类型"
					/>
				</template>
				<template v-if="column.dataIndex === 'enabled'">
					<a-switch v-model:checked="record.enabled" />
				</template>
				<template v-if="column.dataIndex === 'action'">
					<a @click="openAdvancedConfig(record)">配置</a>
				</template>
			</template>
		</a-table>

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
	import { ref, computed, watch } from 'vue'
	import { SaveOutlined, ReloadOutlined } from '@ant-design/icons-vue'

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
		}
	})

	const emit = defineEmits(['save', 'refresh', 'delete', 'modeChange', 'update:mappingList', 'update:useDeviceMapping'])

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
		{ immediate: true }
	)

	// 高级配置弹窗
	const advancedConfigVisible = ref(false)
	const currentConfigItem = ref({})

	// 表格列定义
	const columns = [
		{ title: '属性名称', dataIndex: 'name', width: 180 },
		{ title: '寄存器地址', dataIndex: 'registerAddress', width: 100 },
		{ title: '功能码', dataIndex: 'functionCode', width: 200 },
		{ title: '数据类型', dataIndex: 'dataType', width: 150 },
		{ title: '启用', dataIndex: 'enabled', width: 80, align: 'center' },
		{ title: '操作', dataIndex: 'action', width: 90, align: 'center' }
	]

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

	// 数据类型选项
	const dataTypeOptions = [
		{ label: '开关量 (bool)', value: 'bool' },
		{ label: '整数 (int)', value: 'int' },
		{ label: '浮点数 (float)', value: 'float' }
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

	// 删除设备级配置
	const handleDelete = () => {
		emit('delete')
	}
</script>

<style scoped>
	.register-mapping-config {
		width: 100%;
	}
</style>
