<template>
  <div class="page-container report">
    <!-- 生成表单 + 概览 -->
    <div class="report-top">
      <ChartCard title="生成报表" class="gen-card">
        <el-form label-width="90px">
          <el-form-item label="报表类型">
            <el-select v-model="genForm.reportType" style="width:100%">
              <el-option label="日报" value="daily" />
              <el-option label="周报" value="weekly" />
              <el-option label="月报" value="monthly" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="genForm.range"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width:100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="generating" :icon="Cpu" @click="generate">
              智能生成
            </el-button>
          </el-form-item>
        </el-form>
      </ChartCard>
      <div class="report-summary">
        <div class="sum-total glass">
          <div class="st-label">报表总数</div>
          <DatavNumber :value="summary.count ?? 0" :size="34" color="#00e5ff" />
        </div>
        <div class="sum-recent glass">
          <div class="st-label">最近生成</div>
          <div class="st-time num">{{ summary.lastTime || '—' }}</div>
        </div>
      </div>
    </div>

    <ChartCard title="历史报表" height="100%">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <span class="type-tag" :class="reportTagCls(row.reportType)">{{ typeName(row.reportType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reportName" label="报表名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="关联设备" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceName || '全部' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="180" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" size="small">
              {{ row.status === 1 ? '已生成' : '生成中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :icon="Download" @click="download(row)">下载</el-button>
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
    </ChartCard>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Cpu, Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import ChartCard from '@/components/ChartCard.vue'
import DatavNumber from '@/components/DatavNumber.vue'
import { pageReports, generateReport, reportSummary, downloadReport } from '@/api/report'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const generating = ref(false)
const query = reactive({ page: 1, size: 10 })
const genForm = ref({ reportType: 'daily', range: [] })
const summary = ref({})

const typeName = (t) => ({ daily: '日报', weekly: '周报', monthly: '月报' }[t] || t)
const reportTagCls = (t) => ({ daily: 'pressure', weekly: 'flow', monthly: 'quality' }[t] || 'pressure')

async function load() {
  loading.value = true
  try {
    const res = await pageReports({ ...query })
    rows.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (e) {
  } finally {
    loading.value = false
  }
}
async function loadSummary() {
  try {
    const res = await reportSummary()
    summary.value = res.data || {}
  } catch (e) {}
}
async function generate() {
  generating.value = true
  try {
    const [startTime, endTime] = genForm.value.range || []
    await generateReport({
      reportType: genForm.value.reportType,
      startTime: startTime || dayjs().subtract(1, 'day').format('YYYY-MM-DD 00:00:00'),
      endTime: endTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
    })
    ElMessage.success('报表生成成功')
    load()
    loadSummary()
  } catch (e) {
  } finally {
    generating.value = false
  }
}
async function download(row) {
  try {
    const res = await downloadReport(row.id)
    const blob = res.data
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.reportName || 'report'}.${(blob.type || '').includes('pdf') ? 'pdf' : 'xlsx'}`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

onMounted(() => {
  load()
  loadSummary()
})
</script>

<style scoped>
.report {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.report-top {
  display: flex;
  gap: 16px;
}
.gen-card {
  flex: 1;
}
.report-summary {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sum-total,
.sum-recent {
  flex: 1;
  border-radius: 14px;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}
.st-label {
  color: var(--text-sub);
  font-size: 13px;
}
.st-time {
  color: var(--text-sub);
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
@media (max-width: 1100px) {
  .report-top {
    flex-direction: column;
  }
  .report-summary {
    width: 100%;
    flex-direction: row;
  }
}
</style>