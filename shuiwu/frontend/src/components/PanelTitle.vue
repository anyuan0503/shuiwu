<template>
  <div class="panel-title">
    <span class="pt-deco"></span>
    <slot />
  </div>
</template>

<script setup></script>

<style scoped>
.panel-title {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 3px;
  padding: 10px 16px;
  text-shadow: 0 0 18px var(--primary);
}
.panel-title::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--primary), transparent 70%);
  opacity: 0.6;
}
.pt-deco {
  width: 6px;
  height: 18px;
  background: linear-gradient(180deg, var(--primary), var(--green));
  box-shadow: 0 0 12px var(--primary);
  animation: breathe 2.4s infinite;
}
@keyframes breathe {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}
</style>