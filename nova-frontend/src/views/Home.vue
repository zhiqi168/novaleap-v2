<template>
  <div ref="pageRef" class="home-page h-full overflow-x-hidden overflow-y-auto">
    <div class="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
      <div class="bg-glow bg-glow-a"></div>
      <div class="bg-glow bg-glow-b"></div>
      <div class="bg-glow bg-glow-c"></div>
    </div>

    <div class="page-shell mx-auto max-w-[1260px] px-5 pt-4 sm:px-8 lg:px-10">
      <HomeHero
        id="hero"
        :subtitle="heroSubtitle"
        :greeting-text="greetingText"
        :nickname="displayNickname"
        @primary="router.push('/questions')"
        @secondary="openSupportModal"
      />

      <HomeStorySection
        id="storytelling"
        class="mt-4"
        :steps="storySteps"
        :active-step-id="activeStepId"
        :question-preview="questionPreview"
        :question-top-five="questionTopFive"
        :score-top-five="scoreTopFive"
        @step-change="handleStepChange"
      />
    </div>

    <HomeFooterCta
      id="footer"
      data-reveal
      class="home-footer mt-4"
    />

    <button
      type="button"
      :class="['scroll-cue', 'scroll-cue-left', { 'scroll-cue--on-dark': isFooterZone }]"
      aria-label="向下滚动查看更多"
      @click="scrollToSection('storytelling')"
    >
      <span class="scroll-cue-body">
        <span class="scroll-cue-line"></span>
        <span class="scroll-cue-head"></span>
      </span>
    </button>

    <button
      type="button"
      :class="['scroll-cue', 'scroll-cue-right', { 'scroll-cue--on-dark': isFooterZone }]"
      aria-label="向下滚动查看更多"
      @click="scrollToSection('storytelling')"
    >
      <span class="scroll-cue-body">
        <span class="scroll-cue-line"></span>
        <span class="scroll-cue-head"></span>
      </span>
    </button>

    <Transition name="fade">
      <button
        v-if="showBackToTop"
        type="button"
        class="back-to-top"
        aria-label="返回顶部"
        @click="scrollToTop"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 19V5M12 5L5 12M12 5L19 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </Transition>

    <Transition name="support-modal">
      <div
        v-if="showSupportModal"
        class="support-overlay"
        @click.self="closeSupportModal"
      >
        <div class="support-modal">
          <button
            type="button"
            class="support-close"
            aria-label="关闭弹窗"
            @click="closeSupportModal"
          >
            ×
          </button>
          <h3 class="support-title">请作者喝杯咖啡</h3>
          <p class="support-desc">扫码赞赏支持作者，感谢你对 NovaLeap 的鼓励。</p>
          <img
            :src="tipQr"
            alt="NovaLeap 赞赏码"
            class="support-qr"
          />
          <p class="support-tip">Meet 的赞赏码</p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import tipQr from '@/assets/tip-qr.jpg'
import HomeFooterCta from '@/components/home/HomeFooterCta.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import HomeStorySection from '@/components/home/HomeStorySection.vue'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()
const pageRef = ref(null)
const ranking = ref([])
const currentHour = ref(new Date().getHours())
const activeStepId = ref('question')
const showSupportModal = ref(false)
const isFooterZone = ref(false)
const showBackToTop = ref(false)

let tickTimer = null
let leaderboardTimer = null
let revealObserver = null
let heroObserver = null
let footerObserver = null

const heroSubtitle = '用技术，让知识，轻松跃迁。'

const storySteps = [
  {
    id: 'question',
    step: '01',
    kicker: 'Training Layer',
    tag: 'Question Bank',
    title: '先从拾光题库开始，把训练节奏建立起来。',
    desc: 'NovaLeap 会先用题库和 AI 解析把“开始学习”这件事变得更顺，让你快速进入状态，而不是看完功能却不知道先去哪里。',
    details: ['AI 真题解析', '训练反馈', '节奏推进'],
    cta: '进入拾光题库',
    path: '/questions',
  },
  {
    id: 'notes',
    step: '02',
    kicker: 'Knowledge Layer',
    tag: 'Notes',
    title: '把今天的结论收起来，后面就不会每次都重来。',
    desc: '灵感手记承接题库训练后的想法与复盘，把碎片思考变成可长期复用的知识记录。',
    details: ['结构化沉淀', '错题归档', '方法复盘'],
    cta: '进入灵感手记',
    path: '/notes',
  },
  {
    id: 'resume',
    step: '03',
    kicker: 'Career Layer',
    tag: 'Resume Studio',
    title: '当知识开始成形，就把经历整理成专业表达。',
    desc: '简历工坊把项目背景、关键动作和结果量化串起来，让成长成果真正转化为求职表达能力。',
    details: ['STAR 结构', '项目包装', '结果量化'],
    cta: '进入简历工坊',
    path: '/resume',
  },
  {
    id: 'coach',
    step: '04',
    kicker: 'Expression Layer',
    tag: 'Knowledge Practice',
    title: '再用知识陪练，把“会做”慢慢练成“会说”。',
    desc: '它不是简单问答，而是围绕真实场景不断追问和校正，让技术表达与临场组织感更稳。',
    details: ['模拟追问', '表达校正', '稳定输出'],
    cta: '进入知识陪练',
    path: '/coach',
  },
  {
    id: 'community',
    step: '05',
    kicker: 'Feedback Layer',
    tag: 'Wish Wall',
    title: '让目标和节奏被看见，成长就不再孤立。',
    desc: '星愿墙承接成长反馈和目标表达，让你知道自己正在往哪里走，也知道已经走到了哪里。',
    details: ['目标公开', '成长反馈', '社区感'],
    cta: '进入星愿墙',
    path: '/wishes',
  },
  {
    id: 'game',
    step: '06',
    kicker: 'Relax Layer',
    tag: 'Relax',
    title: '留一点轻松空间，长期节奏才能更可持续。',
    desc: '休闲时刻不是离题，而是让整个成长体验更完整。好的产品感，往往来自这种克制又人性化的安排。',
    details: ['轻量互动', '节奏切换', '长期陪伴'],
    cta: '进入休闲时刻',
    path: '/game',
  },
  {
    id: 'leaderboard',
    step: '07',
    kicker: 'Outcome Layer',
    tag: 'Leaderboard',
    title: '最后把成长结果收住，过程与结果在同一条叙事线上闭环。',
    desc: '排行榜不是孤立模块，而是把训练、沉淀、表达和反馈汇总成可见结果，让每次投入都能被看见。',
    details: ['题名金榜', '竞速先锋', '成长闭环'],
    cta: '查看完整排行榜',
    path: '/leaderboard',
  },
]
const storyStepIdSet = new Set(storySteps.map((step) => step.id))

const isGuest = computed(() => authStore.nickname?.startsWith('游客+') || /^[0-9a-f-]{36}$/i.test(authStore.username))

const displayNickname = computed(() => {
  if (isGuest.value) return 'Nova 访客'
  return authStore.nickname || '学习者'
})

const hideDuplicateSuffix = (nickname, username) => {
  const cleanNickname = String(nickname || '').trim()
  const cleanUsername = String(username || '').trim()
  if (!cleanNickname) return cleanUsername || '用户'
  const duplicateSuffix = `（${cleanUsername}）`
  return cleanUsername && cleanNickname.endsWith(duplicateSuffix)
    ? cleanNickname.slice(0, -duplicateSuffix.length)
    : cleanNickname
}

const rankingView = computed(() => {
  const list = Array.isArray(ranking.value) ? ranking.value : []
  return list.map((item) => {
    const username = item?.username
    return {
      userId: item?.userId ?? item?.id ?? username ?? 'anonymous',
      displayName: hideDuplicateSuffix(item?.nickname, username),
      questionDone: Number(item?.questionDone) || 0,
      gameBestScore: Number(item?.gameBestScore) || 0,
    }
  })
})

const questionTopTen = computed(() => [...rankingView.value]
  .sort((a, b) => b.questionDone - a.questionDone || b.gameBestScore - a.gameBestScore)
  .slice(0, 10)
  .map((item, index) => ({ ...item, rank: index + 1 })))

const scoreTopTen = computed(() => [...rankingView.value]
  .sort((a, b) => b.gameBestScore - a.gameBestScore || b.questionDone - a.questionDone)
  .slice(0, 10)
  .map((item, index) => ({ ...item, rank: index + 1 })))

const questionTopFive = computed(() => questionTopTen.value.slice(0, 5))
const scoreTopFive = computed(() => scoreTopTen.value.slice(0, 5))
const questionPreview = computed(() => questionTopTen.value.slice(0, 3))

const greetingText = computed(() => {
  const hour = currentHour.value
  if (hour < 5) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const loadLeaderboard = async () => {
  try {
    const res = await api.get('/api/leaderboard')
    if (res.code === 200) ranking.value = res.data?.list || []
  } catch (error) {
    console.error('Leaderboard load failed', error)
  }
}

const emitHomeActive = (sectionId) => {
  window.dispatchEvent(new CustomEvent('nova-home-active', { detail: { section: sectionId } }))
}

const handleStepChange = (stepId) => {
  if (!stepId) return
  activeStepId.value = stepId
  emitHomeActive(stepId)
}

const scrollToTop = () => {
  if (pageRef.value) {
    pageRef.value.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const handleScroll = (e) => {
  const scrollTop = e.target.scrollTop
  const mobileThreshold = window.innerWidth <= 768 ? 320 : 600
  showBackToTop.value = scrollTop > mobileThreshold
}

const scrollToSection = (sectionId, options = {}) => {
  const container = pageRef.value
  if (!container) return
  const section = container.querySelector(sectionId.startsWith('#') ? sectionId : `#${sectionId}`)
  if (!section) return

  const behavior = options.instant ? 'auto' : 'smooth'
  const containerRect = container.getBoundingClientRect()
  const sectionRect = section.getBoundingClientRect()
  
  // 计算元素相对于容器的绝对顶部偏移
  const relativeTop = sectionRect.top - containerRect.top + container.scrollTop
  
  // 根据不同区域设置更精准的偏移
  let offset = 80
  if (sectionId === 'hero') {
    offset = 80
  } else if (storyStepIdSet.has(sectionId)) {
    offset = 124 // 列表项稍微下移一点，确保上方留白舒适
  } else {
    offset = 110
  }

  container.scrollTo({
    top: Math.max(relativeTop - offset, 0),
    behavior,
  })

  // 滚动触发后，立即同步导航状态
  if (sectionId !== 'storytelling' && sectionId !== 'footer') {
    emitHomeActive(sectionId)
  }
}

const openSupportModal = () => {
  showSupportModal.value = true
}

const closeSupportModal = () => {
  showSupportModal.value = false
}

const handleGlobalKeydown = (event) => {
  if (event.key === 'Escape' && showSupportModal.value) closeSupportModal()
}

const handleHomeScrollRequest = (event) => {
  const sectionId = event.detail?.section
  if (sectionId) scrollToSection(sectionId, { instant: true })
}

const initRevealObserver = () => {
  const container = pageRef.value
  if (!container) return
  const targets = container.querySelectorAll('[data-reveal]')
  if (!targets.length) return

  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting && entry.intersectionRatio > 0.12) entry.target.classList.add('is-visible')
      else entry.target.classList.remove('is-visible')
    })
  }, {
    root: container,
    threshold: [0, 0.12, 0.28, 0.45],
    rootMargin: '-6% 0px -14% 0px',
  })

  targets.forEach((el) => revealObserver.observe(el))
}

const initHeroObserver = () => {
  const container = pageRef.value
  const hero = container?.querySelector('#hero')
  if (!container || !hero) return

  heroObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting && entry.intersectionRatio > 0.42) emitHomeActive('hero')
    })
  }, {
    root: container,
    threshold: [0, 0.16, 0.42, 0.68],
    rootMargin: '-8% 0px -24% 0px',
  })

  heroObserver.observe(hero)
}

const initFooterObserver = () => {
  const container = pageRef.value
  const tail = container?.querySelector('#footer')
  if (!container || !tail) return

  footerObserver = new IntersectionObserver((entries) => {
    isFooterZone.value = entries.some((entry) => entry.isIntersecting && entry.intersectionRatio > 0.01)
  }, {
    root: container,
    threshold: [0, 0.01, 0.1, 0.2],
    rootMargin: '0px 0px 0px 0px',
  })

  footerObserver.observe(tail)
}

onMounted(async () => {
  await loadLeaderboard()
  leaderboardTimer = window.setInterval(loadLeaderboard, 5000)
  tickTimer = window.setInterval(() => {
    currentHour.value = new Date().getHours()
  }, 60000)

  await nextTick()
  initRevealObserver()
  initHeroObserver()
  initFooterObserver()
  emitHomeActive('hero')

  if (pageRef.value) {
    pageRef.value.addEventListener('scroll', handleScroll, { passive: true })
  }

  window.addEventListener('keydown', handleGlobalKeydown)
  window.addEventListener('nova-home-scroll', handleHomeScrollRequest)
})

onUnmounted(() => {
  if (pageRef.value) {
    pageRef.value.removeEventListener('scroll', handleScroll)
  }
  if (leaderboardTimer) clearInterval(leaderboardTimer)
  if (tickTimer) clearInterval(tickTimer)
  if (revealObserver) revealObserver.disconnect()
  if (heroObserver) heroObserver.disconnect()
  if (footerObserver) footerObserver.disconnect()
  window.removeEventListener('keydown', handleGlobalKeydown)
  window.removeEventListener('nova-home-scroll', handleHomeScrollRequest)
})
</script>

<style scoped>
.home-page {
  position: relative;
  height: 100%;
  scroll-behavior: smooth;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-y;
  scroll-padding-top: 96px;
}

.page-shell {
  animation: pageEnter 920ms cubic-bezier(0.22, 1, 0.36, 1);
}

.bg-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(120px);
  opacity: 0.84;
}

.bg-glow-a {
  left: -6%;
  top: 4%;
  width: 360px;
  height: 360px;
  background: var(--module-glow-b);
}

.bg-glow-b {
  right: -8%;
  top: 16%;
  width: 340px;
  height: 340px;
  background: var(--module-glow-c);
}

.bg-glow-c {
  left: 28%;
  bottom: 10%;
  width: 440px;
  height: 300px;
  background: var(--module-glow-a);
}

/* 暗黑模式深度优化：消除分裂感，增强沉浸式氛围 */
.dark .bg-glow {
  opacity: 0.42;
  filter: blur(160px);
}

.dark .bg-glow-a {
  background: var(--module-glow-c);
  width: 420px;
  height: 420px;
}

.dark .bg-glow-b {
  background: var(--module-glow-a);
  width: 380px;
  height: 380px;
}

.dark .bg-glow-c {
  background: var(--module-glow-b);
  width: 500px;
  height: 340px;
}

.scroll-cue {
  position: fixed;
  top: 52%;
  z-index: 96;
  --cue-color: color-mix(in srgb, var(--text-primary) 48%, transparent);
  width: 44px;
  height: 168px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  border: none;
  background: transparent;
  padding: 0;
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: pointer;
}

.scroll-cue--on-dark {
  opacity: 0 !important;
  pointer-events: none;
  transform: translateY(20px);
}

.scroll-cue-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: arrowMove 1.26s ease-in-out infinite;
}

.scroll-cue-left {
  left: clamp(28px, 4vw, 72px);
}

.scroll-cue-right {
  right: clamp(28px, 4vw, 72px);
}

.scroll-cue-line {
  display: block;
  width: 3px;
  height: 116px;
  background: var(--cue-color);
  border-radius: 999px;
}

.scroll-cue-head {
  width: 28px;
  height: 28px;
  border-right: 3px solid var(--cue-color);
  border-bottom: 3px solid var(--cue-color);
  transform: rotate(45deg);
  margin-top: -2px;
}

.support-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(24, 19, 30, 0.26);
  backdrop-filter: blur(4px);
  padding: 18px;
}

.support-modal {
  position: relative;
  width: min(92vw, 420px);
  border-radius: 24px;
  border: 1px solid var(--border-soft);
  background: var(--surface-panel);
  box-shadow: var(--shadow-modal);
  padding: 24px 22px 20px;
  text-align: center;
}

.support-close {
  position: absolute;
  right: 12px;
  top: 8px;
  border: none;
  background: transparent;
  font-size: 28px;
  line-height: 1;
  color: var(--text-secondary);
  cursor: pointer;
}

.support-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.support-desc {
  margin: 12px auto 0;
  max-width: 330px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.support-qr {
  width: 240px;
  height: 240px;
  margin: 16px auto 0;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  object-fit: contain;
}

.support-tip {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--text-tertiary);
}

.support-modal-enter-active,
.support-modal-leave-active {
  transition: opacity 260ms ease;
}

.support-modal-enter-active .support-modal,
.support-modal-leave-active .support-modal {
  transition: transform 260ms ease, opacity 260ms ease;
}

.support-modal-enter-from,
.support-modal-leave-to {
  opacity: 0;
}

.support-modal-enter-from .support-modal,
.support-modal-leave-to .support-modal {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

:deep([data-reveal]) {
  opacity: 0;
  transform: translateY(20px);
  transition:
    opacity 820ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 820ms cubic-bezier(0.22, 1, 0.36, 1);
}

:deep([data-reveal].is-visible) {
  opacity: 1;
  transform: translateY(0);
}

.home-footer {
  width: 100%;
}

@keyframes pageEnter {
  0% {
    opacity: 0;
    transform: translateY(16px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes arrowMove {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.72;
  }
  50% {
    transform: translateY(18px);
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .scroll-cue-left {
    left: 18px;
  }

  .scroll-cue-right {
    right: 18px;
  }
}

@media (max-width: 768px) {
  .home-page {
    scroll-padding-top: 82px;
  }

  .page-shell {
    padding-left: 16px;
    padding-right: 16px;
    padding-top: 8px;
  }

  .scroll-cue-left,
  .scroll-cue-right {
    display: none;
  }

  .support-qr {
    width: 210px;
    height: 210px;
  }

  .back-to-top {
    right: 12px;
    bottom: calc(12px + env(safe-area-inset-bottom));
    width: 44px;
    height: 44px;
    border-radius: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .page-shell,
  :deep([data-reveal]),
  .support-modal-enter-active,
  .support-modal-leave-active {
    animation: none;
    transition: none;
  }

  :deep([data-reveal]) {
    opacity: 1;
    transform: none;
  }
}

.back-to-top {
  position: fixed;
  right: clamp(16px, 4vw, 32px);
  bottom: clamp(16px, 4vw, 32px);
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--bg-soft);
  backdrop-filter: blur(14px) saturate(180%);
  -webkit-backdrop-filter: blur(14px) saturate(180%);
  border: 1px solid var(--border-soft);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 4px 12px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  z-index: 200;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.back-to-top:hover {
  transform: translateY(-4px);
  background: var(--bg-elevated);
  box-shadow: 
    0 6px 16px rgba(0, 0, 0, 0.08),
    0 14px 32px rgba(0, 0, 0, 0.12);
}

.back-to-top:active {
  transform: translateY(-1px) scale(0.96);
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.92);
}
</style>
