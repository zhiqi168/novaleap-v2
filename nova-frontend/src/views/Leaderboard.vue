<template>
  <div class="leaderboard-page workspace-page workspace-scroll h-full">

    <div class="leaderboard-shell workspace-stack relative z-10">
      <header class="workspace-titlebar leaderboard-titlebar">
        <div class="workspace-titlecopy">
          <h1 class="workspace-title">排行榜</h1>
          <p class="workspace-subtitle">做题成长与游戏记录使用同一套节奏呈现，切换模块时保持连续体验。</p>
        </div>
        <div class="leaderboard-titlebar__meta">
          <span class="workspace-chip px-3 py-1">Top 10</span>
          <span class="workspace-chip px-3 py-1">实时同步</span>
        </div>
      </header>

      <section class="leaderboard-stage workspace-section">
        <div class="leaderboard-stage__glow leaderboard-stage__glow--a"></div>
        <div class="leaderboard-stage__glow leaderboard-stage__glow--b"></div>

        <div class="leaderboard-stage__inner">
          <div class="leaderboard-stage__copy">
            <div class="leaderboard-stage__eyebrow">
              <span class="leaderboard-stage__tag">Growth Leaderboard</span>
              <span class="leaderboard-stage__live">
                <span class="leaderboard-stage__live-dot"></span>
                实时同步
              </span>
            </div>

            <h1 class="leaderboard-stage__title">
              排名不必突兀地闯进来
              <span>它可以自然接住每一次成长</span>
            </h1>

            <p class="leaderboard-stage__desc">
              {{ currentTabMeta.description }}
            </p>
          </div>

          <div class="leaderboard-stage__panel">
            <div v-if="leader" class="leaderboard-stage__leader">
              <div class="leaderboard-stage__leader-label">{{ currentTabMeta.leaderLabel }}</div>
              <div class="leaderboard-stage__leader-main">
                <div class="avatar-box leaderboard-stage__leader-avatar">
                  <span v-if="isEmoji(leader.avatar)">{{ leader.avatar }}</span>
                  <img v-else :src="leader.avatar" alt="leader avatar" />
                </div>

                <div class="leaderboard-stage__leader-copy min-w-0">
                  <div class="leaderboard-stage__leader-name">{{ leader.displayName }}</div>
                  <p class="leaderboard-stage__leader-desc">{{ leader.description }}</p>
                </div>

                <div class="leaderboard-stage__leader-score">
                  <span>{{ formatNumber(leader.val) }}</span>
                  <small>{{ currentTabMeta.scoreSuffix }}</small>
                </div>
              </div>
            </div>

            <div class="leaderboard-stage__stats">
              <article
                v-for="stat in summaryStats"
                :key="stat.label"
                class="leaderboard-stage__stat"
              >
                <p class="leaderboard-stage__stat-label">{{ stat.label }}</p>
                <p class="leaderboard-stage__stat-value">{{ formatNumber(stat.value) }}</p>
                <p class="leaderboard-stage__stat-hint">{{ stat.hint }}</p>
              </article>
            </div>
          </div>
        </div>

        <div class="leaderboard-stage__toolbar">
          <div class="leaderboard-tabs" role="tablist" aria-label="排行榜切换">
            <button
              v-for="tab in tabs"
              :key="tab.val"
              type="button"
            class="leaderboard-tab workspace-chip"
              :class="activeTab === tab.val ? 'is-active' : ''"
              @click="activeTab = tab.val"
            >
              <svg
                v-if="tab.icon && NovaIcons[tab.icon]"
                class="h-4 w-4"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="NovaIcons[tab.icon]"
              ></svg>
              {{ tab.label }}
            </button>
          </div>

          <div class="leaderboard-stage__status">
            <div class="leaderboard-stage__status-time">
              <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="12" cy="12" r="9"></circle>
                <path d="M12 7v5l3 2"></path>
              </svg>
              最后更新：{{ lastUpdate }}
            </div>
            <p class="leaderboard-stage__status-note">{{ currentTabMeta.updateHint }}</p>
          </div>
        </div>
      </section>

      <section class="leaderboard-list-section workspace-panel p-4 sm:p-5">
        <div class="leaderboard-list-head">
          <div>
            <p class="leaderboard-list-head__kicker">Top {{ currentList.length || 0 }}</p>
            <h2 class="leaderboard-list-head__title">{{ currentTabMeta.sectionTitle }}</h2>
          </div>
          <p class="leaderboard-list-head__note">{{ currentTabMeta.sectionCaption }}</p>
        </div>

        <div class="space-y-4">
          <div
            v-for="(item, idx) in currentList"
            :key="item.userId || item.username || `${item.displayName}-${idx}`"
            class="rank-card workspace-list-item group"
            :class="[
              item.isMe ? 'rank-card-me' : '',
              idx < 3 ? 'rank-card-top' : ''
            ]"
          >
            <div class="rank-card__accent"></div>

            <div class="rank-num" :class="idx < 3 ? `rank-num-top-${idx + 1}` : ''">
              <template v-if="idx === 0">🥇</template>
              <template v-else-if="idx === 1">🥈</template>
              <template v-else-if="idx === 2">🥉</template>
              <template v-else>{{ idx + 1 }}</template>
            </div>

            <div class="rank-profile">
              <div class="avatar-box">
                <span v-if="isEmoji(item.avatar)">{{ item.avatar }}</span>
                <img v-else :src="item.avatar" alt="avatar" />
              </div>

              <div class="rank-profile__copy min-w-0">
                <div class="rank-profile__title-row">
                  <span class="rank-profile__name">{{ item.displayName }}</span>
                  <span v-if="idx < 3" class="rank-pill">{{ topLabels[idx] }}</span>
                  <span v-if="item.isMe" class="rank-pill rank-pill--me">我的位置</span>
                </div>
                <p class="rank-profile__desc">{{ item.description }}</p>
              </div>
            </div>

            <div class="rank-score">
              <div
                class="rank-score__value"
                :class="idx < 3 ? `rank-score__value--top-${idx + 1}` : ''"
              >
                {{ formatNumber(item.val) }}
              </div>
              <div class="rank-score__label">{{ currentTabMeta.scoreLabel }}</div>
            </div>
          </div>

          <div v-if="loading" class="leaderboard-state workspace-empty">
            <p class="leaderboard-state__emoji">⏳</p>
            <p class="leaderboard-state__title">榜单更新中</p>
            <p class="leaderboard-state__desc">正在同步最新成绩，请稍候片刻。</p>
          </div>

          <div v-else-if="!currentList.length" class="leaderboard-state workspace-empty">
            <p class="leaderboard-state__emoji">🌫️</p>
            <p class="leaderboard-state__title">{{ currentTabMeta.emptyTitle }}</p>
            <p class="leaderboard-state__desc">{{ currentTabMeta.emptyDesc }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const NovaIcons = {
  Edit3: '<path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>',
  Gamepad2: '<line x1="6" x2="10" y1="11" y2="11"/><line x1="8" x2="8" y1="9" y2="13"/><rect width="20" height="12" x="2" y="6" rx="2"/><path d="M15 12h.01"/><path d="M18 10h.01"/>'
}

const tabs = [
  { label: '题名金榜', val: 'quiz', icon: 'Edit3' },
  { label: '竞技先锋', val: 'game', icon: 'Gamepad2' }
]

const topLabels = ['冠军', '亚军', '季军']
const numberFormatter = new Intl.NumberFormat('zh-CN')

const authStore = useAuthStore()
const activeTab = ref('quiz')
const loading = ref(false)
const rawData = ref([])
const lastUpdate = ref(formatTime())

function formatTime() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(new Date())
}

const formatNumber = (value) => numberFormatter.format(Number(value) || 0)

const isEmoji = (val) => typeof val === 'string' && !val.startsWith('http') && val.length <= 4

const hideDuplicateSuffix = (nickname, username) => {
  const cleanNickname = String(nickname || '').trim()
  const cleanUsername = String(username || '').trim()
  if (!cleanNickname) return '用户'
  const duplicateSuffix = `（${cleanUsername}）`
  return cleanUsername && cleanNickname.endsWith(duplicateSuffix)
    ? cleanNickname.slice(0, -duplicateSuffix.length)
    : cleanNickname
}

const normalizedUsers = computed(() => {
  const currentUserName = authStore.user?.username || authStore.username

  return rawData.value.map((item) => {
    const isGuestUser = item?.nickname?.startsWith('游客+') || /^[0-9a-f-]{36}$/i.test(item?.username)
    const isMe = item.username === currentUserName || item.userId === authStore.user?.id

    return {
      userId: item?.userId,
      username: item?.username,
      displayName: isGuestUser
        ? 'Nova 访客'
        : hideDuplicateSuffix(item?.nickname || item?.username, item?.username),
      avatar: isGuestUser ? '🥳' : (item?.avatar || '👤'),
      questionDone: Number(item?.questionDone) || 0,
      gameBestScore: Number(item?.gameBestScore) || 0,
      isMe
    }
  })
})

const totalParticipants = computed(() => normalizedUsers.value.length)
const totalSolved = computed(() => normalizedUsers.value.reduce((acc, item) => acc + item.questionDone, 0))
const totalGamePlayers = computed(() => normalizedUsers.value.filter((item) => item.gameBestScore > 0).length)
const topQuizScore = computed(() => normalizedUsers.value.reduce((max, item) => Math.max(max, item.questionDone), 0))
const topGameScore = computed(() => normalizedUsers.value.reduce((max, item) => Math.max(max, item.gameBestScore), 0))

const currentList = computed(() => {
  const isQuiz = activeTab.value === 'quiz'

  return [...normalizedUsers.value]
    .sort((a, b) => {
      const valueA = isQuiz ? a.questionDone : a.gameBestScore
      const valueB = isQuiz ? b.questionDone : b.gameBestScore
      return valueB - valueA
    })
    .slice(0, 10)
    .map((item) => {
      const value = isQuiz ? item.questionDone : item.gameBestScore

      return {
        ...item,
        val: value,
        description: isQuiz
          ? `已累计攻克 ${formatNumber(value)} 道知识点`
          : value > 0
            ? `最好成绩定格在 ${formatNumber(value)} 分`
            : '还在等待第一场漂亮起跑'
      }
    })
})

const leader = computed(() => currentList.value[0] || null)

const currentTabMeta = computed(() => {
  if (activeTab.value === 'quiz') {
    return {
      label: '题名金榜',
      description: '把每一次做题记录沉淀成可见的成长轨迹，让进步自然浮出水面，而不是被一块突兀的横幅硬生生切开。',
      leaderLabel: '当前题榜领跑',
      sectionTitle: '题名金榜',
      sectionCaption: '从第一题到第 N 题，每一次积累都会在这里留下清晰坐标。',
      scoreLabel: 'SOLVED',
      scoreSuffix: '题',
      updateHint: '做题记录提交后会自动同步到当前榜单。',
      emptyTitle: '题榜还在生成中',
      emptyDesc: '等第一批做题记录进入系统后，这里就会开始出现成长轨迹。'
    }
  }

  return {
    label: '竞技先锋',
    description: '把每一次起跑、冲刺和刷新纪录收进同一条赛道，让轻松的游戏时刻，也有连续而自然的高光曲线。',
    leaderLabel: '当前赛道最快',
    sectionTitle: '竞技先锋',
    sectionCaption: '不是突然冒出来的高分，而是一次次练习后逐渐形成的手感。',
    scoreLabel: 'SCORE',
    scoreSuffix: '分',
    updateHint: '游戏最高分刷新后会同步进入排行榜统计。',
    emptyTitle: '赛道还在热身中',
    emptyDesc: '等第一位玩家冲过终点，这里就会开始出现新的纪录。'
  }
})

const summaryStats = computed(() => {
  if (activeTab.value === 'quiz') {
    return [
      { label: '上榜人数', value: totalParticipants.value, hint: '当前进入成长统计的学习者' },
      { label: '累计攻克', value: totalSolved.value, hint: '已同步到排行榜的知识点总量' },
      { label: '榜首成绩', value: topQuizScore.value, hint: '当前第一名已攻克的知识点数' }
    ]
  }

  return [
    { label: '上榜人数', value: totalParticipants.value, hint: '当前进入赛道统计的玩家' },
    { label: '破零玩家', value: totalGamePlayers.value, hint: '已经留下有效游戏成绩的人数' },
    { label: '最佳成绩', value: topGameScore.value, hint: '当前游戏榜记录到的最高分' }
  ]
})

const fetchLeaderboard = async () => {
  loading.value = true

  try {
    const res = await api.get('/api/leaderboard')
    if (res.code === 200) {
      rawData.value = res.data?.list || []
    }
  } catch (error) {
    console.error('Fetch leaderboard failed', error)
  } finally {
    loading.value = false
    lastUpdate.value = formatTime()
  }
}

onMounted(() => {
  fetchLeaderboard()
})
</script>

<style scoped>
.leaderboard-page {
  position: relative;
  min-height: 100%;
  background: var(--app-shell-bg);
}

.leaderboard-shell {
  display: flex;
  flex-direction: column;
}

.leaderboard-titlebar__meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.leaderboard-page__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(56px);
}

.leaderboard-page__glow--a {
  right: -96px;
  top: 64px;
  width: 26rem;
  height: 26rem;
  background: var(--module-glow-a);
}

.leaderboard-page__glow--b {
  left: 0;
  top: 33%;
  width: 24rem;
  height: 24rem;
  background: var(--module-glow-b);
}

.leaderboard-page__glow--c {
  bottom: 32px;
  right: 25%;
  width: 22rem;
  height: 22rem;
  background: var(--module-glow-c);
}

.leaderboard-stage {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.04)),
    linear-gradient(180deg, var(--bg-elevated), var(--bg-card));
  box-shadow: var(--shadow-hover);
  backdrop-filter: blur(8px);
}

.leaderboard-stage__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(38px);
  pointer-events: none;
}

.leaderboard-stage__glow--a {
  top: -72px;
  right: -36px;
  width: 220px;
  height: 220px;
  background: var(--module-glow-a);
}

.leaderboard-stage__glow--b {
  bottom: -88px;
  left: 10%;
  width: 240px;
  height: 240px;
  background: var(--module-glow-b);
}

.leaderboard-stage__inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.95fr);
  gap: 28px;
  padding: 30px 30px 24px;
}

.leaderboard-stage__copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}

.leaderboard-stage__eyebrow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.leaderboard-stage__tag,
.leaderboard-stage__live {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--bg-ghost);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.leaderboard-stage__live {
  letter-spacing: 0;
  text-transform: none;
}

.leaderboard-stage__live-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--success);
  box-shadow: 0 0 0 6px rgba(var(--success-rgb), 0.14);
}

.leaderboard-stage__title {
  max-width: 12ch;
  font-size: clamp(34px, 5vw, 56px);
  line-height: 1.04;
  font-weight: 800;
  letter-spacing: -0.05em;
  color: var(--text-primary);
}

.leaderboard-stage__title span {
  display: block;
  margin-top: 8px;
  color: var(--primary);
}

.leaderboard-stage__desc {
  max-width: 560px;
  font-size: 15px;
  line-height: 1.85;
  color: var(--text-secondary);
}

.leaderboard-stage__panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.leaderboard-stage__leader,
.leaderboard-stage__stat {
  border-radius: 24px;
  border: 1px solid var(--border-soft);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.02)),
    var(--bg-surface);
  box-shadow: 0 14px 36px rgba(var(--primary-rgb), 0.1);
}

.leaderboard-stage__leader {
  padding: 18px;
}

.leaderboard-stage__leader-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.leaderboard-stage__leader-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
}

.leaderboard-stage__leader-avatar {
  width: 58px;
  height: 58px;
  border-radius: 20px;
}

.leaderboard-stage__leader-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.leaderboard-stage__leader-desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--text-secondary);
}

.leaderboard-stage__leader-score {
  text-align: right;
}

.leaderboard-stage__leader-score span {
  display: block;
  font-size: 32px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: -0.05em;
  color: var(--text-primary);
}

.leaderboard-stage__leader-score small {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-tertiary);
}

.leaderboard-stage__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.leaderboard-stage__stat {
  padding: 18px 16px 16px;
}

.leaderboard-stage__stat-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.leaderboard-stage__stat-value {
  margin-top: 10px;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: -0.05em;
  color: var(--text-primary);
}

.leaderboard-stage__stat-hint {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.leaderboard-stage__toolbar {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 30px 26px;
}

.leaderboard-tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
}

.leaderboard-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  border-radius: var(--radius-md);
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 700;
  transition: transform 0.2s ease, color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.leaderboard-tab:hover {
  color: var(--text-primary);
}

.leaderboard-tab.is-active {
  background: var(--bg-elevated);
  color: var(--primary);
  box-shadow: 0 12px 24px rgba(var(--primary-rgb), 0.12);
}

.leaderboard-stage__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.leaderboard-stage__status-time {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
}

.leaderboard-stage__status-note {
  font-size: 12px;
  color: var(--text-tertiary);
}

.leaderboard-list-section {
  margin-top: var(--space-5);
}

.leaderboard-list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 0 6px 14px;
  border-bottom: 1px solid var(--border-soft);
}

.leaderboard-list-head__kicker {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.leaderboard-list-head__title {
  margin-top: 6px;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.08;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.leaderboard-list-head__note {
  max-width: 360px;
  text-align: right;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text-secondary);
}

.rank-card {
  position: relative;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 18px 22px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-soft);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.02)),
    linear-gradient(180deg, var(--bg-elevated), var(--bg-card));
  box-shadow: 0 18px 42px rgba(var(--primary-rgb), 0.1);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.rank-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent-border);
  box-shadow: 0 24px 52px rgba(var(--primary-rgb), 0.16);
}

.rank-card-top {
  border-color: var(--accent-border);
}

.rank-card-me {
  border-color: var(--accent-border);
  background:
    linear-gradient(120deg, rgba(var(--primary-rgb), 0.1), transparent 34%),
    linear-gradient(180deg, var(--bg-elevated), var(--bg-card));
}

.rank-card__accent {
  position: absolute;
  left: 18px;
  right: 18px;
  top: 0;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(var(--primary-rgb), 0.58), transparent);
  opacity: 0;
  transition: opacity 0.22s ease;
}

.rank-card-top .rank-card__accent,
.rank-card-me .rank-card__accent,
.rank-card:hover .rank-card__accent {
  opacity: 1;
}

.rank-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: var(--bg-soft);
  border: 1px solid var(--border-soft);
  font-size: 20px;
  font-weight: 800;
  font-style: italic;
  color: var(--rank-muted);
}

.rank-num-top-1 {
  color: var(--rank-gold);
}

.rank-num-top-2 {
  color: var(--rank-silver);
}

.rank-num-top-3 {
  color: var(--rank-bronze);
}

.rank-profile {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.avatar-box {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  box-shadow: 0 10px 20px rgba(var(--primary-rgb), 0.1);
  font-size: 28px;
}

.avatar-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rank-profile__copy {
  min-width: 0;
}

.rank-profile__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.rank-profile__name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-profile__desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--text-secondary);
}

.rank-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--bg-soft);
  border: 1px solid var(--border-soft);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
}

.rank-pill--me {
  color: var(--primary);
  background: var(--accent-soft);
  border-color: var(--accent-border);
}

.rank-score {
  text-align: right;
}

.rank-score__value {
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: -0.06em;
  color: var(--rank-muted);
}

.rank-score__value--top-1 {
  color: var(--rank-gold);
}

.rank-score__value--top-2 {
  color: var(--rank-silver);
}

.rank-score__value--top-3 {
  color: var(--rank-bronze);
}

.rank-score__label {
  margin-top: 6px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.leaderboard-state {
  padding: 54px 20px;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--border-soft);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.02)),
    var(--bg-card);
  text-align: center;
}

.leaderboard-state__emoji {
  font-size: 30px;
}

.leaderboard-state__title {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.leaderboard-state__desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .leaderboard-stage__inner {
    grid-template-columns: 1fr;
  }

  .leaderboard-stage__title {
    max-width: 100%;
  }

  .leaderboard-stage__desc {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .leaderboard-stage__inner,
  .leaderboard-stage__toolbar {
    padding-left: 20px;
    padding-right: 20px;
  }

  .leaderboard-stage__stats {
    grid-template-columns: 1fr;
  }

  .leaderboard-stage__toolbar {
    align-items: stretch;
  }

  .leaderboard-tabs {
    width: 100%;
  }

  .leaderboard-tab {
    flex: 1;
  }

  .leaderboard-stage__status {
    align-items: flex-start;
  }

  .leaderboard-list-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .leaderboard-list-head__note {
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 640px) {
  .leaderboard-shell {
    padding-bottom: 28px;
  }

  .leaderboard-titlebar__meta {
    width: 100%;
  }

  .leaderboard-stage {
    border-radius: var(--radius-lg);
  }

  .leaderboard-stage__inner {
    gap: 20px;
    padding-top: 24px;
    padding-bottom: 18px;
  }

  .leaderboard-stage__title {
    font-size: 32px;
  }

  .leaderboard-stage__leader-main {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .leaderboard-stage__leader-score {
    grid-column: 2;
    text-align: left;
  }

  .rank-card {
    grid-template-columns: 48px minmax(0, 1fr);
    gap: 14px;
    padding: 16px;
  }

  .rank-score {
    grid-column: 2;
    text-align: left;
  }

  .rank-score__value {
    font-size: 28px;
  }

  .avatar-box {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }

  .rank-profile__name {
    font-size: 16px;
  }
}
</style>
