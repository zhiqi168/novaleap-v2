<template>
  <div class="preview-shell">
    <div class="preview-window">
      <div class="preview-header">
        <div>
          <p class="preview-kicker">NovaLeap Preview</p>
          <h3 class="preview-title">{{ currentMeta.title }}</h3>
        </div>
        <span class="preview-pill">{{ currentMeta.tag }}</span>
      </div>

      <Transition name="preview-switch" mode="out-in">
        <div :key="activeStepId" class="preview-scene">
          <template v-if="activeStepId === 'question'">
            <div class="preview-metrics">
              <article class="metric-card">
                <p class="metric-label">今日训练</p>
                <strong class="metric-value">{{ questionPreview[0]?.questionDone || 42 }}</strong>
                <span class="metric-note">题目完成</span>
              </article>
              <article class="metric-card">
                <p class="metric-label">AI 解析</p>
                <strong class="metric-value">18</strong>
                <span class="metric-note">条批注</span>
              </article>
            </div>

            <article class="panel-card">
              <div class="panel-head">
                <span>拾光题库 · 今日进展</span>
                <span class="preview-pill preview-pill-soft">Question Bank</span>
              </div>
              <div class="mt-4 space-y-3">
                <div v-for="item in questionPreview" :key="item.userId" class="row-card">
                  <div class="row-main">
                    <span class="row-index">#{{ item.rank }}</span>
                    <span class="truncate">{{ item.displayName }}</span>
                  </div>
                  <strong class="row-value">{{ item.questionDone }}</strong>
                </div>
                <div v-if="!questionPreview.length" class="empty-card">这里会展示今天的训练节奏与题目完成情况。</div>
              </div>
            </article>
          </template>

          <template v-else-if="activeStepId === 'notes'">
            <article class="panel-card">
              <div class="panel-head">
                <span>灵感手记 · 今日沉淀</span>
                <span class="preview-pill preview-pill-soft">Notes</span>
              </div>
              <div class="note-stack">
                <article class="note-card note-card-main">
                  <h4 class="note-title">缓存一致性复盘</h4>
                  <p class="note-copy">先守住可用性，再逐步补齐一致性，让一条结论以后也能继续复用。</p>
                </article>
                <article class="note-card">
                  <h4 class="note-title">错题归档</h4>
                  <p class="note-copy">把题目、原因和迁移场景记录下来，下次就不会再次从零开始。</p>
                </article>
              </div>
            </article>
          </template>

          <template v-else-if="activeStepId === 'resume'">
            <article class="panel-card">
              <div class="panel-head">
                <span>简历工坊 · 项目表达</span>
                <span class="preview-pill preview-pill-soft">Resume</span>
              </div>
              <div class="timeline">
                <div v-for="item in resumeSteps" :key="item.index" class="timeline-row">
                  <span class="timeline-index">{{ item.index }}</span>
                  <div>
                    <p class="timeline-title">{{ item.title }}</p>
                    <p class="timeline-copy">{{ item.copy }}</p>
                  </div>
                </div>
              </div>
            </article>
          </template>

          <template v-else-if="activeStepId === 'coach'">
            <article class="panel-card">
              <div class="panel-head">
                <span>知识陪练 · 模拟表达</span>
                <span class="preview-pill preview-pill-soft">Practice</span>
              </div>
              <div class="chat-stack">
                <div class="chat-bubble chat-bubble-soft">请在 90 秒内讲清楚你在项目里如何做限流、削峰和降级。</div>
                <div class="chat-bubble chat-bubble-strong">我会先在入口限流，再用队列削峰，核心链路通过熔断与降级保证稳定。</div>
                <div class="chat-bubble chat-bubble-soft">很好，再补充你如何证明这套方案在高峰时段真实起效。</div>
              </div>
            </article>
          </template>

          <template v-else-if="activeStepId === 'community'">
            <article class="panel-card">
              <div class="panel-head">
                <span>星愿墙 · 成长反馈</span>
                <span class="preview-pill preview-pill-soft">Community</span>
              </div>
              <div class="timeline">
                <div v-for="item in communitySteps" :key="item.index" class="timeline-row">
                  <span class="timeline-index">{{ item.index }}</span>
                  <div>
                    <p class="timeline-title">{{ item.title }}</p>
                    <p class="timeline-copy">{{ item.copy }}</p>
                  </div>
                </div>
              </div>
            </article>
          </template>

          <template v-else-if="activeStepId === 'game'">
            <article class="panel-card">
              <div class="panel-head">
                <span>休闲时刻 · 轻松切换</span>
                <span class="preview-pill preview-pill-soft">Relax</span>
              </div>
              <div class="mt-4 space-y-3">
                <div v-for="item in scoreTopFive" :key="`g-${item.userId}`" class="row-card">
                  <div class="row-main">
                    <span class="row-index">#{{ item.rank }}</span>
                    <span class="truncate">{{ item.displayName }}</span>
                  </div>
                  <strong class="row-value">{{ item.gameBestScore }}</strong>
                </div>
                <div v-if="!scoreTopFive.length" class="empty-card">轻互动会留出一小段切换节奏的空间，让长期学习更可持续。</div>
              </div>
            </article>
          </template>

          <template v-else-if="activeStepId === 'leaderboard'">
            <article class="panel-card leaderboard-finale">
              <div class="panel-head">
                <span>排行榜 · 最终收束</span>
                <span class="preview-pill preview-pill-soft">Leaderboard</span>
              </div>
              <p class="leaderboard-copy">
                把整段训练路径汇总成可见结果，首页叙事在这里闭环。
              </p>

              <div class="leaderboard-grid">
                <section class="leaderboard-board">
                  <header class="leaderboard-board-head">
                    <h4 class="leaderboard-board-title">题名金榜</h4>
                    <span class="leaderboard-board-badge">Top {{ questionTopFive.length }}</span>
                  </header>
                  <div v-if="!questionTopFive.length" class="empty-card leaderboard-empty">暂无做题记录</div>
                  <div
                    v-for="item in questionTopFive"
                    :key="`lq-${item.userId}`"
                    class="leaderboard-row"
                  >
                    <div class="row-main">
                      <span class="leaderboard-index">#{{ item.rank }}</span>
                      <span class="truncate">{{ item.displayName }}</span>
                    </div>
                    <strong class="row-value">{{ formatValue(item.questionDone) }}</strong>
                  </div>
                </section>

                <section class="leaderboard-board">
                  <header class="leaderboard-board-head">
                    <h4 class="leaderboard-board-title">竞速先锋</h4>
                    <span class="leaderboard-board-badge">Top {{ scoreTopFive.length }}</span>
                  </header>
                  <div v-if="!scoreTopFive.length" class="empty-card leaderboard-empty">暂无游戏成绩</div>
                  <div
                    v-for="item in scoreTopFive"
                    :key="`lg-${item.userId}`"
                    class="leaderboard-row"
                  >
                    <div class="row-main">
                      <span class="leaderboard-index">#{{ item.rank }}</span>
                      <span class="truncate">{{ item.displayName }}</span>
                    </div>
                    <strong class="row-value">{{ formatValue(item.gameBestScore) }}</strong>
                  </div>
                </section>
              </div>
            </article>
          </template>

          <template v-else>
            <article class="panel-card">
              <div class="panel-head">
                <span>拾光题库 · 今日进展</span>
                <span class="preview-pill preview-pill-soft">Question Bank</span>
              </div>
              <div class="mt-4 space-y-3">
                <div v-for="item in questionPreview" :key="`f-${item.userId}`" class="row-card">
                  <div class="row-main">
                    <span class="row-index">#{{ item.rank }}</span>
                    <span class="truncate">{{ item.displayName }}</span>
                  </div>
                  <strong class="row-value">{{ item.questionDone }}</strong>
                </div>
                <div v-if="!questionPreview.length" class="empty-card">这里会展示当日训练与阶段进度。</div>
              </div>
            </article>
          </template>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeStepId: {
    type: String,
    required: true,
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

const metaMap = {
  question: { title: '拾光题库', tag: 'Training' },
  notes: { title: '灵感手记', tag: 'Knowledge' },
  resume: { title: '简历工坊', tag: 'Career' },
  coach: { title: '知跃陪练', tag: 'AI Practice' },
  community: { title: '星愿墙', tag: 'Feedback' },
  game: { title: '休闲时刻', tag: 'Relax' },
  leaderboard: { title: '排行榜', tag: 'Leaderboard' },
}

const currentMeta = computed(() => metaMap[props.activeStepId] || metaMap.question)
const numberFormatter = new Intl.NumberFormat('zh-CN')
const formatValue = (value) => numberFormatter.format(Number(value) || 0)

const resumeSteps = [
  { index: '1', title: '背景与目标', copy: '先讲清楚你解决的是什么问题，以及为什么值得做。' },
  { index: '2', title: '关键动作', copy: '把真正由你完成的设计、取舍和实现说清楚。' },
  { index: '3', title: '结果反馈', copy: '最终用量化结果和可验证影响收住整段表达。' },
]

const communitySteps = [
  { index: '1', title: '目标公开', copy: '把这一阶段想完成的方向说出来，让目标从脑海里的念头变成可被看见的承诺。' },
  { index: '2', title: '成长反馈', copy: '记录阶段进展、卡点和变化，让每一次推进都能留下被回应和被确认的痕迹。' },
  { index: '3', title: '社区感', copy: '在彼此可见的节奏里获得陪伴，不只知道要去哪里，也知道有人和你一起往前走。' },
]
</script>

<style scoped>
.preview-shell {
  border-radius: var(--radius-xl);
  padding: 18px;
  background: var(--surface-panel);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-hover);
  backdrop-filter: blur(8px);
}

.preview-window {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  padding: 24px;
  min-height: 600px;
}

.preview-header,
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-kicker {
  font-size: 11px;
  letter-spacing: 0.22em;
  color: var(--text-muted);
}

.preview-title {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.12;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.preview-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid var(--accent-border);
  background: var(--accent-soft);
  color: var(--primary);
  padding: 7px 12px;
  font-size: 11px;
  font-weight: 600;
}

.preview-pill-soft {
  padding: 6px 10px;
}

.preview-scene {
  margin-top: 22px;
  display: grid;
  gap: 16px;
}

.preview-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.panel-card,
.note-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  box-shadow: var(--shadow-card);
}

.metric-card {
  padding: 18px;
}

.metric-label,
.metric-note,
.row-index,
.empty-card {
  color: var(--text-muted);
}

.metric-label {
  font-size: 12px;
}

.metric-value {
  display: inline-block;
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
  letter-spacing: -0.05em;
  color: var(--text-primary);
}

.metric-note {
  margin-left: 6px;
  font-size: 12px;
}

.panel-card {
  padding: 18px;
}

.row-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  padding: 12px 14px;
}

.row-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.row-index {
  width: 28px;
  flex-shrink: 0;
  font-size: 12px;
}

.row-value {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.leaderboard-finale {
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent-soft) 78%, transparent) 0%, transparent 58%),
    var(--bg-soft);
}

.leaderboard-copy {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.leaderboard-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.leaderboard-board {
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  padding: 12px;
}

.leaderboard-board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-soft);
}

.leaderboard-board-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.leaderboard-board-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--accent-border);
  background: var(--accent-soft);
  color: var(--primary);
  font-size: 10px;
  font-weight: 700;
}

.leaderboard-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-radius: 14px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  padding: 10px 11px;
}

.leaderboard-index {
  width: 32px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.leaderboard-empty {
  margin-top: 10px;
  padding: 14px;
}

.empty-card {
  border-radius: 18px;
  border: 1px dashed var(--border-light);
  padding: 18px;
  font-size: 13px;
  line-height: 1.8;
}

.note-stack,
.timeline,
.chat-stack {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.note-card {
  padding: 18px;
}

.note-card-main {
  background: var(--surface-panel-soft);
}

.note-title,
.timeline-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.note-copy,
.timeline-copy {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.timeline-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  padding: 14px;
}

.timeline-index {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
}

.chat-bubble {
  max-width: 90%;
  border-radius: 18px;
  padding: 14px 16px;
  font-size: 13px;
  line-height: 1.85;
}

.chat-bubble-soft {
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  color: var(--text-secondary);
}

.chat-bubble-strong {
  margin-left: auto;
  border: 1px solid var(--accent-border);
  background: var(--accent-soft);
  color: var(--primary);
}

.preview-switch-enter-active,
.preview-switch-leave-active {
  transition:
    opacity 540ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 540ms cubic-bezier(0.22, 1, 0.36, 1);
}

.preview-switch-enter-from,
.preview-switch-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.986);
}

.dark .preview-shell,
.dark .preview-window,
.dark .metric-card,
.dark .panel-card,
.dark .row-card,
.dark .leaderboard-board,
.dark .leaderboard-row,
.dark .note-card,
.dark .timeline-row,
.dark .chat-bubble-soft {
  background: var(--surface-panel-soft);
  border-color: var(--border-soft);
}

.dark .preview-pill,
.dark .timeline-index,
.dark .chat-bubble-strong {
  background: var(--accent-soft);
  border-color: var(--accent-border);
}

@media (max-width: 768px) {
  .preview-window {
    min-height: auto;
  }

  .preview-metrics {
    grid-template-columns: 1fr;
  }

  .leaderboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
