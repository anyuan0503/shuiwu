<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo" @click="$router.push('/dashboard')">
        <span class="logo-icon">💧</span>
        <transition name="fade">
          <span class="logo-text" v-if="!collapsed">AQUA VISION</span>
        </transition>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#8fa8cf"
          active-text-color="#00e5ff"
          router
        >
          <template v-for="m in menuTree" :key="m.path">
            <el-sub-menu v-if="m.children && m.children.length" :index="m.path">
              <template #title>
                <el-icon><component :is="iconOf(m.icon)" /></el-icon>
                <span>{{ m.name }}</span>
              </template>
              <el-menu-item
                v-for="c in m.children"
                :key="c.path"
                :index="resolvePath(m.path, c.path)"
              >
                <el-icon><component :is="iconOf(c.icon)" /></el-icon>
                <span>{{ c.name }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="m.path">
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <template #title>{{ m.name }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main">
      <el-header class="header">
        <div class="header-left">
          <el-icon class="fold-btn" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentParent">{{ currentParent }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <TimeBox class="header-time" />
          <el-button
            class="screen-btn"
            circle
            title="进入全屏大屏"
            @click="$router.push('/bigscreen')"
          >
            <el-icon><Monitor /></el-icon>
          </el-button>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="30" class="avatar">
                {{ userStore.realName.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.realName }}</span>
              <span class="role-tag">{{ userStore.roleName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <el-footer class="footer" height="34px">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
          蜀ICP备2026050045号
        </a>
      </el-footer>
    </el-container>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="380px">
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { updatePassword } from '@/api/auth'
import { resolveMenuPath, buildMenuTree } from '@/utils/path'
import TimeBox from '@/components/TimeBox.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const pwdVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

// 组装侧边菜单树：后端返回扁平列表，按 path 前缀构建目录 -> 子菜单
const menuTree = computed(() => buildMenuTree(userStore.menus || []))

function iconOf(name) {
  const map = {
    Monitor: 'Monitor',
    Odometer: 'Odometer',
    DataBoard: 'DataBoard',
    Platform: 'Platform',
    SetUp: 'SetUp',
    User: 'User',
    Cpu: 'Cpu',
    Bell: 'Bell',
    Aim: 'Aim',
    DataAnalysis: 'DataAnalysis',
    ChatDotRound: 'ChatDotRound',
    Document: 'Document',
    DataLine: 'DataLine',
    PieChart: 'PieChart',
    Grid: 'Grid'
  }
  return map[name] || 'Menu'
}

// resolvePath 统一使用 @/utils/path 的 resolveMenuPath，与路由注册保持一致
function resolvePath(parent, child) {
  return resolveMenuPath(parent, child)
}

const activeMenu = computed(() => route.path)

const currentParent = computed(() => {
  const p = route.meta.parent
  return p ? String(p) : ''
})

function onCommand(cmd) {
  if (cmd === 'password') {
    pwdVisible.value = true
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  }
}

async function submitPwd() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    return ElMessage.warning('请填写完整')
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirm) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  try {
    await updatePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  } catch (e) {
    /* msg handled */
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: rgba(8, 12, 34, 0.9);
  border-right: 1px solid var(--border-color);
  transition: width 0.25s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--primary);
  font-weight: 800;
  letter-spacing: 2px;
  font-size: 17px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  white-space: nowrap;
}
.logo-icon {
  font-size: 22px;
  filter: drop-shadow(0 0 8px var(--primary));
}
.logo-text {
  background: linear-gradient(90deg, var(--primary), var(--green));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.menu-scroll {
  flex: 1;
}
.menu-scroll :deep(.el-menu) {
  border-right: none;
}
.menu-scroll :deep(.el-menu-item),
.menu-scroll :deep(.el-sub-menu__title) {
  transition: background 0.25s, color 0.25s;
}
.menu-scroll :deep(.el-menu-item:hover),
.menu-scroll :deep(.el-sub-menu__title:hover) {
  background: rgba(0, 229, 238, 0.08);
}
.menu-scroll :deep(.el-sub-menu .el-menu-item) {
  border-left: 1px solid rgba(0, 229, 238, 0.12);
}
/* 选中菜单：左侧发光指示条 */
.menu-scroll :deep(.el-menu-item.is-active) {
  position: relative;
  background: linear-gradient(90deg, rgba(0, 229, 238, 0.16), transparent);
  color: var(--primary);
}
.menu-scroll :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 2px;
  background: var(--primary);
  box-shadow: 0 0 10px var(--primary), 0 0 20px rgba(0, 229, 238, 0.5);
}

.main {
  height: 100vh;
}
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(8, 12, 34, 0.75);
  border-bottom: 1px solid var(--border-color);
  padding: 0 18px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.fold-btn {
  font-size: 20px;
  color: var(--text-sub);
  cursor: pointer;
}
.fold-btn:hover {
  color: var(--primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.screen-btn {
  background: transparent;
  border-color: var(--border-color);
  color: var(--primary);
}
.screen-btn:hover {
  color: #02131f;
  background: var(--primary);
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}
.user-chip:hover {
  background: var(--bg-hover);
}
.avatar {
  background: linear-gradient(135deg, var(--primary), var(--purple));
  color: #02131f;
  font-weight: 700;
}
.user-name {
  color: var(--text-main);
  font-size: 14px;
}
.role-tag {
  font-size: 12px;
  color: var(--green);
  border: 1px solid rgba(0, 255, 163, 0.4);
  padding: 1px 6px;
  border-radius: 4px;
}
.content {
  padding: 0;
  overflow: auto;
  background: transparent;
}
.footer {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: rgba(8, 12, 34, 0.9);
  border-top: 1px solid var(--border-color);
}
.footer a {
  color: var(--text-dim);
  font-size: 12px;
  letter-spacing: 1px;
  text-decoration: none;
  transition: color 0.2s;
}
.footer a:hover {
  color: var(--primary);
}
</style>