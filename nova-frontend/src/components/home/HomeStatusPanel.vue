<template>
  <section class="status-shell">
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.26em] text-text-muted">Today Focus</p>
        <h2 class="mt-3 text-[clamp(28px,3vw,42px)] font-semibold tracking-[-0.05em] text-text-primary">
          先看清自己的节奏，再开始今天的推进。
        </h2>
      </div>
      <p class="max-w-[420px] text-sm leading-7 text-text-secondary">
        这不是后台统计，而是 NovaLeap 为今天的学习路径做的轻量引导。
      </p>
    </div>

    <div class="status-grid">
      <article class="status-main-card">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-[12px] font-semibold uppercase tracking-[0.22em] text-text-muted">Today's Path</p>
            <h3 class="mt-4 text-[clamp(28px,3vw,42px)] font-semibold tracking-[-0.05em] text-text-primary">
              {{ studyCard.value }}
              <span class="text-[18px] font-medium text-text-secondary">{{ studyCard.suffix }}</span>
            </h3>
            <p class="mt-3 max-w-[360px] text-sm leading-7 text-text-secondary">{{ studyCard.hint }}</p>
          </div>

          <div class="status-progress-badge hidden px-4 py-3 text-right sm:block">
            <p class="text-[11px] uppercase tracking-[0.22em] text-text-muted">完成进度</p>
            <p class="mt-2 text-2xl font-semibold tracking-[-0.05em] text-text-primary">{{ studyCard.progress }}</p>
          </div>
        </div>

        <div class="mt-8 grid gap-3 sm:grid-cols-3">
          <RouterLink
            v-for="task in tasks"
            :key="task.title"
            :to="task.path"
            class="task-chip"
          >
            <div class="flex items-center gap-3">
              <span class="task-icon">{{ task.icon }}</span>
              <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-text-primary">{{ task.title }}</p>
                <p class="truncate text-xs text-text-secondary">{{ task.desc }}</p>
              </div>
            </div>
          </RouterLink>
        </div>
      </article>

      <article
        v-for="card in metrics"
        :key="card.label"
        class="status-mini-card"
      >
        <p class="text-[11px] font-semibold uppercase tracking-[0.22em] text-text-muted">{{ card.label }}</p>
        <div class="mt-5 flex items-end gap-2">
          <strong class="text-[36px] font-semibold leading-none tracking-[-0.05em] text-text-primary">{{ card.value }}</strong>
          <span class="pb-1 text-sm text-text-secondary">{{ card.suffix }}</span>
        </div>
        <p class="mt-3 text-sm leading-7 text-text-secondary">{{ card.hint }}</p>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  studyCard: {
    type: Object,
    required: true,
  },
  metrics: {
    type: Array,
    default: () => [],
  },
  tasks: {
    type: Array,
    default: () => [],
  },
})
</script>

<style scoped>
.status-shell {
  position: relative;
}

.status-grid {
  display: grid;
  grid-template-columns: 1.45fr repeat(3, minmax(0, 0.7fr));
  gap: 16px;
}

.status-main-card,
.status-mini-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background: var(--surface-panel);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow-base);
  transition: transform 0.35s ease, box-shadow 0.35s ease, border-color 0.35s ease;
}

.status-main-card {
  padding: 28px;
  animation: statusFloat 7s ease-in-out infinite;
}

.status-mini-card {
  padding: 24px;
}

.status-main-card:hover,
.status-mini-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-border);
}

.status-main-card::after {
  content: '';
  position: absolute;
  right: -26px;
  top: -18px;
  width: 180px;
  height: 180px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-soft) 88%, white 12%);
  filter: blur(24px);
}

.task-chip {
  position: relative;
  z-index: 1;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-soft);
  background: var(--surface-panel-soft);
  padding: 14px;
  backdrop-filter: blur(8px);
  transition: transform 0.26s ease, box-shadow 0.26s ease;
}

.task-chip:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.task-icon {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 16px;
  background: var(--accent-soft);
}

.status-progress-badge {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-soft);
  background: var(--surface-panel-soft);
  backdrop-filter: blur(8px);
}

@keyframes statusFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@media (max-width: 1180px) {
  .status-grid {
    grid-template-columns: 1fr;
  }
}

.dark .status-main-card,
.dark .status-mini-card,
.dark .task-chip {
  background: var(--surface-panel);
  border-color: var(--border-soft);
}
</style>
