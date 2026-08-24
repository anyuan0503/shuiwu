<template>
  <div class="page-container dashboard">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="s in statCards" :key="s.label" class="stat-card glass">
        <div class="sc-icon" :style="{ color: s.color, background: s.bg }">
          <el-icon :size="24"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <div class="sc-label">{{ s.label }}</div>
          <DatavNumber :value="realStats[s.key] ?? s.value" :color="s.color" :size="26" />
        </div>
        <div class="sc-unit">{{ s.unit }}</div>
        <div class="sc-line" :style="{ background: s.color }"></div>
      </div>
    </div>

    <div class="row-charts">
      <div class="span-8">
        <ChartCard title="设备状态分布" height="320px">
          <BaseChart :option="pieOption" height="280px" />
        </ChartCard>
      </div>
      <div class="span-8">
        <ChartCard title="告警分级概览" height="320px">
          <BaseChart :option="alarmPieOption" height="280px" />
        </ChartCard>
      </div>
      <div class="span-8">
        <ChartCard title="近7日告警趋势" height="320px">
          <BaseChart :option="alarmTrendOption" height="280px" />
        </ChartCard>
      </div>
    </div>

    <div class="row-charts">
      <div class="span-12">
        <ChartCard title="实时压力 · 智能医院管网" height="360px">
          <BaseChart :option="pressureOption" height="320px" />
        </ChartCard>
      </div>
      <div class="span-12">
        <ChartCard title="最新监测值排名" height="360px">
          <BaseChart :option="realtimeBarOption" height="320px" />
        </ChartCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import {
  Odometer, DataLine, Bell, Monitor, Aim, PieChart
} from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import DatavNumber from '@/components/DatavNumber.vue'
import { sysStat } from '@/api/system'
import { onlineCount } from '@/api/device'
import { alarmSummary, alarmTrend } from '@/api/alarm'
import { realtime } from '@/api/monitor'
import { axisCommon, darkTooltip, PRIMARY, GREEN, GOLD, DANGER } from '@/utils/echartsTheme'

const statCards = ref([
  { key: 'userCount', label: '系统用户', icon: 'User', color: PRIMARY, unit: '个' },
  { key: 'deviceCount', label: '监测设备', icon: 'Monitor', color: GREEN, unit: '台' },
  { key: 'alarmUnhandled', label: '待处理告警', icon: 'Bell', color: GOLD, unit: '条' },
  { key: 'onlineRatio', label: '设备在线率', icon: 'Cellphone', color: '#8b7bff', unit: '%', ratio: true }
])
const realStats = reactive({ userCount: 0, deviceCount: 0, alarmUnhandled: 0, onlineRatio: 0 })

const pressureOption = ref({})
const realtimeBarOption = ref({})
const pieOption = ref({})
const alarmPieOption = ref({})
const alarmTrendOption = ref({})

const typeName = (t) => ({ pressure: '压力计', flow: '流量计', quality: '水质仪', level: '液位计' }[t] || t)
const typeColor = (t) => ({ pressure: PRIMARY, flow: GREEN, quality: GOLD, level: '#8b7bff' }[t] || PRIMARY)

async function loadStats() {
  try {
    const res = await sysStat()
    const d = res.data || {}
    realStats.userCount = d.userCount || 0
    realStats.deviceCount = d.deviceCount || 0
    realStats.alarmUnhandled = d.alarmUnhandled || 0
    realStats.onlineRatio = d.onlineRatio || 0
  } catch (e) {
    // fallback
  }
  try {
    const oc = await onlineCount()
    const list = oc.data || []
    const total = list.reduce((s, i) => s + (i.total || 0), 0)
    const online = list.reduce((s, i) => s + (i.online || 0), 0)
    if (total > 0 && !realStats.deviceCount) realStats.deviceCount = total
    if (total > 0) realStats.onlineRatio = Number(((online / total) * 100).toFixed(1))
    renderPie(list)
  } catch (e) {}
  try {
    const s = await alarmSummary()
    const d = s.data || {}
    renderAlarmPie(d)
  } catch (e) {}
  try {
    const t = await alarmTrend()
    renderAlarmTrend(t.data || [])
  } catch (e) {}
  try {
    const r = await realtime()
    renderRealtime(r.data || [])
  } catch (e) {}
}

function renderPie(list) {
  pieOption.value = {
    tooltip: { trigger: 'item', ...darkTooltip() },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: 'rgba(10,14,39,0.9)',
          borderWidth: 2,
          shadowBlur: 8,
          shadowColor: 'rgba(0,0,0,0.3)'
        },
        label: { color: SUB },
        data: list.map((i) => ({
          name: typeName(i.deviceType),
          value: i.total || 0,
          itemStyle: { color: typeColor(i.deviceType) }
        }))
      }
    ]
  }
}

function renderAlarmPie(d) {
  alarmPieOption.value = {
    tooltip: { trigger: 'item', ...darkTooltip() },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: 'rgba(10,14,39,0.9)', borderWidth: 2 },
        label: { color: SUB },
        data: [
          { name: '提示', value: d.level1 || 0, itemStyle: { color: PRIMARY } },
          { name: '警告', value: d.level2 || 0, itemStyle: { color: GOLD } },
          { name: '严重', value: d.level3 || 0, itemStyle: { color: DANGER } }
        ]
      }
    ]
  }
}

function renderAlarmTrend(list) {
  alarmTrendOption.value = {
    tooltip: { trigger: 'axis', ...darkTooltip() },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', ...axisCommon(), data: list.map((i) => i.date) },
    yAxis: { type: 'value', ...axisCommon() },
    series: [
      {
        name: '告警总数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: list.map((i) => i.count),
        lineStyle: { color: GOLD, width: 2 },
        itemStyle: { color: GOLD },
        areaStyle: { color: 'rgba(255,215,94,0.15)' }
      },
      {
        name: '严重告警',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: list.map((i) => i.level3),
        lineStyle: { color: DANGER, width: 2 },
        itemStyle: { color: DANGER }
      }
    ]
  }
}

function renderRealtime(list) {
  const items = list.slice(0, 8)
  pressureOption.value = {
    tooltip: { trigger: 'axis', ...darkTooltip() },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', ...axisCommon(), data: items.map((i) => i.deviceName) },
    yAxis: { type: 'value', ...axisCommon(), name: 'MPa' },
    series: [
      {
        name: '压力',
        type: 'bar',
        barWidth: '46%',
        data: items.map((i) => ({
          value: i.pressure,
          itemStyle: { borderRadius: [4, 4, 0, 0], color: typeColor(i.deviceType) }
        })),
        label: { show: true, position: 'top', color: SUB }
      }
    ]
  }
  const withVal = items.filter((i) => i.flow != null).slice(0, 8)
  realtimeBarOption.value = {
    tooltip: { trigger: 'axis', ...darkTooltip() },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'value', ...axisCommon() },
    yAxis: { type: 'category', ...axisCommon(), data: withVal.map((i) => i.deviceName).reverse() },
    series: [
      {
        name: '流量',
        type: 'bar',
        barWidth: '50%',
        data: withVal
          .map((i) => ({
            value: i.flow,
            itemStyle: {
              borderRadius: [0, 4, 4, 0],
              color: new grad(GREEN, 'horizontal')
            }
          }))
          .reverse(),
        label: { show: true, position: 'right', color: SUB }
      }
    ]
  }
}

function grad(color, dir = 'vertical') {
  return {
    type: 'linear',
    x: 0,
    y: 0,
    x2: dir === 'horizontal' ? 1 : 0,
    y2: dir === 'horizontal' ? 0 : 1,
    colorStops: [
      { offset: 0, color: color },
      { offset: 1, color: 'rgba(0,255,163,0.1)' }
    ]
  }
}

const SUB = '#8fa8cf'

onMounted(loadStats)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: var(--radius);
}
.sc-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sc-label {
  color: var(--text-sub);
  font-size: 13px;
  margin-bottom: 6px;
}
.sc-unit {
  margin-left: auto;
  align-self: flex-end;
  color: var(--text-dim);
}
.sc-line {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}
.row-charts {
  display: flex;
  gap: 16px;
}
.span-8 {
  flex: 1;
  min-width: 0;
}
.span-12 {
  flex: 1.5;
  min-width: 0;
}
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .row-charts {
    flex-wrap: wrap;
  }
  .span-8,
  .span-12 {
    flex: 1 1 100%;
  }
}
</style>