<template>
	<div class="iot-gateway-biz-home">
		<!-- IoT边缘网关核心统计 -->
		<iot-statistics-card class="mb-3" />

		<a-row :gutter="10" class="mb-2">
			<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
				<!-- 驱动运行状态 -->
				<iot-driver-status-card />
			</a-col>
			<a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
				<!-- 实时告警列表 -->
				<iot-alarm-list-card />
			</a-col>
		</a-row>

		<a-row :gutter="10" class="mb-2">
			<a-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
				<!-- 数据采集趋势图 -->
				<iot-data-trend-chart />
			</a-col>
			<a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
				<!-- 系统资源监控 -->
				<iot-system-resource-card />
			</a-col>
		</a-row>
	</div>
</template>

<script setup name="bizIndex">
	import IotStatisticsCard from './components/IotStatisticsCard.vue'
	import IotDriverStatusCard from './components/IotDriverStatusCard.vue'
	import IotAlarmListCard from './components/IotAlarmListCard.vue'
	import IotDataTrendChart from './components/IotDataTrendChart.vue'
	import IotSystemResourceCard from './components/IotSystemResourceCard.vue'
</script>
