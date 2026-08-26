<template>
  <div class="page-container alarm">
    <div class="alarm-summary">
      <div class="sum-item glass" v-for="s in summaryList" :key="s.label">
        <span class="si-label">{{ s.label }}</span>
        <DatavNumber :value="s.value" :color="s.color" :size="26" />
      </div>
    </div>

    <div class="alarm-body">
      <ChartCard title="告警分级分布" class="col-chart">
        <div class="level-bars">
          <div v-for="l in levelRows" :key="l.level" class="lbar">
            <span class="lb-name" :style="{ color: l.color }">L{{ l.level }} {{ l.name }}</span>
            <el-progress
              :percentage="l.percent"
              :stroke-width="8"
              :color="l.color"
              :show-text="true"
              :format="() => l.count + ' 条'"
            />
          </div>
        </div>
      </ChartCard>
      <ChartCard title="近7日告警趋势" class="col-chart">
        <BaseChart :option="trendOption" height="240px" />
      </ChartCard>
    </div>

    <div class="alarm-table glass">
      <div class="at-toolbar">
        <PanelTitle>告警记录</PanelTitle>
        <div class="at-filters">
          <el-select v-model="query.alarmLevel" clearable placeholder="告警级别" style="width:120px">
            <el-option label="提示" :value="1" />
            <el-option label="警告" :value="2" />
            <el-option label="严重" :value="3" />
          </el-select>
          <el-select v-model="query.alarmStatus" clearable placeholder="处理状态" style="width:130px">
            <el-option label="未处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已忽略" :value="3" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
          <el-button type="success" :icon="Setting" @click="ruleVisible = true">告警规则</el-button>
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" stripe height="460">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="deviceName" label="设备" width="160" show-overflow-tooltip />
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag :color="levelColor(row.alarmLevel)" effect="dark" style="color:#061020;border:none">
              {{ levelName(row.alarmLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmDesc" label="告警描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="alarmTime" label="告警时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.alarmStatus)" effect="plain" size="small">
              {{ statusName(row.alarmStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" min-width="140" show-overflow-tooltip>
          <template #default="{ row }"> {{ row.handleResult || '—' }} </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.alarmStatus === 0" link type="primary" size="small" @click="openHandle(row)">处理</el-button>
            <el-button v-if="row.alarmStatus === 0" link type="warning" size="small" @click="ignore(row)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="query.size"
          background
          @current-change="(p) => { query.page = p; load() }"
        />
      </div>
    </div>

    <!-- 处理对话框 -->
    <el-dialog v-model="handleVisible" title="处理告警" width="420px">
      <p style="color:var(--text-sub)">{{ current.row?.deviceName }} · {{ current.row?.alarmDesc }}</p>
      <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="请输入处理结果" />
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>

    <!-- 规则管理对话框 -->
    <RuleDialog :visible="ruleVisible" @update:visible="ruleVisible = $event" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Search, Setting } from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import DatavNumber from '@/components/DatavNumber.vue'
import PanelTitle from '@/components/PanelTitle.vue'
import RuleDialog from '@/views/alarm/RuleDialog.vue'
import { pageAlarms, handleAlarm, ignoreAlarm, alarmSummary, alarmTrend } from '@/api/alarm'
import { axisCommon, darkTooltip, PRIMARY, GOLD, DANGER } from '@/utils/echartsTheme'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, alarmLevel: '', alarmStatus: '' })

const summary = ref({ total: 0, level1: 0, level2: 0, level3: 0, unhandled: 0 })
const summaryList = computed(() => [
  { label: '告警总数', value: summary.value.total, color: PRIMARY },
  { label: '严重告警', value: summary.value.level3, color: DANGER },
  { label: '待处理', value: summary.value.unhandled, color: GOLD },
  { label: '今日告警', value: summary.value.today, color: '#00ffa3' }
])
const levelRows = computed(() => {
  const t = (summary.value.level1 || 0) + (summary.value.level2 || 0) + (summary.value.level3 || 0) || 1
  return [
    { level: 3, name: '严重', color: DANGER, count: summary.value.level3, percent: Math.round(((summary.value.level3 || 0) / t) * 100) },
    { level: 2, name: '警告', color: GOLD, count: summary.value.level2, percent: Math.round(((summary.value.level2 || 0) / t) * 100) },
    { level: 1, name: '提示', color: PRIMARY, count: summary.value.level1, percent: Math.round(((summary.value.level1 || 0) / t) * 100) }
  ]
})

const levelName = (l) => ({ 1: '提示', 2: '警告', 3: '严重' }[l] || '未知')
const levelColor = (l) => ({ 1: '#00e5ff', 2: '#ffd75e', 3: '#ff4d6d' }[l] || '#00e5ff')
const statusName = (s) => ({ 0: '未处理', 1: '处理中', 2: '已处理', 3: '已忽略' }[s] || '未知')
const statusType = (s) => ({ 0: 'danger', 1: 'warning', 2: 'success', 3: 'info' }[s] || 'info')

const handleVisible = ref(false)
const current = ref({})
const handleForm = ref({ handleResult: '' })
const ruleVisible = ref(false)

const trendOption = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageAlarms({ ...query })
    rows.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}
function reload() {
  query.page = 1
  load()
}

async function loadSummary() {
  try {
    const res = await alarmSummary()
    summary.value = res.data || {}
  } catch (e) {}
  try {
    const t = await alarmTrend()
    const list = t.data || []
    trendOption.value = {
      tooltip: { trigger: 'axis', ...darkTooltip() },
      grid: { left: 36, right: 16, top: 20, bottom: 28 },
      xAxis: { type: 'category', ...axisCommon(), data: list.map((i) => i.date) },
      yAxis: { type: 'value', ...axisCommon() },
      series: [
        {
          name: '告警数',
          type: 'bar',
          barWidth: '50%',
          data: list.map((i) => i.count),
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#00e5ff' }, { offset: 1, color: 'rgba(0,229,255,0.1)' }] }
          }
        }
      ]
    }
  } catch (e) {}
}

function openHandle(row) {
  current.value = { row }
  handleForm.value.handleResult = ''
  handleVisible.value = true
}
async function submitHandle() {
  if (!handleForm.value.handleResult) return ElMessage.warning('请输入处理结果')
  try {
    await handleAlarm({ id: current.value.row.id, handleResult: handleForm.value.handleResult })
    ElMessage.success('处理成功')
    handleVisible.value = false
    load()
    loadSummary()
  } catch (e) {}
}
async function ignore(row) {
  await ElMessageBox.confirm('确认忽略该告警？', '提示', { type: 'warning' })
  try {
    await ignoreAlarm({ id: row.id })
    ElMessage.success('已忽略')
    load()
    loadSummary()
  } catch (e) {}
}

onMounted(() => {
  load()
  loadSummary()
})
</script>

<style scoped>
.alarm {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.alarm-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.sum-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 16px 20px;
  border-radius: var(--radius);
}
.si-label {
  color: var(--text-sub);
  font-size: 13px;
}
.alarm-body {
  display: flex;
  gap: 16px;
}
.col-chart {
  flex: 1;
}
.level-bars {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 10px 6px;
}
.lbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lb-name {
  width: 60px;
  font-weight: 600;
  font-size: 13px;
}
.at-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.at-filters {
  display: flex;
  gap: 8px;
  align-items: center;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px 4px;
}
@media (max-width: 1100px) {
  .alarm-summary {
    grid-template-columns: repeat(2, 1fr);
  }
  .alarm-body {
    flex-direction: column;
  }
}
</style>