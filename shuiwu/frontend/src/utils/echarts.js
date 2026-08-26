// ECharts 按需引入配置：只注册项目用到的图表类型与组件，大幅减小打包体积
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, GaugeChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  GaugeChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer
])

// 统一注册科技暗色主题（仅注册一次）
echarts.registerTheme('tech', {
  color: ['#00e5ff', '#00ffa3', '#ffd75e', '#8b7bff', '#ff4d6d', '#4deeff', '#ffb3d9'],
  backgroundColor: 'transparent',
  textStyle: { color: '#8fa8cf', fontSize: 12 },
  title: { textStyle: { color: '#dceaff' } },
  legend: {
    textStyle: { color: '#8fa8cf' },
    pageTextStyle: { color: '#8fa8cf' },
    pageIconColor: '#00e5ff',
    pageIconInactiveColor: '#3a4a7a',
    itemWidth: 12,
    itemHeight: 8
  },
  grid: { show: false }
})

export default echarts
