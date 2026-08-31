<template>
  <div class="page-container ledger">
    <div class="toolbar glass">
      <span class="tb-title">设备台账</span>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增设备</el-button>
    </div>
    <div class="ledger-grid">
      <ChartCard title="设备类型状态分布" height="340px" class="span-4">
        <BaseChart :option="stackOption" height="300px" />
      </ChartCard>
      <div class="span-2">
        <ChartCard title="设备筛选" height="340px">
          <el-form label-position="top" class="filter-form">
            <el-form-item label="关键词">
              <el-input v-model="query.keyword" clearable placeholder="编号/名称/位置" @keyup.enter="reload" />
            </el-form-item>
            <el-form-item label="设备类型">
              <el-select v-model="query.deviceType" clearable placeholder="全部类型" style="width:100%">
                <el-option label="压力计" value="pressure" />
                <el-option label="流量计" value="flow" />
                <el-option label="水质仪" value="quality" />
                <el-option label="液位计" value="level" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部状态" style="width:100%">
                <el-option label="在线" :value="1" />
                <el-option label="离线" :value="0" />
                <el-option label="故障" :value="2" />
                <el-option label="停用" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
              <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </ChartCard>
      </div>
      <ChartCard title="台账明细" class="span-6">
        <el-table :data="rows" v-loading="loading" stripe height="520">
          <el-table-column prop="deviceNo" label="编号" width="110" />
          <el-table-column prop="deviceName" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <span class="type-tag" :class="typeTagCls(row.deviceType)">{{ typeName(row.deviceType) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="area" label="片区" width="110" show-overflow-tooltip />
          <el-table-column prop="model" label="型号" width="120" show-overflow-tooltip />
          <el-table-column prop="location" label="位置" min-width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <span class="status-tag" :class="statusTagCls(row.status)">{{ statusName(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" class="op-btn" @click="openEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button link type="danger" size="small" class="op-btn del" @click="onDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </template>
          </el-table-column>
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑设备' : '新增设备'" width="560px">
      <el-form :model="editForm" label-width="90px">
        <div class="two-col">
          <el-form-item label="编号"><el-input v-model="editForm.deviceNo" /></el-form-item>
          <el-form-item label="名称"><el-input v-model="editForm.deviceName" /></el-form-item>
          <el-form-item label="类型">
            <el-select v-model="editForm.deviceType" style="width:100%">
              <el-option label="压力计" value="pressure" />
              <el-option label="流量计" value="flow" />
              <el-option label="水质仪" value="quality" />
              <el-option label="液位计" value="level" />
            </el-select>
          </el-form-item>
          <el-form-item label="型号"><el-input v-model="editForm.model" /></el-form-item>
          <el-form-item label="片区"><el-input v-model="editForm.area" /></el-form-item>
          <el-form-item label="位置"><el-input v-model="editForm.location" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="editForm.status" style="width:100%">
              <el-option :label="statusName(1)" :value="1" />
              <el-option :label="statusName(0)" :value="0" />
              <el-option :label="statusName(2)" :value="2" />
              <el-option :label="statusName(3)" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="单位"><el-input v-model="editForm.unit" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import { pageDevices, deleteDevice, addDevice, updateDevice, onlineCount } from '@/api/device'
import { axisCommon, darkTooltip } from '@/utils/echartsTheme'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', deviceType: '', status: '' })

const editVisible = ref(false)
const editForm = ref({})

const typeName = (t) => ({ pressure: '压力', flow: '流量', quality: '水质', level: '液位' }[t] || t)
const typeBg = (t) => ({ pressure: '#00e5ee', flow: '#26e890', quality: '#ffc136', level: '#8b7bff' }[t] || '#00e5ee')
const typeTagCls = (t) => ({ pressure: 'pressure', flow: 'flow', quality: 'quality', level: 'level' }[t] || 'pressure')
const statusName = (s) => ({ 1: '在线', 0: '离线', 2: '故障', 3: '停用' }[s] || '未知')
const statusTagCls = (s) => ({ 1: 'ok', 0: 'off', 2: 'fault', 3: 'stop' }[s] || 'off')

const stackOption = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageDevices({ ...query })
    rows.value = res.data.list || []
    total.value = res.data.total || 0
    renderStack(res.data.list || [])
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function renderStack(list) {
  const types = ['pressure', 'flow', 'quality', 'level']
  const data = types.map((t) => ({
    name: typeName(t),
    total: 0,
    online: 0,
    offline: 0,
    fault: 0,
    color: typeBg(t)
  }))
  list.forEach((r) => {
    const d = data.find((x) => x.name === typeName(r.deviceType))
    if (!d) return
    d.total++
    if (r.status === 1) d.online++
    else if (r.status === 0) d.offline++
    else if (r.status === 2) d.fault++
  })
  stackOption.value = {
    tooltip: { trigger: 'axis', ...darkTooltip() },
    legend: { bottom: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 6, itemGap: 14, textStyle: { color: '#8fa8cf', fontSize: 12 } },
    grid: { left: 40, right: 20, top: 10, bottom: 44 },
    xAxis: { type: 'category', ...axisCommon(), data: data.map((d) => d.name) },
    yAxis: { type: 'value', ...axisCommon() },
    series: [
      { name: '在线', type: 'bar', stack: 'a', barWidth: 22, data: data.map((d) => d.online), itemStyle: { color: '#26e890', borderRadius: [0, 0, 0, 0] } },
      { name: '离线', type: 'bar', stack: 'a', barWidth: 22, data: data.map((d) => d.offline), itemStyle: { color: '#667488' } },
      { name: '故障', type: 'bar', stack: 'a', barWidth: 22, data: data.map((d) => d.fault), itemStyle: { color: '#ff4455', borderRadius: [3, 3, 0, 0] } }
    ]
  }
}

function reload() {
  query.page = 1
  load()
}
function resetQuery() {
  query.keyword = ''
  query.deviceType = ''
  query.status = ''
  reload()
}
function openEdit(row) {
  editForm.value = { ...row }
  editVisible.value = true
}
function openAdd() {
  editForm.value = {
    deviceNo: '',
    deviceName: '',
    deviceType: 'pressure',
    model: '',
    location: '',
    area: '',
    status: 1,
    unit: ''
  }
  editVisible.value = true
}
async function save() {
  try {
    if (editForm.value.id) await updateDevice(editForm.value)
    else await addDevice(editForm.value)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } catch (e) {}
}
async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除设备「${row.deviceName}」？`, '提示', { type: 'warning' })
  try {
    await deleteDevice(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {}
}

onMounted(() => {
  load()
  onlineCount().then((res) => {
    // 用于饼图可选
  })
})
</script>

<style scoped>
.ledger-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 18px;
  border-radius: var(--radius);
}
.tb-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary);
}
.span-4 { grid-column: span 4; }
.span-2 { grid-column: span 2; }
.span-6 { grid-column: 1 / -1; }
.filter-form :deep(.el-form-item) {
  margin-bottom: 14px;
}
.filter-form :deep(.el-form-item__label) {
  font-size: 12px;
  color: var(--text-sub);
  padding-bottom: 4px;
  line-height: 1.4;
}
.filter-form :deep(.el-form-item__content) {
  width: 100%;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
/* 操作按钮：图标 + 小字 */
.op-btn {
  gap: 3px;
  font-size: 12px;
}
.op-btn.del {
  opacity: 0.75;
}
.op-btn.del:hover {
  opacity: 1;
}
/* 表格奇偶行微弱色差 */
.ledger :deep(.el-table .el-table__row--striped td.el-table__cell) {
  background: rgba(255, 255, 255, 0.03);
}
.ledger :deep(.el-table .el-table__row--striped:hover > td.el-table__cell) {
  background: var(--bg-hover);
}
@media (max-width: 1200px) {
  .span-4, .span-2 { grid-column: 1 / -1; }
}
</style>