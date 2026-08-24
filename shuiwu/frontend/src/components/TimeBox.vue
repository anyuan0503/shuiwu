<template>
  <div class="time-box">
    <span class="t-time num">{{ hhmmss }}</span>
    <span class="t-date">{{ dateStr }}</span>
    <span class="t-week">{{ weekStr }}</span>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'

const hhmmss = ref('')
const dateStr = ref('')
const weekStr = ref('')

let timer = null
function tick() {
  const now = dayjs()
  hhmmss.value = now.format('HH:mm:ss')
  dateStr.value = now.format('YYYY年MM月DD日')
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  weekStr.value = `星期${weeks[Number(now.day())]}`
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.time-box {
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
  color: var(--text-sub);
}
.t-time {
  font-size: 22px;
  color: var(--primary);
  text-shadow: 0 0 12px rgba(0, 229, 255, 0.5);
  letter-spacing: 2px;
}
.t-date,
.t-week {
  font-size: 13px;
  color: var(--text-sub);
}
@media (max-width: 900px) {
  .t-date,
  .t-week {
    display: none;
  }
}
</style>