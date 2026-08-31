<template>
  <div class="page-container">
    <ChartCard title="角色管理" height="100%">
      <div class="toolbar">
        <span class="hint">角色编码：ADMIN / OPERATOR / VIEWER，用于 RBAC 权限控制</span>
      </div>
      <el-table :data="roleList" v-loading="loading" stripe height="540">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名" width="160" />
        <el-table-column prop="roleCode" label="编码" width="160">
          <template #default="{ row }">
            <span class="type-tag pressure">{{ row.roleCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="roleDesc" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status === 1 ? 'ok' : 'off'">{{ row.status === 1 ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPerm(row)">菜单权限</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ChartCard>

    <!-- 菜单权限 dialog -->
    <el-dialog v-model="permVisible" :title="`菜单权限 · ${currentRole?.roleName}`" width="480px">
      <div class="perm-hint">勾选该角色可见的菜单（保存后下次登录生效）</div>
      <el-tree
        ref="permTree"
        :data="menuTreeValue"
        show-checkbox
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ChartCard from '@/components/ChartCard.vue'
import { listRoles, menuTree } from '@/api/system'

const roleList = ref([])
const loading = ref(false)
const permVisible = ref(false)
const currentRole = ref({})
const permTree = ref(null)
const menuTreeValue = ref([])

async function load() {
  loading.value = true
  try {
    const res = await listRoles()
    roleList.value = res.data || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}
async function openPerm(row) {
  currentRole.value = row
  permVisible.value = true
  try {
    const res = await menuTree()
    menuTreeValue.value = res.data || []
    permTree.value && permTree.value.setCheckedKeys((row.roleMenus || []).map((r) => r.menuId))
  } catch (e) {}
}
async function savePerm() {
  const checked = permTree.value ? permTree.value.getCheckedKeys(true) : []
  // 前端演示：仅提示，实际权限由后端保存
  ElMessage.success(`已保存 ${checked.length} 个菜单权限（演示）`)
  permVisible.value = false
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 14px;
}
.hint {
  color: var(--text-dim);
  font-size: 13px;
}
.perm-hint {
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 10px;
}
</style>