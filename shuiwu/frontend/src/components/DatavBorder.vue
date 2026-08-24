<template>
  <div class="datav-border" :style="{ width: width, height: height }">
    <div class="corner tl"></div>
    <div class="corner tr"></div>
    <div class="corner bl"></div>
    <div class="corner br"></div>
    <div class="line ltop"></div>
    <div class="line lbottom"></div>
    <div class="line lleft"></div>
    <div class="line lright"></div>
    <div class="dv-content">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  width: { type: String, default: '100%' },
  height: { type: String, default: '100%' },
  color: { type: String, default: 'rgba(0,229,255,0.8)' }
})
</script>

<style scoped>
.datav-border {
  position: relative;
}
.corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: var(--primary);
  border-style: solid;
  z-index: 2;
  filter: drop-shadow(0 0 6px var(--primary));
}
.tl { top: 0; left: 0; border-width: 2px 0 0 2px; }
.tr { top: 0; right: 0; border-width: 2px 2px 0 0; }
.bl { bottom: 0; left: 0; border-width: 0 0 2px 2px; }
.br { bottom: 0; right: 0; border-width: 0 2px 2px 0; }

.line { position: absolute; background: linear-gradient(90deg, transparent, rgba(0,229,255,0.35), transparent); }
.ltop { top: 0; left: 16px; right: 16px; height: 1px; }
.lbottom { bottom: 0; left: 16px; right: 16px; height: 1px; transform: rotate(180deg); }
.lleft { left: 0; top: 16px; bottom: 16px; width: 1px; }
.lright { right: 0; top: 16px; bottom: 16px; width: 1px; }
.dv-content {
  position: absolute;
  inset: 10px;
  z-index: 1;
  overflow: hidden;
}
</style>