<template>
  <section class="launch-shell">
    <div class="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.26em] text-text-muted">Launch Pad</p>
        <h2 class="mt-3 text-[clamp(30px,3.4vw,46px)] font-semibold tracking-[-0.05em] text-text-primary">
          从当前状态出发，进入今天最适合你的模块。
        </h2>
      </div>
      <p class="max-w-[460px] text-sm leading-7 text-text-secondary">
        主模块更突出，辅助模块更轻盈。视觉上就能看出今天应该把注意力先放在哪里。
      </p>
    </div>

    <div class="launch-grid">
      <RouterLink
        v-for="item in modules"
        :key="item.path"
        :id="item.anchorId"
        :to="item.path"
        class="launch-card"
        :class="item.variant === 'feature' ? 'launch-card-feature' : 'launch-card-compact'"
      >
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-[12px] font-semibold uppercase tracking-[0.18em] text-text-muted">{{ item.kicker }}</p>
            <h3 class="mt-4 text-[clamp(24px,2.6vw,38px)] font-semibold tracking-[-0.05em] text-text-primary">
              {{ item.name }}
            </h3>
          </div>
          <span class="launch-icon">{{ item.icon }}</span>
        </div>

        <p class="mt-4 max-w-[420px] text-sm leading-7 text-text-secondary">{{ item.desc }}</p>

        <div class="mt-6 flex flex-wrap gap-2.5">
          <span v-for="detail in item.details" :key="`${item.path}-${detail}`" class="launch-pill">
            {{ detail }}
          </span>
        </div>

        <div class="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-primary">
          <span>{{ item.cta }}</span>
          <span aria-hidden="true">↗</span>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<script setup>
defineProps({
  modules: {
    type: Array,
    default: () => [],
  },
})
</script>

<style scoped>
.launch-shell {
  padding-bottom: 72px;
}

.launch-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.launch-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow-base);
  padding: 24px;
  transition:
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.32s ease,
    border-color 0.32s ease;
}

.launch-card::after {
  content: '';
  position: absolute;
  right: -26px;
  top: -24px;
  width: 130px;
  height: 130px;
  border-radius: 999px;
  background: var(--module-glow-c);
  filter: blur(24px);
  opacity: 0;
  transition: opacity 0.32s ease;
}

.launch-card:hover {
  transform: translateY(-8px) scale(1.015);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-border);
}

.launch-card:hover::after {
  opacity: 1;
}

.launch-card-feature {
  grid-column: span 3;
  min-height: 320px;
}

.launch-card-compact {
  grid-column: span 2;
  min-height: 250px;
}

.launch-icon {
  width: 52px;
  height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: var(--accent-soft);
  font-size: 22px;
}

.launch-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--accent-border);
  background: var(--accent-soft);
  color: var(--primary);
  padding: 8px 12px;
  font-size: 11px;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .launch-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .launch-card-feature,
  .launch-card-compact {
    grid-column: span 1;
    min-height: 260px;
  }
}

@media (max-width: 640px) {
  .launch-grid {
    grid-template-columns: 1fr;
  }
}

.dark .launch-card {
  background: var(--surface-panel-soft);
  border-color: var(--border-soft);
}

.dark .launch-pill {
  background: var(--accent-soft);
  border-color: var(--accent-border);
}
</style>
