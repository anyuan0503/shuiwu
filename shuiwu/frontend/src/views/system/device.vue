<template>
  <div class="page-container">
    <ChartCard title="设备管理（系统级）" height="100%">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="编号/名称" clearable style="width:220px" @keyup.enter="reload" />
        <el-select v-model="query.deviceType" clearable placeholder="类型" style="width:130px">
          <el-option label="压力计" value="pressure" />
          <el-option label="流量计" value="flow" />
          <el-option label="水质仪" value="quality" />
          <el-option label="液位计" value="level" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe height="560">
        <el-table-column prop="deviceNo" label="编号" width="110" />
        <el-table-column prop="deviceName" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :color="typeBg(row.deviceType)" style="color:#061020;border:none">
              {{ typeName(row.deviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="manufacturer" label="厂商" width="140" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column prop="installDate" label="安装日期" width="130" />
        <el-table-column prop="lon" label="经度" width="110" />
        <el-table-column prop="lat" label="纬度" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark" size="small">
              {{ statusName(row.status) }}
            </el-tag>
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
import { Search } from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import { pageDevices } from '@/api/device'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', deviceType: '' })

const typeName = (t) => ({ pressure: '压力', flow: '流量', quality: '水质', level: '液位' }[t] || t)
const typeBg = (t) => ({ pressure: '#00e5ff', flow: '#00ffa3', quality: '#ffd75e', level: '#8b7bff' }[t] || '#00e5ff')
const statusName = (s) => ({ 1: '在线', 0: '离线', 2: '故障', 3: '停用' }[s] || '未知')
const statusType = (s) => ({ 1: 'success', 0: 'info', 2: 'danger', 3: 'warning' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await pageDevices({ ...query })
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
.toolbar {
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