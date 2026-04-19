<template>
  <div class="game-root workspace-page workspace-scroll min-h-full flex items-start justify-center px-2 pt-5 pb-2 sm:px-4 sm:pt-7 sm:pb-3">
    <div class="workspace-stack game-stack w-full space-y-3">
      <div v-if="activeGame === null" class="forest-panel workspace-titlebar rounded-2xl border border-border-soft px-4 py-3 shadow-sm backdrop-blur-md sm:px-5 sm:py-3.5">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div class="flex items-center gap-3">
            <span class="hall-hero-mark">✦</span>
            <div>
              <h2 class="game-title-gradient text-lg font-bold text-transparent bg-clip-text">休闲时刻</h2>
              <!-- <p class="mt-0.5 text-xs text-text-tertiary">光跃者当前为暗域重构版</p> -->
            </div>
          </div>

          <div class="hud-grid grid grid-cols-2 gap-2 md:grid-cols-4">
            <div class="hud-item"><span>游戏名</span><strong>{{ GAME_TITLE }}</strong></div>
            <div class="hud-item"><span>最佳记录</span><strong>{{ highScore }}</strong></div>
            <div class="hud-item"><span>当前玩法</span><strong>{{ GAME_MODE_LABEL }}</strong></div>
            <div class="hud-item"><span>一句话</span><strong>{{ GAME_TAGLINE }}</strong></div>
          </div>
        </div>
      </div>

      <div v-if="activeGame === null" class="forest-hall workspace-shell rounded-3xl border border-border-soft p-5 shadow-sm backdrop-blur-md sm:p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <h3 class="text-xl font-semibold text-primary">光跃者 档案</h3>
          <span class="text-xs text-tertiary">NovaLeap Arcade</span>
        </div>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <article class="game-card workspace-panel game-card-active">
            <div class="cover cover-runner" role="button" tabindex="0" @click="enterGame(GAME_ID)" @keydown.enter="enterGame(GAME_ID)">
              <div class="cover-badge">主打</div>
              <div class="cover-halo"></div>
              <div class="cover-threat-glow"></div>
              <div class="cover-track"></div>
              <div class="cover-runner-shadow"></div>
              <div class="cover-runner-avatar" aria-hidden="true">
                <img class="cover-runner-image" :src="shinchanRunnerSrc" alt="" />
              </div>
              <div class="cover-collectible cover-collectible-left"></div>
              <div class="cover-collectible cover-collectible-right"></div>
              <div class="cover-obstacle cover-obstacle-golem cover-obstacle-left">
                <img class="cover-obstacle-image" :src="monsterGolemSrc" alt="" />
              </div>
              <div class="cover-obstacle cover-obstacle-relic cover-obstacle-mid">
                <img class="cover-obstacle-image" :src="monsterRelicSrc" alt="" />
              </div>
              <div class="cover-obstacle cover-obstacle-blush cover-obstacle-right">
                <img class="cover-obstacle-image" :src="monsterBlushSrc" alt="" />
              </div>
            </div>

            <div class="p-4">
              <div class="flex items-center justify-between gap-2">
                <h4 class="text-base font-semibold text-primary">{{ GAME_TITLE }}</h4>
                <span class="game-live-chip rounded-full px-2 py-0.5 text-xs">已上线</span>
              </div>
              <p class="mt-2 text-sm text-secondary">{{ GAME_DESCRIPTION }}</p>
              <div class="mt-3 flex justify-end">
                <button class="workspace-btn workspace-btn-primary game-enter-btn rounded-lg px-4 py-2 text-sm font-medium text-white shadow transition hover:opacity-90" @click="enterGame(GAME_ID)">开始跃迁</button>
              </div>
            </div>
          </article>

          <article class="game-card workspace-panel game-card-coming">
            <div class="cover cover-coming">
              <div class="coming-tag">敬请期待</div>
            </div>
            <div class="p-4">
              <div class="flex items-center justify-between gap-2">
                <h4 class="text-base font-semibold text-primary">下一款游戏</h4>
                <span class="theme-chip-muted rounded-full px-2 py-0.5 text-[10px]">筹备中</span>
              </div>
              <button class="workspace-btn workspace-btn-muted mt-3 w-full cursor-not-allowed rounded-lg px-4 py-2 text-sm" disabled>敬请期待</button>
            </div>
          </article>
        </div>
      </div>

      <div v-else class="game-play-layout">
        <div class="game-stage-shell">
          <div class="game-stage game-stage-frame relative w-full overflow-hidden rounded-3xl ring-1 ring-black/10">
            <div class="stage-top-scrim"></div>
            <div class="stage-topbar">
              <div class="stage-topbar-left">
                <button class="stage-back-btn" type="button" @click="goBackToHall">
                  <span>←</span>
                  <span v-if="!isMobile">返回档案</span>
                </button>

                <div v-if="!isMobile || gameState === 'playing'" class="stage-brand">
                  <span class="stage-brand-icon">✦</span>
                  <div class="stage-brand-copy">
                    <strong>{{ GAME_TITLE }}</strong>
                    <span>{{ GAME_DESCRIPTION }}</span>
                  </div>
                </div>
              </div>

              <div class="stage-topbar-right">
                <div class="stage-meta">
                  <button class="stage-pause-btn" type="button" :disabled="gameState === 'idle' || gameState === 'over'" @click="togglePause">
                    {{ gameState === 'paused' ? '继续' : '暂停' }}
                  </button>
                </div>

                <div v-if="!isMobile || gameState === 'playing'" class="stage-hud">
                  <div class="stage-hud-item"><span>分数</span><strong>{{ score }}</strong></div>
                  <div class="stage-hud-item"><span>最高分</span><strong>{{ highScore }}</strong></div>
                  <div class="stage-hud-item"><span>速度</span><strong>{{ speedKmh }} km/h</strong></div>
                </div>
              </div>
            </div>

            <canvas ref="gameCanvas" class="block h-full w-full touch-none"></canvas>
            <div class="hit-overlay" :class="{ 'hit-active': hitOverlayActive }"></div>

            <div v-if="gameState !== 'playing'" class="stage-overlay absolute inset-0 z-[1] flex items-center justify-center px-4">
              <div class="stage-overlay-card max-w-xl text-center">
                <div class="stage-overlay-mark">{{ overlayMark }}</div>
                <p class="stage-overlay-kicker">{{ gameState === 'over' ? '本局回顾' : '裂隙冲线' }}</p>
                <p class="stage-overlay-title">{{ overlayTitle }}</p>
                <p class="stage-overlay-desc">{{ overlayDescription }}</p>

                <div class="stage-overlay-stats">
                  <div class="stage-overlay-stat"><span>最高分</span><strong>{{ highScore }}</strong></div>
                  <div class="stage-overlay-stat"><span>玩法</span><strong>{{ GAME_MODE_LABEL }}</strong></div>
                  <div class="stage-overlay-stat"><span>目标</span><strong>{{ GAME_GOAL_LABEL }}</strong></div>
                </div>

                <div class="stage-overlay-actions">
                  <button class="workspace-btn workspace-btn-primary game-enter-btn rounded-2xl px-7 py-3 text-white" @click="handlePrimaryAction">{{ primaryActionLabel }}</button>
                  <button v-if="gameState === 'paused'" class="stage-ghost-btn" type="button" @click="goBackToHall">返回档案页</button>
                </div>

                <p class="stage-overlay-hint">{{ interactionHint }}</p>
              </div>
            </div>

            <div class="stage-bottom-controls">
              <div class="stage-bottom-controls-shell">
                <button class="stage-action-btn" :disabled="gameState !== 'playing'" @click="moveLane(-1)">左移</button>
                <button class="stage-action-btn stage-action-btn-primary" :disabled="gameState !== 'playing'" @click="moveLane(1)">右移</button>
              </div>
              <p class="stage-bottom-controls-hint">{{ gameState === 'playing' ? GAME_TAGLINE : '开始后可使用左右控件或键盘切换轨道' }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'
import shinchanRunnerSrc from '@/assets/game/shinchan-runner.svg'
import monsterBlushSrc from '@/assets/game/monster-blush.svg'
import monsterGolemSrc from '@/assets/game/monster-golem.svg'
import monsterRelicSrc from '@/assets/game/monster-relic.svg'

const GAME_ID = 'light-leaper'
const HIGHSCORE_KEY = 'light_leaper_highscore'
const BASE_PACE = 0.34
const PLAYER_DEPTH = 0.9
const PLAYER_ZONE_START = PLAYER_DEPTH - 0.05
const PLAYER_ZONE_END = PLAYER_DEPTH + 0.04
const PLAYER_COLLISION_END = PLAYER_ZONE_END
const RUNNER_SPRITE_ASPECT = 1
const RUNNER_FLOOR_OFFSET = 6
const GAME_TITLE = '光跃者'
const GAME_MODE_LABEL = '三轨裂隙冲线'
const GAME_TAGLINE = '左右切道，收集辉光核心，避开异界怪物'
const GAME_DESCRIPTION = '穿过夜幕裂隙，吞没辉光核心，在怪物压境前把速度和分数一起拉满。'
const GAME_GOAL_LABEL = '收集辉光核心，突破怪群封锁'
const gameCanvas = ref(null)
const gameState = ref('idle')
const activeGame = ref(null)

const authStore = useAuthStore()
const resolvedHighScoreKey = computed(() => {
  const username = authStore.user?.username
  return username ? `${HIGHSCORE_KEY}:${username}` : HIGHSCORE_KEY
})
const readLocalHighScore = (key = resolvedHighScoreKey.value) => Number(localStorage.getItem(key) || 0)

const score = ref(0)
const highScore = ref(readLocalHighScore())
const speedKmh = ref(26)
const level = ref(1)
const speedRate = ref('1.00')
const isMobile = ref(false)
const runnerSpriteReady = ref(false)
const hitOverlayActive = ref(false)

const createSprite = (src) => {
  if (typeof Image === 'undefined') return null
  const sprite = new Image()
  sprite.decoding = 'async'
  sprite.src = src
  return sprite
}
const runnerSprite = typeof Image === 'undefined' ? null : new Image()
const obstacleSprites = {
  golem: createSprite(monsterGolemSrc),
  relic: createSprite(monsterRelicSrc),
  blush: createSprite(monsterBlushSrc),
}
const handleRunnerSpriteLoad = () => {
  runnerSpriteReady.value = true
}
if (runnerSprite) {
  runnerSprite.decoding = 'async'
  runnerSprite.src = shinchanRunnerSrc
  if (runnerSprite.complete) handleRunnerSpriteLoad()
  else runnerSprite.addEventListener('load', handleRunnerSpriteLoad, { once: true })
}

const stateChipLabel = computed(() => gameState.value === 'playing' ? '跃迁中' : gameState.value === 'paused' ? '已暂停' : gameState.value === 'over' ? '已结算' : '待开始')
const overlayMark = computed(() => gameState.value === 'paused' ? '◌' : gameState.value === 'over' ? '✦' : '◎')
const overlayTitle = computed(() => gameState.value === 'paused' ? '节奏冻结，随时继续' : gameState.value === 'over' ? '本轮结束，再冲一局光跃者' : GAME_TITLE)
const overlayDescription = computed(() => gameState.value === 'paused' ? '当前进度已锁定。继续切换轨道，吞没辉光核心，绕开裂隙里苏醒的怪物。' : gameState.value === 'over' ? '再开一局，把连收节奏拉顺，在高压波次里继续抬高速度和分数。' : GAME_DESCRIPTION)
const primaryActionLabel = computed(() => gameState.value === 'paused' ? '继续冲线' : gameState.value === 'over' ? '再来一局' : '开始跃迁')
const interactionHint = computed(() => isMobile.value ? '操作：左右滑动切换轨道，也可使用底部按钮完成闪避与吃币。' : '操作：A / D 或 ← / → 切换轨道，P 或 Esc 暂停')

const view = { width: 0, height: 0 }
const world = { pace: BASE_PACE, targetPace: BASE_PACE, elapsed: 0, distance: 0, spawnTimer: 0.72, stripeOffset: 0, cloudOffset: 0, hillOffset: 0, treeOffset: 0, shakeTime: 0, shakePower: 0, guideTime: 0, collectFlash: 0 }
const runner = { lane: 0, laneVisual: 0, w: 114, h: 114, tilt: 0, hitFlash: 0, collectPulse: 0 }

const OBSTACLE_SET = {
  golem: {
    type: 'golem',
    w: 118,
    h: 118,
    lift: 4,
    glow: 'rgba(241, 202, 136, 0.28)',
    rim: 'rgba(255, 177, 103, 0.82)',
    hitScale: { x: 0.2, y: 0.16, w: 0.58, h: 0.68 },
  },
  relic: {
    type: 'relic',
    w: 104,
    h: 162,
    lift: 4,
    glow: 'rgba(143, 233, 198, 0.26)',
    rim: 'rgba(136, 234, 208, 0.82)',
    hitScale: { x: 0.18, y: 0.08, w: 0.64, h: 0.8 },
  },
  blush: {
    type: 'blush',
    w: 134,
    h: 132,
    lift: 2,
    glow: 'rgba(255, 154, 174, 0.26)',
    rim: 'rgba(255, 139, 164, 0.84)',
    hitScale: { x: 0.2, y: 0.16, w: 0.58, h: 0.72 },
  },
}

let ctx = null
let animationId = null
let lastFrameAt = 0
let obstacles = []
let collectibles = []
let particles = []
let floatingTexts = []
let pointerStart = null
let bonusScore = 0
let collectChain = 0
let collectChainTimer = 0
let finalScoreReported = false
let currentRoundId = ''
let liveReporterTimer = null
let liveReportedScore = 0
let liveReportInFlight = false
let collisionGuideShown = false
let hitOverlayTimer = null

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const lerp = (from, to, t) => from + (to - from) * t
const pick = (list) => list[Math.floor(Math.random() * list.length)]
const chance = (value) => Math.random() < value
const randomLane = () => pick([-1, 0, 1])
const rectsOverlap = (a, b, padX = 0, padY = 0) => a.x + padX < b.x + b.w && a.x + a.w - padX > b.x && a.y + padY < b.y + b.h && a.y + a.h - padY > b.y

const getGroundY = () => view.height * 0.84
const getRoadMetrics = () => ({ horizon: view.height * (isMobile.value ? 0.25 : 0.29), baseY: getGroundY() + (isMobile.value ? 18 : 22), centerX: view.width * 0.5, topWidth: view.width * (isMobile.value ? 0.31 : 0.24), bottomWidth: view.width * (isMobile.value ? 1.14 : 1.04) })
const getPlayerFloorY = () => getGroundY() - runner.h + RUNNER_FLOOR_OFFSET
const roadWidthAtDepth = (depth) => lerp(getRoadMetrics().topWidth, getRoadMetrics().bottomWidth, clamp(depth, 0, 1))
const groundYAtDepth = (depth) => lerp(getRoadMetrics().horizon + 4, getGroundY(), clamp(depth, 0, 1))
const laneXAtDepth = (lane, depth) => getRoadMetrics().centerX + lane * (roadWidthAtDepth(depth) / 3.2)

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

const drawStar = (x, y, outerRadius, innerRadius) => {
  let rot = Math.PI / 2 * 3
  const step = Math.PI / 5
  ctx.beginPath()
  ctx.moveTo(x, y - outerRadius)
  for (let i = 0; i < 5; i++) {
    ctx.lineTo(x + Math.cos(rot) * outerRadius, y + Math.sin(rot) * outerRadius)
    rot += step
    ctx.lineTo(x + Math.cos(rot) * innerRadius, y + Math.sin(rot) * innerRadius)
    rot += step
  }
  ctx.closePath()
}

const drawDiamond = (x, y, w, h) => {
  ctx.beginPath()
  ctx.moveTo(x + w * 0.5, y)
  ctx.lineTo(x + w, y + h * 0.5)
  ctx.lineTo(x + w * 0.5, y + h)
  ctx.lineTo(x, y + h * 0.5)
  ctx.closePath()
}

const drawCloudShape = (x, y, w, h) => {
  ctx.beginPath()
  ctx.arc(x + w * 0.26, y + h * 0.6, h * 0.22, 0, Math.PI * 2)
  ctx.arc(x + w * 0.44, y + h * 0.42, h * 0.28, 0, Math.PI * 2)
  ctx.arc(x + w * 0.62, y + h * 0.36, h * 0.24, 0, Math.PI * 2)
  ctx.arc(x + w * 0.78, y + h * 0.56, h * 0.18, 0, Math.PI * 2)
  ctx.closePath()
}

const obstacleGeometry = (obs) => {
  const depth = clamp(obs.depth, 0, 1.25)
  const scale = (isMobile.value ? 0.48 : 0.54) + depth * (isMobile.value ? 0.78 : 0.92)
  const w = obs.w * scale
  const h = obs.h * scale
  const lift = (obs.lift || 0) * scale
  const x = laneXAtDepth(obs.lane, depth) - w / 2
  const y = groundYAtDepth(depth) - h - lift
  const hs = obs.hitScale
  return { x, y, w, h, depth, hitRect: { x: x + w * hs.x, y: y + h * hs.y, w: w * hs.w, h: h * hs.h } }
}

const collectibleGeometry = (item) => {
  const depth = clamp(item.depth, 0, 1.2)
  const scale = 0.4 + depth * 1.02
  return { depth, scale, x: laneXAtDepth(item.lane, depth), y: groundYAtDepth(depth) - (item.height + Math.sin(world.elapsed * 7.5 + item.phase) * 7) * scale, size: item.size * scale }
}

const runnerPose = (lane = runner.laneVisual) => {
  const visualW = runner.w
  const visualH = runner.h
  return {
    x: laneXAtDepth(lane, PLAYER_DEPTH) - visualW / 2,
    y: getPlayerFloorY(),
    visualW,
    visualH,
  }
}

const runnerThreatLevel = () => {
  const warning = nearestWarningsByLane()[String(runner.lane)]
  return warning ? clamp((warning.depth - (PLAYER_ZONE_START - 0.16)) / 0.24, 0, 1) : 0
}

const runnerDangerRect = () => {
  // Keep obstacle hits aligned with the runner's visible lane so a queued lane switch
  // does not make the player "teleport" into a monster for collision purposes.
  const pose = runnerPose(runner.laneVisual)
  return { x: pose.x + pose.visualW * 0.23, y: pose.y + pose.visualH * 0.17, w: pose.visualW * 0.52, h: pose.visualH * 0.7 }
}

const addParticles = (x, y, count, palette, spread = 240) => {
  for (let i = 0; i < count; i++) {
    particles.push({ x, y, vx: (Math.random() - 0.5) * spread, vy: -Math.random() * 180 - 30, life: 0.34 + Math.random() * 0.32, size: 1.6 + Math.random() * 3.2, gravity: 620, color: pick(palette) })
  }
}

const addFloatingText = (x, y, text, color, options = {}) => {
  floatingTexts.push({
    x,
    y,
    text,
    color,
    stroke: options.stroke || 'rgba(7, 14, 24, 0.48)',
    size: options.size || 18,
    weight: options.weight || 800,
    life: options.life || 0.62,
    maxLife: options.life || 0.62,
    vy: options.vy || -54,
    gravity: options.gravity || 26,
  })
}

const triggerHitOverlay = () => {
  hitOverlayActive.value = true
  if (hitOverlayTimer) clearTimeout(hitOverlayTimer)
  hitOverlayTimer = window.setTimeout(() => {
    hitOverlayActive.value = false
    hitOverlayTimer = null
  }, 220)
}

const syncHighScore = async () => {
  const localBest = readLocalHighScore()
  highScore.value = localBest

  if (!authStore.isLoggedIn || authStore.isGuest || !authStore.user?.username) return

  try {
    const res = await api.get('/api/leaderboard')
    const serverBest = Number(res?.data?.currentUser?.gameBestScore) || 0
    const best = Math.max(localBest, serverBest)
    highScore.value = best
    localStorage.setItem(resolvedHighScoreKey.value, String(best))
  } catch (_) {
    // ignore and keep the scoped local cache as a fallback
  }
}

const nearestWarningsByLane = () => {
  const result = { '-1': null, '0': null, '1': null }
  for (const obs of obstacles) {
    if (obs.depth < 0.03 || obs.depth > PLAYER_ZONE_END + 0.08) continue
    const key = String(obs.lane)
    if (!result[key] || obs.depth > result[key].depth) result[key] = obs
  }
  return result
}

const drawTree = (x, baseY, scale, tone, trunkTone) => {
  ctx.fillStyle = trunkTone
  roundedPath(x - 5 * scale, baseY - 32 * scale, 10 * scale, 32 * scale, 3)
  ctx.fill()
  ctx.fillStyle = tone
  ctx.beginPath()
  ctx.arc(x, baseY - 46 * scale, 22 * scale, 0, Math.PI * 2)
  ctx.arc(x - 12 * scale, baseY - 36 * scale, 16 * scale, 0, Math.PI * 2)
  ctx.arc(x + 12 * scale, baseY - 36 * scale, 16 * scale, 0, Math.PI * 2)
  ctx.fill()
}

const drawSky = (dt) => {
  const { horizon } = getRoadMetrics()
  const sky = ctx.createLinearGradient(0, 0, 0, view.height)
  sky.addColorStop(0, '#040913')
  sky.addColorStop(0.22, '#0d1830')
  sky.addColorStop(0.56, '#261f37')
  sky.addColorStop(1, '#3c2733')
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, view.width, view.height)

  const moonGlow = ctx.createRadialGradient(view.width * 0.82, view.height * 0.14, 0, view.width * 0.82, view.height * 0.14, view.width * 0.22)
  moonGlow.addColorStop(0, 'rgba(169, 235, 255, 0.2)')
  moonGlow.addColorStop(0.4, 'rgba(169, 235, 255, 0.08)')
  moonGlow.addColorStop(1, 'rgba(169, 235, 255, 0)')
  ctx.fillStyle = moonGlow
  ctx.fillRect(0, 0, view.width, view.height)

  const heroGlow = ctx.createRadialGradient(view.width * 0.48, view.height * 0.16, 0, view.width * 0.48, view.height * 0.16, view.width * 0.32)
  heroGlow.addColorStop(0, 'rgba(114, 233, 255, 0.2)')
  heroGlow.addColorStop(0.45, 'rgba(255, 199, 110, 0.14)')
  heroGlow.addColorStop(1, 'rgba(255, 199, 110, 0)')
  ctx.fillStyle = heroGlow
  ctx.fillRect(0, 0, view.width, view.height)

  const threatGlow = ctx.createRadialGradient(view.width * 0.28, view.height * 0.18, 0, view.width * 0.28, view.height * 0.18, view.width * 0.3)
  threatGlow.addColorStop(0, 'rgba(255, 111, 149, 0.22)')
  threatGlow.addColorStop(0.52, 'rgba(134, 44, 92, 0.12)')
  threatGlow.addColorStop(1, 'rgba(134, 44, 92, 0)')
  ctx.fillStyle = threatGlow
  ctx.fillRect(0, 0, view.width, view.height)

  const topWash = ctx.createLinearGradient(0, 0, 0, view.height * 0.28)
  topWash.addColorStop(0, 'rgba(11, 14, 24, 0.34)')
  topWash.addColorStop(1, 'rgba(11, 14, 24, 0)')
  ctx.fillStyle = topWash
  ctx.fillRect(0, 0, view.width, view.height * 0.28)

  world.cloudOffset = (world.cloudOffset + world.pace * dt * view.width * 0.12) % (view.width + 320)
  for (let i = 0; i < 6; i++) {
    const x = ((i * 250) - world.cloudOffset + view.width + 320) % (view.width + 320) - 160
    const y = 52 + (i % 3) * 34
    ctx.fillStyle = 'rgba(38, 29, 44, 0.46)'
    ctx.beginPath()
    ctx.ellipse(x, y, 78, 22, 0, 0, Math.PI * 2)
    ctx.fill()
  }

  world.hillOffset = (world.hillOffset + world.pace * dt * view.width * 0.16) % (view.width + 520)
  ctx.fillStyle = 'rgba(52, 86, 87, 0.78)'
  for (let i = 0; i < 5; i++) {
    const x = ((i * 310) - world.hillOffset + view.width + 520) % (view.width + 520) - 260
    ctx.beginPath()
    ctx.moveTo(x, horizon + 164)
    ctx.quadraticCurveTo(x + 132, horizon - 6, x + 264, horizon + 164)
    ctx.closePath()
    ctx.fill()
  }

  world.treeOffset = (world.treeOffset + world.pace * dt * view.width * 0.28) % (view.width + 220)
  for (let i = 0; i < 12; i++) {
    const x = ((i * 116) - world.treeOffset + view.width + 220) % (view.width + 220) - 110
    drawTree(x, horizon + 186 + (i % 3) * 8, 0.5 + (i % 4) * 0.12, 'rgba(39, 86, 82, 0.96)', '#433127')
  }

  for (let i = 0; i < 18; i++) {
    ctx.fillStyle = `rgba(118, 234, 255, ${0.05 + (Math.sin(world.elapsed * 3 + i) + 1) * 0.04})`
    ctx.beginPath()
    ctx.arc((i / 18) * view.width + Math.sin(world.elapsed + i) * 8, 34 + (i % 4) * 18, 1.5 + (i % 3) * 0.3, 0, Math.PI * 2)
    ctx.fill()
  }
}

const drawRoad = (dt) => {
  const { horizon, baseY, centerX, topWidth, bottomWidth } = getRoadMetrics()
  const road = ctx.createLinearGradient(0, horizon, 0, baseY)
  road.addColorStop(0, '#334669')
  road.addColorStop(0.42, '#5e4d62')
  road.addColorStop(1, '#9f684a')
  ctx.fillStyle = road
  ctx.beginPath()
  ctx.moveTo(centerX - topWidth / 2, horizon)
  ctx.lineTo(centerX + topWidth / 2, horizon)
  ctx.lineTo(centerX + bottomWidth / 2, baseY)
  ctx.lineTo(centerX - bottomWidth / 2, baseY)
  ctx.closePath()
  ctx.fill()

  ctx.strokeStyle = 'rgba(235, 227, 255, 0.62)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(centerX - topWidth / 2, horizon)
  ctx.lineTo(centerX - bottomWidth / 2, baseY)
  ctx.moveTo(centerX + topWidth / 2, horizon)
  ctx.lineTo(centerX + bottomWidth / 2, baseY)
  ctx.stroke()

  ctx.strokeStyle = 'rgba(248, 232, 214, 0.28)'
  ctx.lineWidth = 1.4
  ctx.beginPath()
  for (const laneBoundary of [-0.5, 0.5]) {
    ctx.moveTo(centerX + laneBoundary * (topWidth / 3), horizon)
    ctx.lineTo(centerX + laneBoundary * (bottomWidth / 3), baseY)
  }
  ctx.stroke()

  const warnings = nearestWarningsByLane()
  for (const lane of [-1, 0, 1]) {
    const warning = warnings[String(lane)]
    if (!warning) continue
    const nearDanger = warning.depth > PLAYER_ZONE_START - 0.06
    ctx.fillStyle = nearDanger ? 'rgba(255, 102, 144, 0.18)' : 'rgba(130, 229, 255, 0.07)'
    ctx.beginPath()
    ctx.moveTo(laneXAtDepth(lane - 0.45, 0), horizon)
    ctx.lineTo(laneXAtDepth(lane + 0.45, 0), horizon)
    ctx.lineTo(laneXAtDepth(lane + 0.5, 1), baseY)
    ctx.lineTo(laneXAtDepth(lane - 0.5, 1), baseY)
    ctx.closePath()
    ctx.fill()
  }

  world.stripeOffset = (world.stripeOffset + world.pace * dt * 2.5) % 1
  for (let i = 0; i < 26; i++) {
    const depth = ((i / 26) + world.stripeOffset) % 1
    const y = lerp(horizon + 6, baseY - 18, depth)
    const gap = roadWidthAtDepth(depth) / 3.2
    const markW = lerp(4, 20, depth)
    const markH = lerp(2, 11, depth)
    ctx.fillStyle = 'rgba(255, 241, 222, 0.88)'
    ctx.fillRect(centerX - gap - markW / 2, y, markW, markH)
    ctx.fillRect(centerX - markW / 2, y, markW, markH)
    ctx.fillRect(centerX + gap - markW / 2, y, markW, markH)
  }

  for (const lane of [-1, 0, 1]) {
    const warning = warnings[String(lane)]
    if (!warning) continue
    const dangerLevel = clamp((warning.depth - (PLAYER_ZONE_START - 0.12)) / 0.18, 0, 1)
    const left = laneXAtDepth(lane - 0.48, PLAYER_ZONE_END)
    const right = laneXAtDepth(lane + 0.48, PLAYER_ZONE_END)
    const y = groundYAtDepth(PLAYER_ZONE_END) + 4
    const bandWidth = right - left
    const glow = ctx.createLinearGradient(left, y, right, y)
    glow.addColorStop(0, `rgba(120, 233, 255, ${0.08 + dangerLevel * 0.1})`)
    glow.addColorStop(0.5, `rgba(255, 132, 116, ${0.18 + dangerLevel * 0.26})`)
    glow.addColorStop(1, `rgba(120, 233, 255, ${0.08 + dangerLevel * 0.1})`)
    ctx.fillStyle = glow
    roundedPath(left, y, bandWidth, 10 + dangerLevel * 6, 10)
    ctx.fill()

    if (dangerLevel > 0.18) {
      ctx.strokeStyle = `rgba(228, 250, 255, ${0.2 + dangerLevel * 0.26})`
      ctx.lineWidth = 1.2 + dangerLevel * 1.4
      ctx.beginPath()
      ctx.moveTo(left + 6, y - 3)
      ctx.lineTo(right - 6, y - 3)
      ctx.stroke()
    }
  }

  const collisionLeft = laneXAtDepth(-1.45, PLAYER_ZONE_START + 0.005)
  const collisionRight = laneXAtDepth(1.45, PLAYER_ZONE_START + 0.005)
  const collisionY = groundYAtDepth(PLAYER_ZONE_START + 0.005) + 2
  const guideBoost = clamp(world.guideTime / 2.8, 0, 1)
  const guidePulse = 0.58 + (Math.sin(world.elapsed * 8) + 1) * 0.2
  ctx.strokeStyle = `rgba(255, 223, 204, ${0.44 + guideBoost * 0.22})`
  ctx.lineWidth = 1.8 + guideBoost * 1.6
  ctx.beginPath()
  ctx.moveTo(collisionLeft, collisionY)
  ctx.lineTo(collisionRight, collisionY)
  ctx.stroke()
  if (guideBoost > 0) {
    ctx.strokeStyle = `rgba(255, 72, 84, ${0.28 + guideBoost * 0.34 * guidePulse})`
    ctx.lineWidth = 6 + guideBoost * 8
    ctx.beginPath()
    ctx.moveTo(collisionLeft + 18, collisionY)
    ctx.lineTo(collisionRight - 18, collisionY)
    ctx.stroke()
    ctx.fillStyle = `rgba(236, 221, 230, ${0.72 + guideBoost * 0.1})`
    ctx.font = `700 ${isMobile.value ? 11 : 12}px Manrope`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'bottom'
    ctx.fillText('同轨接触怪物就会受击', view.width * 0.5, collisionY - 10)
  }

  const groundTop = getGroundY() - 4
  const glassDeck = ctx.createLinearGradient(0, groundTop, 0, view.height)
  glassDeck.addColorStop(0, 'rgba(71, 109, 124, 0.34)')
  glassDeck.addColorStop(0.52, 'rgba(35, 66, 84, 0.28)')
  glassDeck.addColorStop(1, 'rgba(13, 23, 34, 0.18)')
  ctx.fillStyle = glassDeck
  ctx.fillRect(0, groundTop, view.width, view.height - groundTop)

  const deckSheen = ctx.createLinearGradient(0, groundTop, 0, view.height)
  deckSheen.addColorStop(0, 'rgba(255, 255, 255, 0.14)')
  deckSheen.addColorStop(0.3, 'rgba(255, 255, 255, 0.04)')
  deckSheen.addColorStop(1, 'rgba(255, 255, 255, 0)')
  ctx.fillStyle = deckSheen
  ctx.fillRect(0, groundTop, view.width, view.height - groundTop)

  ctx.strokeStyle = 'rgba(188, 233, 244, 0.28)'
  ctx.lineWidth = 1.4
  ctx.beginPath()
  ctx.moveTo(0, groundTop + 1)
  ctx.lineTo(view.width, groundTop + 1)
  ctx.stroke()

  const sideGlowLeft = ctx.createRadialGradient(view.width * 0.14, getGroundY() + 46, 0, view.width * 0.14, getGroundY() + 46, view.width * 0.26)
  sideGlowLeft.addColorStop(0, 'rgba(94, 188, 199, 0.18)')
  sideGlowLeft.addColorStop(0.54, 'rgba(94, 188, 199, 0.06)')
  sideGlowLeft.addColorStop(1, 'rgba(94, 188, 199, 0)')
  ctx.fillStyle = sideGlowLeft
  ctx.fillRect(0, getGroundY(), view.width * 0.36, view.height - getGroundY())

  const sideGlowRight = ctx.createRadialGradient(view.width * 0.86, getGroundY() + 42, 0, view.width * 0.86, getGroundY() + 42, view.width * 0.22)
  sideGlowRight.addColorStop(0, 'rgba(255, 154, 109, 0.16)')
  sideGlowRight.addColorStop(0.5, 'rgba(255, 154, 109, 0.06)')
  sideGlowRight.addColorStop(1, 'rgba(255, 154, 109, 0)')
  ctx.fillStyle = sideGlowRight
  ctx.fillRect(view.width * 0.64, getGroundY(), view.width * 0.36, view.height - getGroundY())

  const drawGroundCluster = (baseX, baseY, tintA, tintB) => {
    ctx.fillStyle = tintA
    ctx.beginPath()
    ctx.ellipse(baseX, baseY, 34, 12, -0.18, 0, Math.PI * 2)
    ctx.ellipse(baseX + 24, baseY - 6, 22, 9, 0.14, 0, Math.PI * 2)
    ctx.ellipse(baseX - 26, baseY - 5, 18, 8, 0.1, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = tintB
    roundedPath(baseX - 5, baseY - 34, 10, 32, 4)
    ctx.fill()
    ctx.beginPath()
    ctx.moveTo(baseX - 14, baseY - 24)
    ctx.quadraticCurveTo(baseX - 2, baseY - 52, baseX + 8, baseY - 18)
    ctx.quadraticCurveTo(baseX - 6, baseY - 14, baseX - 14, baseY - 24)
    ctx.fill()
  }

  drawGroundCluster(view.width * 0.1, getGroundY() + 56, 'rgba(67, 129, 137, 0.28)', 'rgba(32, 82, 87, 0.42)')
  drawGroundCluster(view.width * 0.9, getGroundY() + 58, 'rgba(167, 112, 72, 0.16)', 'rgba(92, 51, 39, 0.32)')
}

const drawLaneWarnings = () => {
  const warnings = nearestWarningsByLane()
  const blink = 0.46 + (Math.sin(world.elapsed * 15) + 1) * 0.26
  for (const lane of [-1, 0, 1]) {
    const warning = warnings[String(lane)]
    if (!warning) continue
    const depth = clamp(warning.depth + 0.08, 0.16, PLAYER_ZONE_END)
    const x = laneXAtDepth(lane, depth)
    const y = groundYAtDepth(depth) - 94
    const size = warning.depth > PLAYER_ZONE_START - 0.06 ? 22 : 18
    ctx.save()
    ctx.globalAlpha = blink
    ctx.fillStyle = warning.depth > PLAYER_ZONE_START - 0.06 ? '#ff7da0' : '#8feeff'
    ctx.beginPath()
    ctx.moveTo(x, y - size)
    ctx.lineTo(x + size * 0.9, y + size * 0.75)
    ctx.lineTo(x - size * 0.9, y + size * 0.75)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 14px Manrope'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('!', x, y + 3)
    ctx.restore()
  }
}

const drawObstacle = (obs) => {
  const g = obstacleGeometry(obs)
  obs.hitRect = g.hitRect
  const dangerLevel = clamp((obs.depth - (PLAYER_ZONE_START - 0.12)) / 0.18, 0, 1)
  const pulse = 0.56 + (Math.sin(world.elapsed * 14 + obs.depth * 10) + 1) * 0.2
  const bob = Math.sin(world.elapsed * (2.4 + (obs.lane + 2) * 0.22) + obs.depth * 10) * g.h * 0.012
  const centerX = g.x + g.w * 0.5
  const centerY = g.y + g.h * 0.52 + bob
  const sprite = obstacleSprites[obs.type]

  ctx.save()
  ctx.globalAlpha = 0.54 + g.depth * 0.44
  ctx.fillStyle = `rgba(9, 13, 22, ${0.18 + dangerLevel * 0.08})`
  ctx.beginPath()
  ctx.ellipse(centerX, g.y + g.h * 1.04, g.w * 0.34, g.h * 0.12, 0, 0, Math.PI * 2)
  ctx.fill()

  if (dangerLevel > 0.12) {
    const aura = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, g.w * 0.82)
    aura.addColorStop(0, obs.glow)
    aura.addColorStop(0.48, 'rgba(255,255,255,0.08)')
    aura.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = aura
    ctx.beginPath()
    ctx.arc(centerX, centerY, g.w * 0.68, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.shadowColor = `rgba(8, 12, 20, ${0.16 + dangerLevel * 0.24})`
  ctx.shadowBlur = Math.max(10, g.w * (0.12 + dangerLevel * 0.16))
  ctx.shadowOffsetY = Math.max(5, g.h * 0.12)

  ctx.save()
  ctx.translate(centerX, g.y + g.h * 0.5 + bob)
  ctx.rotate(Math.sin(world.elapsed * 3.4 + obs.depth * 7) * 0.012 + (dangerLevel > 0.32 ? Math.sin(world.elapsed * 16) * 0.01 : 0))
  if (sprite?.complete) {
    ctx.drawImage(sprite, -g.w * 0.5, -g.h * 0.5, g.w, g.h)
  } else {
    const fallbackFill = ctx.createLinearGradient(0, -g.h * 0.5, 0, g.h * 0.5)
    fallbackFill.addColorStop(0, '#edf3f7')
    fallbackFill.addColorStop(1, '#8d98a7')
    ctx.fillStyle = fallbackFill
    roundedPath(-g.w * 0.34, -g.h * 0.36, g.w * 0.68, g.h * 0.72, Math.max(14, g.w * 0.14))
    ctx.fill()
  }
  ctx.restore()

  ctx.shadowBlur = 0
  ctx.shadowOffsetY = 0

  ctx.fillStyle = 'rgba(255,255,255,0.14)'
  ctx.beginPath()
  ctx.ellipse(centerX, g.y + g.h * 0.2 + bob, g.w * 0.18, g.h * 0.06, -0.08, 0, Math.PI * 2)
  ctx.fill()

  if (dangerLevel > 0.18) {
    ctx.strokeStyle = obs.rim
    ctx.lineWidth = Math.max(2.8, g.w * 0.042)
    roundedPath(g.x - 8, g.y - 8, g.w + 16, g.h + 16, Math.max(18, g.w * 0.2))
    ctx.stroke()
  }

  if (dangerLevel > 0.28) {
    ctx.fillStyle = `rgba(255, 230, 206, ${0.76 + pulse * 0.06})`
    drawDiamond(centerX - 12, g.y - 30, 24, 24)
    ctx.fill()
    ctx.fillStyle = `rgba(65, 28, 17, ${0.8 + pulse * 0.06})`
    ctx.font = '700 14px Manrope'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('!', centerX, g.y - 18)
  }
  ctx.restore()
}

const drawCollectibles = () => {
  for (const item of collectibles) {
    const g = collectibleGeometry(item)
    const glow = ctx.createRadialGradient(g.x, g.y, 0, g.x, g.y, g.size * 3.1)
    glow.addColorStop(0, 'rgba(228, 251, 255, 0.96)')
    glow.addColorStop(0.34, 'rgba(116, 233, 255, 0.34)')
    glow.addColorStop(0.64, 'rgba(255, 194, 103, 0.26)')
    glow.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(g.x, g.y, g.size * 2.35, 0, Math.PI * 2)
    ctx.fill()

    ctx.save()
    ctx.strokeStyle = 'rgba(222, 247, 255, 0.74)'
    ctx.lineWidth = Math.max(1, g.size * 0.14)
    ctx.beginPath()
    ctx.arc(g.x, g.y, g.size * 1.56, 0, Math.PI * 2)
    ctx.stroke()
    ctx.restore()

    ctx.save()
    ctx.translate(g.x, g.y)
    ctx.rotate(world.elapsed * item.spin + item.phase)
    const core = ctx.createLinearGradient(0, -g.size * 1.1, 0, g.size * 1.1)
    core.addColorStop(0, '#fbffff')
    core.addColorStop(0.34, '#8aeeff')
    core.addColorStop(0.7, '#ffd274')
    core.addColorStop(1, '#ff935e')
    ctx.fillStyle = core
    ctx.beginPath()
    ctx.arc(0, 0, g.size * 0.92, 0, Math.PI * 2)
    ctx.fill()
    ctx.strokeStyle = 'rgba(255, 250, 244, 0.94)'
    ctx.lineWidth = Math.max(1.2, g.size * 0.16)
    ctx.stroke()

    ctx.fillStyle = 'rgba(9, 20, 31, 0.76)'
    drawDiamond(-g.size * 0.48, -g.size * 0.58, g.size * 0.96, g.size * 1.16)
    ctx.fill()

    ctx.strokeStyle = 'rgba(233, 251, 255, 0.78)'
    ctx.lineWidth = Math.max(1, g.size * 0.12)
    drawDiamond(-g.size * 0.48, -g.size * 0.58, g.size * 0.96, g.size * 1.16)
    ctx.stroke()

    ctx.fillStyle = 'rgba(255,255,255,0.34)'
    ctx.beginPath()
    ctx.arc(-g.size * 0.18, -g.size * 0.2, g.size * 0.18, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }
}

const runnerHitRect = () => {
  const pose = runnerPose()
  return { x: pose.x + pose.visualW * 0.21, y: pose.y + pose.visualH * 0.16, w: pose.visualW * 0.54, h: pose.visualH * 0.72 }
}

const drawRunner = () => {
  const pose = runnerPose()
  const hitImpact = clamp(runner.hitFlash, 0, 1)
  const collectPop = runner.collectPulse > 0 ? Math.sin((1 - runner.collectPulse) * Math.PI) : 0
  const shadowWidth = pose.visualW * 0.28
  const shadowHeight = 12

  const aura = ctx.createRadialGradient(pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.56, 0, pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.56, pose.visualW * 0.58)
  aura.addColorStop(0, 'rgba(124, 236, 255, 0.16)')
  aura.addColorStop(0.45, 'rgba(124, 236, 255, 0.06)')
  aura.addColorStop(1, 'rgba(124, 236, 255, 0)')
  ctx.fillStyle = aura
  ctx.beginPath()
  ctx.arc(pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.52, pose.visualW * 0.52, 0, Math.PI * 2)
  ctx.fill()

  if (collectPop > 0.02) {
    const collectAura = ctx.createRadialGradient(pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.48, 0, pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.48, pose.visualW * (0.44 + collectPop * 0.2))
    collectAura.addColorStop(0, `rgba(204, 248, 255, ${0.18 + collectPop * 0.22})`)
    collectAura.addColorStop(0.45, `rgba(118, 234, 255, ${0.1 + collectPop * 0.16})`)
    collectAura.addColorStop(1, 'rgba(118, 234, 255, 0)')
    ctx.fillStyle = collectAura
    ctx.beginPath()
    ctx.arc(pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.48, pose.visualW * (0.38 + collectPop * 0.22), 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.fillStyle = 'rgba(45, 34, 31, 0.28)'
  ctx.beginPath()
  ctx.ellipse(pose.x + pose.visualW * 0.5, getGroundY() + 11, shadowWidth, shadowHeight, 0, 0, Math.PI * 2)
  ctx.fill()

  ctx.save()
  ctx.shadowColor = 'rgba(42, 24, 18, 0.28)'
  ctx.shadowBlur = 14
  ctx.shadowOffsetY = 14
  ctx.filter = hitImpact > 0.02 ? `brightness(${1 - hitImpact * 0.4})` : 'none'
  if (runnerSpriteReady.value && runnerSprite) {
    ctx.drawImage(runnerSprite, pose.x, pose.y, pose.visualW, pose.visualH)
  } else {
    ctx.fillStyle = '#df6546'
    roundedPath(pose.x + pose.visualW * 0.24, pose.y + pose.visualH * 0.18, pose.visualW * 0.52, pose.visualH * 0.62, pose.visualW * 0.18)
    ctx.fill()
    ctx.fillStyle = '#101114'
    ctx.beginPath()
    ctx.ellipse(pose.x + pose.visualW * 0.5, pose.y + pose.visualH * 0.24, pose.visualW * 0.18, pose.visualH * 0.14, 0, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.filter = 'none'
  ctx.restore()

  if (runner.hitFlash > 0) {
    ctx.fillStyle = `rgba(255,255,255,${runner.hitFlash * 0.16})`
    ctx.fillRect(0, 0, view.width, view.height)
  }
  if (world.collectFlash > 0) {
    ctx.fillStyle = `rgba(143, 236, 255, ${world.collectFlash * 0.14})`
    ctx.fillRect(0, 0, view.width, view.height)
  }
}

const drawParticles = (dt) => {
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]
    p.life -= dt
    if (p.life <= 0) {
      particles.splice(i, 1)
      continue
    }
    p.vy += p.gravity * dt
    p.x += p.vx * dt
    p.y += p.vy * dt
    ctx.globalAlpha = clamp(p.life * 1.7, 0, 1)
    ctx.fillStyle = p.color
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalAlpha = 1
}

const drawFloatingTexts = (dt) => {
  for (let i = floatingTexts.length - 1; i >= 0; i--) {
    const text = floatingTexts[i]
    text.life -= dt
    if (text.life <= 0) {
      floatingTexts.splice(i, 1)
      continue
    }
    text.vy += text.gravity * dt
    text.y += text.vy * dt
    const progress = 1 - text.life / text.maxLife
    const alpha = clamp((text.life / text.maxLife) * 1.25, 0, 1)
    ctx.save()
    ctx.translate(text.x, text.y)
    ctx.scale(1 + Math.sin(Math.min(1, progress) * Math.PI) * 0.08, 1 + Math.sin(Math.min(1, progress) * Math.PI) * 0.08)
    ctx.globalAlpha = alpha
    ctx.font = `${text.weight} ${text.size}px Manrope`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.lineWidth = Math.max(2, text.size * 0.12)
    ctx.strokeStyle = text.stroke
    ctx.strokeText(text.text, 0, 0)
    ctx.fillStyle = text.color
    ctx.fillText(text.text, 0, 0)
    ctx.restore()
  }
  ctx.globalAlpha = 1
}

const drawScene = (dt) => {
  if (!ctx) return
  ctx.clearRect(0, 0, view.width, view.height)
  ctx.save()
  if (world.shakeTime > 0) {
    const power = world.shakePower * (world.shakeTime / 0.25)
    ctx.translate((Math.random() - 0.5) * power, (Math.random() - 0.5) * power * 0.56)
    world.shakeTime = Math.max(0, world.shakeTime - dt)
  }
  drawSky(dt)
  drawRoad(dt)
  drawLaneWarnings()
  drawCollectibles()
  for (const obs of obstacles) drawObstacle(obs)
  drawRunner()
  drawParticles(dt)
  drawFloatingTexts(dt)
  ctx.restore()
}

const createObstacle = (kind, lane, baseDepth = 0.025) => {
  const meta = OBSTACLE_SET[kind]
  if (!meta) return
  const speedMul = 0.92 + Math.random() * 0.16
  obstacles.push({ ...meta, lane, depth: baseDepth + Math.random() * 0.05, speedMul, checked: false, hitRect: null })
}

const createCollectible = (lane, depth, height) => {
  collectibles.push({ kind: 'lumen-core', lane, depth, height, size: 10.8, spin: 2.4, phase: Math.random() * Math.PI * 2, value: 20 })
}

const spawnCollectibleSequence = (lanes, startDepth = 0.04, gap = 0.056) => {
  lanes.forEach((lane, index) => {
    const laneHeightBoost = lane === 0 ? 0 : 6
    createCollectible(lane, startDepth + index * gap, 80 + laneHeightBoost + Math.random() * 16)
  })
}

const spawnCollectiblePattern = (openLanes) => {
  if (!openLanes.length) return
  const lanes = openLanes.length ? [...openLanes].sort((a, b) => a - b) : [-1, 0, 1]
  if (lanes.length === 3 && chance(0.34)) {
    const sweep = chance(0.5) ? [-1, 0, 1] : [1, 0, -1]
    spawnCollectibleSequence(sweep, 0.04, 0.054)
    return
  }
  if (lanes.length >= 2 && chance(0.44)) {
    const pair = lanes.length === 2 ? lanes : (chance(0.5) ? [-1, 0, 1] : [1, 0, -1]).filter((lane) => lanes.includes(lane))
    const zigzag = [pair[0], pair[pair.length - 1], pair[0]]
    spawnCollectibleSequence(zigzag, 0.04, 0.056)
    return
  }
  const lane = pick(lanes)
  const count = chance(0.48) ? 3 : 2
  spawnCollectibleSequence(Array(count).fill(lane), 0.04, 0.056)
}

const getObstacleEta = (depth, speedMul = 1) => {
  const distance = PLAYER_ZONE_START - depth
  if (distance <= 0) return 0
  return distance / (Math.max(world.pace, BASE_PACE) * speedMul)
}

const lanesBlockedAroundEta = (eta, tolerance = 0.24) => {
  const blocked = new Set()
  for (const obs of obstacles) {
    if (obs.checked) continue
    const obsEta = getObstacleEta(obs.depth, obs.speedMul || 1)
    if (Math.abs(obsEta - eta) <= tolerance) blocked.add(obs.lane)
  }
  return blocked
}

const spawnWave = () => {
  const pool = world.pace > 0.54 ? ['golem', 'relic', 'blush'] : ['golem', 'relic', 'blush', 'golem']
  const eta = getObstacleEta(0.025, 1)
  const blocked = lanesBlockedAroundEta(eta)
  const openLanes = [-1, 0, 1].filter((lane) => !blocked.has(lane))

  if (openLanes.length === 3 && world.pace > 0.58 && chance(0.16)) {
    const safeLane = randomLane()
    for (const lane of [-1, 0, 1]) {
      if (lane !== safeLane) createObstacle(pick(pool), lane)
    }
  } else if (openLanes.length >= 2) {
    createObstacle(pick(pool), pick(openLanes))
  } else if (openLanes.length === 1) {
    if (chance(0.42)) {
      // 当前时间窗里已经只有一条安全道时，不再补障碍，避免出现理论无解局。
    }
  }

  spawnCollectiblePattern(openLanes)
}

const resetRunner = () => {
  runner.lane = 0
  runner.laneVisual = 0
  runner.tilt = 0
  runner.hitFlash = 0
  runner.collectPulse = 0
}

const resetWorld = () => {
  Object.assign(world, { pace: BASE_PACE, targetPace: BASE_PACE, elapsed: 0, distance: 0, spawnTimer: 0.72, stripeOffset: 0, cloudOffset: 0, hillOffset: 0, treeOffset: 0, shakeTime: 0, shakePower: 0, guideTime: 0, collectFlash: 0 })
  score.value = 0
  speedKmh.value = 26
  level.value = 1
  speedRate.value = '1.00'
  bonusScore = 0
  collectChain = 0
  collectChainTimer = 0
  obstacles = []
  collectibles = []
  particles = []
  floatingTexts = []
}

const moveLane = (delta) => {
  if (gameState.value !== 'playing') return
  const next = clamp(runner.lane + delta, -1, 1)
  if (next === runner.lane) return
  const prev = runner.lane
  runner.lane = next
  addParticles(laneXAtDepth(prev + delta * 0.25, PLAYER_DEPTH), getGroundY() - 8, 8, ['#c9fbff', '#8eebff', '#ffd57a'], 110)
}

const genRoundId = () => `light-leaper-${Date.now()}-${Math.floor(Math.random() * 1e6)}`

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
  if (liveReporterTimer) clearInterval(liveReporterTimer)
  liveReporterTimer = null
}

const startLiveReporter = () => {
  stopLiveReporter()
  if (authStore.isGuest) return
  liveReporterTimer = window.setInterval(() => {
    if (gameState.value !== 'playing') return
    const now = Number(score.value || 0)
    if (now <= 0 || now - liveReportedScore < 18) return
    liveReportedScore = now
    submitScoreReport(now, { final: false })
  }, 1100)
}

const finishRound = () => {
  if (gameState.value !== 'playing') return
  runner.hitFlash = 1
  gameState.value = 'over'
  world.shakeTime = 0.24
  world.shakePower = 14
  triggerHitOverlay()
  addParticles(laneXAtDepth(runner.laneVisual, PLAYER_DEPTH), getPlayerFloorY() + runner.h * 0.46, 34, ['#ff9cb0', '#ffe19a', '#d7fbff'], 320)
  if (score.value > highScore.value) {
    highScore.value = score.value
    localStorage.setItem(resolvedHighScoreKey.value, String(score.value))
  }
  stopLiveReporter()
  submitScoreReport(score.value, { final: true, force: true })
}

const updateRunner = (dt) => {
  runner.laneVisual += (runner.lane - runner.laneVisual) * Math.min(1, dt * 13)
  runner.tilt += (clamp((runner.lane - runner.laneVisual) * 0.34, -0.28, 0.28) - runner.tilt) * Math.min(1, dt * 10)
  runner.hitFlash = Math.max(0, runner.hitFlash - dt * 2.2)
  runner.collectPulse = Math.max(0, runner.collectPulse - dt * 5.4)
  world.collectFlash = Math.max(0, world.collectFlash - dt * 3.2)
}

const updateGame = (dt) => {
  world.elapsed += dt
  world.targetPace = BASE_PACE + Math.min(0.68, world.elapsed * 0.012 + score.value / 3600)
  world.pace += (world.targetPace - world.pace) * Math.min(1, dt * 2)
  world.distance += world.pace * dt * 162
  score.value = Math.floor(world.distance) + bonusScore
  speedKmh.value = Math.round(26 + world.pace * 410)
  level.value = Math.max(1, Math.floor((world.pace - BASE_PACE) * 10.8) + 1)
  speedRate.value = (world.pace / BASE_PACE).toFixed(2)
  world.guideTime = Math.max(0, world.guideTime - dt)
  if (collectChainTimer > 0) {
    collectChainTimer = Math.max(0, collectChainTimer - dt)
    if (collectChainTimer === 0) collectChain = 0
  }

  world.spawnTimer -= dt
  if (world.spawnTimer <= 0) {
    spawnWave()
    world.spawnTimer = Math.max(0.24, 0.76 - (world.pace - BASE_PACE) * 0.72) + Math.random() * 0.22
  }

  const dangerRect = runnerDangerRect()
  for (let i = obstacles.length - 1; i >= 0; i--) {
    const obs = obstacles[i]
    obs.depth += world.pace * obs.speedMul * dt
    obs.hitRect = obstacleGeometry(obs).hitRect
    // Once the obstacle has crossed the player's hit line, it may remain on screen
    // for visual continuity, but it should no longer be lethal.
    const nearCollisionWindow = obs.depth >= PLAYER_ZONE_START - 0.06 && obs.depth <= PLAYER_COLLISION_END
    const laneLockedImpact = Math.abs(runner.laneVisual - obs.lane) < 0.34
      && obs.depth >= PLAYER_ZONE_START + 0.01
      && obs.depth <= PLAYER_COLLISION_END
    const visualImpact = rectsOverlap(dangerRect, obs.hitRect, 1, 2)
    if (!obs.checked && nearCollisionWindow && (visualImpact || laneLockedImpact)) {
      finishRound()
      return
    }
    if (!obs.checked && obs.depth >= PLAYER_ZONE_END + 0.02) {
      obs.checked = true
      bonusScore += Math.min(8 + level.value, 20)
      obstacles.splice(i, 1)
      continue
    }
    if (obs.depth > PLAYER_ZONE_END + 0.08) obstacles.splice(i, 1)
  }

  const rr = runnerHitRect()
  for (let i = collectibles.length - 1; i >= 0; i--) {
    const item = collectibles[i]
    item.depth += world.pace * 0.95 * dt
    const g = collectibleGeometry(item)
    const dx = rr.x + rr.w * 0.5 - g.x
    const dy = rr.y + rr.h * 0.5 - g.y
    const touch = Math.max(rr.w * 0.32, 14) + g.size
    if (dx * dx + dy * dy <= touch * touch) {
      collectChain = collectChainTimer > 0 ? collectChain + 1 : 1
      collectChainTimer = 1.18
      const comboBonus = Math.min(24, Math.max(0, collectChain - 1) * 4)
      const gainedScore = item.value + comboBonus
      bonusScore += gainedScore
      runner.collectPulse = 1
      world.shakeTime = Math.max(world.shakeTime, 0.07)
      world.shakePower = Math.max(world.shakePower, 4.5)
      addParticles(g.x, g.y, 18, ['#8eeeff', '#d9fbff', '#ffd77f', '#fffdf4'], 180)
      addParticles(lerp(g.x, rr.x + rr.w * 0.5, 0.44), lerp(g.y, rr.y + rr.h * 0.46, 0.44), 10, ['#9deeff', '#f3fdfd', '#ffd990'], 120)
      addFloatingText(g.x, g.y - g.size * 1.8, `+${gainedScore}`, '#f5feff', { size: isMobile.value ? 14 : 18, life: 0.7, vy: -58, stroke: 'rgba(8, 14, 22, 0.52)' })
      if (collectChain >= 2) {
        addFloatingText(g.x, g.y + g.size * 0.9, `连收 x${collectChain}`, '#8feeff', { size: isMobile.value ? 11 : 13, life: 0.62, vy: -34, stroke: 'rgba(12, 23, 34, 0.44)' })
      }
      world.collectFlash = 1
      collectibles.splice(i, 1)
      continue
    }
    if (item.depth > 1.2) collectibles.splice(i, 1)
  }
}

const frame = (ts) => {
  if (gameState.value !== 'playing' && !(gameState.value === 'over' && runner.hitFlash > 0.02)) return
  if (!lastFrameAt) lastFrameAt = ts
  const dt = Math.min(0.033, (ts - lastFrameAt) / 1000 || 0.016)
  lastFrameAt = ts
  if (gameState.value === 'playing') {
    updateRunner(dt)
    updateGame(dt)
  } else {
    runner.hitFlash = Math.max(0, runner.hitFlash - dt * 3.6)
    runner.collectPulse = Math.max(0, runner.collectPulse - dt * 6.2)
    world.collectFlash = Math.max(0, world.collectFlash - dt * 4)
    world.shakeTime = Math.max(0, world.shakeTime - dt)
  }
  drawScene(dt)
  if (gameState.value === 'playing' || runner.hitFlash > 0.02) animationId = requestAnimationFrame(frame)
  else animationId = null
}

const startGame = () => {
  if (!ctx || activeGame.value !== GAME_ID) return
  if (animationId) cancelAnimationFrame(animationId)
  stopLiveReporter()
  if (hitOverlayTimer) clearTimeout(hitOverlayTimer)
  hitOverlayTimer = null
  hitOverlayActive.value = false
  gameState.value = 'playing'
  finalScoreReported = false
  currentRoundId = genRoundId()
  liveReportedScore = 0
  lastFrameAt = 0
  resetWorld()
  resetRunner()
  if (!collisionGuideShown) {
    world.guideTime = 2.8
    collisionGuideShown = true
  }
  startLiveReporter()
  animationId = requestAnimationFrame(frame)
}

const pauseGame = () => {
  if (gameState.value !== 'playing') return
  if (animationId) cancelAnimationFrame(animationId)
  animationId = null
  gameState.value = 'paused'
  drawScene(1 / 60)
}

const resumeGame = () => {
  if (!ctx || gameState.value !== 'paused') return
  gameState.value = 'playing'
  lastFrameAt = 0
  animationId = requestAnimationFrame(frame)
}

const togglePause = () => {
  if (activeGame.value !== GAME_ID) return
  if (gameState.value === 'playing') pauseGame()
  else if (gameState.value === 'paused') resumeGame()
}

const handlePrimaryAction = () => {
  if (gameState.value === 'paused') resumeGame()
  else startGame()
}

const onKeyDown = (e) => {
  if (activeGame.value !== GAME_ID) return
  if (['ArrowLeft', 'ArrowRight', 'KeyA', 'KeyD', 'Enter', 'Space', 'KeyP', 'Escape'].includes(e.code)) e.preventDefault()
  if (e.code === 'ArrowLeft' || e.code === 'KeyA') moveLane(-1)
  else if (e.code === 'ArrowRight' || e.code === 'KeyD') moveLane(1)
  else if (e.code === 'Space' && gameState.value !== 'playing') handlePrimaryAction()
  else if (e.code === 'KeyP' || e.code === 'Escape') togglePause()
  else if (e.code === 'Enter' && gameState.value !== 'playing') handlePrimaryAction()
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
  const threshold = isMobile.value ? 22 : 30
  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > threshold) return moveLane(dx > 0 ? 1 : -1)
  if (dt < 260 && Math.abs(dx) < 20 && Math.abs(dy) < 20 && gameState.value !== 'playing') handlePrimaryAction()
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
  const runnerHeight = isMobile.value ? Math.round(clamp(view.width * 0.19, 84, 108)) : 124
  runner.h = runnerHeight
  runner.w = Math.round(runnerHeight * RUNNER_SPRITE_ASPECT)
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
  if (hitOverlayTimer) clearTimeout(hitOverlayTimer)
  hitOverlayTimer = null
  hitOverlayActive.value = false
  unbindGameEvents()
  if (animationId) cancelAnimationFrame(animationId)
  animationId = null
  pointerStart = null
  ctx = null
  gameState.value = 'idle'
}

const enterGame = (id) => { activeGame.value = id }
const goBackToHall = () => { activeGame.value = null }

watch(() => activeGame.value, async (value) => {
  if (value === GAME_ID) {
    gameState.value = 'idle'
    await syncHighScore()
    resetWorld()
    await setupGameCanvas()
    drawScene(1 / 60)
    return
  }
  teardownGame()
})

watch(() => authStore.user?.username, () => {
  syncHighScore()
}, { immediate: true })

onUnmounted(() => { teardownGame() })
</script>

<style scoped>
.game-root { background: transparent; padding-top: max(0.75rem, env(safe-area-inset-top)); --game-stage-max-height: calc(100dvh - 236px); }
.game-stack { max-width: 100%; }
.game-play-layout { display: grid; }
.game-stage-shell { display: flex; align-items: flex-start; justify-content: center; }
.game-stage-frame { aspect-ratio: 16 / 9; width: min(100%, 1420px, calc(var(--game-stage-max-height) * 16 / 9)); max-height: min(780px, var(--game-stage-max-height)); }
.game-stage {
  touch-action: none;
  border: 1px solid rgba(255,255,255,0.2);
  background: radial-gradient(140% 90% at 50% -10%, rgba(147,236,255,0.14), rgba(255,255,255,0)), linear-gradient(180deg, rgba(6,10,19,0.2), rgba(6,10,19,0.08));
  box-shadow: 0 36px 70px rgba(12,14,26,0.3), inset 0 1px 0 rgba(255,255,255,0.22);
}
.forest-panel, .forest-hall {
  border-color: rgba(255,255,255,0.54);
  background:
    linear-gradient(180deg, rgba(252,253,255,0.82), rgba(244,247,252,0.76)),
    radial-gradient(120% 120% at 16% 0%, rgba(135, 227, 255, 0.16), transparent 42%),
    radial-gradient(110% 120% at 84% 100%, rgba(255, 203, 137, 0.12), transparent 42%);
  box-shadow: 0 24px 48px rgba(35,47,75,0.08);
}
.dark .forest-panel, .dark .forest-hall {
  border-color: rgba(255,255,255,0.08);
  background:
    linear-gradient(180deg, rgba(30, 34, 46, 0.72), rgba(18, 22, 32, 0.68)),
    radial-gradient(120% 120% at 16% 0%, rgba(135, 227, 255, 0.08), transparent 42%),
    radial-gradient(110% 120% at 84% 100%, rgba(255, 203, 137, 0.06), transparent 42%);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.42);
}
.hall-hero-mark, .stage-brand-icon {
  display: grid; place-items: center; color: #fff;
  background: linear-gradient(135deg, #78ecff, #ffbf73);
  box-shadow: 0 16px 32px rgba(120, 235, 255, 0.22), 0 10px 24px rgba(255,191,115,0.18);
}
.hall-hero-mark { width: 42px; height: 42px; border-radius: 16px; }
.hud-item, .stage-hud-item, .stage-overlay-stat {
  border: 1px solid rgba(255,255,255,0.56);
  background: rgba(255,255,255,0.16);
  box-shadow: 0 14px 28px rgba(49,46,67,0.12);
  backdrop-filter: blur(18px) saturate(120%);
}
.hud-item { display: grid; min-width: 100px; gap: 1px; padding: 8px 12px; border-radius: 16px; background: rgba(255,255,255,0.58); }
.dark .hud-item { background: rgba(255, 255, 255, 0.06); border-color: rgba(255, 255, 255, 0.1); }
.hud-item span { font-size: 10px; color: var(--text-secondary); }
.hud-item strong { font-size: 13px; color: var(--text-primary); }
.game-card {
  overflow: hidden; border-radius: 28px; border: 1px solid rgba(255,255,255,0.58); background: rgba(252,253,255,0.82);
  transition: transform .3s cubic-bezier(.2,0,0,1), box-shadow .3s cubic-bezier(.2,0,0,1);
  box-shadow: 0 22px 42px rgba(35,47,75,0.09);
}
.dark .game-card { border-color: rgba(255, 255, 255, 0.1); background: rgba(30, 36, 48, 0.64); box-shadow: 0 22px 42px rgba(0, 0, 0, 0.32); }
.game-card:hover { transform: translateY(-4px); box-shadow: 0 28px 46px rgba(35,47,75,0.15); }
.dark .game-card:hover { box-shadow: 0 28px 54px rgba(0, 0, 0, 0.48); }
.game-title-gradient { background-image: linear-gradient(135deg, #68e8ff, #ffbf73 58%, #ff7da0); }
.game-live-chip { color: #0c3956; background: rgba(120, 235, 255, 0.16); border: 1px solid rgba(120, 235, 255, 0.24); }
.dark .game-live-chip { color: #8feeff; background: rgba(120, 235, 255, 0.1); border-color: rgba(120, 235, 255, 0.2); }
.game-enter-btn { background: linear-gradient(135deg, #6fe9ff, #ffbf73); box-shadow: 0 20px 34px -22px rgba(111, 233, 255, 0.84); }
.cover { position: relative; height: 176px; overflow: hidden; }
.cover-runner {
  cursor: pointer;
  background:
    radial-gradient(90% 62% at 50% 4%, rgba(118, 237, 255, 0.34), rgba(118, 237, 255, 0) 58%),
    radial-gradient(48% 42% at 18% 22%, rgba(255, 122, 165, 0.22), rgba(255, 122, 165, 0) 70%),
    radial-gradient(48% 42% at 84% 22%, rgba(255, 194, 113, 0.22), rgba(255, 194, 113, 0) 70%),
    linear-gradient(180deg, #0a1322 0%, #1a2236 38%, #322537 70%, #53352a 100%);
}
.cover-badge { position: absolute; top: 12px; left: 12px; z-index: 2; padding: 2px 10px; font-size: 11px; color: #07111f; border-radius: 999px; background: linear-gradient(90deg, #c8f7ff, #ffcd88); box-shadow: 0 10px 18px rgba(120, 235, 255, 0.24); }
.cover-halo { position: absolute; inset: 10px 16px auto; height: 98px; border-radius: 999px; background: radial-gradient(circle, rgba(110,231,255,0.52), rgba(110,231,255,0)); filter: blur(12px); }
.cover-threat-glow { position: absolute; inset: 18px auto auto 66%; width: 120px; height: 72px; border-radius: 999px; background: radial-gradient(circle, rgba(255, 119, 158, 0.32), rgba(255, 119, 158, 0)); filter: blur(14px); }
.cover-track {
  position: absolute; left: 50%; bottom: -8px; width: 76%; height: 88%; transform: translateX(-50%);
  clip-path: polygon(46% 0, 54% 0, 100% 100%, 0 100%);
  background: linear-gradient(180deg, rgba(58, 82, 126, 0.96), rgba(82, 71, 94, 0.94) 56%, rgba(152, 104, 72, 0.98));
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.26), 0 18px 32px rgba(29, 24, 42, 0.24);
}
.cover-track::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, transparent 33%, rgba(255,255,255,0.55) 33.6%, transparent 34.2%, transparent 66%, rgba(255,255,255,0.55) 66.6%, transparent 67.2%),
    linear-gradient(90deg, rgba(255,255,255,0.58), rgba(255,255,255,0.18) 24%, rgba(255,255,255,0.18) 76%, rgba(255,255,255,0.58));
  opacity: 0.58;
}
.cover-track::after {
  content: '';
  position: absolute;
  inset: 8% 20% 0;
  clip-path: polygon(48.5% 0, 51.5% 0, 70% 100%, 30% 100%);
  background: repeating-linear-gradient(180deg, rgba(255,248,233,0.98) 0 8px, rgba(255,248,233,0) 8px 18px);
  opacity: 0.95;
}
.cover-runner-shadow { position: absolute; left: 50%; bottom: 14px; z-index: 1; width: 74px; height: 18px; transform: translateX(-50%); border-radius: 999px; background: rgba(42, 30, 45, 0.28); filter: blur(5px); }
.cover-runner-avatar {
  position: absolute; left: 50%; bottom: 4px; z-index: 2; display: flex; align-items: flex-end; justify-content: center; width: 106px; height: 128px; transform: translateX(-50%);
}
.cover-runner-avatar::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 8px;
  width: 94px;
  height: 94px;
  transform: translateX(-50%);
  border-radius: 999px;
  background: radial-gradient(circle, rgba(110, 231, 255, 0.3), rgba(110, 231, 255, 0) 68%);
  z-index: -1;
}
.cover-runner-avatar::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 28px;
  height: 18px;
  transform: translateX(-50%);
  border-radius: 999px;
  background: radial-gradient(circle, rgba(36, 23, 19, 0.68), rgba(36, 23, 19, 0));
  filter: blur(2px);
}
.cover-runner-image {
  width: 108px;
  height: 124px;
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 16px 24px rgba(9, 20, 31, 0.3));
}
.cover-collectible, .cover-obstacle { position: absolute; bottom: 16px; }
.cover-collectible {
  width: 22px; height: 22px; top: 50px; border-radius: 999px;
  background:
    radial-gradient(circle at 35% 30%, rgba(255,255,255,0.96) 0 12%, rgba(255,255,255,0) 20%),
    radial-gradient(circle, rgba(156, 240, 255, 0.96) 0%, rgba(156, 240, 255, 0.7) 26%, rgba(255, 209, 121, 0.74) 56%, rgba(255, 209, 121, 0) 78%);
  box-shadow: 0 0 24px rgba(149, 236, 255, 0.56), inset 0 0 0 1px rgba(255,255,255,0.26);
}
.cover-collectible-left { left: 32%; top: 58px; }
.cover-collectible-right { right: 30%; top: 48px; }
.cover-obstacle { filter: drop-shadow(0 14px 18px rgba(10, 17, 29, 0.28)); display: flex; align-items: flex-end; justify-content: center; pointer-events: none; }
.cover-obstacle-image { width: 100%; height: 100%; object-fit: contain; object-position: center bottom; }
.cover-obstacle-golem { width: 56px; height: 56px; }
.cover-obstacle-relic { width: 42px; height: 76px; }
.cover-obstacle-blush { width: 60px; height: 60px; }
.cover-obstacle-left, .cover-obstacle-right { transform: translateY(2px) scale(0.9); }
.cover-obstacle-left { left: 24%; bottom: 28px; }
.cover-obstacle-mid { left: 50%; bottom: 52px; transform: translateX(-50%) scale(0.78); opacity: 0.84; }
.cover-obstacle-right { right: 24%; bottom: 22px; }
.cover-coming {
  display: grid; place-items: center;
  background: repeating-linear-gradient(135deg, rgba(255,255,255,0.18) 0, rgba(255,255,255,0.18) 14px, rgba(205,218,231,0.26) 14px, rgba(205,218,231,0.26) 28px), linear-gradient(135deg, #f7f8ff, #e9edf6);
}
.dark .cover-coming {
  background: repeating-linear-gradient(135deg, rgba(255,255,255,0.04) 0, rgba(255,255,255,0.04) 14px, rgba(255,255,255,0.08) 14px, rgba(255,255,255,0.08) 28px), linear-gradient(135deg, #1e293b, #0f172a);
}
.coming-tag { padding: 6px 16px; border-radius: 999px; background: rgba(44,47,64,0.78); color: #fff; font-size: 14px; font-weight: 600; }
.dark .coming-tag { background: rgba(255, 255, 255, 0.14); backdrop-filter: blur(8px); }
.stage-top-scrim {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 148px;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(7, 12, 24, 0.72), rgba(7, 12, 24, 0.3) 58%, rgba(7, 12, 24, 0)),
    radial-gradient(80% 100% at 52% 0%, rgba(118, 234, 255, 0.18), rgba(255,255,255,0));
}
.hit-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  opacity: 0;
  background:
    radial-gradient(circle at center, rgba(255, 132, 116, 0.22), rgba(255, 82, 109, 0.05) 48%, rgba(255, 82, 109, 0.02) 72%, rgba(255, 82, 109, 0)),
    rgba(255, 82, 109, 0.18);
  mix-blend-mode: screen;
  transition: opacity 0.2s ease;
}
.hit-active {
  opacity: 1;
}
.stage-topbar { position: absolute; top: 16px; left: 16px; right: 16px; z-index: 2; display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; pointer-events: none; }
.stage-topbar-left { display: grid; gap: 8px; max-width: min(360px, 42%); min-width: 0; }
.stage-topbar-right { display: grid; justify-items: end; gap: 8px; flex: 1; min-width: 0; }
.stage-back-btn, .stage-pause-btn, .stage-action-btn, .stage-ghost-btn {
  pointer-events: auto; transition: transform .18s ease, opacity .18s ease, box-shadow .18s ease;
}
.stage-back-btn, .stage-pause-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 34px; padding: 0 12px; border-radius: 999px; font-size: 11px; font-weight: 700;
  border: 1px solid rgba(188, 233, 244, 0.28);
  background: linear-gradient(180deg, rgba(40, 54, 78, 0.92), rgba(17, 24, 40, 0.9));
  backdrop-filter: blur(18px) saturate(132%);
  box-shadow: 0 12px 22px rgba(7, 12, 22, 0.24), inset 0 1px 0 rgba(255, 255, 255, 0.14);
  color: #effcff;
  text-shadow: 0 1px 1px rgba(7, 12, 19, 0.28);
}
.stage-back-btn:hover, .stage-pause-btn:hover:not(:disabled), .stage-action-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 14px 24px rgba(19, 16, 26, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.2); }
.stage-back-btn span:first-child { color: #8feeff; }
.stage-pause-btn:disabled, .stage-action-btn:disabled { opacity: .72; }
.stage-pause-btn:disabled {
  border-color: rgba(188, 233, 244, 0.16);
  background: linear-gradient(180deg, rgba(50, 59, 76, 0.78), rgba(24, 31, 43, 0.72));
  color: rgba(239, 252, 255, 0.72);
}
.stage-brand { max-width: 100%; min-height: 44px; padding: 7px 11px; display: inline-flex; align-items: center; gap: 10px; border-radius: 18px; border: 1px solid rgba(188,233,244,0.28); background: linear-gradient(180deg, rgba(23,34,54,0.46), rgba(15,23,38,0.26)); backdrop-filter: blur(16px) saturate(128%); box-shadow: 0 10px 20px rgba(8, 12, 20, 0.18); }
.stage-brand-icon { width: 30px; height: 30px; border-radius: 12px; box-shadow: inset 0 1px 0 rgba(255,255,255,0.4); }
.stage-brand-copy { display: grid; gap: 2px; min-width: 0; }
.stage-brand-copy strong { color: #f4fdff; font-size: 14px; font-weight: 800; }
.stage-brand-copy span { color: rgba(223, 245, 255, 0.78); font-size: 10px; line-height: 1.35; }
.stage-meta { display: flex; align-items: center; justify-content: flex-end; gap: 6px; flex-wrap: wrap; }
.stage-bottom-controls-hint {
  display: inline-flex; align-items: center; min-height: 22px; padding: 0 10px; border-radius: 999px; font-size: 10px; font-weight: 700;
  background: linear-gradient(180deg, rgba(24,37,57,0.42), rgba(12,20,34,0.22));
  border-color: rgba(188,233,244,0.2);
  color: rgba(232, 250, 255, 0.78);
  backdrop-filter: blur(18px) saturate(132%);
  box-shadow: 0 10px 18px rgba(8, 12, 20, 0.14);
}
.stage-hud { display: grid; grid-template-columns: repeat(3, minmax(90px, auto)); gap: 7px; }
.stage-hud-item { min-height: 44px; padding: 6px 10px; display: grid; align-content: center; gap: 1px; border-radius: 16px; border: 1px solid rgba(188,233,244,0.2); background: linear-gradient(180deg, rgba(20,31,49,0.34), rgba(11,19,31,0.14)); backdrop-filter: blur(16px) saturate(128%); box-shadow: 0 8px 16px rgba(8,12,20,0.12); }
.stage-hud-item span { color: rgba(214, 242, 252, 0.7); font-size: 10px; }
.stage-hud-item strong { color: #f4fdff; font-size: 13px; font-weight: 800; }
.stage-overlay { background: linear-gradient(180deg, rgba(10,14,25,0.32), rgba(10,14,25,0.48)); backdrop-filter: blur(9px); }
.stage-overlay-card {
  padding: 28px 28px 24px; border-radius: 32px; border: 1px solid rgba(255,255,255,0.58);
  background:
    radial-gradient(80% 80% at 50% 0%, rgba(124, 236, 255, 0.14), transparent 58%),
    linear-gradient(180deg, rgba(26,38,58,0.74), rgba(15,22,36,0.6)),
    rgba(13,18,30,0.42);
  box-shadow: 0 30px 64px rgba(7,10,18,0.26); color: #fff;
}
.dark .stage-overlay-card { border-color: rgba(255, 255, 255, 0.14); background: linear-gradient(180deg, rgba(15, 23, 42, 0.92), rgba(10, 15, 28, 0.88)); }
.stage-overlay-mark { font-size: clamp(2.8rem, 5vw, 4.1rem); line-height: 1; }
.stage-overlay-kicker { margin-top: 10px; font-size: 11px; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; color: rgba(202,242,255,0.76); }
.stage-overlay-title { margin-top: 10px; font-size: clamp(1.8rem, 4vw, 2.6rem); line-height: 1.1; font-weight: 800; }
.stage-overlay-desc { margin-top: 12px; font-size: 14px; line-height: 1.65; color: rgba(233,248,255,0.84); }
.stage-overlay-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 18px; }
.stage-overlay-stat { display: grid; gap: 4px; padding: 12px 14px; border-radius: 18px; }
.stage-overlay-stat span { color: rgba(255,255,255,0.72); font-size: 10px; }
.stage-overlay-stat strong { color: #fff; font-size: 13px; font-weight: 800; }
.stage-overlay-actions { display: flex; justify-content: center; gap: 10px; margin-top: 18px; }
.stage-ghost-btn { min-height: 48px; padding: 0 18px; border-radius: 999px; border: 1px solid rgba(188,233,244,0.42); background: rgba(17,27,43,0.42); color: #f4fdff; font-weight: 700; backdrop-filter: blur(12px); }
.stage-overlay-hint { margin-top: 16px; font-size: 12px; color: rgba(211, 244, 255, 0.74); }
.stage-bottom-controls { position: absolute; left: 14px; right: 14px; bottom: max(2px, env(safe-area-inset-bottom)); z-index: 3; display: grid; gap: 6px; justify-items: center; pointer-events: none; }
.stage-bottom-controls-shell {
  display: grid; grid-template-columns: repeat(2, minmax(0, 102px)); gap: 8px; padding: 6px; pointer-events: auto;
  border-radius: 24px; border: 1px solid rgba(188,233,244,0.18);
  background: linear-gradient(180deg, rgba(24,37,57,0.34), rgba(11,18,30,0.14));
  backdrop-filter: blur(22px) saturate(140%);
  box-shadow: 0 12px 24px rgba(8,12,20,0.12), inset 0 1px 0 rgba(255,255,255,0.12);
}
.stage-action-btn {
  min-height: 42px; padding: 0 14px; border-radius: 18px; border: 1px solid rgba(255,255,255,0.2);
  background: linear-gradient(180deg, rgba(31,45,68,0.52), rgba(18,27,42,0.18)), rgba(255,255,255,0.04);
  color: rgba(244, 253, 255, 0.96); font-size: 13px; font-weight: 800; letter-spacing: .02em;
  text-shadow: 0 1px 0 rgba(7,12,19,0.18);
  backdrop-filter: blur(18px) saturate(128%); box-shadow: 0 10px 18px rgba(8,12,20,0.12), inset 0 1px 0 rgba(255,255,255,0.16), inset 0 -8px 16px rgba(59, 83, 109, 0.12);
}
.stage-action-btn:active:not(:disabled) { transform: translateY(1px) scale(0.988); box-shadow: 0 8px 14px rgba(49,46,67,0.12), inset 0 2px 10px rgba(112, 94, 101, 0.12); }
.stage-action-btn-primary {
  background: linear-gradient(180deg, rgba(111,233,255,0.52), rgba(255,191,115,0.28)), rgba(255,255,255,0.08);
  color: #09111f;
  box-shadow: 0 10px 18px rgba(111, 233, 255, 0.18), inset 0 1px 0 rgba(240,253,255,0.34), inset 0 -10px 18px rgba(255, 191, 115, 0.12);
}
.dark .stage-action-btn-primary { color: #fff; }


@media (max-height: 920px) and (min-width: 641px) { .game-root { --game-stage-max-height: calc(100dvh - 216px); } }
@media (max-height: 820px) and (min-width: 641px) { .game-root { --game-stage-max-height: calc(100dvh - 188px); } }
@media (max-width: 960px) { .stage-hud { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 640px) {
  .game-root { padding-top: max(.15rem, env(safe-area-inset-top)); padding-bottom: 10px; --game-stage-max-height: calc(100dvh - 126px - env(safe-area-inset-bottom)); }
  .game-stack, .game-play-layout { min-height: calc(100dvh - 126px - env(safe-area-inset-bottom)); }
  .game-stage-shell { flex: 1; align-items: stretch; }
  .game-stage-frame { aspect-ratio: auto; width: 100%; height: var(--game-stage-max-height); max-height: none; min-height: calc(100dvh - 126px - env(safe-area-inset-bottom)); }
  .stage-topbar { top: 10px; left: 10px; right: 10px; gap: 8px; }
  .stage-topbar-left { max-width: 30%; gap: 4px; }
  .stage-topbar-right { gap: 6px; max-width: 70%; }
  .stage-back-btn, .stage-pause-btn { min-height: 30px; padding: 0 9px; font-size: 10px; }
  .stage-brand { min-height: 34px; padding: 5px 8px; gap: 6px; border-radius: 14px; }
  .stage-brand-icon { width: 22px; height: 22px; border-radius: 8px; font-size: 12px; }
  .stage-brand-copy strong { font-size: 10px; line-height: 1.1; }
  .stage-brand-copy span { display: none; }
  .stage-bottom-controls-hint { min-height: 20px; padding: 0 8px; font-size: 9px; }
  .stage-hud { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 5px; max-width: 244px; }
  .stage-hud-item { min-height: 34px; padding: 5px 7px; border-radius: 12px; }
  .stage-hud-item span, .stage-overlay-stat span { font-size: 8px; }
  .stage-hud-item strong, .stage-overlay-stat strong { font-size: 11px; line-height: 1.15; }
  .stage-overlay-card { padding: 22px 18px 18px; border-radius: 24px; }
  .stage-overlay-desc { font-size: 12px; }
  .stage-overlay-stats { gap: 8px; }
  .stage-overlay-stat { padding: 10px 8px; border-radius: 14px; }
  .stage-bottom-controls { left: 8px; right: 8px; bottom: max(0px, env(safe-area-inset-bottom)); }
  .stage-bottom-controls-shell { width: 100%; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; padding: 6px; border-radius: 20px; }
  .stage-action-btn { min-height: 38px; padding: 0 8px; border-radius: 16px; font-size: 12px; }
  .hud-grid { width: 100%; }
  .hud-item { min-width: 0; padding: 7px 10px; }
  .hud-item strong { font-size: 12px; }
}
</style>
