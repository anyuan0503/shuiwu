// ECharts 统一科技暗色主题工具
// 注意：echarts 实例与主题注册统一在 ./echarts.js 中按需配置，此处仅导出常量与通用配置
const PRIMARY = '#00e5ff'
const GREEN = '#00ffa3'
const GOLD = '#ffd75e'
const PURPLE = '#8b7bff'
const DANGER = '#ff4d6d'
const SUB = '#8fa8cf'
const AXIS = '#2a3a66'

// 深色面板 tooltip 样式
export const darkTooltip = (formatter) => ({
  trigger: 'axis',
  backgroundColor: 'rgba(10,17,44,0.92)',
  borderColor: 'rgba(0,229,255,0.4)',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#dceaff', fontSize: 12 },
  axisPointer: { type: 'line', lineStyle: { color: 'rgba(0,229,255,0.5)' } },
  formatter
})

export const axisCommon = () => ({
  axisLine: { lineStyle: { color: AXIS } },
  axisTick: { show: false },
  axisLabel: { color: SUB, fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(42,58,102,0.35)', type: 'dashed' } },
  splitArea: { show: false }
})

export function registerTheme() {
  // 主题已在 ./echarts.js 中统一注册，保留空实现兼容旧调用
}

export { PRIMARY, GREEN, GOLD, PURPLE, DANGER, SUB }