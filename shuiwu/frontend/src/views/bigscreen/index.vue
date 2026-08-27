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
            <div class="tw-box">
              <TimeBox />
              <span class="weather">☀️ 晴 26℃ 西北风2级</span>
            </div>
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
            <div class="cs-val num" :style="{ color: s.color, textShadow: `0 0 14px ${s.color}` }">
              {{ s.value }}<span v-if="s.unit" class="cs-unit">{{ s.unit }}</span>
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
                  <!-- 管网连线（粗细代表管径，粒子模拟水流） -->
                  <g fill="none" :class="{ flowing }">
                    <template v-for="(e, idx) in edges" :key="idx">
                      <path :d="e.d" :stroke="e.color" :stroke-width="e.w" class="flow-line" />
                      <circle v-if="e.flow" r="2.6" fill="#bffdf6" opacity="0.85">
                        <animateMotion :dur="e.dur + 's'" repeatCount="indefinite" :path="e.d" />
                      </circle>
                    </template>
                  </g>

                  <!-- 节点 -->
                  <g
                    v-for="n in nodes"
                    :key="n.id"
                    class="topo-node"
                    @click="activeNode = n"
                  >
                    <!-- 外圈光圈 -->
                    <circle
                      :cx="n.x" :cy="n.y" r="20" fill="none"
                      :stroke="nodeColor(n.status)" stroke-width="1.6"
                      :class="'ring-' + n.status"
                    />
                    <!-- 类型图标 -->
                    <g :transform="`translate(${n.x}, ${n.y})`" class="node-icon"
                       :fill="nodeColor(n.status)" :stroke="nodeColor(n.status)" stroke-width="1.3">
                      <!-- 水源地 -->
                      <template v-if="n.type === 'source'">
                        <path d="M0 -11 C5 -6 8 -1 8 4 A8 8 0 0 1 -8 4 C-8 -1 -5 -6 0 -11 Z" fill="rgba(38,232,144,0.18)" />
                        <path d="M-6 6 L6 6" stroke-width="1.4" fill="none" />
                      </template>
                      <!-- 水厂 -->
                      <template v-else-if="n.type === 'plant'">
                        <rect x="-10" y="-5" width="20" height="13" rx="1.5" fill="rgba(0,229,238,0.18)" />
                        <rect x="-7" y="-12" width="3" height="7" />
                        <rect x="2" y="-10" width="3" height="5" />
                        <circle cx="-5.5" cy="-15" r="1.8" fill="none" />
                      </template>
                      <!-- 泵站 -->
                      <template v-else-if="n.type === 'pump'">
                        <circle r="9" fill="rgba(0,229,238,0.18)" />
                        <path d="M0 -6 L0 6 M-5.2 -3 L5.2 3 M-5.2 3 L5.2 -3" stroke-width="1.5" />
                        <circle r="2.4" fill="none" />
                      </template>
                      <!-- 高位池 -->
                      <template v-else-if="n.type === 'tank'">
                        <path d="M-11 -7 L11 -7 L8 11 L-8 11 Z" fill="rgba(139,123,255,0.18)" />
                        <path d="M-9.5 -2 L9.5 -2" stroke-width="1.4" />
                      </template>
                      <!-- 片区 -->
                      <template v-else>
                        <rect x="-11" y="-2" width="6" height="10" fill="rgba(0,229,238,0.18)" />
                        <rect x="-3" y="-8" width="6" height="16" />
                        <rect x="5" y="-4" width="6" height="12" />
                      </template>
                    </g>
                    <!-- 名称标签（半透明深色底） -->
                    <g :transform="`translate(${n.x}, ${n.y - 34})`">
                      <rect
                        :x="-n.name.length * 7 - 6" y="-10"
                        :width="n.name.length * 14 + 12" height="20" rx="4"
                        fill="rgba(3,9,24,0.72)"
                      />
                      <text text-anchor="middle" y="4" class="topo-name">{{ n.name }}</text>
                    </g>
                    <!-- 压力数值标签（半透明深色底） -->
                    <g :transform="`translate(${n.x}, ${n.y + 36})`">
                      <rect
                        :x="-(n.val.length * 7 + 30) / 2" y="-9"
                        :width="n.val.length * 7 + 30" height="18" rx="4"
                        fill="rgba(3,9,24,0.72)"
                      />
                      <text text-anchor="middle" y="4" class="topo-val num" :fill="nodeColor(n.status)">
                        {{ n.val }}<tspan class="topo-unit"> MPa</tspan>
                      </text>
                    </g>
                  </g>
                </svg>

                <div class="topo-legend">
                  <span><i class="lg on"></i>在线</span>
                  <span><i class="lg warn"></i>告警</span>
                  <span><i class="lg fault"></i>故障</span>
                  <span class="lg-hint">点击节点查看详情</span>
                </div>

                <!-- 节点详情弹层 -->
                <transition name="fade">
                  <div v-if="activeNode" class="node-detail" :style="detailPos">
                    <div class="nd-head">
                      <span class="nd-type">{{ typeLabel(activeNode.type) }}</span>
                      <span class="nd-name">{{ activeNode.name }}</span>
                      <span class="nd-status" :style="{ color: nodeColor(activeNode.status) }">
                        {{ statusLabel(activeNode.status) }}
                      </span>
                      <button class="nd-close" @click.stop="activeNode = null">×</button>
                    </div>
                    <div class="nd-grid">
                      <div class="nd-item">
                        <span>压力</span><b :style="{ color: PRIMARY }">{{ activeNode.pressure }} MPa</b>
                      </div>
                      <div class="nd-item">
                        <span>流量</span><b :style="{ color: GREEN }">{{ activeNode.flow }} m³/h</b>
                      </div>
                      <div class="nd-item">
                        <span>pH</span><b :style="{ color: GOLD }">{{ activeNode.ph }}</b>
                      </div>
                      <div class="nd-item">
                        <span>浊度</span><b :style="{ color: PURPLE }">{{ activeNode.turbidity }} NTU</b>
                      </div>
                    </div>
                  </div>
                </transition>
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
                <div v-if="!alarmRows.length" class="alarm-empty">当前无告警信息</div>
                <div v-else class="alarm-rows" :style="{ transform: `translateY(${-ty}px)` }">
                  <div
                    v-for="(a, i) in alarmRows"
                    :key="a.id + '-' + i"
                    class="alarm-row"
                    :class="'al-' + a.alarmLevel"
                  >
                    <span class="ar-level">{{ levelName(a.alarmLevel) }}</span>
                    <span class="ar-dev">{{ a.deviceName }}</span>
                    <span class="ar-msg">{{ a.alarmDesc }}</span>
                  </div>
                </div>
              </div>
            </div>
          </DatavBorder>
        </div>
      </div>
    </div>

    <a
      class="bs-footer"
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noopener noreferrer"
    >
      蜀ICP备2026050045号
    </a>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import PanelTitle from '@/components/PanelTitle.vue'
import DatavBorder from '@/components/DatavBorder.vue'
import TimeBox from '@/components/TimeBox.vue'
import connectWs from '@/utils/websocket'
import { realtime, trend } from '@/api/monitor'
import { onlineCount } from '@/api/device'
import { alarmSummary } from '@/api/alarm'
import {
  axisCommon, darkTooltip,
  PRIMARY, GREEN, GOLD, PURPLE, DANGER, INFO, HEALTHY, OFFLINE, SUB
} from '@/utils/echartsTheme'

// ===== websocket realtime data =====
const summary = ref({})
const onlineInfo = ref({ total: 0, online: 0 })
const realtimeList = ref([])
const flowing = ref(true)
const activeNode = ref(null)

// ===== 顶部指标卡片 =====
const centerStats = computed(() => {
  const list = realtimeList.value
  const avgPress = list.length
    ? (list.reduce((s, d) => s + (Number(d.pressure) || 0), 0) / list.length).toFixed(2)
    : '0.00'
  const totalFlow = list.reduce((s, d) => s + (Number(d.flow) || 0), 0).toFixed(1)
  const avgPh = list.length
    ? (list.reduce((s, d) => s + (Number(d.ph) || 0), 0) / list.length).toFixed(1)
    : '0.0'
  const un = Number(summary.value.unhandled ?? 0)
  return [
    { value: avgPress, unit: 'MPa', label: '平均水压', color: PRIMARY },
    { value: totalFlow, unit: 'm³/h', label: '总流量', color: GREEN },
    { value: avgPh, unit: '', label: '平均 pH', color: GOLD },
    { value: String(un), unit: '条', label: '待处理告警', color: un > 0 ? DANGER : HEALTHY }
  ]
})

// ===== 拓扑节点 =====
const nodeColor = (s) => (s === 'fault' ? DANGER : s === 'warn' ? GOLD : HEALTHY)
const typeLabel = (t) => ({ source: '水源地', plant: '水厂', tank: '高位池', pump: '泵站', zone: '片区' }[t] || t)
const statusLabel = (s) => ({ online: '在线', warn: '告警', fault: '故障' }[s] || s)

const nodes = [
  { id: 1, type: 'source', x: 100, y: 380, name: '水源地', val: '0.52', status: 'online', pressure: 0.52, flow: 1820, ph: 7.2, turbidity: 0.6 },
  { id: 2, type: 'plant', x: 260, y: 240, name: '水厂1', val: '0.48', status: 'warn', pressure: 0.48, flow: 2260, ph: 7.4, turbidity: 0.8 },
  { id: 3, type: 'tank', x: 420, y: 150, name: '高位池', val: '0.55', status: 'online', pressure: 0.55, flow: 1540, ph: 7.1, turbidity: 0.5 },
  { id: 4, type: 'pump', x: 640, y: 110, name: '泵站A', val: '0.60', status: 'online', pressure: 0.6, flow: 1980, ph: 7.3, turbidity: 0.7 },
  { id: 5, type: 'plant', x: 380, y: 300, name: '水厂2', val: '0.44', status: 'online', pressure: 0.44, flow: 1640, ph: 7.0, turbidity: 0.9 },
  { id: 6, type: 'pump', x: 520, y: 280, name: '泵站B', val: '0.39', status: 'fault', pressure: 0.39, flow: 520, ph: 6.8, turbidity: 2.4 },
  { id: 7, type: 'zone', x: 700, y: 330, name: '片区1', val: '0.33', status: 'online', pressure: 0.33, flow: 860, ph: 7.2, turbidity: 1.1 },
  { id: 8, type: 'zone', x: 300, y: 420, name: '片区2', val: '0.35', status: 'online', pressure: 0.35, flow: 920, ph: 7.1, turbidity: 1.0 },
  { id: 9, type: 'zone', x: 580, y: 400, name: '片区3', val: '0.30', status: 'online', pressure: 0.3, flow: 780, ph: 7.3, turbidity: 1.2 }
]

// 管线（w 代表管径粗细，flow 开启粒子水流）
const edges = [
  { d: 'M100,380 C200,380 180,240 260,240', w: 3.2, color: 'rgba(0,229,238,0.65)', flow: true, dur: 7 },
  { d: 'M260,240 L420,150', w: 3, color: 'rgba(0,229,238,0.65)', flow: true, dur: 6 },
  { d: 'M260,240 L380,300', w: 2.6, color: 'rgba(40,226,140,0.55)', flow: true, dur: 8 },
  { d: 'M380,300 L520,280', w: 2.8, color: 'rgba(0,229,238,0.6)', flow: true, dur: 7 },
  { d: 'M420,150 L640,110', w: 2.6, color: 'rgba(0,229,238,0.55)', flow: true, dur: 9 },
  { d: 'M520,280 L700,330', w: 2.4, color: 'rgba(40,226,140,0.5)', flow: true, dur: 9 },
  { d: 'M640,110 L720,150', w: 2, color: 'rgba(0,229,238,0.45)' },
  { d: 'M260,350 L100,380', w: 2, color: 'rgba(40,226,140,0.45)' },
  { d: 'M380,300 L300,420', w: 2, color: 'rgba(40,226,140,0.45)' },
  { d: 'M520,280 L580,400', w: 2, color: 'rgba(0,229,238,0.4)' }
]

// 详情弹层位置（按 viewBox 800x470 换算百分比并夹取防溢出）
const detailPos = computed(() => {
  if (!activeNode.value) return {}
  const px = (activeNode.value.x / 800) * 100
  const py = (activeNode.value.y / 470) * 100
  return { left: `${Math.min(Math.max(px, 12), 78)}%`, top: `${Math.min(Math.max(py, 10), 82)}%` }
})

// ===== charts =====
const trendOption = ref({})
const waterOption = ref({})
const gaugeOption = ref({})
const ringOption = ref({})

function loadTrend() {
  Promise.all([
    trend({ deviceId: '', type: 'pressure', pointCount: 30 }),
    trend({ deviceId: '', type: 'flow', pointCount: 30 })
  ])
    .then(([pr, fl]) => {
      const p = pr.data || []
      const f = fl.data || []
      const times = p.map((i) => i.time)
      trendOption.value = {
        tooltip: { trigger: 'axis', ...darkTooltip() },
        legend: { top: 2, icon: 'roundRect', itemWidth: 10, itemHeight: 4, textStyle: { color: SUB, fontSize: 11 } },
        grid: { left: 40, right: 14, top: 30, bottom: 24 },
        xAxis: { type: 'category', ...axisCommon(), data: times, axisLabel: { color: SUB, fontSize: 10, interval: 3 } },
        yAxis: { type: 'value', ...axisCommon(), axisLabel: { color: SUB, fontSize: 10 } },
        series: [
          {
            name: '压力', type: 'line', smooth: true, showSymbol: false,
            data: p.map((i) => i.value),
            lineStyle: { color: PRIMARY, width: 2 },
            areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0,229,238,0.35)' }, { offset: 1, color: 'transparent' }] } }
          },
          {
            name: '流量', type: 'line', smooth: true, showSymbol: false,
            data: f.map((i) => i.value),
            lineStyle: { color: GREEN, width: 2 }
          }
        ]
      }
    })
    .catch(() => {})
}

function renderZoneWater() {
  const data = [
    { name: '片区1', value: 2860 },
    { name: '片区2', value: 2210 },
    { name: '片区3', value: 1980 },
    { name: '水厂2', value: 1760 },
    { name: '高位池', value: 1520 }
  ]
  const total = data.reduce((s, i) => s + i.value, 0)
  const palette = [PRIMARY, GREEN, GOLD, PURPLE, INFO]
  waterOption.value = {
    tooltip: { trigger: 'item', ...darkTooltip() },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, itemGap: 10, textStyle: { color: SUB, fontSize: 11 } },
    graphic: [
      { type: 'text', left: 'center', top: '30%', style: { text: (total / 1000).toFixed(1) + 'k', fill: '#eaf6ff', fontSize: 22, fontWeight: 700, textAlign: 'center' } },
      { type: 'text', left: 'center', top: '43%', style: { text: '日用水量 m³', fill: SUB, fontSize: 10, textAlign: 'center' } }
    ],
    series: [
      {
        type: 'pie', radius: ['50%', '72%'], center: ['50%', '38%'],
        label: { show: false },
        itemStyle: { borderColor: 'rgba(8,12,36,0.9)', borderWidth: 2 },
        data: data.map((d, i) => ({ name: d.name, value: d.value, itemStyle: { color: palette[i % palette.length] } }))
      }
    ]
  }
}

function renderGauges() {
  const d = realtimeList.value[0] || {}
  const ph = Math.max(0, Math.min(14, Number(d.ph) || 7))
  // 业务配色：正常绿 / 偏酸碱黄 / 异常红
  const phColor = ph >= 6.5 && ph <= 8.5 ? HEALTHY : ph >= 4 && ph <= 11 ? GOLD : DANGER
  gaugeOption.value = {
    series: [
      {
        type: 'gauge',
        startAngle: 200, endAngle: -20,
        center: ['50%', '58%'], radius: '94%',
        min: 0, max: 14,
        axisLine: {
          lineStyle: {
            width: 12,
            color: [[0.3, DANGER], [0.46, GOLD], [0.6, HEALTHY], [0.78, GOLD], [1, DANGER]]
          }
        },
        pointer: {
          length: '62%', width: 5,
          itemStyle: { color: phColor, shadowBlur: 14, shadowColor: phColor, shadowOffsetY: 2 }
        },
        anchor: { show: true, size: 8, itemStyle: { color: phColor, shadowBlur: 10, shadowColor: phColor } },
        axisTick: { show: false },
        splitLine: { distance: -16, length: 10, lineStyle: { color: 'rgba(255,255,255,0.4)', width: 2 } },
        axisLabel: { color: SUB, distance: 14, fontSize: 9 },
        detail: { valueAnimation: true, offsetCenter: [0, '8%'], formatter: (v) => Number(v).toFixed(1), color: '#eaf6ff', fontSize: 30, fontWeight: 700 },
        title: { offsetCenter: [0, '40%'], color: SUB, fontSize: 12 },
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
        legend: {
          bottom: 0, icon: 'circle', itemWidth: 9, itemHeight: 9, itemGap: 16,
          textStyle: { color: SUB, fontSize: 13, fontWeight: 600 }
        },
        graphic: [
          { type: 'text', left: 'center', top: '28%', style: { text: `${online} 台`, fill: HEALTHY, fontSize: 22, fontWeight: 800, textAlign: 'center' } },
          { type: 'text', left: 'center', top: '42%', style: { text: '在线设备', fill: SUB, fontSize: 12, textAlign: 'center' } }
        ],
        series: [
          {
            type: 'pie', radius: ['56%', '78%'], center: ['50%', '40%'],
            label: { show: false },
            itemStyle: { borderColor: 'rgba(8,12,36,0.9)', borderWidth: 2 },
            data: [
              { name: '在线', value: online, itemStyle: { color: HEALTHY } },
              { name: '离线', value: offline, itemStyle: { color: OFFLINE } },
              { name: '故障', value: fault, itemStyle: { color: DANGER } }
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
  alarmRows.value = [
    { id: 1, deviceName: '泵站B压力计', alarmLevel: 3, alarmDesc: '压力超上限，请及时处置' },
    { id: 2, deviceName: '水厂2浊度仪', alarmLevel: 2, alarmDesc: '浊度偏高' },
    { id: 3, deviceName: '片区1流量计', alarmLevel: 1, alarmDesc: '瞬时流量波动' },
    { id: 4, deviceName: '水源地余氯仪', alarmLevel: 2, alarmDesc: '余氯低于下限' },
    { id: 5, deviceName: '高位池液位计', alarmLevel: 1, alarmDesc: '液位接近上限' },
    { id: 6, deviceName: '泵站A流量计', alarmLevel: 3, alarmDesc: '管道流量骤降' }
  ]
}
function pushAlarm(a) {
  alarmRows.value.unshift({ ...a })
  if (alarmRows.value.length > 40) alarmRows.value.pop()
}
function startMarquee() {
  const rowH = 38
  const half = Math.ceil(alarmRows.value.length / 2) * rowH
  marqueeTimer = setInterval(() => {
    ty.value += rowH
    if (ty.value >= half) ty.value = 0
  }, 2600)
}

const levelName = (l) => ({ 1: '提示', 2: '告警', 3: '严重' }[l] || '未知')

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

  const N = 40
  const pts = Array.from({ length: N }).map(() => ({
    x: Math.random() * 1600,
    y: Math.random() * 900,
    vx: (Math.random() - 0.5) * 0.35,
    vy: (Math.random() - 0.5) * 0.35,
    r: Math.random() * 1.4 + 0.4
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
      ctx.fillStyle = 'rgba(0,229,238,0.4)'
      ctx.fill()
    })
    for (let i = 0; i < pts.length; i++) {
      for (let j = i + 1; j < pts.length; j++) {
        const a = pts[i]
        const b = pts[j]
        const d = Math.hypot(a.x - b.x, a.y - b.y)
        if (d < 110) {
          ctx.strokeStyle = `rgba(0,229,238,${(1 - d / 110) * 0.22})`
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
  renderZoneWater()
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
  background:
    radial-gradient(ellipse at 50% 0%, #0c1a36 0%, #081424 55%, #050d1c 100%);
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bs-footer {
  position: absolute;
  right: 22px;
  bottom: 10px;
  z-index: 5;
  color: rgba(143, 168, 207, 0.5);
  font-size: 12px;
  letter-spacing: 1px;
  text-decoration: none;
  transition: color 0.2s;
}
.bs-footer:hover {
  color: var(--primary);
}
.bg-particle {
  position: absolute;
  inset: 0;
  opacity: 0.3;
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
    rgba(0, 229, 238, 0.025) 4px,
    transparent 5px
  );
  animation: noize 8s linear infinite;
  opacity: 0.25;
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
/* 顶部时间天气：半透明深色背景框 */
.tw-box {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 6px 14px;
  background: rgba(8, 20, 36, 0.6);
  border: 1px solid rgba(0, 229, 238, 0.18);
  border-radius: 8px;
  backdrop-filter: blur(4px);
  box-shadow: inset 0 0 18px rgba(0, 229, 238, 0.05);
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
  font-size: 38px;
  letter-spacing: 6px;
  font-weight: 800;
  text-shadow:
    0 0 6px rgba(0, 229, 238, 0.35),
    0 0 22px rgba(0, 229, 238, 0.25);
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
  -webkit-text-stroke: 1px rgba(0, 229, 238, 0.28);
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
  color: rgba(143, 168, 207, 0.5);
  font-size: 10px;
  letter-spacing: 3px;
  margin-top: 4px;
  font-weight: 300;
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
.col-left { grid-column: 1 / span 5; }
.col-center { grid-column: 6 / span 14; }
.col-right { grid-column: 20 / span 5; }
@media (max-width: 1400px) {
  .col-left { grid-column: 1 / span 6; }
  .col-center { grid-column: 7 / span 12; }
  .col-right { grid-column: 19 / span 6; }
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
  background: linear-gradient(160deg, rgba(18, 25, 55, 0.85), rgba(8, 12, 34, 0.65));
  border: 1px solid rgba(0, 229, 238, 0.22);
  border-radius: 10px;
  padding: 10px 6px;
  box-shadow:
    0 0 14px rgba(0, 229, 238, 0.12),
    inset 0 0 18px rgba(0, 229, 238, 0.04);
}
.cs-val {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.1;
  font-family: 'DIN Alternate', 'Bahnschrift', monospace;
}
.cs-unit {
  font-size: 12px;
  font-weight: 600;
  margin-left: 3px;
  opacity: 0.75;
}
.cs-label {
  color: var(--text-dim);
  font-size: 11px;
  margin-top: 5px;
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

/* 管线流动 */
.flow-line {
  stroke-dasharray: 5 14;
  animation: dashflow 1.8s linear infinite;
}
@keyframes dashflow {
  to { stroke-dashoffset: -38; }
}

/* 节点 */
.topo-node {
  cursor: pointer;
}
.topo-node circle {
  transform-box: fill-box;
  transform-origin: center;
}
.ring-on { animation: nodeBreathe 2.6s ease-in-out infinite; }
.ring-fault { animation: nodePulseFault 1.1s ease-in-out infinite; }
.ring-warn { animation: nodeBlink 1.8s ease-in-out infinite; }
@keyframes nodeBreathe {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.95; transform: scale(1.08); }
}
@keyframes nodePulseFault {
  0%, 100% { opacity: 0.4; transform: scale(0.96); }
  50% { opacity: 1; transform: scale(1.28); }
}
@keyframes nodeBlink {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}
.node-icon {
  filter: drop-shadow(0 0 6px currentColor);
}
.topo-name {
  fill: #dceaff;
  font-size: 13px;
  font-weight: 600;
}
.topo-val {
  font-size: 12px;
  font-weight: 700;
}
.topo-unit {
  font-size: 9px;
  font-weight: 400;
  fill-opacity: 0.7;
}

.topo-legend {
  position: absolute;
  top: 2px;
  right: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: var(--text-sub);
  padding: 4px 10px;
  background: rgba(3, 9, 24, 0.6);
  border-radius: 6px;
}
.topo-legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 3px;
}
.lg.on { background: var(--healthy); box-shadow: 0 0 6px var(--healthy); }
.lg.warn { background: var(--warning); box-shadow: 0 0 6px var(--warning); }
.lg.fault { background: var(--danger); box-shadow: 0 0 6px var(--danger); }
.lg-hint { color: rgba(143, 168, 207, 0.5); }

/* 节点详情弹层 */
.node-detail {
  position: absolute;
  z-index: 6;
  min-width: 220px;
  transform: translate(-50%, -112%);
  background: rgba(8, 16, 36, 0.92);
  border: 1px solid rgba(0, 229, 238, 0.35);
  border-radius: 8px;
  padding: 10px 12px;
  box-shadow: 0 0 22px rgba(0, 229, 238, 0.18);
  backdrop-filter: blur(6px);
}
.nd-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 229, 238, 0.18);
}
.nd-type {
  font-size: 11px;
  color: var(--primary);
  border: 1px solid rgba(0, 229, 238, 0.4);
  border-radius: 4px;
  padding: 1px 6px;
}
.nd-name {
  font-size: 14px;
  font-weight: 700;
  color: #eaf6ff;
}
.nd-status {
  font-size: 12px;
  font-weight: 600;
  margin-left: auto;
}
.nd-close {
  background: none;
  border: none;
  color: var(--text-sub);
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
  padding: 0 2px;
}
.nd-close:hover { color: var(--danger); }
.nd-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
  padding-top: 8px;
}
.nd-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nd-item span {
  font-size: 11px;
  color: var(--text-dim);
}
.nd-item b {
  font-size: 15px;
  font-weight: 700;
  font-family: 'DIN Alternate', monospace;
}

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
  transition: transform 1.1s linear;
}
.alarm-row {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 32px;
  padding: 0 10px;
  margin-bottom: 6px;
  border-radius: 6px;
  font-size: 12px;
  border-left: 3px solid transparent;
}
.al-1 { background: rgba(0, 200, 240, 0.1); border-left-color: var(--info); }
.al-2 { background: rgba(255, 193, 54, 0.1); border-left-color: var(--gold); }
.al-3 { background: rgba(255, 68, 85, 0.13); border-left-color: var(--danger); }
.ar-level {
  flex-shrink: 0;
  font-weight: 800;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 4px;
}
.al-1 .ar-level { color: var(--info); background: rgba(0, 200, 240, 0.18); }
.al-2 .ar-level { color: var(--gold); background: rgba(255, 193, 54, 0.18); }
.al-3 .ar-level { color: var(--danger); background: rgba(255, 68, 85, 0.22); }
.ar-dev {
  flex-shrink: 0;
  color: #dceaff;
  font-weight: 600;
  max-width: 42%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ar-msg {
  flex: 1;
  color: var(--text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alarm-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--healthy);
  font-size: 14px;
  letter-spacing: 2px;
}
</style>
