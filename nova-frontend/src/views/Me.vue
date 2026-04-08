<template>
  <div class="h-full relative overflow-hidden me-bg workspace-page workspace-scroll">
    <canvas ref="confettiCanvas" class="absolute inset-0 w-full h-full pointer-events-none"></canvas>

    <div class="relative z-10 h-full overflow-y-auto custom-scrollbar">
      <div class="workspace-stack max-w-[1120px] mx-auto text-center">
        <header class="workspace-titlebar me-titlebar text-left">
          <div class="workspace-titlecopy">
            <h1 class="workspace-title">我的空间</h1>
            <p class="workspace-subtitle">聚合身份信息、状态反馈与个人激励，保持所有模块中的体验一致。</p>
          </div>
        </header>

        <section class="workspace-section">
        <h1 class="text-[clamp(40px,6vw,66px)] font-black leading-tight text-text-primary break-words">
          {{ greetingPrefix }}锛?
          <span class="me-name-highlight text-transparent bg-clip-text">
            {{ displayNickname }}
          </span>
        </h1>
        <p class="mt-3 text-[clamp(28px,4.4vw,52px)] text-text-secondary leading-tight">{{ subLine }}</p>

        <div class="mt-8 flex flex-col items-center relative">
          <button class="avatar-orbit" type="button" @click="goProfile" title="鍓嶅線涓汉璧勬枡">
            <div class="avatar-core">
              <span v-if="isEmoji(displayAvatar)">{{ displayAvatar }}</span>
              <img v-else :src="displayAvatar" alt="avatar" />
            </div>
          </button>

          <button class="workspace-btn workspace-btn-primary boost-btn mt-6" @click="cheerUp">鈾?缁欐垜鎵撴皵</button>
          <button class="workspace-btn workspace-btn-muted profile-btn mt-3" @click="goProfile">涓汉璧勬枡</button>

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

        <div class="workspace-shell mt-10 bg-bg-surface backdrop-blur-xl rounded-3xl shadow-card border border-border-subtle p-7 md:p-8 text-left">
          <div class="text-xl font-bold text-text-primary">浠婃棩鏍艰█</div>
          <div class="mt-5 text-[clamp(30px,4.2vw,52px)] font-medium text-text-primary tracking-tight leading-tight">{{ quote }}</div>
        </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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

const quotePool = [
  '你写下的每一行代码，都在改变未来。',
  '坚持的背后，藏着最美的风景。',
  '今天的一点进步，就是明天的一大步。',
  '把复杂问题拆小，你已经赢了一半。',
  '稳定输出，比偶尔爆发更强大。',
  '保持专注，时间会给你复利。',
]

const createNonRepeatPicker = (pool) => {
  let queue = []
  let last = ''

  const shuffle = (arr) => {
    for (let i = arr.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[arr[i], arr[j]] = [arr[j], arr[i]]
    }
    return arr
  }

  return () => {
    if (queue.length === 0) {
      queue = shuffle([...pool])
      if (queue.length > 1 && queue[0] === last) {
        ;[queue[0], queue[1]] = [queue[1], queue[0]]
      }
    }
    const next = queue.shift() || pool[0] || ''
    last = next
    return next
  }
}

const pickQuote = createNonRepeatPicker(quotePool)

const quote = ref(pickQuote())
let copyTimer = 0

const rotateCopy = () => {
  quote.value = pickQuote()
}

const guestIdPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const isGuest = computed(() => {
  if (authStore.isGuest) return true
  const nickname = String(authStore.nickname || '')
  const username = String(authStore.user?.username || authStore.username || '')
  return nickname.startsWith('娓稿') || guestIdPattern.test(username)
})

const displayAvatar = computed(() => {
  const avatar = String(authStore.avatar || '').trim()
  if (!avatar) return '馃コ'
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

const confettiCanvas = ref(null)
let ctx = null
let rafId = 0
let cheerLoopTimer = 0
const pieces = []
const startupBurstTimers = []

const CHEER_CHAIN_BURSTS = 3
const CHEER_CHAIN_GAP_MS = 90
const CHEER_STRENGTH = 1.55
const CHEER_POPUP_LIMIT = 3
const CHEER_POPUP_STAY_MS = 3000

const ENTRY_CHAIN_BURSTS = 2
const ENTRY_CHAIN_GAP_MS = 120
const ENTRY_STRENGTH = 1.45

const colors = ['#dc2626', '#ffffff', '#3b82f6', '#eab308', '#22c55e', '#ef4444', '#a855f7']
const rand = (a, b) => a + Math.random() * (b - a)

const spawn = (side, strength = 1) => {
  const w = confettiCanvas.value?.clientWidth || window.innerWidth
  const h = confettiCanvas.value?.clientHeight || window.innerHeight
  const count = Math.floor(280 * strength)

  for (let i = 0; i < count; i += 1) {
    const fromLeft = side === 'left'
    pieces.push({
      x: fromLeft ? rand(-60, w * 0.2) : rand(w * 0.8, w + 60),
      y: rand(h * 0.18, h * 0.96),
      vx: fromLeft ? rand(2.2, 6.2) : rand(-6.2, -2.2),
      vy: rand(-6.2, -1.3),
      size: rand(4.5, 10.8),
      angle: rand(0, Math.PI * 2),
      spin: rand(-0.23, 0.23),
      color: colors[Math.floor(Math.random() * colors.length)],
      life: rand(108, 218),
      shape: Math.random() > 0.5 ? 'rect' : 'circle',
    })
  }
}

const triggerSideCannons = (strength = 1) => {
  spawn('left', strength)
  spawn('right', strength)
}

const clearCheerResources = () => {
  while (startupBurstTimers.length) {
    const timer = startupBurstTimers.pop()
    if (timer) clearTimeout(timer)
  }

  while (cheerPopupTimers.length) {
    const timer = cheerPopupTimers.pop()
    if (timer) clearTimeout(timer)
  }

  if (cheerLoopTimer) {
    clearInterval(cheerLoopTimer)
    cheerLoopTimer = 0
  }

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
  const left = 18 + Math.random() * 64
  const top = 12 + Math.random() * 66
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

const restartCheerSequence = () => {
  if (cheerLoopTimer) {
    clearInterval(cheerLoopTimer)
    cheerLoopTimer = 0
  }

  pieces.length = 0

  let burstIndex = 0
  triggerSideCannons(CHEER_STRENGTH)
  burstIndex += 1

  cheerLoopTimer = window.setInterval(() => {
    if (burstIndex >= CHEER_CHAIN_BURSTS) {
      clearInterval(cheerLoopTimer)
      cheerLoopTimer = 0
      return
    }

    triggerSideCannons(CHEER_STRENGTH)
    burstIndex += 1
  }, CHEER_CHAIN_GAP_MS)
}

const loop = () => {
  if (!ctx || !confettiCanvas.value) return

  const w = confettiCanvas.value.clientWidth
  const h = confettiCanvas.value.clientHeight
  ctx.clearRect(0, 0, w, h)

  for (let i = pieces.length - 1; i >= 0; i -= 1) {
    const p = pieces[i]
    p.x += p.vx
    p.y += p.vy
    p.vy += 0.045
    p.vx *= 0.996
    p.angle += p.spin
    p.life -= 1

    ctx.save()
    ctx.translate(p.x, p.y)
    ctx.rotate(p.angle)
    ctx.fillStyle = p.color
    ctx.globalAlpha = Math.max(0.06, p.life / 220)
    if (p.shape === 'rect') {
      ctx.fillRect(-p.size * 0.5, -p.size * 0.22, p.size, p.size * 0.44)
    } else {
      ctx.beginPath()
      ctx.arc(0, 0, p.size * 0.3, 0, Math.PI * 2)
      ctx.fill()
    }
    ctx.restore()

    if (p.life <= 0 || p.y > h + 24) {
      pieces.splice(i, 1)
    }
  }

  rafId = requestAnimationFrame(loop)
}

const fitCanvas = () => {
  if (!confettiCanvas.value || !ctx) return
  const dpr = Math.min(window.devicePixelRatio || 1, 1.4)
  const w = confettiCanvas.value.clientWidth
  const h = confettiCanvas.value.clientHeight
  confettiCanvas.value.width = Math.floor(w * dpr)
  confettiCanvas.value.height = Math.floor(h * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

const cheerUp = () => {
  rotateCopy()
  pushCheerPopup()
  restartCheerSequence()
}

const goProfile = () => {
  router.push('/profile')
}

onMounted(() => {
  ctx = confettiCanvas.value?.getContext('2d')
  if (!ctx) return

  fitCanvas()
  for (let i = 0; i < ENTRY_CHAIN_BURSTS; i += 1) {
    const timer = window.setTimeout(() => {
      triggerSideCannons(ENTRY_STRENGTH)
    }, i * ENTRY_CHAIN_GAP_MS)
    startupBurstTimers.push(timer)
  }

  rafId = requestAnimationFrame(loop)
  window.addEventListener('resize', fitCanvas)

  copyTimer = window.setInterval(() => {
    rotateCopy()
  }, 8000)

  hourTimer = window.setInterval(() => {
    currentHour.value = new Date().getHours()
  }, 60000)
})

onUnmounted(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', fitCanvas)
  clearCheerResources()
  if (copyTimer) clearInterval(copyTimer)
  if (hourTimer) clearInterval(hourTimer)
})
</script>

<style scoped>
.me-bg {
  background: var(--app-shell-bg);
}

.me-name-highlight {
  background-image: linear-gradient(135deg, var(--ai-from), var(--ai-to));
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
    height: 250px;
  }

  .cheer-popup {
    font-size: 12px;
  }
}
</style>

