<template>
	<a-card title="数据采集趋势" :bordered="false" class="data-trend-card">
		<template #extra>
			<a-radio-group v-model:value="timeRange" button-style="solid" size="small" @change="handleTimeRangeChange">
				<a-radio-button value="1h">1小时</a-radio-button>
				<a-radio-button value="6h">6小时</a-radio-button>
				<a-radio-button value="24h">24小时</a-radio-button>
			</a-radio-group>
		</template>

		<div ref="chartRef" class="chart-container"></div>

		<a-row :gutter="16" class="chart-stats">
			<a-col :span="8">
				<div class="stat-item">
					<div class="stat-label">平均采集速率</div>
					<div class="stat-value">{{ avgRate }}<span class="stat-unit">条/秒</span></div>
				</div>
			</a-col>
			<a-col :span="8">
				<div class="stat-item">
					<div class="stat-label">通信成功率</div>
					<div class="stat-value success">{{ successRate }}<span class="stat-unit">%</span></div>
				</div>
			</a-col>
			<a-col :span="8">
				<div class="stat-item">
					<div class="stat-label">平均延迟</div>
					<div class="stat-value">{{ avgDelay }}<span class="stat-unit">ms</span></div>
				</div>
			</a-col>
		</a-row>
	</a-card>
</template>

<script setup>
	import { ref, onMounted, onUnmounted, nextTick } from 'vue'
	import * as echarts from 'echarts'
	import { message } from 'ant-design-vue'
	import iotStatisticsApi from '@/api/iot/iotStatisticsApi'

	const chartRef = ref(null)
	let chartInstance = null
	const timeRange = ref('1h')

	const avgRate = ref(156)
	const successRate = ref(98.5)
	const avgDelay = ref(45)

	// 初始化图表
	const initChart = () => {
		if (!chartRef.value) return

		chartInstance = echarts.init(chartRef.value)

		const option = {
			tooltip: {
				trigger: 'axis',
				axisPointer: {
					type: 'cross',
					label: {
						backgroundColor: '#6a7985'
					}
				}
			},
			legend: {
				data: ['采集速率', '成功率'],
				bottom: 0
			},
			grid: {
				left: '3%',
				right: '4%',
				bottom: '15%',
				top: '10%',
				containLabel: true
			},
			xAxis: {
				type: 'category',
				boundaryGap: false,
				data: generateTimeLabels()
			},
			yAxis: [
				{
					type: 'value',
					name: '速率(条/秒)',
					position: 'left',
					axisLine: {
						show: true,
						lineStyle: {
							color: '#1890ff'
						}
					},
					axisLabel: {
						formatter: '{value}'
					}
				},
				{
					type: 'value',
					name: '成功率(%)',
					position: 'right',
					min: 0,
					max: 100,
					axisLine: {
						show: true,
						lineStyle: {
							color: '#52c41a'
						}
					},
					axisLabel: {
						formatter: '{value}%'
					}
				}
			],
			series: [
				{
					name: '采集速率',
					type: 'line',
					smooth: true,
					data: generateRateData(),
					itemStyle: {
						color: '#1890ff'
					},
					areaStyle: {
						color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
							{
								offset: 0,
								color: 'rgba(24, 144, 255, 0.3)'
							},
							{
								offset: 1,
								color: 'rgba(24, 144, 255, 0.05)'
							}
						])
					}
				},
				{
					name: '成功率',
					type: 'line',
					smooth: true,
					yAxisIndex: 1,
					data: generateSuccessRateData(),
					itemStyle: {
						color: '#52c41a'
					}
				}
			]
		}

		chartInstance.setOption(option)
	}

	// 生成时间标签
	const generateTimeLabels = () => {
		const labels = []
		const count = timeRange.value === '1h' ? 12 : timeRange.value === '6h' ? 12 : 24
		const interval = timeRange.value === '1h' ? 5 : timeRange.value === '6h' ? 30 : 60

		for (let i = count; i > 0; i--) {
			const time = new Date(Date.now() - i * interval * 60 * 1000)
			labels.push(time.getHours().toString().padStart(2, '0') + ':' + time.getMinutes().toString().padStart(2, '0'))
		}
		return labels
	}

	// 生成采集速率数据
	const generateRateData = () => {
		const count = timeRange.value === '1h' ? 12 : timeRange.value === '6h' ? 12 : 24
		return Array.from({ length: count }, () => Math.floor(Math.random() * 100) + 100)
	}

	// 生成成功率数据
	const generateSuccessRateData = () => {
		const count = timeRange.value === '1h' ? 12 : timeRange.value === '6h' ? 12 : 24
		return Array.from({ length: count }, () => Math.floor(Math.random() * 5) + 95)
	}

	// 时间范围变化
	const handleTimeRangeChange = () => {
		updateChart()
	}

	// 更新图表
	const updateChart = () => {
		if (!chartInstance) return

		chartInstance.setOption({
			xAxis: {
				data: generateTimeLabels()
			},
			series: [
				{
					data: generateRateData()
				},
				{
					data: generateSuccessRateData()
				}
			]
		})
	}

	let refreshTimer = null

	// 获取数据
	const fetchData = async () => {
		try {
			const res = await iotStatisticsApi.getDataTrend({ timeRange: timeRange.value })
			if (res) {
				// 更新统计数据
				avgRate.value = res.avgRate || 0
				successRate.value = res.successRate || 0
				avgDelay.value = res.avgDelay || 0

				// 更新图表
				if (res.chartData && chartInstance) {
					chartInstance.setOption({
						xAxis: {
							data: res.chartData.timeLabels || []
						},
						series: [
							{
								data: res.chartData.rateData || []
							},
							{
								data: res.chartData.successRateData || []
							}
						]
					})
				}
			}
		} catch (error) {
			console.error('获取数据趋势失败:', error)
			message.error('获取数据趋势失败')
		}
	}

	// 窗口大小变化
	const handleResize = () => {
		chartInstance?.resize()
	}

	onMounted(() => {
		nextTick(() => {
			initChart()
			fetchData()
			window.addEventListener('resize', handleResize)

			// 每30秒刷新一次
			refreshTimer = setInterval(() => {
				fetchData()
			}, 30000)
		})
	})

	onUnmounted(() => {
		window.removeEventListener('resize', handleResize)
		chartInstance?.dispose()
		if (refreshTimer) {
			clearInterval(refreshTimer)
			refreshTimer = null
		}
	})
</script>

<style scoped lang="less">
	.data-trend-card {
		:deep(.ant-card-body) {
			padding: 20px;
		}
	}

	.chart-container {
		height: 300px;
		width: 100%;
	}

	.chart-stats {
		margin-top: 16px;
		padding-top: 16px;
		border-top: 1px solid #f0f0f0;
	}

	.stat-item {
		text-align: center;

		.stat-label {
			font-size: 14px;
			color: rgba(0, 0, 0, 0.45);
			margin-bottom: 8px;
		}

		.stat-value {
			font-size: 24px;
			font-weight: 600;
			color: #1890ff;

			&.success {
				color: #52c41a;
			}

			.stat-unit {
				font-size: 14px;
				font-weight: 400;
				margin-left: 4px;
				color: rgba(0, 0, 0, 0.45);
			}
		}
	}
</style>
