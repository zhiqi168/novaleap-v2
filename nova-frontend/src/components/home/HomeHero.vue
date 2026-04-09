<template>
  <section class="hero-shell">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ eyebrow }}</p>

      <h1 class="hero-title">{{ title }}</h1>

      <p class="hero-subtitle">{{ subtitle }}</p>

      <div class="hero-meta">
        <span class="hero-chip">{{ greetingText }}</span>
        <span class="hero-chip">欢迎回来，{{ nickname }}</span>
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
  font-size: clamp(72px, 15.5vw, 178px);
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: -0.08em;
  color: transparent;
  background-image: 
    linear-gradient(110deg, transparent 40%, rgba(255, 255, 255, 0.45) 49%, rgba(255, 255, 255, 0.45) 51%, transparent 60%),
    linear-gradient(180deg, #272235 0%, #1a1624 100%);
  background-size: 280% 100%, 100% 100%;
  background-position: -140% 0, 0 0;
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow: 0 0 42px rgba(39, 34, 53, 0.04);
  padding-bottom: 0.12em;
  padding-right: 0.05em;
  text-rendering: geometricPrecision;
  animation: 
    logoShimmer 5s infinite linear,
    heroIn 820ms cubic-bezier(0.22, 1, 0.36, 1) 120ms forwards;
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

.dark .hero-chip,
.dark .hero-btn-secondary {
  background: var(--bg-soft);
  border-color: var(--border-soft);
}

.dark .hero-title {
  background-image: 
    linear-gradient(110deg, transparent 40%, rgba(255, 255, 255, 0.16) 49%, rgba(255, 255, 255, 0.16) 51%, transparent 60%),
    linear-gradient(180deg, #e4c97a 0%, #cda24a 100%);
  background-size: 280% 100%, 100% 100%;
  background-position: -140% 0, 0 0;
  text-shadow: 0 0 34px rgba(212, 171, 84, 0.2);
}

@keyframes logoShimmer {
  0% { background-position: -140% 0, 0 0; }
  100% { background-position: 140% 0, 0 0; }
}

@media (max-width: 640px) {
  .hero-shell {
    min-height: auto;
    padding-top: 42px;
  }
}
</style>
