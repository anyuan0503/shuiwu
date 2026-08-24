<template>
  <span class="dnum num" :style="{ color: color, fontSize: size + 'px' }" :title="title">
    {{ display }}
  </span>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: { type: [Number, String], default: 0 },
  decorate: { type: Boolean, default: true }, // 千分位
  color: { type: String, default: '#00e5ff' },
  size: { type: Number, default: 28 },
  title: { type: String, default: '' }
})

const display = ref('0')

function format(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return String(v)
  return props.decorate ? n.toLocaleString('en-US') : String(n)
}

let raf = null
function animateTo(target) {
  cancelAnimationFrame(raf)
  const from = Number(display.value.replace(/,/g, '') || 0)
  const to = Number(target) || 0
  if (from === to) {
    display.value = format(to)
    return
  }
  const start = performance.now()
  const dur = 800
  const step = (t) => {
    const p = Math.min(1, (t - start) / dur)
    const eased = 1 - Math.pow(1 - p, 3)
    const cur = from + (to - from) * eased
    display.value = format(cur)
    if (p < 1) raf = requestAnimationFrame(step)
    else display.value = format(to)
  }
  raf = requestAnimationFrame(step)
}

watch(
  () => props.value,
  (v) => animateTo(v)
)
onMounted(() => {
  display.value = format(props.value)
})
</script>

<style scoped>
.dnum {
  text-shadow: 0 0 14px currentColor;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}
</style>