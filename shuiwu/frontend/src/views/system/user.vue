<template>
  <div class="page-container">
    <ChartCard title="用户管理" height="100%">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width:220px" @keyup.enter="reload" />
        <el-button type="primary" :icon="Search" @click="reload">查询</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe height="540">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <span v-for="r in row.roles" :key="r.id" class="type-tag level" style="margin-right:4px">
              {{ r.roleName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(v) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
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

    <!-- 编辑/新增 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width:100%" placeholder="请选择角色">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" v-if="!form.id">
          <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
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
import { Search, Plus } from '@element-plus/icons-vue'
import ChartCard from '@/components/ChartCard.vue'
import { pageUsers, addUser, updateUser, updateUserStatus, deleteUser, listRoles } from '@/api/system'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const roleList = ref([])
const query = reactive({ page: 1, size: 10, keyword: '' })
const editVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageUsers({ ...query })
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
function openAdd() {
  form.value = { username: '', realName: '', phone: '', email: '', password: '', roleIds: [] }
  editVisible.value = true
}
function openEdit(row) {
  form.value = {
    ...row,
    roleIds: (row.roles || []).map((r) => r.id)
  }
  editVisible.value = true
}
async function save() {
  const payload = { ...form.value }
  if (payload.password === '' || payload.password == null) delete payload.password
  try {
    if (form.value.id) {
      delete payload.password
      await updateUser(payload)
    } else await addUser(payload)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } catch (e) {}
}
async function toggleStatus(row, v) {
  try {
    await updateUserStatus(row.id, v ? 1 : 0)
    row.status = v ? 1 : 0
    ElMessage.success('状态更新')
  } catch (e) {}
}
async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {}
}

onMounted(() => {
  load()
  listRoles().then((res) => (roleList.value = res.data || [])).catch(() => {})
})
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