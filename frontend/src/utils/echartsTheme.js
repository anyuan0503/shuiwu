// ECharts 统一科技暗色主题工具
import * as echarts from 'echarts'

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
  try {
    echarts.registerTheme('tech', {
      color: [PRIMARY, GREEN, GOLD, PURPLE, DANGER, '#4deeff', '#ffb3d9'],
      backgroundColor: 'transparent',
      textStyle: { color: SUB, fontSize: 12 },
      title: { textStyle: { color: '#dceaff' } },
      legend: {
        textStyle: { color: SUB },
        pageTextStyle: { color: SUB },
        pageIconColor: PRIMARY,
        pageIconInactiveColor: '#3a4a7a',
        itemWidth: 12,
        itemHeight: 8
      },
      grid: { show: false }
    })
  } catch (e) {
    /* ignore */
  }
}

export { PRIMARY, GREEN, GOLD, PURPLE, DANGER, SUB }