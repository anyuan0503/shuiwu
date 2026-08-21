<template>
  <div ref="el" class="base-chart" :style="{ height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { registerTheme } from '@/utils/echartsTheme'

registerTheme()

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '100%' },
  notMerge: { type: Boolean, default: true }
})

const el = ref(null)
let chart = null
let observer = null

function render() {
  if (!el.value) return
  if (!chart) {
    chart = echarts.init(el.value, 'tech')
  }
  chart.setOption(props.option, { notMerge: props.notMerge })
}

onMounted(() => {
  nextTick(() => {
    render()
    observer = new ResizeObserver(() => chart && chart.resize())
    observer.observe(el.value)
  })
})

watch(() => props.option, () => render(), { deep: true })

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.base-chart {
  width: 100%;
}
</style>