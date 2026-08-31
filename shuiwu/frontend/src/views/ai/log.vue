<template>
  <div class="page-container">
    <ChartCard title="AI 分析日志" height="100%">
      <div class="log-toolbar">
        <el-select v-model="query.logType" clearable placeholder="日志类型" style="width:160px">
          <el-option label="Normal Query" value="normal" />
          <el-option label="Data Clean" value="clean" />
          <el-option label="Anomaly" value="anomaly" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="关键词" clearable style="width:220px" @keyup.enter="reload" />
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe height="560">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="logType" label="类型" width="130">
          <template #default="{ row }">
            <span class="status-tag" :class="logTagCls(row.logType)">{{ typeName(row.logType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题/描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="usedEngine" label="引擎" width="120" />
        <el-table-column prop="costTime" label="耗时(ms)" width="100">
          <template #default="{ row }">
            <span class="num">{{ row.costTime }}<small> ms</small></span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="query.size"
          :page-sizes="[10, 20, 50]"
          background
          @current-change="(p) => { query.page = p; load() }"
          @size-change="(s) => { query.size = s; reload() }"
        />
      </div>
    </ChartCard>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import { pageAiLog } from '@/api/ai'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, logType: '', keyword: '' })

const typeName = (t) => ({ normal: 'Normal Query', clean: 'Data Clean', anomaly: 'Anomaly' }[t] || t || '—')
const logTagCls = (t) => ({ normal: 'ok', clean: 'info', anomaly: 'fault' }[t] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await pageAiLog({ ...query })
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
onMounted(load)
</script>

<style scoped>
.log-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>