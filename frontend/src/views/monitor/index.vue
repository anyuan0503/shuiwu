<template>
  <div class="page-container monitor">
    <div class="conn-bar glass">
      <span class="conn-dot" :class="{ on: connected }"></span>
      <span class="conn-text" :class="{ on: connected }">
        {{ connected ? '实时数据已连接' : '连接中 / 重连中…' }}
      </span>
      <span class="conn-time num" v-if="lastUpdate">{{ lastUpdate }}</span>
      <div class="conn-right">
        <span class="field-label">监测指标：</span>
        <el-radio-group v-model="currentType" size="small">
          <el-radio-button value="pressure">压力</el-radio-button>
          <el-radio-button value="flow">流量</el-radio-button>
          <el-radio-button value="ph">pH</el-radio-button>
          <el-radio-button value="turbidity">浊度</el-radio-button>
          <el-radio-button value="residualCl">余氯</el-radio-button>
          <el-radio-button value="level">液位</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="monitor-body">
      <!-- 左：设备列表卡片 -->
      <div class="dev-list glass">
        <div class="dl-head">
          <PanelTitle>设备实时状态</PanelTitle>
        </div>
        <el-scrollbar class="dl-scroll">
          <div
            v-for="d in deviceCards"
            :key="d.deviceId"
            class="dev-item"
            :class="{ active: activeId === d.deviceId }"
            @click="select(d.deviceId)"
          >
            <span class="dot" :style="{ background: qualityColor(d.qualityStatus) }"></span>
            <div class="dev-info">
              <div class="dev-name">{{ d.deviceName }}</div>
              <div class="dev-meta">
                {{ typeName(d.deviceType) }} · {{ d.area || '—' }}
              </div>
            </div>
            <div class="dev-val num">
              <span :style="{ color: qualityColor(d.qualityStatus) }">
                {{ formatMetric(d, currentType) }}
              </span>
              <small>{{ unitOf(d.deviceType, currentType) }}</small>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 右：趋势大图 -->
      <div class="trend-area">
        <ChartCard :title="`实时趋势 · ${typeLabel(currentType)}`" :height="'100%'">
          <BaseChart :option="trendOption" height="520px" />
        </ChartCard>
      </div>
    </div>

    <!-- 底部整体指标卡片 -->
    <div class="metric-strip">
      <div
        v-for="m in metricStrip"
        :key="m.key"
        class="metric glass"
        :style="{ borderColor: m.color }"
      >
        <span class="ms-label">{{ m.label }}</span>
        <DatavNumber :value="m.value" :color="m.color" :size="24" />
        <span class="ms-unit">{{ m.unit }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import ChartCard from '@/components/ChartCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import DatavNumber from '@/components/DatavNumber.vue'
import PanelTitle from '@/components/PanelTitle.vue'
import connectWs from '@/utils/websocket'
import { realtime, trend } from '@/api/monitor'
import { axisCommon, darkTooltip } from '@/utils/echartsTheme'

const connected = ref(false)
const lastUpdate = ref('')
const activeId = ref(null)
const currentType = ref('pressure')
const deviceCards = ref([])
let samples = reactive({}) // deviceId -> { times:[], values:{} }
let ws = null

const typeName = (t) => ({ pressure: '压力计', flow: '流量计', quality: '水质仪', level: '液位计' }[t] || t)
const typeLabel = (t) => ({ pressure: '压力', flow: '流量', ph: 'pH', turbidity: '浊度', residualCl: '余氯', level: '液位' }[t] || t)
const unitOf = (deviceType, metric) => {
  if (metric === 'pressure') return 'MPa'
  if (metric === 'flow') return 'm³/h'
  if (deviceType === 'quality') return metric === 'turbidity' ? 'NTU' : metric === 'ph' ? '' : 'mg/L'
  if (metric === 'residualCl') return 'mg/L'
  if (metric === 'level') return 'm'
  return ''
}
const qualityColor = (q) => ({ 优: '#00ffa3', 良: '#00e5ff', 中: '#ffd75e', 差: '#ff4d6d', 3: '#ff4d6d', 2: '#ffd75e', 0: '#00ffa3', 1: '#00e5ff' }[q] || '#00e5ff')

function metricValue(d, m) {
  if (d == null) return '--'
  const map = { pressure: d.pressure, flow: d.flow, ph: d.ph, turbidity: d.turbidity, residualCl: d.residualCl, level: d.level }
  const v = map[m]
  if (v == null) return '--'
  return typeof v === 'number' ? v.toFixed(2) : v
}

function formatMetric(d, m) {
  const v = metricValue(d, m)
  return v === '--' ? '--' : v
}

const trendOption = computed(() => {
  const meta = {
    pressure: { name: '压力', color: '#00e5ff', unit: 'MPa' },
    flow: { name: '流量', color: '#00ffa3', unit: 'm³/h' },
    ph: { name: 'pH', color: '#8b7bff', unit: '' },
    turbidity: { name: '浊度', color: '#ffd75e', unit: 'NTU' },
    residualCl: { name: '余氯', color: '#00e5ff', unit: 'mg/L' },
    level: { name: '液位', color: '#4deeff', unit: 'm' }
  }
  const c = meta[currentType.value]
  const data = []
  if (activeId.value && samples[activeId.value]) {
    const s = samples[activeId.value]
    const times = s.times || []
    const vals = s.values?.[currentType.value] || []
    times.forEach((t, i) => data.push([t, vals[i]]))
  }
  return {
    tooltip: { trigger: 'axis', ...darkTooltip((p) => {
      const d = p[0]
      return d ? `${d.axisValue}<br/>${c.name}：<b style="color:${c.color}">${d.data[1]}</b> ${c.unit}` : ''
    }) },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', ...axisCommon(), data: data.map((d) => d[0]), boundaryGap: false },
    yAxis: { type: 'value', ...axisCommon(), name: c.unit },
    series: [
      {
        name: c.name,
        type: 'line',
        smooth: true,
        showSymbol: false,
        data,
        lineStyle: { color: c.color, width: 2 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: c.color + '55' },
              { offset: 1, color: 'transparent' }
            ]
          }
        }
      }
    ]
  }
})

const metricStrip = computed(() => {
  const list = deviceCards.value
  const avg = (k) => {
    const v = list.map((d) => Number(d[k])).filter((n) => !Number.isNaN(n))
    if (!v.length) return '--'
    return (v.reduce((a, b) => a + b, 0) / v.length).toFixed(2)
  }
  return [
    { key: 'pressure', label: '平均压力', value: avg('pressure'), unit: 'MPa', color: '#00e5ff' },
    { key: 'flow', label: '总流量', value: list.reduce((s, d) => s + (Number(d.flow) || 0), 0).toFixed(1), unit: 'm³/h', color: '#00ffa3' },
    { key: 'turbidity', label: '平均浊度', value: avg('turbidity'), unit: 'NTU', color: '#ffd75e' },
    { key: 'temperature', label: '平均水温', value: avg('temperature'), unit: '℃', color: '#8b7bff' }
  ]
})

function select(id) {
  activeId.value = id
  loadTrend(id)
}
async function loadTrend(id) {
  try {
    const res = await trend({ deviceId: id, type: currentType.value, pointCount: 60 })
    // 初始化 samples
    const t = res.data || []
    samples[id] = samples[id] || { times: [], values: {} }
    samples[id].times = t.map((i) => i.time)
    samples[id].values[currentType.value] = t.map((i) => i.value)
  } catch (e) {}
}

function ingest(list) {
  deviceCards.value = list
  if (activeId.value == null && list.length) activeId.value = list[0].deviceId
  const now = dayjs()
  list.forEach((d) => {
    if (!samples[d.deviceId]) samples[d.deviceId] = { times: [], values: {} }
    const s = samples[d.deviceId]
    s.times.push(now.format('HH:mm:ss'))
    if (s.times.length > 60) s.times.shift()
    const pushVal = (k) => {
      const v = Number(d[k])
      if (Number.isNaN(v)) return
      s.values[k] = s.values[k] || []
      s.values[k].push(v)
      if (s.values[k].length > 60) s.values[k].shift()
    }
    ;['pressure', 'flow', 'ph', 'turbidity', 'residualCl', 'temperature', 'level'].forEach(pushVal)
  })
  lastUpdate.value = now.format('HH:mm:ss')
}

// 切换指标时如有历史数据则加载
function onTypeChange() {
  if (activeId.value && samples[activeId.value] && !samples[activeId.value].values[currentType.value]) {
    loadTrend(activeId.value)
  }
}

let wsClient = null
onMounted(() => {
  realtime().then((res) => {
    if (res.data && res.data.length) ingest(res.data)
  }).catch(() => {})

  wsClient = connectWs({
    url: '/ws',
    handlers: {
      onOpen: () => (connected.value = true),
      onClose: () => (connected.value = false),
      onMessage: (msg) => {
        if (msg.type === 'realtime' && msg.data) ingest(msg.data)
        else if (msg.type === 'heartbeat') {
          // 心跳回复
        }
      }
    }
  })
})
onBeforeUnmount(() => {
  wsClient && wsClient.close()
})
</script>

<style scoped>
.monitor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.conn-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: var(--radius);
}
.conn-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--warning);
  box-shadow: 0 0 8px var(--warning);
  animation: blink 1.2s infinite;
}
.conn-dot.on {
  background: var(--green);
  box-shadow: 0 0 8px var(--green);
  animation: none;
}
.conn-text {
  color: var(--warning);
  font-size: 13px;
}
.conn-text.on {
  color: var(--green);
}
.conn-time {
  color: var(--text-dim);
  margin-left: 4px;
}
.conn-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.field-label {
  color: var(--text-sub);
  font-size: 13px;
}
@keyframes blink {
  50% { opacity: 0.3; }
}
.monitor-body {
  display: flex;
  gap: 16px;
}
.dev-list {
  width: 300px;
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.dl-scroll {
  flex: 1;
}
.dev-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin: 0 6px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.dev-item:hover {
  background: var(--bg-hover);
}
.dev-item.active {
  border-color: var(--border-glow);
  background: linear-gradient(90deg, rgba(0, 229, 255, 0.12), transparent);
  box-shadow: inset 0 0 12px rgba(0, 229, 255, 0.05);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 6px currentColor;
}
.dev-info {
  flex: 1;
  min-width: 0;
}
.dev-name {
  color: var(--text-main);
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dev-meta {
  color: var(--text-dim);
  font-size: 11px;
  margin-top: 2px;
}
.dev-val {
  text-align: right;
  font-weight: 700;
  font-size: 16px;
  white-space: nowrap;
}
.dev-val small {
  color: var(--text-dim);
  font-size: 10px;
  margin-left: 2px;
}
.trend-area {
  flex: 1;
  min-width: 0;
}
.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.metric {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 14px 18px;
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
}
.ms-label {
  color: var(--text-sub);
  font-size: 13px;
}
.ms-unit {
  color: var(--text-dim);
  font-size: 11px;
}
@media (max-width: 1100px) {
  .monitor-body {
    flex-direction: column;
  }
  .dev-list {
    width: 100%;
    max-height: 320px;
  }
  .metric-strip {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>