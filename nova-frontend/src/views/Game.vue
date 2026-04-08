<template>
  <div class="game-root workspace-page workspace-scroll h-full flex items-start sm:items-center justify-center px-3 py-3 sm:px-6 sm:py-5">
    <div class="workspace-stack w-full space-y-3 sm:space-y-4">
      <div class="forest-panel workspace-titlebar rounded-2xl bg-bg-surface/60 backdrop-blur-md border border-border-soft shadow-sm px-4 py-3 sm:px-5 sm:py-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3 sm:gap-4">
        <div class="flex items-center gap-3">
          <span class="text-2xl">🎮</span>
          <div>
            <h2 class="game-title-gradient text-lg font-bold text-transparent bg-clip-text">休闲时刻</h2>
            <p class="text-xs text-text-tertiary mt-0.5">森林跑酷三赛道，翠花快跑重构版</p>
          </div>
        </div>

        <template v-if="activeGame === 'cuihua-runner'">
          <div class="hud-grid grid grid-cols-2 md:grid-cols-4 gap-2">
            <div class="hud-item"><span>当前分</span><strong>{{ score }}</strong></div>
            <div class="hud-item"><span>最高分</span><strong>{{ highScore }}</strong></div>
            <div class="hud-item"><span>速度</span><strong>{{ speedKmh }} km/h</strong></div>
            <div class="hud-item"><span>等级</span><strong>Lv.{{ level }}</strong></div>
          </div>
        </template>
        <template v-else>
          <div class="rounded-xl border border-black/10 bg-slate-50 px-4 py-2 text-sm text-slate-600">选择一个小游戏开始放松</div>
        </template>
      </div>

      <div v-if="activeGame === null" class="forest-hall workspace-shell rounded-3xl bg-bg-surface/50 backdrop-blur-md border border-border-soft shadow-sm p-5 sm:p-6">
        <div class="flex items-center justify-between gap-3 mb-4">
          <h3 class="text-xl font-semibold text-slate-900">游戏大厅</h3>
          <span class="text-xs text-slate-500">持续上新中</span>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <article class="game-card workspace-panel game-card-active">
            <div class="cover cover-runner" @click="enterGame('cuihua-runner')" role="button" tabindex="0" @keydown.enter="enterGame('cuihua-runner')">
              <div class="cover-badge">热门</div>
              <div class="cover-track"></div>
              <div class="cover-runner-avatar">👷</div>
              <div class="cover-obstacle cover-obstacle-left"></div>
              <div class="cover-obstacle cover-obstacle-mid"></div>
              <div class="cover-obstacle cover-obstacle-right"></div>
            </div>
            <div class="p-4">
              <div class="flex items-center justify-between gap-2">
                <h4 class="text-base font-semibold text-slate-900">翠花快跑</h4>
                <span class="game-live-chip text-xs rounded-full px-2 py-0.5">已上线</span>
              </div>
              <p class="mt-2 text-sm text-slate-600">森林三赛道跑酷，左右切道规避光头强障碍。</p>
              <div class="mt-3 flex items-center justify-between">
                <span class="text-xs text-slate-500">历史最高：{{ highScore }}</span>
                <button @click="enterGame('cuihua-runner')" class="workspace-btn workspace-btn-primary game-enter-btn px-4 py-2 rounded-lg text-white text-sm font-medium shadow hover:opacity-90 transition">进入游戏</button>
              </div>
            </div>
          </article>

          <article class="game-card workspace-panel game-card-coming">
            <div class="cover cover-coming">
              <div class="coming-tag">敬请期待</div>
              <div class="coming-sub">下一款小游戏正在开发中</div>
            </div>
            <div class="p-4">
              <div class="flex items-center justify-between gap-2">
                <h4 class="text-base font-semibold text-slate-900">神秘新游</h4>
                <span class="text-xs text-slate-500 bg-slate-100 border border-slate-200 rounded-full px-2 py-0.5">未上线</span>
              </div>
              <p class="mt-2 text-sm text-slate-600">更多玩法准备中，后续会在这里解锁。</p>
              <button class="workspace-btn workspace-btn-muted mt-3 w-full px-4 py-2 rounded-lg border border-slate-200 bg-slate-100 text-slate-400 text-sm cursor-not-allowed" disabled>敬请期待</button>
            </div>
          </article>
        </div>
      </div>

      <div v-else class="space-y-2.5 sm:space-y-3">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <button @click="goBackToHall" class="workspace-btn workspace-btn-muted inline-flex items-center justify-center sm:justify-start gap-2 px-3 py-2 rounded-lg border border-border-soft bg-bg-card text-sm text-text-secondary hover:bg-bg-surface transition"><span>←</span><span>返回游戏大厅</span></button>
          <div class="flex items-center justify-center sm:justify-end gap-2 text-xs text-slate-500">
            <span class="game-tip-chip tip-lane">← → 切道</span>
            <span>速度倍率 x{{ speedRate }}</span>
          </div>
        </div>

        <div class="game-stage relative w-full aspect-[4/3] sm:aspect-[16/9] rounded-3xl overflow-hidden shadow-2xl ring-1 ring-black/10 bg-slate-900/95">
          <canvas ref="gameCanvas" class="w-full h-full block touch-none"></canvas>
          <div v-if="gameState !== 'playing'" class="absolute inset-0 bg-black/52 backdrop-blur-sm flex items-center justify-center select-none px-4">
            <div class="text-center text-white max-w-xl">
              <div class="text-6xl mb-4">{{ gameState === 'idle' ? '🚇' : '💥' }}</div>
              <p class="text-2xl sm:text-3xl font-display font-bold tracking-wide">{{ gameState === 'idle' ? '翠花快跑·森林赛道' : '挑战结束，再冲一局' }}</p>
              <p class="mt-3 text-sm text-white/85">森林赛道已开启，左右切道避开光头强障碍。</p>
              <button @click="startGame" class="workspace-btn workspace-btn-primary game-enter-btn mt-6 px-8 py-3.5 text-white font-bold rounded-xl shadow-lg hover:opacity-90 active:scale-95 transition-all">{{ gameState === 'idle' ? '开始挑战' : '再来一局' }}</button>
              <p class="mt-4 text-xs text-white/70">操作：A/D 或 ←/→ 切道，移动端左右滑动</p>
            </div>
          </div>
        </div>

        <div class="mobile-controls sm:hidden">
          <button class="mobile-lane-btn" :disabled="gameState !== 'playing'" @click="moveLane(-1)">← 左移</button>
          <button class="mobile-lane-btn" :disabled="gameState !== 'playing'" @click="moveLane(1)">右移 →</button>
        </div>
        <p class="sm:hidden text-center text-[11px] text-slate-500">手机端可左右滑动画布，或使用下方按钮切道</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const gameCanvas = ref(null)
const gameState = ref('idle')
const activeGame = ref(null)

const score = ref(0)
const highScore = ref(Number(localStorage.getItem('cuihua_highscore') || 0))
const speedKmh = ref(24)
const level = ref(1)
const speedRate = ref('1.00')
const isMobile = ref(false)

const authStore = useAuthStore()

const view = { width: 0, height: 0 }
const PLAYER_DEPTH = 0.9
const PLAYER_ZONE_START = PLAYER_DEPTH - 0.045
const PLAYER_ZONE_END = PLAYER_DEPTH + 0.035

const world = {
  pace: 0.34,
  targetPace: 0.34,
  elapsed: 0,
  distance: 0,
  spawnTimer: 0.74,
  stripeOffset: 0,
  cloudOffset: 0,
  hillOffset: 0,
  treeOffset: 0,
  shakeTime: 0,
  shakePower: 0,
}

const runner = {
  lane: 0,
  laneVisual: 0,
  w: 92,
  h: 92,
  tilt: 0,
}

const OBSTACLE_SET = {
  train: { type: 'train', w: 98, h: 146, hitScale: { x: 0.1, y: 0.06, w: 0.8, h: 0.9 } },
}

let ctx = null
let animationId = null
let lastFrameAt = 0
let obstacles = []
let coins = []
let particles = []
let pointerStart = null
let bonusScore = 0

let finalScoreReported = false
let currentRoundId = ''
let liveReporterTimer = null
let liveReportedScore = 0
let liveReportInFlight = false

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const lerp = (from, to, t) => from + (to - from) * t
const pick = (list) => list[Math.floor(Math.random() * list.length)]
const randomLane = () => pick([-1, 0, 1])

const getGroundY = () => view.height * 0.84
const getPlayerFloorY = () => getGroundY() - runner.h
const getRoadMetrics = () => ({
  horizon: view.height * (isMobile.value ? 0.27 : 0.3),
  baseY: getGroundY() + (isMobile.value ? 14 : 20),
  centerX: view.width * 0.5,
  topWidth: view.width * (isMobile.value ? 0.24 : 0.19),
  bottomWidth: view.width * (isMobile.value ? 1.08 : 0.98),
})

const roadWidthAtDepth = (depth) => {
  const { topWidth, bottomWidth } = getRoadMetrics()
  return lerp(topWidth, bottomWidth, clamp(depth, 0, 1))
}

const groundYAtDepth = (depth) => {
  const { horizon } = getRoadMetrics()
  return lerp(horizon + 4, getGroundY(), clamp(depth, 0, 1))
}

const laneXAtDepth = (lane, depth) => {
  const { centerX } = getRoadMetrics()
  const gap = roadWidthAtDepth(depth) / 3.26
  return centerX + lane * gap
}

const obstacleGeometry = (obs) => {
  const depth = clamp(obs.depth, 0, 1.25)
  const scale = isMobile.value ? 0.84 : 0.92
  const w = obs.w * scale
  const h = obs.h * scale
  const x = laneXAtDepth(obs.lane, depth) - w / 2
  const y = groundYAtDepth(depth) - h
  const hs = obs.hitScale || { x: 0.12, y: 0.12, w: 0.76, h: 0.76 }
  return {
    x,
    y,
    w,
    h,
    hitRect: {
      x: x + w * hs.x,
      y: y + h * hs.y,
      w: w * hs.w,
      h: h * hs.h,
    },
  }
}

const addParticles = (x, y, count, palette) => {
  for (let i = 0; i < count; i++) {
    particles.push({
      x,
      y,
      vx: (Math.random() - 0.5) * 260,
      vy: -Math.random() * 160 - 40,
      life: 0.34 + Math.random() * 0.45,
      size: 1.5 + Math.random() * 3,
      color: pick(palette),
    })
  }
}

const roundedPath = (x, y, w, h, radius) => {
  const r = Math.min(radius, w * 0.5, h * 0.5)
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

const nearestWarningsByLane = () => {
  const result = { '-1': null, '0': null, '1': null }
  for (const obs of obstacles) {
    if (obs.depth < 0.03 || obs.depth > PLAYER_ZONE_END + 0.06) continue
    const laneKey = String(obs.lane)
    const old = result[laneKey]
    if (!old || obs.depth > old.depth) {
      result[laneKey] = obs
    }
  }
  return result
}

const drawForestTree = (x, baseY, scale, tone, trunkTone = '#3f2b1a') => {
  const trunkW = 10 * scale
  const trunkH = 34 * scale
  ctx.fillStyle = trunkTone
  roundedPath(x - trunkW / 2, baseY - trunkH, trunkW, trunkH, Math.max(3, 2.5 * scale))
  ctx.fill()

  ctx.fillStyle = tone
  ctx.beginPath()
  ctx.arc(x, baseY - trunkH - 18 * scale, 24 * scale, 0, Math.PI * 2)
  ctx.arc(x - 14 * scale, baseY - trunkH - 6 * scale, 18 * scale, 0, Math.PI * 2)
  ctx.arc(x + 14 * scale, baseY - trunkH - 6 * scale, 18 * scale, 0, Math.PI * 2)
  ctx.fill()
}

const drawSky = (dt) => {
  const { horizon } = getRoadMetrics()
  const sky = ctx.createLinearGradient(0, 0, 0, view.height * 0.9)
  sky.addColorStop(0, '#102112')
  sky.addColorStop(0.42, '#1f4b31')
  sky.addColorStop(1, '#3c6b45')
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, view.width, view.height)

  const mist = ctx.createLinearGradient(0, horizon - 60, 0, horizon + 120)
  mist.addColorStop(0, 'rgba(221, 255, 230, 0.18)')
  mist.addColorStop(1, 'rgba(221, 255, 230, 0)')
  ctx.fillStyle = mist
  ctx.fillRect(0, horizon - 80, view.width, 220)

  world.cloudOffset = (world.cloudOffset + world.pace * dt * view.width * 0.11) % (view.width + 320)
  for (let i = 0; i < 7; i++) {
    const x = ((i * 260) - world.cloudOffset + view.width + 320) % (view.width + 320) - 160
    const y = 54 + (i % 3) * 44
    ctx.fillStyle = 'rgba(226, 255, 235, 0.14)'
    ctx.beginPath()
    ctx.ellipse(x, y, 78, 21, 0, 0, Math.PI * 2)
    ctx.fill()
  }

  world.hillOffset = (world.hillOffset + world.pace * dt * view.width * 0.16) % (view.width + 540)
  ctx.fillStyle = 'rgba(27, 58, 37, 0.74)'
  for (let i = 0; i < 5; i++) {
    const x = ((i * 320) - world.hillOffset + view.width + 540) % (view.width + 540) - 270
    ctx.beginPath()
    ctx.moveTo(x, horizon + 172)
    ctx.quadraticCurveTo(x + 138, horizon + 8, x + 276, horizon + 172)
    ctx.closePath()
    ctx.fill()
  }

  world.treeOffset = (world.treeOffset + world.pace * dt * view.width * 0.34) % (view.width + 220)
  for (let i = 0; i < 14; i++) {
    const x = ((i * 120) - world.treeOffset + view.width + 220) % (view.width + 220) - 110
    const baseY = horizon + 165 + (i % 3) * 10
    const scale = 0.52 + (i % 4) * 0.15
    drawForestTree(x, baseY, scale, 'rgba(18, 62, 35, 0.86)', '#3d2a1a')
  }

  for (let i = 0; i < 7; i++) {
    const x = ((i * 190) - world.treeOffset * 1.45 + view.width + 260) % (view.width + 260) - 120
    const baseY = horizon + 198 + (i % 2) * 8
    const scale = 0.82 + (i % 3) * 0.16
    drawForestTree(x, baseY, scale, 'rgba(20, 74, 40, 0.96)', '#4a3220')
  }

  for (let i = 0; i < 18; i++) {
    const x = (i / 18) * view.width + Math.sin(world.elapsed * 0.8 + i) * 12
    const y = horizon + 40 + (i % 5) * 10
    const flicker = 0.18 + ((Math.sin(world.elapsed * 3 + i * 0.7) + 1) * 0.12)
    ctx.fillStyle = `rgba(253, 224, 71, ${flicker})`
    ctx.beginPath()
    ctx.arc(x, y, 2.2, 0, Math.PI * 2)
    ctx.closePath()
    ctx.fill()
  }
}

const drawRoad = (dt) => {
  const { horizon, baseY, centerX, topWidth, bottomWidth } = getRoadMetrics()

  const road = ctx.createLinearGradient(0, horizon, 0, baseY)
  road.addColorStop(0, '#5c472b')
  road.addColorStop(0.44, '#4a3622')
  road.addColorStop(1, '#2e2116')
  ctx.fillStyle = road
  ctx.beginPath()
  ctx.moveTo(centerX - topWidth / 2, horizon)
  ctx.lineTo(centerX + topWidth / 2, horizon)
  ctx.lineTo(centerX + bottomWidth / 2, baseY)
  ctx.lineTo(centerX - bottomWidth / 2, baseY)
  ctx.closePath()
  ctx.fill()

  ctx.fillStyle = 'rgba(34, 85, 44, 0.6)'
  ctx.beginPath()
  ctx.moveTo(0, baseY + 22)
  ctx.lineTo(centerX - bottomWidth / 2, baseY)
  ctx.lineTo(centerX - topWidth / 2, horizon)
  ctx.lineTo(0, horizon + 24)
  ctx.closePath()
  ctx.fill()
  ctx.beginPath()
  ctx.moveTo(view.width, baseY + 22)
  ctx.lineTo(centerX + bottomWidth / 2, baseY)
  ctx.lineTo(centerX + topWidth / 2, horizon)
  ctx.lineTo(view.width, horizon + 24)
  ctx.closePath()
  ctx.fill()

  const warningByLane = nearestWarningsByLane()
  for (const lane of [-1, 0, 1]) {
    const laneWarning = warningByLane[String(lane)]
    if (!laneWarning) continue
    const nearHit = laneWarning.depth > PLAYER_ZONE_START - 0.06
    const laneColor = nearHit ? 'rgba(239, 68, 68, 0.2)' : 'rgba(239, 68, 68, 0.14)'

    ctx.fillStyle = laneColor
    ctx.beginPath()
    ctx.moveTo(laneXAtDepth(lane - 0.45, 0), horizon)
    ctx.lineTo(laneXAtDepth(lane + 0.45, 0), horizon)
    ctx.lineTo(laneXAtDepth(lane + 0.5, 1), baseY)
    ctx.lineTo(laneXAtDepth(lane - 0.5, 1), baseY)
    ctx.closePath()
    ctx.fill()
  }

  ctx.strokeStyle = 'rgba(233, 213, 179, 0.24)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(centerX - topWidth / 2, horizon)
  ctx.lineTo(centerX - bottomWidth / 2, baseY)
  ctx.moveTo(centerX + topWidth / 2, horizon)
  ctx.lineTo(centerX + bottomWidth / 2, baseY)
  ctx.stroke()

  ctx.strokeStyle = 'rgba(252, 246, 227, 0.26)'
  ctx.beginPath()
  for (const laneBoundary of [-0.5, 0.5]) {
    const xTop = centerX + laneBoundary * (topWidth / 3)
    const xBottom = centerX + laneBoundary * (bottomWidth / 3)
    ctx.moveTo(xTop, horizon)
    ctx.lineTo(xBottom, baseY)
  }
  ctx.stroke()

  const gatePulse = 0.5 + (Math.sin(world.elapsed * 12) + 1) * 0.25
  const gateDepth0 = PLAYER_ZONE_START
  const gateDepth1 = PLAYER_ZONE_END
  for (const lane of [-1, 0, 1]) {
    const laneWarning = warningByLane[String(lane)]
    const danger = !!laneWarning && laneWarning.depth > PLAYER_ZONE_START - 0.06
    const left0 = laneXAtDepth(lane - 0.5, gateDepth0)
    const right0 = laneXAtDepth(lane + 0.5, gateDepth0)
    const left1 = laneXAtDepth(lane - 0.5, gateDepth1)
    const right1 = laneXAtDepth(lane + 0.5, gateDepth1)

    ctx.fillStyle = danger
      ? `rgba(239, 68, 68, ${0.2 + gatePulse * 0.24})`
      : lane === runner.lane
        ? 'rgba(56, 189, 248, 0.16)'
        : 'rgba(255, 255, 255, 0.06)'
    ctx.beginPath()
    ctx.moveTo(left0, groundYAtDepth(gateDepth0))
    ctx.lineTo(right0, groundYAtDepth(gateDepth0))
    ctx.lineTo(right1, groundYAtDepth(gateDepth1))
    ctx.lineTo(left1, groundYAtDepth(gateDepth1))
    ctx.closePath()
    ctx.fill()

    ctx.lineWidth = danger ? 2.4 : 1.4
    ctx.strokeStyle = danger
      ? `rgba(254, 202, 202, ${0.62 + gatePulse * 0.34})`
      : 'rgba(186, 230, 253, 0.72)'
    ctx.beginPath()
    ctx.moveTo(left0, groundYAtDepth(gateDepth0))
    ctx.lineTo(right0, groundYAtDepth(gateDepth0))
    ctx.lineTo(right1, groundYAtDepth(gateDepth1))
    ctx.lineTo(left1, groundYAtDepth(gateDepth1))
    ctx.closePath()
    ctx.stroke()
  }

  world.stripeOffset = (world.stripeOffset + world.pace * dt * 2.3) % 1
  for (let i = 0; i < 22; i++) {
    const depth = ((i / 22) + world.stripeOffset) % 1
    const y = lerp(horizon + 6, baseY - 18, depth)
    const roadWidth = roadWidthAtDepth(depth)
    const gap = roadWidth / 3.26
    const markW = lerp(4, 19, depth)
    const markH = lerp(2, 10, depth)

    ctx.fillStyle = 'rgba(255, 244, 220, 0.9)'
    ctx.fillRect(centerX - gap - markW / 2, y, markW, markH)
    ctx.fillRect(centerX - markW / 2, y, markW, markH)
    ctx.fillRect(centerX + gap - markW / 2, y, markW, markH)
  }

  ctx.fillStyle = '#112217'
  ctx.fillRect(0, getGroundY(), view.width, view.height - getGroundY())
}
const drawObstacle = (obs) => {
  const g = obstacleGeometry(obs)
  obs.hitRect = g.hitRect
  const near = clamp((obs.depth - 0.54) / 0.42, 0, 1)
  const danger = obs.depth >= PLAYER_ZONE_START - 0.05 && obs.depth <= PLAYER_ZONE_END + 0.05
  const pulse = 0.58 + (Math.sin(world.elapsed * 16) + 1) * 0.21

  ctx.save()
  ctx.globalAlpha = 0.68 + near * 0.32
  ctx.shadowColor = 'rgba(8, 20, 14, 0.5)'
  ctx.shadowBlur = Math.max(7, g.w * 0.16)
  ctx.shadowOffsetY = Math.max(2, g.h * 0.1)

  if (obs.type === 'train') {
    ctx.fillStyle = `rgba(12, 20, 14, ${0.24 + near * 0.34})`
    ctx.beginPath()
    ctx.ellipse(g.x + g.w * 0.5, g.y + g.h * 1.04, g.w * (0.35 + near * 0.1), g.h * 0.1, 0, 0, Math.PI * 2)
    ctx.fill()

    const cx = g.x + g.w * 0.5
    const headY = g.y + g.h * 0.27
    const headR = g.w * 0.19

    const coat = ctx.createLinearGradient(g.x, g.y + g.h * 0.36, g.x + g.w, g.y + g.h)
    coat.addColorStop(0, '#1e3a8a')
    coat.addColorStop(0.52, '#1e40af')
    coat.addColorStop(1, '#172554')
    ctx.fillStyle = coat
    roundedPath(g.x + g.w * 0.23, g.y + g.h * 0.42, g.w * 0.54, g.h * 0.34, Math.max(8, g.w * 0.08))
    ctx.fill()

    ctx.fillStyle = '#fbbf24'
    roundedPath(g.x + g.w * 0.34, g.y + g.h * 0.39, g.w * 0.32, g.h * 0.31, Math.max(7, g.w * 0.07))
    ctx.fill()

    ctx.fillStyle = '#e5e7eb'
    roundedPath(g.x + g.w * 0.44, g.y + g.h * 0.4, g.w * 0.12, g.h * 0.3, Math.max(6, g.w * 0.06))
    ctx.fill()

    ctx.fillStyle = '#334155'
    roundedPath(g.x + g.w * 0.24, g.y + g.h * 0.76, g.w * 0.22, g.h * 0.18, Math.max(5, g.w * 0.05))
    ctx.fill()
    roundedPath(g.x + g.w * 0.54, g.y + g.h * 0.76, g.w * 0.22, g.h * 0.18, Math.max(5, g.w * 0.05))
    ctx.fill()

    ctx.fillStyle = '#f2c6a0'
    ctx.beginPath()
    ctx.arc(cx, headY, headR, 0, Math.PI * 2)
    ctx.fill()

    if (obs.variant === 'furhat') {
      ctx.fillStyle = '#6b3f20'
      roundedPath(g.x + g.w * 0.26, g.y + g.h * 0.06, g.w * 0.48, g.h * 0.14, Math.max(9, g.w * 0.09))
      ctx.fill()
      ctx.fillStyle = '#8b5a2b'
      roundedPath(g.x + g.w * 0.2, g.y + g.h * 0.12, g.w * 0.16, g.h * 0.2, Math.max(7, g.w * 0.07))
      ctx.fill()
      roundedPath(g.x + g.w * 0.64, g.y + g.h * 0.12, g.w * 0.16, g.h * 0.2, Math.max(7, g.w * 0.07))
      ctx.fill()
    } else {
      const helmet = ctx.createLinearGradient(g.x, g.y + g.h * 0.06, g.x + g.w, g.y + g.h * 0.23)
      helmet.addColorStop(0, '#fb923c')
      helmet.addColorStop(0.6, '#f97316')
      helmet.addColorStop(1, '#ea580c')
      ctx.fillStyle = helmet
      roundedPath(g.x + g.w * 0.25, g.y + g.h * 0.06, g.w * 0.5, g.h * 0.15, Math.max(9, g.w * 0.09))
      ctx.fill()
      ctx.fillStyle = '#1f2937'
      ctx.fillRect(g.x + g.w * 0.25, g.y + g.h * 0.15, g.w * 0.5, g.h * 0.024)
    }

    ctx.strokeStyle = '#111827'
    ctx.lineWidth = Math.max(1.5, g.w * 0.02)
    ctx.beginPath()
    ctx.moveTo(cx - headR * 0.5, headY - headR * 0.24)
    ctx.lineTo(cx - headR * 0.16, headY - headR * 0.28)
    ctx.moveTo(cx + headR * 0.5, headY - headR * 0.24)
    ctx.lineTo(cx + headR * 0.16, headY - headR * 0.28)
    ctx.stroke()

    ctx.fillStyle = '#0f172a'
    ctx.beginPath()
    ctx.arc(cx - headR * 0.22, headY - headR * 0.05, headR * 0.08, 0, Math.PI * 2)
    ctx.arc(cx + headR * 0.22, headY - headR * 0.05, headR * 0.08, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = '#b91c1c'
    ctx.beginPath()
    ctx.arc(cx, headY + headR * 0.08, headR * 0.1, 0, Math.PI * 2)
    ctx.fill()

    ctx.strokeStyle = '#0f172a'
    ctx.lineWidth = Math.max(1.4, g.w * 0.02)
    ctx.beginPath()
    ctx.arc(cx, headY + headR * 0.2, headR * 0.34, 0.15, Math.PI - 0.15)
    ctx.stroke()

    ctx.fillStyle = '#111827'
    roundedPath(cx - headR * 0.45, headY + headR * 0.13, headR * 0.9, headR * 0.18, headR * 0.08)
    ctx.fill()

    ctx.fillStyle = '#2f1d14'
    roundedPath(g.x + g.w * 0.06, g.y + g.h * 0.48, g.w * 0.18, g.h * 0.2, Math.max(5, g.w * 0.05))
    ctx.fill()
    roundedPath(g.x + g.w * 0.76, g.y + g.h * 0.48, g.w * 0.18, g.h * 0.2, Math.max(5, g.w * 0.05))
    ctx.fill()
  }

  ctx.shadowColor = 'transparent'
  ctx.lineWidth = Math.max(2, g.w * 0.06)
  ctx.strokeStyle = 'rgba(15, 23, 42, 0.9)'
  roundedPath(g.x, g.y, g.w, g.h, Math.max(8, g.w * 0.08))
  ctx.stroke()
  ctx.lineWidth = Math.max(1.4, g.w * 0.035)
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.9)'
  roundedPath(g.x, g.y, g.w, g.h, Math.max(8, g.w * 0.08))
  ctx.stroke()

  if (danger) {
    ctx.lineWidth = Math.max(2.4, g.w * 0.05)
    ctx.strokeStyle = `rgba(254, 202, 202, ${pulse})`
    roundedPath(g.x - 5, g.y - 5, g.w + 10, g.h + 10, Math.max(10, g.w * 0.1))
    ctx.stroke()

    ctx.fillStyle = `rgba(239, 68, 68, ${0.62 + pulse * 0.3})`
    ctx.beginPath()
    ctx.moveTo(g.x + g.w * 0.5, g.y - 24)
    ctx.lineTo(g.x + g.w * 0.66, g.y - 2)
    ctx.lineTo(g.x + g.w * 0.34, g.y - 2)
    ctx.closePath()
    ctx.fill()

    ctx.fillStyle = 'rgba(255,255,255,0.96)'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('!', g.x + g.w * 0.5, g.y - 9)
  }

  ctx.restore()
}

const drawLaneWarnings = () => {
  const nearest = nearestWarningsByLane()
  const blink = 0.45 + (Math.sin(world.elapsed * 15) + 1) * 0.28

  for (const lane of [-1, 0, 1]) {
    const warning = nearest[String(lane)]
    if (!warning) continue

    const markerDepth = clamp(warning.depth + 0.08, 0.16, PLAYER_ZONE_END)
    const x = laneXAtDepth(lane, markerDepth)
    const y = groundYAtDepth(markerDepth) - 74
    const nearHit = warning.depth > PLAYER_ZONE_START - 0.06
    const size = isMobile.value ? (nearHit ? 21 : 17) : (nearHit ? 24 : 20)
    const color = nearHit ? '#ef4444' : '#f97316'
    const icon = '!'

    ctx.save()
    ctx.globalAlpha = blink
    ctx.fillStyle = color
    ctx.beginPath()
    ctx.moveTo(x, y - size)
    ctx.lineTo(x + size * 0.9, y + size * 0.75)
    ctx.lineTo(x - size * 0.9, y + size * 0.75)
    ctx.closePath()
    ctx.fill()

    ctx.fillStyle = '#fff'
    ctx.font = 'bold 15px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(icon, x, y + 3)
    ctx.restore()
  }
}

const drawCoins = () => {
  for (const coin of coins) {
    const depth = clamp(coin.depth, 0, 1.2)
    const scale = 0.42 + depth * 1.08
    const x = laneXAtDepth(coin.lane, depth)
    const y = groundYAtDepth(depth) - (coin.height + Math.sin(world.elapsed * 8 + coin.pulse) * 8) * scale
    const r = coin.r * scale

    const glow = ctx.createRadialGradient(x, y, 0, x, y, r * 2)
    glow.addColorStop(0, 'rgba(253, 224, 71, 0.95)')
    glow.addColorStop(0.66, 'rgba(245, 158, 11, 0.7)')
    glow.addColorStop(1, 'rgba(245, 158, 11, 0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(x, y, r * 2, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = '#fde047'
    ctx.beginPath()
    ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fill()

    ctx.strokeStyle = 'rgba(255,255,255,0.9)'
    ctx.lineWidth = Math.max(1, r * 0.2)
    ctx.beginPath()
    ctx.arc(x, y, r * 0.5, 0, Math.PI * 2)
    ctx.stroke()
  }
}

const runnerHitRect = () => {
  const x = laneXAtDepth(runner.laneVisual, PLAYER_DEPTH) - runner.w / 2
  const y = getPlayerFloorY()
  return {
    x: x + runner.w * 0.24,
    y: y + runner.h * 0.18,
    w: runner.w * 0.52,
    h: runner.h * 0.7,
  }
}

const drawRunner = () => {
  const x = laneXAtDepth(runner.laneVisual, PLAYER_DEPTH) - runner.w / 2
  const y = getPlayerFloorY()

  ctx.fillStyle = 'rgba(15, 23, 42, 0.34)'
  ctx.beginPath()
  ctx.ellipse(x + runner.w * 0.5, getGroundY() + 8, runner.w * 0.34, 10, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.save()
  const bob = Math.sin(world.elapsed * 9.5) * 2.4
  ctx.translate(x + runner.w * 0.5, y + runner.h * 0.5 + bob)
  ctx.rotate(runner.tilt)

  const body = ctx.createLinearGradient(-runner.w * 0.45, -runner.h * 0.4, runner.w * 0.45, runner.h * 0.55)
  body.addColorStop(0, '#d4996b')
  body.addColorStop(0.6, '#b17345')
  body.addColorStop(1, '#87522e')
  ctx.fillStyle = body
  ctx.beginPath()
  ctx.ellipse(0, 16, runner.w * 0.38, runner.h * 0.43, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#8f5933'
  roundedPath(-runner.w * 0.46, runner.h * 0.08, runner.w * 0.16, runner.h * 0.25, runner.w * 0.07)
  ctx.fill()
  roundedPath(runner.w * 0.3, runner.h * 0.08, runner.w * 0.16, runner.h * 0.25, runner.w * 0.07)
  ctx.fill()

  const chest = ctx.createRadialGradient(0, runner.h * 0.24, runner.w * 0.03, 0, runner.h * 0.22, runner.w * 0.28)
  chest.addColorStop(0, '#fff8e8')
  chest.addColorStop(1, '#eadfcb')
  ctx.fillStyle = chest
  ctx.beginPath()
  ctx.ellipse(0, runner.h * 0.26, runner.w * 0.24, runner.h * 0.26, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#d4996b'
  ctx.beginPath()
  ctx.ellipse(0, -runner.h * 0.08, runner.w * 0.34, runner.h * 0.3, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#8f5933'
  ctx.beginPath()
  ctx.arc(-runner.w * 0.21, -runner.h * 0.28, runner.w * 0.11, 0, Math.PI * 2)
  ctx.arc(runner.w * 0.21, -runner.h * 0.28, runner.w * 0.11, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#e8bf95'
  ctx.beginPath()
  ctx.arc(-runner.w * 0.21, -runner.h * 0.28, runner.w * 0.05, 0, Math.PI * 2)
  ctx.arc(runner.w * 0.21, -runner.h * 0.28, runner.w * 0.05, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#f6ebd9'
  ctx.beginPath()
  ctx.ellipse(0, runner.h * 0.03, runner.w * 0.16, runner.h * 0.12, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(0, -runner.h * 0.02, runner.w * 0.22, runner.h * 0.16, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#111827'
  ctx.beginPath()
  ctx.arc(-runner.w * 0.1, -runner.h * 0.11, runner.w * 0.03, 0, Math.PI * 2)
  ctx.arc(runner.w * 0.1, -runner.h * 0.11, runner.w * 0.03, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.arc(-runner.w * 0.09, -runner.h * 0.12, runner.w * 0.011, 0, Math.PI * 2)
  ctx.arc(runner.w * 0.11, -runner.h * 0.12, runner.w * 0.011, 0, Math.PI * 2)
  ctx.fill()

  ctx.strokeStyle = '#111827'
  ctx.lineWidth = Math.max(1.2, runner.w * 0.016)
  ctx.beginPath()
  ctx.moveTo(-runner.w * 0.15, -runner.h * 0.16)
  ctx.lineTo(-runner.w * 0.06, -runner.h * 0.14)
  ctx.moveTo(runner.w * 0.15, -runner.h * 0.16)
  ctx.lineTo(runner.w * 0.06, -runner.h * 0.14)
  ctx.stroke()

  ctx.fillStyle = '#1f2937'
  ctx.beginPath()
  ctx.ellipse(0, -runner.h * 0.01, runner.w * 0.046, runner.h * 0.033, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(0, runner.h * 0.036, runner.w * 0.03, runner.h * 0.022, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.strokeStyle = '#3f3f46'
  ctx.lineWidth = Math.max(1.4, runner.w * 0.017)
  ctx.beginPath()
  ctx.arc(0, runner.h * 0.05, runner.w * 0.074, 0.18, Math.PI - 0.18)
  ctx.stroke()

  ctx.fillStyle = 'rgba(244, 114, 182, 0.52)'
  ctx.beginPath()
  ctx.ellipse(-runner.w * 0.17, -runner.h * 0.01, runner.w * 0.052, runner.h * 0.04, 0, 0, Math.PI * 2)
  ctx.ellipse(runner.w * 0.17, -runner.h * 0.01, runner.w * 0.052, runner.h * 0.04, 0, 0, Math.PI * 2)
  ctx.fill()

  const crownColors = ['#f472b6', '#f9a8d4', '#fb7185', '#f472b6', '#f9a8d4', '#fb7185']
  for (let i = 0; i < crownColors.length; i++) {
    const px = (i - 2.5) * runner.w * 0.084
    const py = -runner.h * 0.34 + Math.sin(i * 0.9) * 2.6
    ctx.fillStyle = crownColors[i]
    ctx.beginPath()
    ctx.arc(px, py, runner.w * 0.045, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = '#fef08a'
    ctx.beginPath()
    ctx.arc(px, py, runner.w * 0.018, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.fillStyle = 'rgba(15, 23, 42, 0.16)'
  roundedPath(-runner.w * 0.2, runner.h * 0.38, runner.w * 0.15, runner.h * 0.12, runner.w * 0.06)
  ctx.fill()
  roundedPath(runner.w * 0.05, runner.h * 0.38, runner.w * 0.15, runner.h * 0.12, runner.w * 0.06)
  ctx.fill()

  ctx.restore()
}

const drawParticles = (dt) => {
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]
    p.life -= dt
    if (p.life <= 0) { particles.splice(i, 1); continue }
    p.vy += 680 * dt
    p.x += p.vx * dt
    p.y += p.vy * dt
    ctx.globalAlpha = clamp(p.life * 1.8, 0, 1)
    ctx.fillStyle = p.color
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1
}
const createObstacle = (kind, lane, baseDepth = 0.025) => {
  const meta = OBSTACLE_SET[kind]
  if (!meta) return
  obstacles.push({
    ...meta,
    lane,
    variant: Math.random() < 0.58 ? 'furhat' : 'helmet',
    depth: baseDepth + Math.random() * 0.04,
    speedMul: 0.92 + Math.random() * 0.2,
    checked: false,
    passed: false,
    hitRect: null,
  })
}

const spawnWave = () => {
  const pace = world.pace
  const r = Math.random()

  if (pace > 0.5 && r < 0.2) {
    const safe = randomLane()
    for (const lane of [-1, 0, 1]) {
      if (lane !== safe) createObstacle('train', lane)
    }
  } else {
    const lane = randomLane()
    createObstacle('train', lane)
    if (pace > 0.58 && Math.random() < 0.28) {
      const secondLane = pick([-1, 0, 1].filter((it) => it !== lane))
      createObstacle('train', secondLane, 0.1)
    }
  }

  if (Math.random() < 0.48) {
    const lane = randomLane()
    const count = Math.random() < 0.32 ? 2 : 1
    for (let i = 0; i < count; i++) {
      coins.push({
        lane,
        depth: 0.03 + i * 0.055,
        r: 10 + Math.random() * 2,
        speedMul: 0.94 + Math.random() * 0.14,
        height: 78 + Math.random() * 30,
        pulse: Math.random() * Math.PI * 2,
      })
    }
  }
}

const resetRunner = () => {
  runner.lane = 0
  runner.laneVisual = 0
  runner.tilt = 0
}

const resetWorld = () => {
  world.pace = 0.34
  world.targetPace = 0.34
  world.elapsed = 0
  world.distance = 0
  world.spawnTimer = 0.74
  world.stripeOffset = 0
  world.cloudOffset = 0
  world.hillOffset = 0
  world.treeOffset = 0
  world.shakeTime = 0
  world.shakePower = 0

  score.value = 0
  speedKmh.value = 24
  level.value = 1
  speedRate.value = '1.00'

  bonusScore = 0
  obstacles = []
  coins = []
  particles = []
}

const moveLane = (delta) => {
  if (gameState.value !== 'playing') return
  const prev = runner.lane
  runner.lane = clamp(runner.lane + delta, -1, 1)
  if (runner.lane !== prev) {
    addParticles(laneXAtDepth(runner.laneVisual, PLAYER_DEPTH), getGroundY() - 7, 8, ['#60a5fa', '#bfdbfe', '#f8fafc'])
  }
}

const genRoundId = () => `cuihua-${Date.now()}-${Math.floor(Math.random() * 1e6)}`

const submitScoreReport = async (value, options = {}) => {
  const { final = false, force = false } = options
  if (value <= 0 || authStore.isGuest) return
  if (final && finalScoreReported && !force) return
  if (liveReportInFlight && !final) return

  liveReportInFlight = true
  try {
    await api.post('/api/leaderboard/game-score', { score: value, roundId: currentRoundId || null, finalScore: final })
    if (final) finalScoreReported = true
  } catch (_) {
    // ignore
  } finally {
    liveReportInFlight = false
  }
}

const stopLiveReporter = () => {
  if (liveReporterTimer) {
    clearInterval(liveReporterTimer)
    liveReporterTimer = null
  }
}

const startLiveReporter = () => {
  stopLiveReporter()
  if (authStore.isGuest) return

  liveReporterTimer = window.setInterval(() => {
    if (gameState.value !== 'playing') return
    const now = Number(score.value || 0)
    if (now <= 0 || now - liveReportedScore < 15) return
    liveReportedScore = now
    submitScoreReport(now, { final: false })
  }, 1100)
}

const finishRound = () => {
  if (gameState.value !== 'playing') return
  gameState.value = 'over'
  world.shakeTime = 0.25
  world.shakePower = 14

  addParticles(laneXAtDepth(runner.laneVisual, PLAYER_DEPTH), getPlayerFloorY() + runner.h * 0.5, 30, ['#f87171', '#f43f5e', '#fbbf24'])

  if (score.value > highScore.value) {
    highScore.value = score.value
    localStorage.setItem('cuihua_highscore', String(score.value))
  }
  stopLiveReporter()
  submitScoreReport(score.value, { final: true, force: true })
}

const obstacleHitsRunner = (obs) => {
  const sameLane = Math.abs(runner.lane - obs.lane) < 0.5 || Math.abs(runner.laneVisual - obs.lane) < 0.32
  return sameLane
}

const updateRunner = (dt) => {
  runner.laneVisual += (runner.lane - runner.laneVisual) * Math.min(1, dt * 13)

  const laneTilt = clamp((runner.lane - runner.laneVisual) * 0.34, -0.3, 0.3)
  runner.tilt += (laneTilt - runner.tilt) * Math.min(1, dt * 9)
}
const updateGame = (dt) => {
  world.elapsed += dt
  world.targetPace = 0.34 + Math.min(0.72, world.elapsed * 0.012 + score.value / 3400)
  world.pace += (world.targetPace - world.pace) * Math.min(1, dt * 2)
  world.distance += world.pace * dt * 156

  score.value = Math.floor(world.distance) + bonusScore
  speedKmh.value = Math.round(24 + world.pace * 390)
  level.value = Math.max(1, Math.floor((world.pace - 0.34) * 11) + 1)
  speedRate.value = (world.pace / 0.34).toFixed(2)

  world.spawnTimer -= dt
  if (world.spawnTimer <= 0) {
    spawnWave()
    const minGap = Math.max(0.28, 0.8 - (world.pace - 0.34) * 0.72)
    world.spawnTimer = minGap + Math.random() * 0.24
  }

  for (let i = obstacles.length - 1; i >= 0; i--) {
    const obs = obstacles[i]
    obs.depth += world.pace * obs.speedMul * dt
    const g = obstacleGeometry(obs)
    obs.hitRect = g.hitRect

    if (!obs.checked && obs.depth >= PLAYER_ZONE_START) {
      obs.checked = true
      if (obstacleHitsRunner(obs)) {
        finishRound()
        return
      }
      obs.passed = true
      bonusScore += Math.min(8 + level.value, 22)
      addParticles(g.x + g.w * 0.5, g.y + g.h * 0.45, 8, ['#fde047', '#fbbf24', '#f8fafc'])
      obstacles.splice(i, 1)
      continue
    }

    if (obs.depth > PLAYER_ZONE_END + 0.08) {
      obstacles.splice(i, 1)
      continue
    }
  }

  const rr = runnerHitRect()
  for (let i = coins.length - 1; i >= 0; i--) {
    const coin = coins[i]
    coin.depth += world.pace * coin.speedMul * dt

    const depth = clamp(coin.depth, 0, 1.2)
    const scale = 0.42 + depth * 1.08
    const cx = laneXAtDepth(coin.lane, depth)
    const cy = groundYAtDepth(depth) - (coin.height + Math.sin(world.elapsed * 8 + coin.pulse) * 8) * scale
    const cr = coin.r * scale

    const rx = rr.x + rr.w * 0.5
    const ry = rr.y + rr.h * 0.5
    const dx = rx - cx
    const dy = ry - cy
    const touch = Math.max(rr.w * 0.33, 14) + cr
    if (dx * dx + dy * dy <= touch * touch) {
      bonusScore += 24
      addParticles(cx, cy, 14, ['#fef08a', '#fcd34d', '#fff7ed'])
      coins.splice(i, 1)
      continue
    }

    if (coin.depth > 1.2) coins.splice(i, 1)
  }
}

const drawScene = (dt) => {
  if (!ctx) return
  ctx.clearRect(0, 0, view.width, view.height)

  ctx.save()
  if (world.shakeTime > 0) {
    const intensity = world.shakePower * (world.shakeTime / 0.25)
    ctx.translate((Math.random() - 0.5) * intensity, (Math.random() - 0.5) * intensity * 0.56)
    world.shakeTime = Math.max(0, world.shakeTime - dt)
  }

  drawSky(dt)
  drawRoad(dt)
  drawLaneWarnings()
  drawCoins()
  for (const obs of obstacles) drawObstacle(obs)
  drawRunner()
  drawParticles(dt)
  ctx.restore()
}

const frame = (ts) => {
  if (gameState.value !== 'playing') return
  if (!lastFrameAt) lastFrameAt = ts
  const dt = Math.min(0.033, (ts - lastFrameAt) / 1000 || 0.016)
  lastFrameAt = ts

  updateRunner(dt)
  updateGame(dt)
  drawScene(dt)

  if (gameState.value === 'playing') animationId = requestAnimationFrame(frame)
}

const startGame = () => {
  if (!ctx || activeGame.value !== 'cuihua-runner') return
  if (animationId) cancelAnimationFrame(animationId)
  stopLiveReporter()

  gameState.value = 'playing'
  finalScoreReported = false
  currentRoundId = genRoundId()
  liveReportedScore = 0
  lastFrameAt = 0

  resetWorld()
  resetRunner()
  startLiveReporter()
  animationId = requestAnimationFrame(frame)
}

const onActionTap = () => {
  if (gameState.value !== 'playing') startGame()
}

const onKeyDown = (e) => {
  if (activeGame.value !== 'cuihua-runner') return
  if (['ArrowLeft', 'ArrowRight', 'KeyA', 'KeyD', 'Enter', 'Space'].includes(e.code)) e.preventDefault()

  if (e.code === 'ArrowLeft' || e.code === 'KeyA') moveLane(-1)
  else if (e.code === 'ArrowRight' || e.code === 'KeyD') moveLane(1)
  else if (e.code === 'Enter' || e.code === 'Space') onActionTap()
}

const onPointerDown = (e) => {
  pointerStart = { x: e.clientX, y: e.clientY, t: performance.now() }
}

const onPointerUp = (e) => {
  if (!pointerStart) return
  const dx = e.clientX - pointerStart.x
  const dy = e.clientY - pointerStart.y
  const dt = performance.now() - pointerStart.t
  pointerStart = null

  const swipeThreshold = isMobile.value ? 22 : 30
  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > swipeThreshold) { moveLane(dx > 0 ? 1 : -1); return }
  if (dt < 260 && Math.abs(dx) < 20 && Math.abs(dy) < 20) onActionTap()
}

const resizeCanvas = () => {
  if (!gameCanvas.value || !ctx) return
  const rect = gameCanvas.value.getBoundingClientRect()
  const ratio = Math.min(window.devicePixelRatio || 1, 2)
  isMobile.value = window.innerWidth < 768

  view.width = Math.max(isMobile.value ? 280 : 360, Math.round(rect.width))
  view.height = Math.max(isMobile.value ? 260 : 220, Math.round(rect.height))
  gameCanvas.value.width = Math.round(view.width * ratio)
  gameCanvas.value.height = Math.round(view.height * ratio)
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  const runnerSize = isMobile.value ? Math.round(clamp(view.width * 0.16, 64, 76)) : 92
  runner.w = runnerSize
  runner.h = runnerSize

  resetRunner()
  drawScene(1 / 60)
}

const bindGameEvents = () => {
  window.addEventListener('resize', resizeCanvas)
  window.addEventListener('orientationchange', resizeCanvas)
  window.addEventListener('keydown', onKeyDown)
  gameCanvas.value?.addEventListener('pointerdown', onPointerDown)
  gameCanvas.value?.addEventListener('pointerup', onPointerUp)
}

const unbindGameEvents = () => {
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('orientationchange', resizeCanvas)
  window.removeEventListener('keydown', onKeyDown)
  gameCanvas.value?.removeEventListener('pointerdown', onPointerDown)
  gameCanvas.value?.removeEventListener('pointerup', onPointerUp)
}

const setupGameCanvas = async () => {
  await nextTick()
  if (!gameCanvas.value) return
  ctx = gameCanvas.value.getContext('2d')
  if (!ctx) return
  resizeCanvas()
  bindGameEvents()
}

const teardownGame = () => {
  stopLiveReporter()
  unbindGameEvents()
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  pointerStart = null
  ctx = null
  gameState.value = 'idle'
}

const enterGame = (id) => { activeGame.value = id }
const goBackToHall = () => { activeGame.value = null }

watch(() => activeGame.value, async (value) => {
  if (value === 'cuihua-runner') {
    gameState.value = 'idle'
    resetWorld()
    await setupGameCanvas()
    drawScene(1 / 60)
    return
  }
  teardownGame()
})

onUnmounted(() => {
  teardownGame()
})
</script>
<style scoped>
.game-root {
  background: transparent;
}

.game-stage {
  touch-action: none;
  border: 1px solid rgba(17, 94, 49, 0.2);
  box-shadow: 0 26px 56px rgba(12, 74, 32, 0.26);
}

.forest-panel {
  border-color: var(--border-soft);
  background: var(--surface-panel-soft);
}

.forest-hall {
  border-color: var(--border-soft);
  background: var(--surface-panel-soft);
}

.hud-item {
  border-radius: 12px;
  background: var(--accent-soft);
  border: 1px solid var(--accent-border);
  padding: 8px 12px;
  min-width: 108px;
  display: grid;
  gap: 2px;
}

.hud-item span {
  font-size: 11px;
  color: var(--text-secondary);
}

.hud-item strong {
  font-size: 14px;
  color: var(--text-primary);
}

.game-tip-chip {
  border-radius: 999px;
  border: 1px solid rgba(21, 128, 61, 0.24);
  background: rgba(240, 253, 244, 0.88);
  padding: 1px 7px;
}

.tip-lane { color: #b91c1c; }

.game-card {
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid var(--border-soft);
  background: var(--bg-surface);
  transition: transform 0.3s cubic-bezier(0.2, 0, 0, 1), box-shadow 0.3s cubic-bezier(0.2, 0, 0, 1);
}

.game-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card);
}

.game-card-active { border-color: var(--accent-border); }
.game-card-coming { opacity: 0.92; }

.game-title-gradient {
  background-image: linear-gradient(135deg, var(--ai-from), var(--ai-to));
}

.game-live-chip {
  color: color-mix(in srgb, var(--success) 72%, var(--text-primary));
  background: var(--success-soft);
  border: 1px solid var(--success-border);
}

.game-enter-btn {
  background: linear-gradient(135deg, var(--ai-from), var(--ai-to));
  box-shadow: 0 16px 30px -18px rgba(var(--primary-rgb), 0.56);
}

.cover {
  position: relative;
  height: 176px;
  overflow: hidden;
}

.cover-runner {
  cursor: pointer;
  background:
    radial-gradient(120% 90% at 50% -10%, rgba(242, 255, 247, 0.36), rgba(255, 255, 255, 0) 45%),
    linear-gradient(165deg, #17371f 6%, #255b36 52%, #4f8f62 100%);
}

.cover-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 2px 10px;
  font-size: 11px;
  color: #fff;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--ai-from), var(--ai-to));
}

.game-root :deep([class~='bg-slate-50']),
.game-root :deep([class~='bg-slate-100']),
.game-root :deep([class~='bg-white/75']),
.game-root :deep([class~='bg-white/70']) {
  background: var(--bg-elevated) !important;
}

.game-root :deep([class~='text-slate-900']) {
  color: var(--text-primary) !important;
}

.game-root :deep([class~='text-slate-700']),
.game-root :deep([class~='text-slate-600']),
.game-root :deep([class~='text-slate-500']),
.game-root :deep([class~='text-slate-400']) {
  color: var(--text-secondary) !important;
}

.game-root :deep([class~='bg-emerald-50']) {
  background: var(--success-soft) !important;
}

.game-root :deep([class~='border-emerald-100']),
.game-root :deep([class~='border-slate-200']),
.game-root :deep([class~='border-black/10']),
.game-root :deep([class~='border-black/5']) {
  border-color: var(--border-soft) !important;
}

.cover-track {
  position: absolute;
  left: 50%;
  bottom: -6px;
  width: 74%;
  height: 84%;
  transform: translateX(-50%);
  clip-path: polygon(47% 0, 53% 0, 100% 100%, 0 100%);
  background: linear-gradient(180deg, rgba(92, 71, 43, 0.76), rgba(41, 30, 20, 0.96));
}

.cover-runner-avatar {
  position: absolute;
  left: 50%;
  bottom: 26px;
  width: 48px;
  height: 48px;
  transform: translateX(-50%);
  display: grid;
  place-items: center;
  font-size: 24px;
  border-radius: 16px;
  background: linear-gradient(145deg, #fcd34d, #fb923c);
  box-shadow: 0 10px 18px rgba(180, 83, 9, 0.35);
}

.cover-obstacle {
  position: absolute;
  bottom: 16px;
  width: 24px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(170deg, #f59e0b, #b45309);
  border: 2px solid rgba(17, 24, 39, 0.35);
}

.cover-obstacle-left { left: 32%; }
.cover-obstacle-mid { left: 50%; transform: translateX(-50%); }
.cover-obstacle-right { right: 32%; }

.cover-coming {
  display: grid;
  place-items: center;
  background:
    repeating-linear-gradient(
      135deg,
      rgba(148, 163, 184, 0.16) 0,
      rgba(148, 163, 184, 0.16) 14px,
      rgba(148, 163, 184, 0.24) 14px,
      rgba(148, 163, 184, 0.24) 28px
    ),
    linear-gradient(135deg, #f8fafc, #e2e8f0);
}

.coming-tag {
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.coming-sub {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
}

.mobile-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 2px;
}

.mobile-lane-btn {
  border-radius: 14px;
  border: 1px solid rgba(21, 128, 61, 0.24);
  background: linear-gradient(140deg, rgba(220, 252, 231, 0.92), rgba(187, 247, 208, 0.92));
  color: #14532d;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}

.mobile-lane-btn:disabled {
  opacity: 0.46;
}

@media (max-width: 640px) {
  .hud-grid {
    width: 100%;
  }

  .hud-item {
    min-width: 0;
    padding: 7px 10px;
  }

  .hud-item strong {
    font-size: 13px;
  }

  .game-tip-chip {
    padding: 1px 6px;
  }
}
</style>
