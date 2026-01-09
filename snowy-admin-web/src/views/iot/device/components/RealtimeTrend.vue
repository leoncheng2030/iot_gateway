<template>
	<div ref="chartContainer" style="height: 500px"></div>
</template>

<script setup>
	import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
	import * as echarts from 'echarts'

	const props = defineProps({
		thingModelProperties: {
			type: Array,
			default: () => []
		},
		realtimeChartData: {
			type: Array,
			default: () => []
		}
	})

	const chartContainer = ref(null)
	let chartInstance = null

	// 获取单位
	const getValueUnit = (valueSpecs) => {
		if (!valueSpecs) return ''
		try {
			const specs = JSON.parse(valueSpecs)
			return specs.unit || ''
		} catch (e) {
			return ''
		}
	}

	// 渲染实时趋势图表
	const renderRealtimeChart = () => {
		if (!chartContainer.value) {
			console.log('⚠️ 图表容器未初始化')
			return
		}

		if (props.thingModelProperties.length === 0) {
			console.log('⚠️ 物模型未加载')
			return
		}

		// 初始化图表实例
		if (!chartInstance) {
			chartInstance = echarts.init(chartContainer.value)
			console.log('✅ 图表实例已初始化')
		}

		if (!props.realtimeChartData || props.realtimeChartData.length === 0) {
			chartInstance.clear()
			chartInstance.setOption({
				title: {
					text: '等待设备数据上报...',
					left: 'center',
					top: 'center',
					textStyle: {
						color: '#999',
						fontSize: 18
					}
				}
			})
			return
		}

		console.log('🎨 开始渲染实时图表，数据点数:', props.realtimeChartData.length)

		// 提取时间轴数据
		const timeLabels = props.realtimeChartData.map((item) => item.time)

		// 为每个属性准备数据系列
		const series = []
		const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16']

		props.thingModelProperties.forEach((property, index) => {
			const identifier = property.identifier
			const propertyName = property.name
			const unit = property.valueSpecs ? getValueUnit(property.valueSpecs) : ''

			// 从分组数据中提取该属性的值
			const values = props.realtimeChartData.map((item) => {
				const properties = item.properties || {}
				const value = properties[identifier]
				// 如果没有值，返回null
				return value !== undefined && value !== null ? parseFloat(value) : null
			})

			// 检查是否有有效数据
			const hasData = values.some((v) => v !== null)
			if (!hasData) return // 没有数据的属性跳过
			series.push({
				name: unit ? `${propertyName} (${unit})` : propertyName,
				type: 'line',
				smooth: true,
				data: values,
				connectNulls: true, // 连接空值
				lineStyle: {
					width: 2
				},
				itemStyle: {
					color: colors[index % colors.length]
				},
				symbol: 'circle',
				symbolSize: 6
			})
		})

		// 如果没有任何数据系列，显示空状态
		if (series.length === 0) {
			console.log('⚠️ 没有数据系列，显示空状态')
			chartInstance.clear()
			chartInstance.setOption({
				title: {
					text: '等待设备数据上报...',
					left: 'center',
					top: 'center',
					textStyle: {
						color: '#999',
						fontSize: 18
					}
				}
			})
			return
		}

		console.log('✅ 生成', series.length, '个数据系列')

		// 配置图表选项
		const option = {
			title: {
				text: '设备实时数据趋势',
				left: 'center'
			},
			tooltip: {
				trigger: 'axis',
				axisPointer: {
					type: 'cross'
				}
			},
			legend: {
				data: series.map((s) => s.name),
				top: 30,
				type: 'scroll' // 图例过多时可滚动
			},
			grid: {
				left: '3%',
				right: '4%',
				bottom: '15%',
				top: '15%',
				containLabel: true
			},
			xAxis: {
				type: 'category',
				data: timeLabels,
				axisLabel: {
					rotate: 45,
					interval: Math.floor(timeLabels.length / 15) || 0
				}
			},
			yAxis: {
				type: 'value',
				name: '数值'
			},
			series: series,
			dataZoom: [
				{
					type: 'inside',
					start: 0,
					end: 100
				},
				{
					start: 0,
					end: 100,
					height: 20
				}
			]
		}

		chartInstance.setOption(option, true) // true表示不合并，完全替换
		console.log('🎉 实时图表渲染完成!')
	}

	// 监听数据变化，重新渲染图表
	watch(
		() => props.realtimeChartData,
		() => {
			renderRealtimeChart()
		},
		{ deep: true }
	)

	// 监听物模型变化
	watch(
		() => props.thingModelProperties,
		() => {
			renderRealtimeChart()
		},
		{ deep: true }
	)

	// 组件挂载时初始化图表
	onMounted(() => {
		renderRealtimeChart()
	})

	// 组件卸载时销毁图表实例
	onBeforeUnmount(() => {
		if (chartInstance) {
			chartInstance.dispose()
			chartInstance = null
		}
	})

	// 暴露重新渲染方法
	defineExpose({
		renderRealtimeChart
	})
</script>
