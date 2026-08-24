<template>
  <div class="login-page">
    <!-- 动态粒子网格背景 -->
    <canvas ref="canvas" class="bg-canvas"></canvas>
    <div class="grid-overlay"></div>
    <div class="scanline"></div>

    <div class="login-wrap">
      <div class="logo-title">
        <div class="logo-badge">💧</div>
        <h1 class="title-gradient">AQUA VISION</h1>
        <p class="slogan">水务监测智能分析平台 · WATER MONITORING INTELLIGENCE</p>
        <div class="sub-line"></div>
      </div>

      <div class="login-card glass">
        <div class="card-head">
          <span class="ch-title">用户登录</span>
          <span class="ch-en">SIGN IN</span>
        </div>
        <el-form :model="form" @keyup.enter="doLogin">
          <el-form-item>
            <el-input
              v-model="form.username"
              placeholder="账号"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <div class="row">
            <el-checkbox v-model="remember">记住我</el-checkbox>
          </div>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="doLogin"
          >
            登 入 系 统
          </el-button>
        </el-form>
        <div class="tips">
          <span>admin / 123456</span><i>·</i>
          <span>operator / 123456</span><i>·</i>
          <span>viewer / 123456</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const canvas = ref(null)
const form = ref({
  username: localStorage.getItem('sw_remember_user') || 'admin',
  password: localStorage.getItem('sw_remember_pwd') || '',
  remember: !!localStorage.getItem('sw_remember_pwd')
})
const loading = ref(false)

function doLogin() {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('请输入账号和密码')
  }
  loading.value = true
  userStore
    .login({ username: form.value.username, password: form.value.password })
    .then(() => {
      if (form.value.remember) {
        localStorage.setItem('sw_remember_user', form.value.username)
        localStorage.setItem('sw_remember_pwd', form.value.password)
      } else {
        localStorage.removeItem('sw_remember_user')
        localStorage.removeItem('sw_remember_pwd')
      }
      ElMessage.success('登录成功')
      const redirect = route.query.redirect
      router.push(redirect || '/dashboard')
    })
    .catch(() => {})
    .finally(() => (loading.value = false))
}

// 粒子背景
let raf = null
let particles = []

function initParticles() {
  const c = canvas.value
  const ctx = c.getContext('2d')
  let w, h
  const resize = () => {
    w = c.width = window.innerWidth
    h = c.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const N = 80
  particles = Array.from({ length: N }).map(() => ({
    x: Math.random() * (w || 1),
    y: Math.random() * (h || 1),
    vx: (Math.random() - 0.5) * 0.6,
    vy: (Math.random() - 0.5) * 0.6,
    r: Math.random() * 2 + 0.6
  }))

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(0,229,255,0.55)'
      ctx.fill()
    })
    // 连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i]
        const b = particles[j]
        const d = Math.hypot(a.x - b.x, a.y - b.y)
        if (d < 130) {
          ctx.strokeStyle = `rgba(0,229,255,${(1 - d / 130) * 0.35})`
          ctx.lineWidth = 1
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }
    raf = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => initParticles())
onBeforeUnmount(() => cancelAnimationFrame(raf))
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 50% -20%, #10204e 0%, #0a0e27 55%, #05070f 100%);
}
.bg-canvas {
  position: absolute;
  inset: 0;
}
.grid-overlay {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(0,229,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,229,255,0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.9), transparent 75%);
}
.scanline {
  position: absolute;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(180deg, transparent, rgba(0,229,255,0.06), transparent);
  animation: scanline 7s linear infinite;
}
.login-wrap {
  position: relative;
  z-index: 2;
  width: 400px;
  max-width: 92vw;
}
.logo-title {
  text-align: center;
  margin-bottom: 28px;
}
.logo-badge {
  font-size: 52px;
  filter: drop-shadow(0 0 18px var(--primary));
  animation: breathe 2.5s infinite;
  margin-bottom: 6px;
}
.title-gradient {
  font-size: 34px;
  margin: 0;
  letter-spacing: 8px;
  background: linear-gradient(90deg, var(--primary), var(--green), var(--primary));
  background-size: 200% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: shimmer 4s linear infinite;
}
@keyframes shimmer {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
.slogan {
  color: var(--text-dim);
  font-size: 12px;
  letter-spacing: 2px;
  margin: 8px 0 0;
}
.sub-line {
  width: 60px;
  height: 2px;
  margin: 12px auto 0;
  background: linear-gradient(90deg, transparent, var(--primary), transparent);
}
.login-card {
  border-radius: 14px;
  padding: 30px 30px 22px;
}
.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 22px;
}
.ch-title {
  font-size: 20px;
  color: var(--primary);
  font-weight: 700;
  position: relative;
  padding-left: 12px;
}
.ch-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: var(--green);
  box-shadow: 0 0 10px var(--green);
}
.ch-en {
  color: var(--text-dim);
  letter-spacing: 3px;
  font-size: 12px;
}
.row {
  margin-bottom: 18px;
}
.login-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 6px;
  font-weight: 600;
}
.tips {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: var(--text-dim);
  font-size: 12px;
}
</style>