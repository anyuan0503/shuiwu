<template>
  <div class="bigscreen">
    <canvas ref="particleCv" class="bg-particle"></canvas>
    <div class="scan-noize"></div>

    <!-- 顶部标题 -->
    <header class="bs-header">
      <div class="h-left">
        <DatavBorder :width="'100%'" :height="'100%'">
          <div class="h-inner">
            <span class="info">{{
              `在线 ${onlineInfo.online} / ${onlineInfo.total} · 待处理告警 ${summary.unhandled ?? 0}`
            }}</span>
          </div>
        </DatavBorder>
      </div>
      <div class="h-center">
        <h1 class="main-title">
          <span class="t-water">💧</span>
          <span class="t-gradient">水务监测智能分析中心</span>
        </h1>
        <div class="sub-title">WATER MONITORING &amp; INTELLIGENT ANALYSIS CENTER</div>
      </div>
      <div class="h-right">
        <DatavBorder :width="'100%'" :height="'100%'">
          <div class="h-inner right">
            <TimeBox />
            <span class="weather">☀️ 晴 26℃ 西北风2级</span>
          </div>
        </DatavBorder>
      </div>
    </header>

    <!-- 主体 24列grid -->
    <div class="bs-body">
      <!-- 左列 -->
      <div class="col col-left">
        <div class="panel">
          <DatavBorder :height="'100%'">
            <div class="panel-inner">
              <PanelTitle>管网压力 / 流量趋势</PanelTitle>
              <BaseChart :option="trendOption" height="100%" />
            </div>
          </DatavBorder>
        </div>
        <div class="panel">
          <DatavBorder :height="'100%'">
            <div class="panel-inner">
              <PanelTitle>片区日用水量</PanelTitle>
              <BaseChart :option="waterOption" height="100%" />
            </div>
          </DatavBorder>
        </div>
      </div>

      <!-- 中间 -->
      <div class="col col-center">
        <div class="center-stats">
          <div class="cstat" v-for="s in centerStats" :key="s.label">
            <div class="cs-val num" :style="{ color: s.color }">
              <DatavNumber :value="s.value" :color="s.color" :size="30" />
            </div>
            <div class="cs-label">{{ s.label }}</div>
          </div>
        </div>
        <div class="topology-panel">
          <DatavBorder :height="'100%'">
            <div class="panel-inner topology">
              <PanelTitle>片区管网拓扑示意</PanelTitle>
              <div class="topo-wrap">
                <svg viewBox="0 0 800 470" class="topo-svg">
                  <!-- 管网连线 -->
                  <g :stroke="edgeColor" stroke-width="2" fill="none" :class="{ flow: flowing }">
                    <path d="M100,380 C200,380 180,240 260,240" />
                    <path d="M260,240 L420,150" />
                    <path d="M260,240 L380,300" />
                    <path d="M380,300 L520,280" />
                    <path d="M420,150 L640,110" />
                    <path d="M520,280 L700,330" />
                    <path d="M640,110 L720,150" />
                    <path d="M260,350 L100,380" />
                    <path d="M380,300 L300,420" />
                    <path d="M520,280 L580,400" />
                  </g>
                  <!-- 节点 -->
                  <g v-for="n in nodes" :key="n.id">
                    <circle
                      :cx="n.x" :cy="n.y" r="16" fill="none"
                      :stroke="n.alarm ? '#ff4d6d' : '#00e5ff'"
                      stroke-width="2"
                      stroke-dasharray="6 4"
                      :class="{ pulse: n.alarm }"
                    />
                    <circle :cx="n.x" :cy="n.y" r="6" :fill="n.alarm ? '#ff4d6d' : '#00e5ff'" />
                    <text :x="n.x" :y="n.y - 26" text-anchor="middle" class="topo-name">{{ n.name }}</text>
                    <text :x="n.x" :y="n.y + 32" text-anchor="middle" class="topo-val num"
                      :fill="n.alarm ? '#ff4d6d' : '#00ffa3'">
                      {{ n.val }}
                    </text>
                  </g>
                </svg>
                <div class="topo-legend">
                  <span><i class="lg on"></i>在线</span>
                  <span><i class="lg warn"></i>故障/告警</span>
                </div>
              </div>
            </div>
          </DatavBorder>
        </div>
      </div>

      <!-- 右列 -->
      <div class="col col-right">
        <div class="panel">
          <DatavBorder :height="'100%'">
            <div class="panel-inner">
              <PanelTitle>水质综合指标</PanelTitle>
              <BaseChart :option="gaugeOption" height="100%" />
            </div>
          </DatavBorder>
        </div>
        <div class="panel">
          <DatavBorder :height="'100%'">
            <div class="panel-inner">
              <PanelTitle>设备状态分布</PanelTitle>
              <BaseChart :option="ringOption" height="100%" />
            </div>
          </DatavBorder>
        </div>
        <div class="panel">
          <DatavBorder :height="'100%'">
            <div class="panel-inner alarm-list">
              <PanelTitle>实时告警滚动</PanelTitle>
              <div class="alarm-marquee">
                <div class="alarm-rows" :style="{ transform: `translateY(${-ty}px)` }">
                  <div
                    v-for="(a, i) in alarmRows"
                    :key="a.id + '-' + i"
                    class="alarm-row"
                  >
                    <span class="ar-level" :style="{ color: levelColor(a.alarmLevel) }">
                      [{{ levelName(a.alarmLevel) }}]
                    </span>
                    <span class="ar-msg">{{ a.deviceName }} · {{ a.alarmDesc }}</span>
                    <span class="ar-time num">{{ a.alarmTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </DatavBorder>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import PanelTitle from '@/components/PanelTitle.vue'
import DatavNumber from '@/components/DatavNumber.vue'
import DatavBorder from '@/components/DatavBorder.vue'
import TimeBox from '@/components/TimeBox.vue'
import connectWs from '@/utils/websocket'
import { realtime, trend } from '@/api/monitor'
import { onlineCount } from '@/api/device'
import { alarmSummary } from '@/api/alarm'
import { axisCommon, darkTooltip } from '@/utils/echartsTheme'

// ===== websocket realtime data =====
const summary = ref({})
const onlineInfo = ref({ total: 0, online: 0 })
const realtimeList = ref([])

const centerStats = computed(() => {
  const list = realtimeList.value
  const avgPress = list.length
    ? (list.reduce((s, d) => s + (Number(d.pressure) || 0), 0) / list.length).toFixed(2)
    : '0.00'
  const totalFlow = list.reduce((s, d) => s + (Number(d.flow) || 0), 0).toFixed(1)
  const avgPh = list.length
    ? (list.reduce((s, d) => s + (Number(d.ph) || 0), 0) / list.length).toFixed(1)
    : '0.0'
  return [
    { value: avgPress, label: '平均水压 (MPa)', color: '#00e5ff' },
    { value: totalFlow, label: '总流量 (m³/h)', color: '#00ffa3' },
    { value: avgPh, label: '平均 pH', color: '#ffd75e' },
    { value: (summary.value.unhandled ?? 0), label: '待处理告警', color: '#ff4d6d' }
  ]
})

// 拓扑节点
const nodes = [
  { id: 1, x: 100, y: 380, name: '水源地', val: '0.52MPa', alarm: false },
  { id: 2, x: 260, y: 240, name: '水厂1', val: '0.48MPa', alarm: false },
  { id: 3, x: 420, y: 150, name: '高位池', val: '0.55MPa', alarm: false },
  { id: 4, x: 640, y: 110, name: '泵站A', val: '0.60MPa', alarm: false },
  { id: 5, x: 380, y: 300, name: '水厂2', val: '0.44MPa', alarm: false },
  { id: 6, x: 520, y: 280, name: '泵站B', val: '0.39MPa', alarm: true },
  { id: 7, x: 700, y: 330, name: '片区1', val: '0.33MPa', alarm: false },
  { id: 8, x: 300, y: 420, name: '片区2', val: '0.35MPa', alarm: false },
  { id: 9, x: 580, y: 400, name: '片区3', val: '0.30MPa', alarm: false }
]
const edgeColor = 'rgba(0,229,255,0.55)'
const flowing = ref(true)

// ===== charts =====
const trendOption = ref({})
const waterOption = ref({})
const gaugeOption = ref({})
const ringOption = ref({})

function loadTrend() {
  trend({ deviceId: '', type: 'flow', pointCount: 30 })
    .then((res) => {
      const rows = res.data || []
      trendOption.value = {
        tooltip: { trigger: 'axis', ...darkTooltip() },
        legend: { show: false },
        grid: { left: 34, right: 14, top: 24, bottom: 24 },
        xAxis: { type: 'category', ...axisCommon(), data: rows.map((i) => i.time) },
        yAxis: { type: 'value', ...axisCommon() },
        series: [
          {
            name: '流量', type: 'line', smooth: true, showSymbol: false,
            data: rows.map((i) => i.value),
            lineStyle: { color: '#00e5ff', width: 2 },
            areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0,229,255,0.4)' }, { offset: 1, color: 'transparent' }] } }
          }
        ]
      }
    })
    .catch(() => {})
}

function loadWater() {
  trend({ deviceId: '', type: 'flow', pointCount: 12 })
    .then((res) => {
      const rows = res.data || []
      const times = rows.map((i) => (i.time || '').slice(5))
      const vals = rows.map((i) => i.value)
      waterOption.value = {
        tooltip: { trigger: 'axis', ...darkTooltip() },
        grid: { left: 34, right: 14, top: 24, bottom: 24 },
        xAxis: { type: 'category', ...axisCommon(), data: times },
        yAxis: { type: 'value', ...axisCommon() },
        series: [
          {
            name: '用水量', type: 'bar', barWidth: '55%',
            data: vals.map((v) => ({
              value: v,
              itemStyle: { borderRadius: [4, 4, 0, 0], color: gradient('#00ffa3') }
            }))
          }
        ]
      }
    })
    .catch(() => {})
}

function gradient(color) {
  return { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color }, { offset: 1, color: 'rgba(0,255,163,0.1)' }] }
}

function renderGauges() {
  const d = realtimeList.value[0] || {}
  const ph = Math.max(0, Math.min(14, Number(d.ph) || 7))
  const turb = Math.min(100, Number(d.turbidity) || 1)
  gaugeOption.value = {
    series: [
      {
        type: 'gauge',
        startAngle: 200, endAngle: -20,
        center: ['50%', '58%'], radius: '92%',
        min: 0, max: 14,
        axisLine: { lineStyle: { width: 10, color: [[0.15, '#ff4d6d'], [0.35, '#ffd75e'], [0.5, '#00e5ff'], [0.85, '#00e5ff'], [1, '#00ffa3']] } },
        pointer: { itemStyle: { color: 'auto' }, length: '60%', width: 4 },
        axisTick: { distance: -12, length: 6, lineStyle: { color: '#fff', width: 1 } },
        splitLine: { distance: -14, length: 14, lineStyle: { color: '#fff', width: 2 } },
        axisLabel: { color: '#8fa8cf', distance: 12, fontSize: 10 },
        detail: {
          valueAnimation: true, offsetCenter: [0, '7%'], formatter: '{value}',
          color: '#00e5ff', fontSize: 26, fontFamily: 'DIN, monospace'
        },
        title: { offsetCenter: [0, '35%'], color: '#8fa8cf', fontSize: 13 },
        data: [{ value: Number(ph.toFixed(1)), name: 'pH 指数' }]
      }
    ]
  }
}

function renderRing() {
  onlineCount()
    .then((res) => {
      const list = res.data || []
      const total = list.reduce((s, i) => s + (i.total || 0), 0) || 1
      const online = list.reduce((s, i) => s + (i.online || 0), 0)
      const offline = list.reduce((s, i) => s + (i.offline || 0), 0)
      const fault = list.reduce((s, i) => s + (i.fault || 0), 0)
      onlineInfo.value = { total, online }
      ringOption.value = {
        tooltip: { trigger: 'item', ...darkTooltip() },
        legend: { bottom: 0, icon: 'circle' },
        series: [
          {
            type: 'pie', radius: ['55%', '78%'], center: ['50%', '44%'],
            label: { color: '#8fa8cf', formatter: '{b} {c}台' },
            itemStyle: { borderColor: 'rgba(10,14,39,0.9)', borderWidth: 2 },
            data: [
              { name: '在线', value: online, itemStyle: { color: '#00ffa3' } },
              { name: '离线', value: offline, itemStyle: { color: '#5a6f96' } },
              { name: '故障', value: fault, itemStyle: { color: '#ff4d6d' } }
            ]
          }
        ]
      }
    })
    .catch(() => {})
}

// ===== alarm marquee =====
const alarmRows = ref([])
const ty = ref(0)
let marqueeTimer = null

function seedAlarms() {
  const base = [
    { id: 1, deviceName: '泵站B压力计', alarmLevel: 3, alarmDesc: '压力超上限', alarmTime: '10:21:40' },
    { id: 2, deviceName: '水厂2浊度仪', alarmLevel: 2, alarmDesc: '浊度偏高', alarmTime: '10:19:12' },
    { id: 3, deviceName: '片区1流量计', alarmLevel: 1, alarmDesc: '瞬时流量波动', alarmTime: '10:15:05' },
    { id: 4, deviceName: '水源地余氯仪', alarmLevel: 2, alarmDesc: '余氯低于下限', alarmTime: '10:10:33' },
    { id: 5, deviceName: '高位池液位计', alarmLevel: 1, alarmDesc: '液位接近上限', alarmTime: '10:02:18' },
    { id: 6, deviceName: '泵站A流量计', alarmLevel: 3, alarmDesc: '管道流量骤降', alarmTime: '09:58:46' }
  ]
  // 复制一份衔接滚动实现无缝循环
  alarmRows.value = [...base, ...base.map((a, i) => ({ ...a, id: a.id + 100, alarmTime: '09:5' + i + ':00' }))]
}
function pushAlarm(a) {
  alarmRows.value.unshift({ ...a })
  if (alarmRows.value.length > 40) alarmRows.value.pop()
}
function startMarquee() {
  const rowH = 26
  const half = Math.ceil(alarmRows.value.length / 2) * rowH
  marqueeTimer = setInterval(() => {
    ty.value += rowH
    // 滚完前半段后回到0，实现循环滚动
    if (ty.value >= half) ty.value = 0
  }, 2000)
}

const levelName = (l) => ({ 1: '提示', 2: '警告', 3: '严重' }[l] || '未知')
const levelColor = (l) => ({ 1: '#00e5ff', 2: '#ffd75e', 3: '#ff4d6d' }[l] || '#00e5ff')

// ===== load =====
let ws = null

// ===== particle bg (数码粒子) =====
const particleCv = ref(null)
let raf = null
function initParticles() {
  const c = particleCv.value
  if (!c) return
  const ctx = c.getContext('2d')
  let w = (c.width = window.innerWidth)
  let h = (c.height = window.innerHeight)
  const onResize = () => {
    w = c.width = window.innerWidth
    h = c.height = window.innerHeight
  }
  window.addEventListener('resize', onResize)

  const N = 60
  const pts = Array.from({ length: N }).map(() => ({
    x: Math.random() * 1600,
    y: Math.random() * 900,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    r: Math.random() * 1.5 + 0.5
  }))
  const step = () => {
    ctx.clearRect(0, 0, w, h)
    pts.forEach((p) => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(0,229,255,0.5)'
      ctx.fill()
    })
    for (let i = 0; i < pts.length; i++) {
      for (let j = i + 1; j < pts.length; j++) {
        const a = pts[i]
        const b = pts[j]
        const d = Math.hypot(a.x - b.x, a.y - b.y)
        if (d < 120) {
          ctx.strokeStyle = `rgba(0,229,255,${(1 - d / 120) * 0.3})`
          ctx.lineWidth = 1
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }
    raf = requestAnimationFrame(step)
  }
  step()
}

async function loadAll() {
  try { const s = await alarmSummary(); summary.value = s.data || {} } catch (e) {}
  try {
    const r = await realtime()
    realtimeList.value = r.data || []
    renderGauges()
  } catch (e) {}
  loadTrend()
  loadWater()
  renderRing()
}

onMounted(() => {
  loadAll()
  seedAlarms()
  startMarquee()

  ws = connectWs({
    url: '/ws',
    handlers: {
      onMessage: (msg) => {
        if (msg.type === 'realtime' && msg.data) {
          realtimeList.value = msg.data
          renderGauges()
        } else if (msg.type === 'alarm' && msg.data) {
          pushAlarm(msg.data)
        }
      }
    }
  })
  initParticles()
})

onBeforeUnmount(() => {
  ws && ws.close()
  clearInterval(marqueeTimer)
  cancelAnimationFrame(raf)
})
</script>

<style scoped>
.bigscreen {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 0%, #0d1740 0%, #070b20 50%, #04060f 100%);
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bg-particle {
  position: absolute;
  inset: 0;
  opacity: 0.5;
  pointer-events: none;
}
.scan-noize {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: repeating-linear-gradient(
    0deg,
    transparent 0px,
    transparent 3px,
    rgba(0, 229, 255, 0.03) 4px,
    transparent 5px
  );
  animation: noize 8s linear infinite;
  opacity: 0.4;
}
@keyframes noize {
  0% { transform: translateY(0); }
  100% { transform: translateY(-12px); }
}

/* header */
.bs-header {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
}
.h-left,
.h-right {
  height: 46px;
}
.h-inner {
  display: flex;
  align-items: center;
  height: 100%;
  color: var(--green);
  font-size: 13px;
  padding: 0 6px;
}
.h-inner.right {
  justify-content: flex-end;
  gap: 14px;
}
.weather {
  color: var(--text-sub);
  font-size: 12px;
}
.h-center {
  text-align: center;
}
.main-title {
  margin: 0;
  font-size: 32px;
  letter-spacing: 6px;
  font-weight: 800;
}
.t-water {
  margin-right: 8px;
  filter: drop-shadow(0 0 10px var(--primary));
  animation: breathe 2.4s infinite;
}
.t-gradient {
  background: linear-gradient(90deg, var(--primary), var(--green), var(--primary));
  background-size: 200% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: shimmer 4s linear infinite;
}
@keyframes shimmer {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
@keyframes breathe {
  0%, 100% { filter: drop-shadow(0 0 6px var(--primary)); }
  50% { filter: drop-shadow(0 0 20px var(--primary)); }
}
.sub-title {
  color: var(--text-dim);
  font-size: 11px;
  letter-spacing: 3px;
  margin-top: 4px;
}

/* body grid: 24 columns */
.bs-body {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(24, 1fr);
  gap: 12px;
  min-height: 0;
}
.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.col-left { grid-column: 1 / span 6; }
.col-center { grid-column: 7 / span 11; }
.col-right { grid-column: 18 / span 7; }
@media (max-width: 1400px) {
  .col-left { grid-column: 1 / span 7; }
  .col-center { grid-column: 8 / span 10; }
  .col-right { grid-column: 18 / span 7; }
}

.panel {
  flex: 1;
  min-height: 0;
}
.panel-inner {
  display: flex;
  flex-direction: column;
  height: calc(100% - 12px);
  padding: 0 6px;
}
.panel-inner :deep(.base-chart) {
  flex: 1;
}

/* center */
.center-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 8px;
}
.cstat {
  text-align: center;
  background: linear-gradient(160deg, rgba(18, 25, 55, 0.8), rgba(8, 12, 34, 0.6));
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px 4px;
}
.cs-val {
  font-size: 30px;
  font-weight: 700;
  text-shadow: 0 0 12px currentColor;
}
.cs-label {
  color: var(--text-dim);
  font-size: 11px;
  margin-top: 4px;
}

.topology-panel {
  flex: 1;
  min-height: 0;
}
.topology .panel-inner {
  height: calc(100% - 4px);
}
.topo-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
}
.topo-svg {
  width: 100%;
  height: 100%;
}
.topology .flow path {
  stroke-dasharray: 8 6;
  animation: dashflow 1.4s linear infinite;
}
@keyframes dashflow {
  to { stroke-dashoffset: -14; }
}
.topo-name {
  fill: #8fa8cf;
  font-size: 13px;
}
.topo-val {
  font-size: 12px;
  font-weight: 600;
}
circle.pulse {
  animation: nodePulse 1.6s ease-in-out infinite;
  transform-origin: center;
}
@keyframes nodePulse {
  0%, 100% { opacity: 0.4; r: 16; }
  50% { opacity: 1; r: 22; }
}
.topo-legend {
  position: absolute;
  top: 0;
  right: 4px;
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-sub);
}
.topo-legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 3px;
}
.lg.on { background: #00e5ff; box-shadow: 0 0 6px #00e5ff; }
.lg.warn { background: #ff4d6d; box-shadow: 0 0 6px #ff4d6d; }

/* alarm marquee */
.alarm-list {
  height: calc(100% - 4px);
}
.alarm-marquee {
  flex: 1;
  overflow: hidden;
  position: relative;
}
.alarm-rows {
  transition: transform 0.8s linear;
}
.alarm-row {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 26px;
  font-size: 12px;
  border-bottom: 1px dashed rgba(0, 229, 255, 0.1);
}
.ar-level { font-weight: 700; }
.ar-msg { flex: 1; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ar-time { color: var(--text-dim); }
</style>