<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="告警规则管理"
    width="760px"
  >
    <div class="rule-toolbar">
      <el-button type="primary" size="small" :icon="Plus" @click="openAdd">新增规则</el-button>
    </div>
    <el-table :data="rules" v-loading="loading" stripe height="360">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="ruleName" label="规则名" width="150" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备" width="150" show-overflow-tooltip />
      <el-table-column prop="monitorField" label="监测项" width="110" />
      <el-table-column label="级别" width="80">
        <template #default="{ row }">
          <el-tag :color="levelColor(row.alarmLevel)" effect="dark" style="color:#061020;border:none">
            {{ levelName(row.alarmLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="阈值" width="130">
        <template #default="{ row }">
          {{ row.thresholdMin ?? '—' }} ~ {{ row.thresholdMax ?? '∞' }}
        </template>
      </el-table-column>
      <el-table-column label="启用" width="70">
        <template #default="{ row }">
          <el-switch :model-value="!!row.enabled" size="small" @change="(v) => toggleEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" title="规则编辑" width="520px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="规则名"><el-input v-model="form.ruleName" /></el-form-item>
        <el-form-item label="监测项">
          <el-select v-model="form.monitorField" style="width:100%">
            <el-option label="压力" value="pressure" />
            <el-option label="流量" value="flow" />
            <el-option label="pH" value="ph" />
            <el-option label="浊度" value="turbidity" />
            <el-option label="余氯" value="residualCl" />
            <el-option label="液位" value="level" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警级别">
          <el-select v-model="form.alarmLevel" style="width:100%">
            <el-option label="提示" :value="1" />
            <el-option label="警告" :value="2" />
            <el-option label="严重" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小值"><el-input-number v-model="form.thresholdMin" style="width:100%" /></el-form-item>
        <el-form-item label="最大值"><el-input-number v-model="form.thresholdMax" style="width:100%" /></el-form-item>
        <el-form-item label="窗口(分)"><el-input-number v-model="form.windowMinutes" style="width:100%" /></el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { listAlarmRules, addAlarmRule, updateAlarmRule, deleteAlarmRule } from '@/api/alarm'

const props = defineProps({
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['update:visible'])

const rules = ref([])
const loading = ref(false)
const editVisible = ref(false)
const form = ref({})

const levelName = (l) => ({ 1: '提示', 2: '警告', 3: '严重' }[l] || '未知')
const levelColor = (l) => ({ 1: '#00e5ff', 2: '#ffd75e', 3: '#ff4d6d' }[l] || '#00e5ff')

async function load() {
  loading.value = true
  try {
    const res = await listAlarmRules()
    rules.value = res.data || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}
function openAdd() {
  form.value = { ruleName: '', monitorField: 'pressure', alarmLevel: 2, thresholdMin: 0, thresholdMax: null, windowMinutes: 10, enabled: true, remark: '' }
  editVisible.value = true
}
function openEdit(row) {
  form.value = { ...row }
  editVisible.value = true
}
async function save() {
  try {
    if (form.value.id) await updateAlarmRule(form.value)
    else await addAlarmRule(form.value)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } catch (e) {}
}
async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除规则「${row.ruleName}」？`, '提示', { type: 'warning' })
  try {
    await deleteAlarmRule(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {}
}
async function toggleEnabled(row, v) {
  try {
    await updateAlarmRule({ ...row, enabled: v ? 1 : 0 })
    row.enabled = v
    ElMessage.success('更新成功')
  } catch (e) {}
}

watch(
  () => props.visible,
  (v) => {
    if (v) load()
  }
)
</script>