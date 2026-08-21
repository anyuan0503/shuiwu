<template>
  <div class="chart-card glass">
    <div class="cc-head" v-if="title">
      <span class="cc-title">{{ title }}</span>
      <span class="cc-sub" v-if="subtitle">{{ subtitle }}</span>
      <div class="cc-extra"><slot name="extra" /></div>
    </div>
    <div class="cc-body" :style="{ height }">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  height: { type: String, default: '300px' }
})
</script>

<style scoped>
.chart-card {
  position: relative;
  border-radius: var(--radius);
  overflow: hidden;
  transition: box-shadow 0.3s;
  display: flex;
  flex-direction: column;
}
.chart-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--primary));
}
.chart-card:hover {
  box-shadow: var(--glow-primary);
}
.cc-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px 0;
}
.cc-title {
  position: relative;
  color: var(--primary);
  font-weight: 600;
  font-size: 15px;
  padding-left: 10px;
  letter-spacing: 1px;
}
.cc-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  background: linear-gradient(180deg, var(--primary), var(--green));
  border-radius: 2px;
  box-shadow: 0 0 8px var(--primary);
}
.cc-sub {
  color: var(--text-dim);
  font-size: 12px;
}
.cc-extra {
  margin-left: auto;
}
.cc-body {
  padding: 8px 12px 12px;
  flex: 1;
  min-height: 0;
}
</style>