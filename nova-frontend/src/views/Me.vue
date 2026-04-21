<template>
  <div class="h-full relative overflow-hidden me-bg workspace-page workspace-scroll">
    <div class="relative z-10 h-full overflow-y-auto custom-scrollbar">
      <div class="workspace-stack me-shell max-w-[1120px] mx-auto text-center">
        <section class="me-hero pt-1">
        <h1 class="me-title text-[clamp(40px,6vw,66px)] font-black leading-tight text-text-primary break-words">
          {{ greetingPrefix }}，
          <span class="me-name-highlight me-title-name text-transparent bg-clip-text">
            {{ displayNickname }}
          </span>
        </h1>
        <p class="me-subline mt-3 text-[clamp(28px,4.4vw,52px)] text-text-secondary leading-tight">{{ subLine }}</p>

        <div class="me-action-stack mt-8 flex flex-col items-center relative">
          <button class="avatar-orbit" type="button" @click="goProfile" title="前往个人资料">
            <div class="avatar-core">
              <span v-if="isEmoji(displayAvatar)">{{ displayAvatar }}</span>
              <img v-else :src="displayAvatar" alt="avatar" />
            </div>
          </button>

          <button class="workspace-btn workspace-btn-primary boost-btn mt-6" @click="cheerUp">❤ 给我打气</button>
          <button class="workspace-btn workspace-btn-muted profile-btn mt-3" @click="goProfile">个人资料</button>

          <div class="cheer-popup-layer" aria-live="polite">
            <div
              v-for="item in cheerPopups"
              :key="item.id"
              class="cheer-popup"
              :style="item.style"
            >
              {{ item.text }}
            </div>
          </div>
        </div>

        <div class="workspace-shell me-quote-card mt-10 bg-bg-surface backdrop-blur-xl rounded-3xl shadow-card border border-border-subtle p-7 md:p-8 text-left">
          <div class="text-xl font-bold text-text-primary">今日格言</div>
          <div class="me-quote-text mt-5 text-[clamp(30px,4.2vw,52px)] font-medium text-text-primary tracking-tight leading-tight">{{ quote }}</div>
        </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fireConfetti, resetConfetti } from '@/composables/useConfetti'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/composables/useRequest'

const authStore = useAuthStore()
const router = useRouter()

const currentHour = ref(new Date().getHours())
let hourTimer = 0
const greetingPrefix = computed(() => {
  const hour = currentHour.value
  if (hour < 6) return '夜深了，早点休息'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const subLine = '今天也是充满可能的一天'

const DAILY_QUOTE_STORAGE_KEY = 'nova_me_daily_quote_v1'
const DAILY_QUOTE_FALLBACK = '把复杂问题拆小，你已经赢了一半。'

const quote = ref(DAILY_QUOTE_FALLBACK)
let dailyQuoteTimer = 0
let latestQuoteRequestId = 0
const isCompactViewport = () => window.innerWidth <= 768

const todayKey = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const readDailyQuoteCache = () => {
  try {
    const raw = localStorage.getItem(DAILY_QUOTE_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null
    return parsed
  } catch {
    return null
  }
}

const writeDailyQuoteCache = (date, text) => {
  try {
    localStorage.setItem(DAILY_QUOTE_STORAGE_KEY, JSON.stringify({ date, text }))
  } catch {
    // ignore cache failures
  }
}

const applyCachedDailyQuote = () => {
  const cached = readDailyQuoteCache()
  if (cached?.date === todayKey() && typeof cached.text === 'string' && cached.text.trim()) {
    quote.value = cached.text.trim()
    return true
  }
  return false
}

const fetchDailyQuote = async ({ force = false } = {}) => {
  const date = todayKey()
  if (!force && applyCachedDailyQuote()) {
    return
  }

  const requestId = ++latestQuoteRequestId

  try {
    const res = await api.get('/api/ai/quote/daily')
    const nextQuote = typeof res?.data === 'string' && res.data.trim() ? res.data.trim() : DAILY_QUOTE_FALLBACK
    if (requestId !== latestQuoteRequestId) return
    quote.value = nextQuote
    writeDailyQuoteCache(date, nextQuote)
  } catch {
    if (requestId !== latestQuoteRequestId) return
    const cached = readDailyQuoteCache()
    if (cached?.date === date && typeof cached.text === 'string' && cached.text.trim()) {
      quote.value = cached.text.trim()
      return
    }
    quote.value = DAILY_QUOTE_FALLBACK
    writeDailyQuoteCache(date, DAILY_QUOTE_FALLBACK)
  }
}

const scheduleDailyQuoteRefresh = () => {
  if (dailyQuoteTimer) {
    clearTimeout(dailyQuoteTimer)
    dailyQuoteTimer = 0
  }

  const now = new Date()
  const nextMidnight = new Date(now)
  nextMidnight.setHours(24, 0, 0, 0)
  const delay = Math.max(1000, nextMidnight.getTime() - now.getTime() + 1000)

  dailyQuoteTimer = window.setTimeout(async () => {
    await fetchDailyQuote({ force: true })
    scheduleDailyQuoteRefresh()
  }, delay)
}

const guestIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const isGuest = computed(() => {
  if (authStore.isGuest) return true
  const nickname = String(authStore.nickname || '')
  const username = String(authStore.user?.username || authStore.username || '')
  return nickname.startsWith('游客') || guestIdPattern.test(username)
})

const displayAvatar = computed(() => {
  const avatar = String(authStore.avatar || '').trim()
  if (!avatar) return '🥳'
  if (/^https?:\/\//i.test(avatar)) return avatar
  return avatar
})

const displayNickname = computed(() => {
  const raw = isGuest.value ? 'Nova 访客' : (authStore.nickname || '学习者')
  const text = String(raw || '').trim()
  return text.length > 14 ? `${text.slice(0, 12)}...` : text
})

const isEmoji = (val) => typeof val === 'string' && !val.startsWith('http')

const cheerLinePool = [
  '你的努力终将绽放光芒',
  '坚持不懈，你比想象中更强大',
  '每一次挑战都是成长的机会',
  '今天的努力是明天的收获',
  '专注当下，未来自有答案',
  '别和别人比，做最好的自己',
]

const cheerPopups = ref([])
const cheerPopupTimers = []
let lastCheerLine = ''
let cheerPopupSeed = 0

const CHEER_POPUP_LIMIT = 3
const CHEER_POPUP_STAY_MS = 3000


const CONFETTI_DURATION_MS = 1000
const CONFETTI_SPREAD = 40
const CONFETTI_RICH_COLORS = ['#bb0000','#f89aa5','#121314']
let confettiFrameId = 0
let startupBurstTimer = 0

const stopConfettiBurst = () => {
  if (startupBurstTimer) {
    clearTimeout(startupBurstTimer)
    startupBurstTimer = 0
  }
  if (confettiFrameId) {
    cancelAnimationFrame(confettiFrameId)
    confettiFrameId = 0
  }
  resetConfetti()
}

const playConfettiBurst = ({ duration = CONFETTI_DURATION_MS, particleCount = 2 } = {}) => {
  stopConfettiBurst()

  const end = Date.now() + duration
  const sharedOptions = {
    particleCount,
    spread: CONFETTI_SPREAD,
    colors: CONFETTI_RICH_COLORS,
    startVelocity: 60,
    decay: 0.88,
    scalar: 0.92,
    zIndex: 1400,
  }

  const frame = () => {
    fireConfetti({
      ...sharedOptions,
      angle: 60,
      origin: { x: 0 },
    })
    fireConfetti({
      ...sharedOptions,
      angle: 120,
      origin: { x: 1 },
    })

    if (Date.now() < end) {
      confettiFrameId = requestAnimationFrame(frame)
      return
    }

    confettiFrameId = 0
  }

  frame()
}

const clearCheerResources = () => {
  while (cheerPopupTimers.length) {
    const timer = cheerPopupTimers.pop()
    if (timer) clearTimeout(timer)
  }

  stopConfettiBurst()
  cheerPopups.value = []
}

const pickCheerLine = () => {
  if (cheerLinePool.length === 0) return ''
  let next = cheerLinePool[Math.floor(Math.random() * cheerLinePool.length)] || cheerLinePool[0]
  if (cheerLinePool.length > 1 && next === lastCheerLine) {
    const fallbackIndex = (cheerLinePool.indexOf(next) + 1) % cheerLinePool.length
    next = cheerLinePool[fallbackIndex]
  }
  lastCheerLine = next
  return next
}

const pickCheerPopupStyle = () => {
  const compact = isCompactViewport()
  const left = compact ? 28 + Math.random() * 44 : 18 + Math.random() * 64
  const top = compact ? 4 + Math.random() * 34 : 12 + Math.random() * 66
  return {
    left: `${left}%`,
    top: `${top}%`,
    transform: 'translate(-50%, 0)',
  }
}

const pushCheerPopup = () => {
  if (cheerPopups.value.length >= CHEER_POPUP_LIMIT) {
    cheerPopups.value.shift()
  }

  const item = {
    id: cheerPopupSeed++,
    text: pickCheerLine(),
    style: pickCheerPopupStyle(),
  }

  cheerPopups.value.push(item)

  const timer = window.setTimeout(() => {
    cheerPopups.value = cheerPopups.value.filter((entry) => entry.id !== item.id)
  }, CHEER_POPUP_STAY_MS)

  cheerPopupTimers.push(timer)
}

const cheerUp = () => {
  pushCheerPopup()
  playConfettiBurst()
}

const triggerStartupBurst = () => {
  if (startupBurstTimer) {
    clearTimeout(startupBurstTimer)
  }
  startupBurstTimer = window.setTimeout(() => {
    startupBurstTimer = 0
    playConfettiBurst()
  }, 260)
}

const goProfile = () => {
  router.push('/profile')
}

onMounted(async () => {
  applyCachedDailyQuote()
  fetchDailyQuote()
  scheduleDailyQuoteRefresh()
  triggerStartupBurst()

  hourTimer = window.setInterval(() => {
    currentHour.value = new Date().getHours()
  }, 60000)
})

onActivated(async () => {
  if (!applyCachedDailyQuote()) {
    fetchDailyQuote({ force: true })
  }
  scheduleDailyQuoteRefresh()
  triggerStartupBurst()
})

onUnmounted(() => {
  clearCheerResources()
  if (dailyQuoteTimer) clearTimeout(dailyQuoteTimer)
  if (hourTimer) clearInterval(hourTimer)
})

watch(displayNickname, () => {
  if (!applyCachedDailyQuote()) {
    fetchDailyQuote({ force: true })
  }
})
</script>

<style scoped>
.me-bg {
  background: var(--app-shell-bg);
}

.me-name-highlight {
  background-image: linear-gradient(135deg, var(--ai-from), var(--ai-to));
}

.me-shell {
  padding-inline: 24px;
}

.me-hero {
  padding-block: 10px 64px;
}

.me-title {
  max-width: 10.5em;
  margin: 0 auto;
  text-wrap: balance;
}

.me-subline {
  max-width: 12em;
  margin-inline: auto;
  text-wrap: balance;
}

.me-action-stack {
  padding-bottom: 12px;
}

.me-quote-card {
  width: min(100%, 980px);
  margin-inline: auto;
}

.me-quote-text {
  text-wrap: balance;
}

.avatar-orbit {
  width: 210px;
  height: 210px;
  border-radius: 9999px;
  display: grid;
  place-items: center;
  border: none;
  padding: 0;
  cursor: pointer;
  background:
    radial-gradient(60% 60% at 30% 30%, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.45)),
    linear-gradient(145deg, var(--module-glow-b), var(--module-glow-c));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  outline: none;
  box-shadow: 0 22px 54px -26px rgba(15, 23, 42, 0.5);
}

.dark .avatar-orbit {
  background:
    radial-gradient(60% 60% at 30% 30%, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05)),
    linear-gradient(145deg, var(--module-glow-b), var(--module-glow-c));
}

.avatar-orbit:hover {
  transform: translateY(-2px);
  box-shadow: 0 28px 58px -26px rgba(15, 23, 42, 0.55);
}

.avatar-orbit:focus-visible {
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.2), 0 22px 54px -26px rgba(15, 23, 42, 0.5);
}

.avatar-core {
  width: 150px;
  height: 150px;
  border-radius: 9999px;
  display: grid;
  place-items: center;
  background: var(--bg-elevated);
  font-size: 90px;
  line-height: 1;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.12);
  animation: avatarFloat 3.4s ease-in-out infinite;
}

.avatar-core img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.boost-btn {
  min-width: 210px;
  height: 66px;
  border-radius: 18px;
  font-size: 34px;
  font-weight: 700;
  color: #f8fafc;
  background: linear-gradient(135deg, var(--ai-from), var(--ai-to));
  box-shadow: 0 20px 40px -25px rgba(var(--primary-rgb), 0.62);
}

.profile-btn {
  min-width: 170px;
  height: 44px;
  border-radius: 14px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-card);
  transition: all 0.2s ease;
}

.profile-btn:hover {
  transform: translateY(-1px);
}

.cheer-popup-layer {
  position: absolute;
  left: 50%;
  top: 0;
  width: min(420px, 90vw);
  height: 280px;
  transform: translateX(-50%);
  pointer-events: none;
}

.cheer-popup {
  position: absolute;
  max-width: min(74vw, 220px);
  padding: 7px 12px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  text-align: center;
  color: var(--text-primary);
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(4px);
  white-space: normal;
  animation: cheerPopupFloat 3s ease forwards;
}

@keyframes cheerPopupFloat {
  0% {
    opacity: 0;
    transform: translate(-50%, 14px) scale(0.95);
  }
  14% {
    opacity: 1;
    transform: translate(-50%, 0) scale(1);
  }
  84% {
    opacity: 1;
    transform: translate(-50%, -18px) scale(1);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -28px) scale(0.98);
  }
}

@keyframes avatarFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@media (max-width: 960px) {
  .me-shell {
    padding-inline: 20px;
  }

  .me-hero {
    padding-block: 18px 48px;
  }

  .me-title {
    max-width: 9em;
    font-size: clamp(34px, 8.5vw, 52px) !important;
    line-height: 1.08;
  }

  .me-subline {
    max-width: 10.5em;
    margin-top: 14px;
    font-size: clamp(22px, 6vw, 34px) !important;
    line-height: 1.18;
  }

  .avatar-orbit {
    width: 150px;
    height: 150px;
  }

  .avatar-core {
    width: 110px;
    height: 110px;
    font-size: 64px;
  }

  .boost-btn {
    min-width: 180px;
    height: 56px;
    font-size: 28px;
  }

  .profile-btn {
    min-width: 146px;
    height: 40px;
    font-size: 16px;
  }

  .cheer-popup-layer {
    width: min(360px, 92vw);
    height: 170px;
  }

  .cheer-popup {
    font-size: 12px;
  }
}

@media (max-width: 640px) {
  .me-shell {
    padding-inline: 14px;
  }

  .me-hero {
    padding-block: 20px 40px;
  }

  .me-title {
    max-width: 8.2em;
    font-size: clamp(28px, 8vw, 40px) !important;
    line-height: 1.1;
  }

  .me-title-name {
    display: inline-block;
  }

  .me-subline {
    max-width: 9.4em;
    font-size: clamp(18px, 5.8vw, 28px) !important;
    line-height: 1.24;
  }

  .me-action-stack {
    margin-top: 28px !important;
    gap: 0;
  }

  .avatar-orbit {
    width: 138px;
    height: 138px;
  }

  .avatar-core {
    width: 102px;
    height: 102px;
    font-size: 58px;
  }

  .boost-btn {
    min-width: 0;
    width: min(100%, 246px);
    height: 58px;
    margin-top: 20px !important;
    font-size: 24px;
    border-radius: 20px;
  }

  .profile-btn {
    min-width: 0;
    width: min(100%, 194px);
    height: 44px;
    font-size: 15px;
  }

  .cheer-popup-layer {
    top: 10px;
    width: min(300px, 88vw);
    height: 132px;
  }

  .cheer-popup {
    max-width: min(58vw, 160px);
    padding: 6px 10px;
    font-size: 11px;
    line-height: 1.3;
  }

  .me-quote-card {
    margin-top: 28px !important;
    padding: 22px 18px !important;
    border-radius: 28px;
  }

  .me-quote-text {
    margin-top: 14px !important;
    font-size: clamp(18px, 5.6vw, 30px) !important;
    line-height: 1.18;
  }
}
</style>
