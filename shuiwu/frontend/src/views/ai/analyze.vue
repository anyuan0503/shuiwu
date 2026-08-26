<template>
  <div class="page-container ai">
    <!-- 顶部输入区 -->
    <div class="ai-input glass">
      <div class="ai-head">
        <span class="ai-badge">🧠</span>
        <div>
          <div class="ai-title">水务智能问答</div>
          <div class="ai-sub">面向自然语言的智能数据分析引擎 · NL2SQL</div>
        </div>
        <span class="ai-engine" v-if="lastResult.usedEngine">引擎：{{ lastResult.usedEngine }}</span>
      </div>

      <div class="ai-ask">
        <el-input
          v-model="question"
          size="large"
          placeholder="用自然语言提问，例如：查询今天各片区用水量"
          :prefix-icon="ChatDotRound"
          @keyup.enter="submit"
          clearable
        />
        <el-button type="primary" size="large" :loading="loading" @click="submit">
          解析查数
        </el-button>
      </div>

      <!-- 示例问题 -->
      <div class="ai-samples">
        <el-tag
          v-for="q in samples"
          :key="q"
          class="sample-tag"
          :effect="'plain'"
          @click="ask(q)"
        >
          {{ q }}
        </el-tag>
      </div>
    </div>

    <!-- 结果区 -->
    <div class="ai-result" v-if="hasResult">
      <div class="ai-panel glass">
        <PanelTitle>智能回答</PanelTitle>
        <!-- 打字机效果 -->
        <div class="answer-box num">
          <span v-for="(w, i) in answerWords" :key="i" :class="{ caret: i === answerWords.length - 1 }">{{ w }}</span>
        </div>
        <div class="answer-sql" v-if="lastResult.rawSql">
          <span class="sql-label">SQL</span>
          <code>{{ lastResult.rawSql }}</code>
        </div>
      </div>

      <!-- 图表 -->
      <div class="ai-panel glass" v-if="chartOption">
        <PanelTitle>{{ lastResult.chartType === 'table' ? '数据明细' : (lastResult.title || '可视化分析') }}</PanelTitle>
        <!-- 流式加载动画占位消失后 -->
        <div class="chart-holder">
          <BaseChart v-if="!loadingRender" :option="chartOption" height="360px" />
          <div v-else class="loading-render">
            <span class="spinner"></span><span>分析数据中…</span>
          </div>
        </div>
        <div class="table-result" v-if="tableData && tableData.length">
          <div class="tr-table">
            <table>
              <thead>
                <tr><th v-for="h in tableHead" :key="h">{{ h }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="(r, i) in tableData.slice(0, 8)" :key="i">
                  <td v-for="h in tableHead" :key="h">{{ r[h] }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 空态引导 -->
    <div class="ai-empty glass" v-if="!hasResult && !loading">
      <div class="empty-big">💡</div>
      <p>试试上方「示例问题」，或输入你自己的问题，让 AI 为你解读水务数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import PanelTitle from '@/components/PanelTitle.vue'
import BaseChart from '@/components/BaseChart.vue'
import { nlsql } from '@/api/ai'
import { axisCommon, darkTooltip, PRIMARY, GREEN, GOLD, PURPLE, DANGER } from '@/utils/echartsTheme'

const question = ref('')
const loading = ref(false)
const loadingRender = ref(false)
const lastResult = ref({})
const answer = ref('')
const answerWords = ref([])
const chartOption = ref(null)
const tableData = ref([])

const hasResult = computed(() => Object.keys(lastResult.value).length > 0)

const samples = [
  '查询今天各片区用水量',
  '哪些设备压力低于0.2MPa',
  '最近7天告警统计',
  '展示各类型设备在线数量',
  '查询水质异常的设备'
]

function ask(q) {
  question.value = q
  submit()
}

async function submit() {
  const q = question.value.trim()
  if (!q) return ElMessage.warning('请输入问题')
  loading.value = true
  loadingRender.value = true
  try {
    const res = await nlsql({ question: q })
    const d = res.data || {}
    lastResult.value = d
    answer.value = d.answer || '（无自然语言回答）'
    typewrite(answer.value)
    tableData.value = d.tableData || []
    renderChart(d.chartConfig)
  } catch (e) {
    ElMessage.error('分析失败：' + (e.message || '服务异常'))
  } finally {
    loading.value = false
    loadingRender.value = false
  }
}

let typeTimer = null
function typewrite(text) {
  clearInterval(typeTimer)
  const words = Array.from(text)
  answerWords.value = []
  let i = 0
  typeTimer = setInterval(() => {
    answerWords.value.push(words[i])
    i++
    if (i >= words.length) clearInterval(typeTimer)
  }, 40)
}

const COLOR_SET = [PRIMARY, GREEN, GOLD, PURPLE, DANGER]

function renderChart(cfg) {
  if (!cfg || !cfg.chartType) {
    // 纯文本结果，构造简单表格或柱状
    chartOption.value = null
    return
  }
  const t = cfg.chartType
  const xData = cfg.x || []
  const series = cfg.series || []

  if (t === 'table') {
    chartOption.value = null
    return
  }
  if (t === 'pie') {
    chartOption.value = {
      tooltip: { trigger: 'item', ...darkTooltip() },
      legend: { bottom: 0, icon: 'circle' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: 'rgba(10,14,39,0.9)', borderWidth: 2 },
          label: { color: SUB },
          data: xData.map((name, i) => {
            const s = series[0] || {}
            return { name, value: s.data?.[i] ?? 0, itemStyle: { color: COLOR_SET[i % COLOR_SET.length] } }
          })
        }
      ]
    }
  } else {
    const isBar = t === 'bar' || t === 'histogram'
    chartOption.value = {
      tooltip: { trigger: 'axis', ...darkTooltip() },
      legend: series.length > 1 ? { bottom: 0 } : undefined,
      grid: { left: 50, right: 20, top: 24, bottom: 34 },
      xAxis: { type: 'category', ...axisCommon(), data: xData },
      yAxis: { type: 'value', ...axisCommon(), name: series[0]?.unit || '' },
      series: series.map((s, si) => ({
        name: s.name || '数值',
        type: isBar ? 'bar' : 'line',
        smooth: !isBar,
        barWidth: '50%',
        showSymbol: !isBar,
        data: s.data || [],
        lineStyle: { color: COLOR_SET[si % COLOR_SET.length], width: 2 },
        itemStyle: isBar
          ? { borderRadius: [4, 4, 0, 0], color: COLOR_SET[si % COLOR_SET.length] }
          : { color: COLOR_SET[si % COLOR_SET.length] },
        areaStyle: isBar
          ? undefined
          : { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: COLOR_SET[si % COLOR_SET.length] + '44' }, { offset: 1, color: 'transparent' }] } }
      }))
    }
  }
}

const tableHead = computed(() => {
  const data = tableData.value
  if (!data || !data.length) return []
  return Object.keys(data[0]).filter((k) => !['id', '_id'].includes(k))
})

const SUB = '#8fa8cf'

onBeforeUnmount(() => clearInterval(typeTimer))
</script>

<style scoped>
.ai {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ai-input {
  border-radius: 14px;
  padding: 22px 24px;
}
.ai-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.ai-badge {
  font-size: 30px;
  filter: drop-shadow(0 0 10px var(--primary));
}
.ai-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: 2px;
}
.ai-sub {
  color: var(--text-dim);
  font-size: 12px;
}
.ai-engine {
  margin-left: auto;
  color: var(--green);
  font-size: 13px;
  border: 1px solid rgba(0, 255, 163, 0.4);
  padding: 2px 10px;
  border-radius: 20px;
}
.ai-ask {
  display: flex;
  gap: 10px;
}
.ai-samples {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.sample-tag {
  cursor: pointer;
  border-color: var(--border-color);
  background: transparent;
  color: var(--text-sub);
}
.sample-tag:hover {
  color: var(--primary);
  border-color: var(--primary);
}
.ai-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ai-panel {
  border-radius: 14px;
  padding: 4px 18px 18px;
}
.answer-box {
  min-height: 48px;
  padding: 12px 4px;
  color: var(--text-main);
  line-height: 1.8;
  font-size: 15px;
}
.answer-box .caret {
  border-right: 2px solid var(--primary);
  animation: caretBlink 0.7s steps(1) infinite;
}
@keyframes caretBlink {
  50% { border-color: transparent; }
}
.answer-sql {
  margin-top: 12px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.sql-label {
  color: var(--gold);
  font-weight: 700;
  font-size: 12px;
  padding: 2px 8px;
  border: 1px solid rgba(255, 215, 94, 0.5);
  border-radius: 4px;
  flex-shrink: 0;
}
.answer-sql code {
  color: var(--green);
  font-family: var(--font-num);
  font-size: 13px;
  word-break: break-all;
}
.chart-holder {
  margin-top: 8px;
}
.loading-render {
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-sub);
}
.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(0, 229, 255, 0.2);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.table-result {
  margin-top: 10px;
}
.tr-table {
  max-height: 220px;
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
.tr-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.tr-table th,
.tr-table td {
  border-bottom: 1px solid rgba(0, 229, 255, 0.12);
  padding: 6px 10px;
  text-align: left;
  white-space: nowrap;
}
.tr-table th {
  color: var(--primary);
  background: rgba(0, 229, 255, 0.07);
  position: sticky;
  top: 0;
}
.ai-empty {
  border-radius: 14px;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-dim);
}
.empty-big {
  font-size: 44px;
  filter: drop-shadow(0 0 12px var(--primary));
}
</style>