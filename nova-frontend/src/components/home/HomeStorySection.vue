<template>
  <section ref="sectionRef" class="story-shell">
    <div class="story-head" data-reveal>
      <p class="story-eyebrow">Sticky Storytelling</p>
      <h2 class="story-title">不只是展示功能，</h2>
      <h2 class="story-title">而是让每一步，都有继续探索的理由。</h2>
      <!-- <p class="story-copy">
        左侧继续讲述每个阶段的价值，右侧保持稳定展示当前模块的样例。滚动时，内容会非常克制地切换，让页面更像成熟产品官网的叙事方式。
      </p> -->
    </div>

    <div class="story-grid">
      <div ref="stepsRef" class="story-steps">
        <article
          v-for="step in steps"
          :id="step.id"
          :key="step.id"
          :ref="(el) => registerStep(step.id, el)"
          class="story-step"
        >
          <div class="story-card" :class="{ 'story-card-active': activeStepId === step.id }">
            <div class="story-card-head">
              <span class="story-index">{{ step.step }}</span>
              <span class="story-tag">{{ step.tag }}</span>
            </div>

            <p class="story-kicker">{{ step.kicker }}</p>
            <h3 class="story-card-title">{{ step.title }}</h3>
            <p class="story-card-copy">{{ step.desc }}</p>

            <div class="story-card-details">
              <span v-for="detail in step.details" :key="`${step.id}-${detail}`" class="detail-pill">
                {{ detail }}
              </span>
            </div>

            <div class="story-card-actions">
              <RouterLink :to="step.path" class="story-link">{{ step.cta }}</RouterLink>
              <RouterLink v-if="step.secondaryPath" :to="step.secondaryPath" class="story-link story-link-soft">
                {{ step.secondaryCta }}
              </RouterLink>
            </div>
          </div>
        </article>
      </div>

      <div class="story-sticky-column" :style="{ '--sticky-column-min-height': stickyColumnMinHeight }">
        <div class="story-sticky-panel">
          <HomeStickyPreview
            :active-step-id="activeStepId"
            :question-preview="questionPreview"
            :question-top-five="questionTopFive"
            :score-top-five="scoreTopFive"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import HomeStickyPreview from './HomeStickyPreview.vue'

const props = defineProps({
  steps: {
    type: Array,
    default: () => [],
  },
  activeStepId: {
    type: String,
    default: '',
  },
  questionPreview: {
    type: Array,
    default: () => [],
  },
  questionTopFive: {
    type: Array,
    default: () => [],
  },
  scoreTopFive: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['step-change'])

const sectionRef = ref(null)
const stepsRef = ref(null)
const stickyColumnMinHeight = ref('520vh')
const stepMap = new Map()
let stepObserver = null
let resizeRaf = null
const isCompactLayout = () => window.innerWidth <= 1080

const destroyObserver = () => {
  if (stepObserver) {
    stepObserver.disconnect()
    stepObserver = null
  }
}

const syncStickyHeight = () => {
  if (isCompactLayout()) {
    stickyColumnMinHeight.value = 'auto'
    return
  }

  const stepsEl = stepsRef.value
  if (!stepsEl) return
  
  const steps = stepsEl.querySelectorAll('.story-step')
  if (!steps.length) return
  
  const lastStep = steps[steps.length - 1]
  const lastCard = lastStep.querySelector('.story-card')
  const stickyPanel = sectionRef.value?.querySelector('.story-sticky-panel')
  
  if (!lastCard || !stickyPanel) return

  // 计算左侧最后一个卡片相对于容器顶部的绝对偏移，不受滚动影响
  const stepsRect = stepsEl.getBoundingClientRect()
  const cardRect = lastCard.getBoundingClientRect()
  const cardAbsoluteTop = cardRect.top - stepsRect.top

  // 精准锁定计算：右侧粘性容器总高度 = 左侧最后一张卡片 Top + 右侧面板高度
  // 这确保了在触底一瞬间，右侧面板在视口中的 top 会与左侧卡片完美齐平，两者随后可以同频上滚
  const panelHeight = stickyPanel.offsetHeight || 650
  const idealHeight = Math.ceil(cardAbsoluteTop + panelHeight)

  // 获取真实的内部总内容高度作为兜底参考
  const nextHeight = Math.ceil(stepsEl.scrollHeight)
  const adjustedHeight = Math.max(Math.min(idealHeight, nextHeight), 900)
  
  stickyColumnMinHeight.value = `${adjustedHeight}px`
}

const handleResize = () => {
  if (resizeRaf) {
    cancelAnimationFrame(resizeRaf)
  }
  resizeRaf = requestAnimationFrame(() => {
    initObserver()
    syncStickyHeight()
    resizeRaf = null
  })
}

const registerStep = (id, el) => {
  if (el) stepMap.set(id, el)
  else stepMap.delete(id)
}

const pickCenteredStep = (root, nodes) => {
  const rootRect = root.getBoundingClientRect()
  const centerY = rootRect.top + rootRect.height * 0.48
  const minVisibleY = rootRect.top + rootRect.height * 0.08
  const maxVisibleY = rootRect.bottom - rootRect.height * 0.08

  let bestId = ''
  let bestDistance = Number.POSITIVE_INFINITY

  nodes.forEach((node) => {
    const rect = node.getBoundingClientRect()
    if (rect.bottom < minVisibleY || rect.top > maxVisibleY) return
    const nodeCenter = rect.top + rect.height / 2
    const distance = Math.abs(nodeCenter - centerY)
    if (distance < bestDistance) {
      bestDistance = distance
      bestId = node.id
    }
  })

  return bestId
}

const initObserver = () => {
  destroyObserver()

  const root = sectionRef.value?.closest('.home-page')
  if (!root || !props.steps.length) return

  if (isCompactLayout()) {
    emit('step-change', props.activeStepId || props.steps[0]?.id || '')
    return
  }

  const nodes = props.steps
    .map((step) => stepMap.get(step.id))
    .filter(Boolean)

  if (!nodes.length) return

  stepObserver = new IntersectionObserver(() => {
    const centeredId = pickCenteredStep(root, nodes)
    if (centeredId) emit('step-change', centeredId)
  }, {
    root,
    threshold: [0, 0.25, 0.5, 0.75, 1],
    rootMargin: '0px 0px 0px 0px',
  })

  nodes.forEach((node) => {
    stepObserver.observe(node)
  })

  const centeredId = pickCenteredStep(root, nodes)
  if (centeredId) emit('step-change', centeredId)
}

onMounted(async () => {
  await nextTick()
  initObserver()
  syncStickyHeight()
  window.addEventListener('resize', handleResize, { passive: true })
})

onUnmounted(() => {
  destroyObserver()
  window.removeEventListener('resize', handleResize)
  if (resizeRaf) cancelAnimationFrame(resizeRaf)
})
</script>

<style scoped>
.story-shell {
  padding-block: 8vh 5vh;
}

.story-head {
  max-width: 950px;
  padding-bottom: 42px;
}

.story-eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.26em;
  color: var(--text-muted);
}

.story-title {
  margin-top: 16px;
  font-size: clamp(34px, 4.2vw, 58px);
  line-height: 1.08;
  letter-spacing: -0.05em;
  color: var(--text-primary);
}

.story-copy {
  margin-top: 16px;
  max-width: 700px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--text-secondary);
}

.story-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 44px;
  align-items: start;
}

.story-steps {
  position: relative;
}

.story-steps::after {
  content: '';
  display: block;
  height: clamp(56px, 9vh, 110px);
}

.story-step {
  min-height: clamp(430px, 82vh, 700px);
  display: flex;
  align-items: center;
  scroll-margin-top: 128px;
}

.story-card {
  width: 100%;
  max-width: 560px;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background: var(--bg-surface);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 34px 30px;
  opacity: 0.6;
  transform: translateY(24px) scale(0.96);
  transition:
    opacity 620ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 620ms cubic-bezier(0.22, 1, 0.36, 1),
    background-color 620ms ease,
    backdrop-filter 620ms ease,
    box-shadow 620ms ease;
  will-change: transform, opacity;
}

.story-card-active {
  opacity: 1;
  transform: translateY(0) scale(1);
  background: var(--bg-elevated);
  border-color: var(--primary-soft);
  box-shadow: var(--shadow-hover);
}

.dark .story-card {
  width: 100%;
  max-width: 560px;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background: var(--bg-surface);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 34px 30px;
  opacity: 0.6;
  transform: translateY(24px) scale(0.96);
  transition:
    opacity 620ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 620ms cubic-bezier(0.22, 1, 0.36, 1),
    background-color 620ms ease,
    backdrop-filter 620ms ease,
    box-shadow 620ms ease;
  will-change: transform, opacity;
}

.story-card-active {
  opacity: 1;
  transform: translateY(0) scale(1);
  background: var(--bg-elevated);
  border-color: var(--primary-soft);
  box-shadow: var(--shadow-hover);
}

.story-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.story-index {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: var(--text-muted);
}

.story-tag,
.detail-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--accent-border);
  background: var(--accent-soft);
  color: var(--primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  transition: all 0.3s ease;
}

.story-tag {
  padding: 6px 10px;
}

.story-kicker {
  margin-top: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--text-muted);
}

.story-card-title {
  margin-top: 14px;
  font-size: clamp(28px, 3.2vw, 42px);
  line-height: 1.18;
  letter-spacing: -0.05em;
  color: var(--text-primary);
}

.story-card-copy {
  margin-top: 16px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--text-secondary);
}

.story-card-details {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-pill {
  padding: 6px 14px;
  border: 1.5px solid var(--accent-border);
}

.detail-pill:hover {
  background: var(--accent-strong);
  transform: translateY(-1px);
}

.story-card-actions {
  margin-top: 26px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.story-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  border-radius: 999px;
  border: 1px solid var(--accent-border);
  background: var(--accent-soft);
  color: var(--primary);
  padding: 0 18px;
  font-size: 13px;
  font-weight: 600;
}

.story-link-soft {
  border-color: var(--border-soft);
  background: var(--bg-soft);
  color: var(--text-secondary);
}

.story-sticky-column {
  min-height: var(--sticky-column-min-height, 500vh);
}

.story-sticky-panel {
  position: sticky;
  top: 112px;
}

@media (max-width: 1080px) {
  .story-grid {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .story-sticky-column {
    min-height: auto;
    order: 1;
  }

  .story-steps::after {
    height: 12px;
  }

  .story-sticky-panel {
    position: relative;
    top: 0;
  }

  .story-step {
    min-height: auto;
    padding-block: 14px;
    scroll-margin-top: 100px;
  }

  .story-card {
    max-width: none;
    padding: 28px 24px;
    opacity: 1;
    transform: none;
    will-change: auto;
  }

  .story-card-active {
    box-shadow: var(--shadow-base);
  }

  .dark .story-tag,
  .dark .detail-pill,
  .dark .story-link {
    background: var(--accent-soft);
    border-color: var(--accent-border);
  }
}

@media (max-width: 768px) {
  .story-shell {
    padding-block: 40px 24px;
  }

  .story-head {
    padding-bottom: 24px;
  }

  .story-title {
    font-size: clamp(30px, 10vw, 42px);
    line-height: 1.12;
  }

  .story-grid {
    gap: 18px;
  }

  .story-card {
    padding: 22px 18px;
    border-radius: 22px;
    transition:
      border-color 220ms ease,
      background-color 220ms ease,
      box-shadow 220ms ease;
  }

  .story-card-title {
    font-size: clamp(22px, 7vw, 30px);
  }

  .story-card-copy {
    font-size: 14px;
    line-height: 1.8;
  }

  .story-card-details {
    gap: 8px;
  }

  .detail-pill {
    padding: 6px 12px;
  }
}
</style>
