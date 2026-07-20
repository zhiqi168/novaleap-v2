<template>
  <section class="hero-shell">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ eyebrow }}</p>

      <h1 class="hero-title" :data-text="title">{{ title }}</h1>

      <p class="hero-subtitle">{{ subtitle }}</p>

      <div class="hero-meta">
        <span class="hero-chip">{{ greetingText }}</span>
        <span class="hero-chip">欢迎回来，{{ nickname }}</span>
        <span v-if="showStreak" class="hero-chip hero-streak" :class="{ 'streak-done': signedToday }">
          <span>🔥 连续</span>
          <strong>{{ streakDays }}</strong>
          <span>天</span>
          <span class="streak-status">{{ signedToday ? '✓ 已签到' : '今日未签' }}</span>
        </span>
      </div>

      <div class="hero-actions">
        <div class="btn-wrap">
          <button
            type="button"
            class="btn-primary hero-btn-primary"
            @click="$emit('primary')"
          >
            💖 开始我的成长路径
          </button>
          <div class="btn-chin">Level Up! 📈</div>
        </div>
        <button
          type="button"
          class="btn-ghost hero-btn-secondary"
          @click="$emit('secondary')"
        >
           💰 请作者喝杯咖啡
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

defineProps({
  eyebrow: {
    type: String,
    default: 'NOVALEAP AI LEARNING PLATFORM',
  },
  title: {
    type: String,
    default: 'NovaLeap',
  },
  subtitle: {
    type: String,
    required: true,
  },
  greetingText: {
    type: String,
    required: true,
  },
  nickname: {
    type: String,
    default: '学习者',
  },
})

defineEmits(['primary', 'secondary'])

const showStreak = computed(() => {
  const c = authStore.user?.checkin
  return c && typeof c.streakDays === 'number' && c.streakDays >= 0
})

const streakDays = computed(() => authStore.user?.checkin?.streakDays ?? 0)

const signedToday = computed(() => !!authStore.user?.checkin?.signedToday)
</script>

<style scoped>
.hero-shell {
  position: relative;
  min-height: calc(92vh - 132px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 0 12px;
}

.hero-copy {
  max-width: 980px;
  text-align: center;
}

.hero-eyebrow,
.hero-title,
.hero-subtitle,
.hero-meta,
.hero-actions {
  opacity: 0;
  transform: translateY(18px);
  animation: heroIn 820ms cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.hero-title {
  animation-delay: 120ms;
}

.hero-subtitle {
  animation-delay: 240ms;
}

.hero-meta {
  animation-delay: 360ms;
}

.hero-actions {
  animation-delay: 480ms;
}

.hero-eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.34em;
  color: var(--text-tertiary);
}

.hero-title {
  margin-top: 22px;
  margin-bottom: 10px;
  display: inline-block;
  position: relative;
  overflow: hidden;
  font-size: clamp(72px, 15.5vw, 178px);
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: -0.08em;
  color: #000000;
  text-shadow: 0 0 24px rgba(17, 17, 17, 0.06);
  padding-bottom: 0.12em;
  padding-right: 0.05em;
  text-rendering: geometricPrecision;
  animation: heroIn 820ms cubic-bezier(0.22, 1, 0.36, 1) 120ms forwards;
}

.hero-title::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  color: transparent;
  -webkit-text-fill-color: transparent;
  background: linear-gradient(
    110deg,
    transparent 40%,
    rgba(255, 255, 255, 0.52) 42%,
    rgba(255, 255, 255, 0.88) 50%,
    rgba(255, 255, 255, 0.52) 58%,
    transparent 60%
  );
  background-size: 200% 100%;
  background-repeat: no-repeat;
  -webkit-background-clip: text;
  background-clip: text;
  pointer-events: none;
  animation: logoSweep 5s cubic-bezier(0.25, 1, 0.5, 1) infinite;
}

.hero-subtitle {
  margin: 4px auto 0;
  max-width: 760px;
  font-size: clamp(18px, 2.2vw, 24px);
  line-height: 1.9;
  color: var(--text-secondary);
}

.hero-meta {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  padding: 0 16px;
  font-size: 13px;
  color: var(--text-secondary);
  backdrop-filter: blur(8px);
}

.hero-streak {
  gap: 4px;
  background: rgba(255, 241, 224, 0.88);
  border-color: rgba(255, 181, 77, 0.28);
  color: #b45309;
}

.hero-streak strong {
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  color: #c2410c;
}

.hero-streak .streak-status {
  font-size: 11px;
  opacity: 0.75;
}

.hero-streak.streak-done {
  background: rgba(220, 252, 231, 0.88);
  border-color: rgba(74, 222, 128, 0.28);
  color: #15803d;
}

.hero-streak.streak-done strong {
  color: #166534;
}

.hero-actions {
  margin-top: 36px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
}

.btn-wrap {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.btn-chin {
  position: absolute;
  top: 100%;
  left: 60%;
  transform: translateX(-50%) translateY(-6px) scale(0.8) rotate(-2deg);
  margin-top: 8px;
  background: linear-gradient(135deg, #272235, #3a2a4d);
  color: #ffde59;
  padding: 4px 14px;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.btn-wrap:hover .btn-chin {
  opacity: 1;
  transform: translateX(-50%) translateY(0) scale(1) rotate(-6deg);
}

.btn-wrap:hover .hero-btn-primary {
  animation: heartPulse 1.2s cubic-bezier(0.12, 0, 0.39, 0) infinite;
  box-shadow: 0 10px 40px 0 rgba(0, 0, 0, 0.2);
}

.hero-btn-secondary:hover {
  animation: heartPulse 1.2s cubic-bezier(0.12, 0, 0.39, 0) infinite;
  box-shadow: 0 10px 40px 0 rgba(0, 0, 0, 0.2);
}

@keyframes heartPulse {
  0% { transform: scale(1); }
  10% { transform: scale(1.1); }
  20% { transform: scale(1); }
  30% { transform: scale(1.06); }
  45% { transform: scale(1); }
}

.hero-btn-primary,
.hero-btn-secondary {
  min-width: 210px;
  min-height: 52px;
  border-radius: var(--radius-lg);
}

.hero-btn-primary {
  background: linear-gradient(135deg, var(--ai-from), var(--ai-to));
  box-shadow: 
    0 14px 28px rgba(0, 0, 0, 0.1),
    0 4px 10px rgba(239, 114, 134, 0.16);

    /* 新增：定义边缘和悬浮感 */
  border: 1px solid rgba(255, 255, 255, 0.2); /* 玻璃边缘光泽 */
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.1); /* 柔和深度的阴影 */
  transition: all 0.3s ease; /* 增加平滑过渡 */

}

.hero-btn-secondary {
  
  background: var(--bg-soft);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

  /* 新增：定义边缘和悬浮感 */
  border: 1px solid rgba(255, 255, 255, 0.2); /* 玻璃边缘光泽 */
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.1); /* 柔和深度的阴影 */
  transition: all 0.3s ease; /* 增加平滑过渡 */
}

@keyframes heroIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes logoSweep {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -100% 0;
  }
}

.dark .hero-chip,
.dark .hero-btn-secondary {
  background: var(--bg-soft);
  border-color: var(--border-soft);
}

.dark .hero-streak {
  background: rgba(120, 53, 15, 0.25);
  border-color: rgba(251, 191, 36, 0.18);
  color: #fbbf24;
}

.dark .hero-streak strong {
  color: #fcd34d;
}

.dark .hero-streak.streak-done {
  background: rgba(22, 101, 52, 0.25);
  border-color: rgba(74, 222, 128, 0.18);
  color: #86efac;
}

.dark .hero-streak.streak-done strong {
  color: #4ade80;
}

.dark .hero-title {
  color: #e4c97a;
  text-shadow: 0 0 34px rgba(212, 171, 84, 0.2);
}

@media (max-width: 640px) {
  .hero-shell {
    min-height: auto;
    padding-top: 42px;
  }
}
</style>
