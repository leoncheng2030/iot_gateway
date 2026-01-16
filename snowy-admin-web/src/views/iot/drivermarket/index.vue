<template>
	<a-card :bordered="false">
		<!-- 搜索区域 -->
		<a-form ref="searchFormRef" :model="searchFormState" layout="inline" class="mb-4">
			<a-form-item label="关键字">
				<a-input v-model:value="searchFormState.searchKey" placeholder="驱动名称" allow-clear style="width: 180px" />
			</a-form-item>
			<a-form-item label="来源">
				<a-select v-model:value="searchFormState.sourceType" placeholder="全部" allow-clear style="width: 120px">
					<a-select-option value="BUILTIN">内置驱动</a-select-option>
					<a-select-option value="UPLOADED">上传驱动</a-select-option>
				</a-select>
			</a-form-item>
			<a-form-item label="状态">
				<a-select v-model:value="searchFormState.installStatus" placeholder="全部" allow-clear style="width: 120px">
					<a-select-option value="UNINSTALLED">未安装</a-select-option>
					<a-select-option value="INSTALLED">已安装</a-select-option>
					<a-select-option value="ENABLED">已启用</a-select-option>
					<a-select-option value="DISABLED">已禁用</a-select-option>
				</a-select>
			</a-form-item>
			<a-form-item label="分类">
				<a-select v-model:value="searchFormState.category" placeholder="全部" allow-clear style="width: 120px">
					<a-select-option value="INDUSTRIAL">工业协议</a-select-option>
					<a-select-option value="NETWORK">网络协议</a-select-option>
					<a-select-option value="GATEWAY">网关驱动</a-select-option>
					<a-select-option value="OTHER">其他</a-select-option>
				</a-select>
			</a-form-item>
			<a-form-item>
				<a-space>
					<a-button type="primary" @click="loadData">
						<template #icon><SearchOutlined /></template>查询
					</a-button>
					<a-button @click="resetSearch">
						<template #icon><RedoOutlined /></template>重置
					</a-button>
				</a-space>
			</a-form-item>
			<a-form-item style="margin-left: auto">
				<a-space>
					<a-button type="primary" @click="uploadModalRef.onOpen()">
						<template #icon><UploadOutlined /></template>上传驱动
					</a-button>
					<a-button @click="syncDrivers">
						<template #icon><SyncOutlined /></template>同步
					</a-button>
				</a-space>
			</a-form-item>
		</a-form>

		<!-- 驱动卡片列表 -->
		<a-spin :spinning="loading">
			<a-empty v-if="driverList.length === 0" description="暂无驱动" />
			<a-row :gutter="[16, 16]" v-else>
				<a-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-for="driver in driverList" :key="driver.id">
					<a-card hoverable class="driver-card" @click="detailRef.onOpen(driver)">
						<template #cover>
							<div class="driver-icon">
								<ApiOutlined v-if="driver.category === 'INDUSTRIAL'" />
								<CloudOutlined v-else-if="driver.category === 'NETWORK'" />
								<GatewayOutlined v-else-if="driver.category === 'GATEWAY'" />
								<AppstoreOutlined v-else />
							</div>
						</template>
						<a-card-meta>
							<template #title>
								<div class="driver-title">
									{{ driver.driverName }}
									<a-tag v-if="driver.sourceType === 'BUILTIN'" color="blue" size="small">内置</a-tag>
									<a-tag v-else color="green" size="small">上传</a-tag>
								</div>
							</template>
							<template #description>
								<div class="driver-desc">
									<div class="driver-type">{{ driver.driverType }}</div>
									<div class="driver-version">v{{ driver.driverVersion || '1.0.0' }}</div>
								</div>
							</template>
						</a-card-meta>
						<template #actions>
							<!-- 未安装 -->
							<template v-if="driver.installStatus === 'UNINSTALLED'">
								<a-button type="link" size="small" @click.stop="installDriver(driver)">
									<DownloadOutlined /> 安装
								</a-button>
							</template>
							<!-- 已安装 -->
							<template v-else-if="driver.installStatus === 'INSTALLED'">
								<a-button type="link" size="small" @click.stop="enableDriver(driver)">
									<CheckCircleOutlined /> 启用
								</a-button>
								<a-popconfirm title="确定卸载该驱动？" @confirm="uninstallDriver(driver)" @click.stop>
									<a-button type="link" size="small" danger>
										<DeleteOutlined /> 卸载
									</a-button>
								</a-popconfirm>
							</template>
							<!-- 已启用 -->
							<template v-else-if="driver.installStatus === 'ENABLED'">
								<a-button type="link" size="small" @click.stop="disableDriver(driver)">
									<PauseCircleOutlined /> 禁用
								</a-button>
								<a-popconfirm title="确定卸载该驱动？" @confirm="uninstallDriver(driver)" @click.stop>
									<a-button type="link" size="small" danger>
										<DeleteOutlined /> 卸载
									</a-button>
								</a-popconfirm>
							</template>
							<!-- 已禁用 -->
							<template v-else-if="driver.installStatus === 'DISABLED'">
								<a-button type="link" size="small" @click.stop="enableDriver(driver)">
									<CheckCircleOutlined /> 启用
								</a-button>
								<a-popconfirm title="确定卸载该驱动？" @confirm="uninstallDriver(driver)" @click.stop>
									<a-button type="link" size="small" danger>
										<DeleteOutlined /> 卸载
									</a-button>
								</a-popconfirm>
							</template>
						</template>
						<!-- 状态标签 -->
						<div class="driver-status">
							<a-badge v-if="driver.installStatus === 'ENABLED'" status="success" text="已启用" />
							<a-badge v-else-if="driver.installStatus === 'INSTALLED'" status="processing" text="已安装" />
							<a-badge v-else-if="driver.installStatus === 'DISABLED'" status="warning" text="已禁用" />
							<a-badge v-else status="default" text="未安装" />
						</div>
					</a-card>
				</a-col>
			</a-row>
		</a-spin>

		<!-- 分页 -->
		<div class="pagination-wrapper" v-if="total > 0">
			<a-pagination
				v-model:current="searchFormState.current"
				v-model:pageSize="searchFormState.size"
				:total="total"
				show-size-changer
				show-quick-jumper
				:show-total="(total) => `共 ${total} 个驱动`"
				@change="loadData"
			/>
		</div>

		<!-- 详情抽屉 -->
		<detail ref="detailRef" />

		<!-- 上传弹窗 -->
		<upload-modal ref="uploadModalRef" @success="loadData" />
	</a-card>
</template>

<script setup name="iotDrivermarket">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
	SearchOutlined,
	RedoOutlined,
	UploadOutlined,
	SyncOutlined,
	ApiOutlined,
	CloudOutlined,
	GatewayOutlined,
	AppstoreOutlined,
	DownloadOutlined,
	CheckCircleOutlined,
	PauseCircleOutlined,
	DeleteOutlined
} from '@ant-design/icons-vue'
import iotDriverMarketApi from '@/api/iot/iotDriverMarketApi'
import detail from './detail.vue'
import uploadModal from './uploadModal.vue'

const searchFormRef = ref()
const detailRef = ref()
const uploadModalRef = ref()
const loading = ref(false)
const driverList = ref([])
const total = ref(0)

const searchFormState = reactive({
	current: 1,
	size: 12,
	searchKey: '',
	sourceType: undefined,
	installStatus: undefined,
	category: undefined
})

// 加载数据
const loadData = async () => {
	loading.value = true
	try {
		const res = await iotDriverMarketApi.iotDriverMarketPage(searchFormState)
		driverList.value = res.records || []
		total.value = res.total || 0
	} catch (e) {
		console.error(e)
	} finally {
		loading.value = false
	}
}

// 重置搜索
const resetSearch = () => {
	searchFormState.searchKey = ''
	searchFormState.sourceType = undefined
	searchFormState.installStatus = undefined
	searchFormState.category = undefined
	searchFormState.current = 1
	loadData()
}

// 同步内置驱动
const syncDrivers = async () => {
	try {
		await iotDriverMarketApi.iotDriverMarketSync()
		message.success('同步成功')
		loadData()
	} catch (e) {
		console.error(e)
	}
}

// 安装驱动
const installDriver = async (driver) => {
	try {
		await iotDriverMarketApi.iotDriverMarketInstall({ driverType: driver.driverType })
		message.success('安装成功')
		loadData()
	} catch (e) {
		console.error(e)
	}
}

// 卸载驱动
const uninstallDriver = async (driver) => {
	try {
		await iotDriverMarketApi.iotDriverMarketUninstall({ driverType: driver.driverType })
		message.success('卸载成功')
		loadData()
	} catch (e) {
		console.error(e)
	}
}

// 启用驱动
const enableDriver = async (driver) => {
	try {
		await iotDriverMarketApi.iotDriverMarketEnable({ driverType: driver.driverType })
		message.success('启用成功')
		loadData()
	} catch (e) {
		console.error(e)
	}
}

// 禁用驱动
const disableDriver = async (driver) => {
	try {
		await iotDriverMarketApi.iotDriverMarketDisable({ driverType: driver.driverType })
		message.success('禁用成功')
		loadData()
	} catch (e) {
		console.error(e)
	}
}

onMounted(() => {
	loadData()
})
</script>

<style scoped lang="less">
.driver-card {
	position: relative;
	height: 100%;

	.driver-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 80px;
		font-size: 40px;
		color: #1890ff;
		background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
	}

	.driver-title {
		display: flex;
		align-items: center;
		gap: 8px;
		font-size: 14px;
	}

	.driver-desc {
		.driver-type {
			font-size: 12px;
			color: #666;
			margin-bottom: 4px;
		}
		.driver-version {
			font-size: 12px;
			color: #999;
		}
	}

	.driver-status {
		position: absolute;
		top: 8px;
		right: 8px;
	}
}

.pagination-wrapper {
	display: flex;
	justify-content: flex-end;
	margin-top: 16px;
}

.mb-4 {
	margin-bottom: 16px;
}
</style>
