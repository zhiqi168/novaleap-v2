<template>
  <section class="deck-shell" data-reveal>
    <div class="deck-grid">
      <RouterLink
        v-for="item in modules"
        :key="item.path"
        :to="item.path"
        class="deck-card"
        data-reveal
      >
        <h3 class="deck-title">{{ item.name }}</h3>
        <p class="deck-desc">{{ item.subtitle }}</p>
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
.deck-shell {
  margin-top: 10px;
}

.deck-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.deck-card {
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.62);
  box-shadow: var(--shadow-base);
  backdrop-filter: blur(8px);
  padding: 26px 24px;
  min-height: 144px;
  transition:
    transform 0.28s ease,
    box-shadow 0.28s ease,
    background-color 0.28s ease;
}

.deck-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
  background: rgba(255, 255, 255, 0.78);
}

.deck-title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--text-primary);
}

.deck-desc {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-muted);
}

@media (max-width: 980px) {
  .deck-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .deck-grid {
    grid-template-columns: 1fr;
  }
}

.dark .deck-card {
  background: rgba(20, 17, 26, 0.72);
  border-color: rgba(255, 255, 255, 0.08);
}
</style>
