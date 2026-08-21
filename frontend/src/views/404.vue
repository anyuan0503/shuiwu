<template>
  <div class="nf">
    <div class="glitch num" data-text="404">404</div>
    <p class="nf-msg">页面去向不明，可能已渗入管网深处</p>
    <el-button type="primary" @click="$router.replace(nav)">返回首页</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
const store = useUserStore()
const nav = computed(() => (store.isLoggedIn ? '/dashboard' : '/login'))
</script>

<style scoped>
.nf {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}
.glitch {
  font-size: 120px;
  font-weight: 900;
  color: var(--primary);
  position: relative;
  text-shadow: 0 0 30px var(--primary);
}
.nf-msg {
  color: var(--text-sub);
}
</style>