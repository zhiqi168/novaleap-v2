<template>
  <div class="leaderboard-page workspace-page workspace-scroll h-full">
    <div class="leaderboard-shell workspace-stack">
      <header class="workspace-titlebar leaderboard-hero">
        <div class="workspace-titlecopy">
          <h1 class="workspace-title">排行榜</h1>
          <p class="workspace-subtitle">把做题进度和游戏成绩放进同一个成长面板里，随时看到自己和大家的当前位置。</p>
        </div>
        <div class="leaderboard-meta">
          <span class="workspace-chip px-3 py-1">实时同步</span>
          <span class="workspace-chip px-3 py-1">Top {{ currentList.length || 0 }}</span>
        </div>
      </header>

      <section class="workspace-panel leaderboard-summary">
        <div class="leaderboard-tabs" role="tablist" aria-label="排行榜切换">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            class="leaderboard-tab"
            :class="{ 'leaderboard-tab-active': activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="leaderboard-stat-grid">
          <article v-for="item in summaryCards" :key="item.label" class="leaderboard-stat-card">
            <p class="leaderboard-stat-label">{{ item.label }}</p>
            <p class="leaderboard-stat-value">{{ formatNumber(item.value) }}</p>
            <p class="leaderboard-stat-hint">{{ item.hint }}</p>
          </article>
        </div>
      </section>

      <section class="workspace-panel leaderboard-board">
        <div class="leaderboard-board-head">
          <div>
            <p class="leaderboard-board-kicker">{{ currentMeta.kicker }}</p>
            <h2 class="leaderboard-board-title">{{ currentMeta.title }}</h2>
          </div>
          <p class="leaderboard-board-note">{{ currentMeta.note }}</p>
        </div>

        <div v-if="loading" class="workspace-empty leaderboard-state">
          <p class="leaderboard-state-emoji">⏳</p>
          <p class="leaderboard-state-title">排行榜加载中</p>
          <p class="leaderboard-state-desc">正在同步最新记录，请稍候。</p>
        </div>

        <div v-else-if="!currentList.length" class="workspace-empty leaderboard-state">
          <p class="leaderboard-state-emoji">🌫️</p>
          <p class="leaderboard-state-title">{{ currentMeta.emptyTitle }}</p>
          <p class="leaderboard-state-desc">{{ currentMeta.emptyDesc }}</p>
        </div>

        <div v-else class="leaderboard-list">
          <article
            v-for="(item, index) in currentList"
            :key="item.userId || item.username || index"
            class="leaderboard-row workspace-list-item"
            :class="{ 'leaderboard-row-me': item.isMe }"
          >
            <div class="leaderboard-rank">
              <span v-if="index === 0">🥇</span>
              <span v-else-if="index === 1">🥈</span>
              <span v-else-if="index === 2">🥉</span>
              <span v-else>#{{ index + 1 }}</span>
            </div>

            <div class="leaderboard-user">
              <div class="leaderboard-avatar">
                <span v-if="isEmoji(item.avatar)">{{ item.avatar }}</span>
                <img v-else :src="item.avatar" alt="avatar" />
              </div>
              <div class="min-w-0">
                <div class="leaderboard-user-name">
                  {{ item.displayName }}
                  <span v-if="item.isMe" class="leaderboard-pill">我</span>
                </div>
                <p class="leaderboard-user-desc">{{ item.description }}</p>
              </div>
            </div>

            <div class="leaderboard-score">
              <div class="leaderboard-score-value">{{ formatNumber(item.score) }}</div>
              <div class="leaderboard-score-label">{{ currentMeta.scoreLabel }}</div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const tabs = [
  { label: '题名金榜', value: 'question' },
  { label: '竞技先锋', value: 'game' },
]

const activeTab = ref('question')
const loading = ref(false)
const leaderboard = ref([])
const summary = ref({})

const formatNumber = (value) => new Intl.NumberFormat('zh-CN').format(Number(value) || 0)
const isEmoji = (value) => typeof value === 'string' && !value.startsWith('http') && value.length <= 4

const currentMeta = computed(() => {
  if (activeTab.value === 'game') {
    return {
      kicker: 'Game Ranking',
      title: '竞技先锋',
      note: '按当前最高游戏分数排序，分数相同时保留原榜单先后顺序。',
      scoreLabel: '最高分',
      emptyTitle: '还没有游戏成绩',
      emptyDesc: '等第一位玩家留下成绩后，这里就会出现赛道记录。',
    }
  }

  return {
    kicker: 'Question Ranking',
    title: '题名金榜',
    note: '按已记录的做题数量排序，持续刷题会直接反映到榜单里。',
    scoreLabel: '完成题数',
    emptyTitle: '还没有做题记录',
    emptyDesc: '等第一批学习记录入库后，这里就会显示成长排名。',
  }
})

const normalizedList = computed(() => {
  const currentUsername = authStore.user?.username || ''
  return (Array.isArray(leaderboard.value) ? leaderboard.value : []).map((item) => ({
    userId: item?.userId,
    username: item?.username,
    displayName: String(item?.nickname || item?.username || '用户').trim() || '用户',
    avatar: item?.avatar || '👤',
    questionDone: Number(item?.questionDone) || 0,
    gameBestScore: Number(item?.gameBestScore) || 0,
    isMe: !!currentUsername && item?.username === currentUsername,
  }))
})

const currentList = computed(() => {
  const sorted = [...normalizedList.value].sort((a, b) => {
    if (activeTab.value === 'game') {
      return b.gameBestScore - a.gameBestScore
    }
    return b.questionDone - a.questionDone
  })

  return sorted.slice(0, 10).map((item) => {
    const score = activeTab.value === 'game' ? item.gameBestScore : item.questionDone
    return {
      ...item,
      score,
      description: activeTab.value === 'game'
        ? `当前最高成绩 ${formatNumber(score)} 分`
        : `累计完成 ${formatNumber(score)} 道题目`,
    }
  })
})

const summaryCards = computed(() => {
  if (activeTab.value === 'game') {
    return [
      { label: '上榜人数', value: normalizedList.value.length, hint: '当前进入游戏统计的玩家数' },
      { label: '最高分', value: summary.value?.maxGameScore || 0, hint: '当前榜单记录到的历史最高分' },
      { label: '活跃用户', value: summary.value?.activeUsers || 0, hint: '最近仍有互动记录的用户数' },
    ]
  }

  return [
    { label: '上榜人数', value: normalizedList.value.length, hint: '当前进入学习统计的用户数' },
    { label: '累计做题', value: summary.value?.questionDoneTotal || 0, hint: '已汇总到排行榜的做题总量' },
    { label: '活跃用户', value: summary.value?.activeUsers || 0, hint: '最近仍有互动记录的用户数' },
  ]
})

const loadLeaderboard = async () => {
  loading.value = true
  try {
    const res = await api.get('/api/leaderboard')
    if (res.code !== 200) {
      throw new Error(res.msg || '排行榜加载失败')
    }
    leaderboard.value = Array.isArray(res.data?.list) ? res.data.list : []
    summary.value = res.data?.summary || {}
  } catch (error) {
    leaderboard.value = []
    summary.value = {}
    console.error('Leaderboard load failed', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLeaderboard()
})
</script>

<style scoped>
.leaderboard-page {
  background: var(--app-shell-bg);
}

.leaderboard-shell {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.leaderboard-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.leaderboard-meta {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.leaderboard-summary,
.leaderboard-board {
  padding: 20px;
}

.leaderboard-tabs {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
}

.leaderboard-tab {
  min-height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 700;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.leaderboard-tab-active {
  background: var(--bg-elevated);
  color: var(--primary);
}

.leaderboard-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.leaderboard-stat-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--border-soft);
  background: linear-gradient(180deg, var(--bg-elevated), var(--bg-card));
}

.leaderboard-stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.leaderboard-stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
  color: var(--text-primary);
}

.leaderboard-stat-hint {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.leaderboard-board-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-soft);
}

.leaderboard-board-kicker {
  font-size: 12px;
  color: var(--text-tertiary);
}

.leaderboard-board-title {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--text-primary);
}

.leaderboard-board-note {
  max-width: 360px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
  text-align: right;
}

.leaderboard-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.leaderboard-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
}

.leaderboard-row-me {
  border-color: var(--accent-border);
  background: linear-gradient(120deg, rgba(var(--primary-rgb), 0.08), transparent 38%), var(--bg-card);
}

.leaderboard-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  font-weight: 800;
  color: var(--text-secondary);
}

.leaderboard-user {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.leaderboard-avatar {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  font-size: 26px;
}

.leaderboard-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.leaderboard-user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.leaderboard-pill {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--primary);
  font-size: 11px;
  font-weight: 700;
}

.leaderboard-user-desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.leaderboard-score {
  text-align: right;
}

.leaderboard-score-value {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
  color: var(--text-primary);
}

.leaderboard-score-label {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-tertiary);
}

.leaderboard-state {
  margin-top: 18px;
  padding: 48px 20px;
}

.leaderboard-state-emoji {
  font-size: 28px;
}

.leaderboard-state-title {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.leaderboard-state-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

@media (max-width: 860px) {
  .leaderboard-stat-grid {
    grid-template-columns: 1fr;
  }

  .leaderboard-board-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .leaderboard-board-note {
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 640px) {
  .leaderboard-hero {
    flex-direction: column;
  }

  .leaderboard-tabs {
    width: 100%;
  }

  .leaderboard-tab {
    flex: 1;
  }

  .leaderboard-row {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .leaderboard-score {
    grid-column: 2;
    text-align: left;
  }
}
</style>
