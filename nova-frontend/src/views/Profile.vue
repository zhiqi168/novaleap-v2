<template>
  <div class="profile-bg workspace-page workspace-scroll h-full bg-bg-base transition-colors duration-300">
    <div class="workspace-stack">
      <header class="workspace-titlebar">
        <div class="workspace-titlecopy">
          <h1 class="workspace-title">个人资料</h1>
          <p class="workspace-subtitle">统一维护昵称、密码、头像和个人标签，保持你在 NovaLeap 各模块中的身份一致性。</p>
        </div>
      </header>

      <div class="workspace-shell mt-5 p-7 sm:p-9">

      <div class="grid grid-cols-1 lg:grid-cols-[340px_1fr] gap-8">
        <section class="workspace-panel p-5 sm:p-6">
          <div class="avatar-preview" @mousemove="onAvatarMove" @mouseleave="resetAvatarTilt">
            <div class="avatar-inner" :style="avatarTransformStyle">{{ selectedAvatar }}</div>
          </div>

          <h2 class="mt-6 text-[20px] font-semibold text-text-primary">选择头像</h2>
          <div class="avatar-grid mt-4">
            <button
              v-for="avatar in avatarOptions"
              :key="avatar"
              type="button"
              class="avatar-item workspace-list-item"
              :class="{ active: selectedAvatar === avatar }"
              @click="selectedAvatar = avatar"
            >
              {{ avatar }}
            </button>
          </div>
          <p class="text-xs text-text-tertiary mt-3">仅支持选择表情头像，暂不支持自定义上传。</p>
        </section>

        <section class="workspace-panel p-5 sm:p-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <label class="field">
              <span>用户名</span>
              <input class="workspace-control" :value="profile.username" disabled />
            </label>
            <label class="field">
              <span>注册方式</span>
              <input class="workspace-control" :value="accountLabel" disabled />
            </label>
            <label class="field">
              <span>昵称</span>
              <input class="workspace-control" v-model.trim="form.nickname" placeholder="请输入昵称" />
            </label>
            <label class="field">
              <span>新密码</span>
              <input class="workspace-control" v-model="form.password" type="password" placeholder="不修改则留空" />
            </label>
          </div>

          <div class="mt-7">
            <h3 class="text-[20px] font-semibold text-text-primary mb-3">我的标签</h3>
            <div class="tag-wall">
              <button
                v-for="tag in tags"
                :key="tag"
                type="button"
                class="tag-item workspace-chip"
                :class="{ active: selectedTags.includes(tag) }"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
          </div>

          <div class="mt-8 flex items-center gap-3">
            <button class="workspace-btn workspace-btn-primary save-btn" :disabled="saving" @click="saveProfile">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
            <span v-if="tip" class="text-sm" :class="tipOk ? 'text-emerald-500' : 'text-red-500'">{{ tip }}</span>
          </div>
        </section>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAutoPageRefresh } from '@/composables/useAutoPageRefresh'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const saving = ref(false)
const tip = ref('')
const tipOk = ref(true)

const avatarOptions = [
  '🥳', '😃', '🤗', '😅', '😉',
  '🫡', '😊', '😏', '🤩', '😎',
  '😁', '🥸', '🧐', '🙅', '😆',
  '😺', '😈', '😌', '🥰', '🤓',
  '😒', '😍', '🥹', '😷', '🤔',
  '😄', '😀', '😋', '🙌', '😇',
]

const tags = [
  '刷题达人', '数学爱好者', '喜欢挑战', '错题收藏家', '解题速度快',
  '喜欢分享', '逻辑思维强', '喜欢竞赛', '夜猫子', '早起鸟',
  '自律王者', '打卡狂魔', '喜欢组队', '目标清晰', '随缘做题',
  '喜欢聊天', '喜欢用草稿纸', '喜欢朋友讨论', '喜欢做笔记',
]

const profile = reactive({
  username: '',
  nickname: '',
  role: '',
  accountType: '',
  account: '',
  avatar: '🥳',
})

const form = reactive({
  nickname: '',
  password: '',
})

const selectedAvatar = ref('🥳')
const selectedTags = ref([])
const tiltX = ref(0)
const tiltY = ref(0)

const avatarTransformStyle = computed(
  () => `translateZ(16px) rotateX(${tiltX.value}deg) rotateY(${tiltY.value}deg)`
)

const accountLabel = computed(() => {
  if (!profile.accountType) return '-'
  if (profile.accountType === 'email') return `邮箱：${profile.account || '-'}`
  if (profile.accountType === 'phone') return `手机：${profile.account || '-'}`
  if (profile.accountType === 'qq') return `QQ：${profile.account || '-'}`
  if (profile.accountType === 'guest') return '游客'
  return `账号：${profile.account || '-'}`
})

const toggleTag = (tag) => {
  if (selectedTags.value.includes(tag)) {
    selectedTags.value = selectedTags.value.filter((t) => t !== tag)
  } else {
    selectedTags.value = [...selectedTags.value, tag]
  }
}

const onAvatarMove = (event) => {
  const element = event.currentTarget
  if (!element) return
  const rect = element.getBoundingClientRect()
  const relativeX = (event.clientX - rect.left) / rect.width
  const relativeY = (event.clientY - rect.top) / rect.height
  tiltY.value = (relativeX - 0.5) * 14
  tiltX.value = (0.5 - relativeY) * 14
}

const resetAvatarTilt = () => {
  tiltX.value = 0
  tiltY.value = 0
}

const loadProfile = async () => {
  const res = await api.get('/api/auth/profile')
  if (res.code !== 200) throw new Error(res.msg || '资料加载失败')

  const data = res.data || {}
  Object.assign(profile, data)
  form.nickname = data.nickname || ''

  const incomingAvatar = String(data.avatar || '').trim()
  selectedAvatar.value = avatarOptions.includes(incomingAvatar) ? incomingAvatar : '🥳'

  const tagCache = JSON.parse(localStorage.getItem('nova_profile_tags') || '[]')
  if (Array.isArray(tagCache)) {
    selectedTags.value = tagCache.filter((t) => tags.includes(t))
  }
}

const saveProfile = async () => {
  saving.value = true
  tip.value = ''

  try {
    const payload = {
      nickname: form.nickname,
      avatar: selectedAvatar.value,
    }
    if (form.password) payload.password = form.password

    const res = await api.put('/api/auth/profile', payload)
    if (res.code !== 200) throw new Error(res.msg || '保存失败')

    const data = res.data || {}
    Object.assign(profile, data)
    form.password = ''

    const nextNickname = data.nickname || form.nickname
    const nextAvatar = data.avatar || selectedAvatar.value
    authStore.patchUser({
      nickname: nextNickname,
      avatar: nextAvatar,
    })

    localStorage.setItem('nova_profile_tags', JSON.stringify(selectedTags.value))
    tipOk.value = true
    tip.value = '保存成功'
  } catch (e) {
    tipOk.value = false
    tip.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

useAutoPageRefresh(async () => {
  try {
    await loadProfile()
  } catch (e) {
    tipOk.value = false
    tip.value = e.message || '资料加载失败'
  }
}, {
  throttleMs: 4000,
})

onMounted(async () => {
  try {
    await loadProfile()
  } catch (e) {
    tipOk.value = false
    tip.value = e.message || '资料加载失败'
  }
})
</script>

<style scoped>
.profile-bg {
  background: var(--app-shell-bg);
}

.avatar-preview {
  width: 128px;
  height: 128px;
  border-radius: 9999px;
  border: 2px solid var(--border-subtle);
  display: grid;
  place-items: center;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
  background: var(--bg-elevated);
  perspective: 480px;
  transform-style: preserve-3d;
}

.avatar-inner {
  font-size: 80px;
  line-height: 1;
  transition: transform 0.18s ease;
  animation: avatarPulse 2.8s ease-in-out infinite;
  will-change: transform;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.avatar-item {
  height: 42px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1;
  transition: all 0.2s ease;
}

.avatar-item.active {
  border-color: var(--accent-border);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.18);
  transform: translateY(-1px);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field span {
  font-size: 15px;
  color: var(--text-secondary);
}

.field input {
  height: 46px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
  color: var(--text-primary);
  padding: 0 12px;
  outline: none;
}

.field input:focus {
  border-color: var(--accent-border);
  box-shadow: 0 0 0 4px rgba(var(--primary-rgb), 0.15);
}

.field input:disabled {
  background: var(--bg-base);
  color: var(--text-tertiary);
  opacity: 0.7;
}

.tag-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  border: 1px dashed var(--border-subtle);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-elevated);
}

.tag-item.active {
  border-style: solid;
  border-color: var(--ai-from);
  background: rgba(var(--ai-from-rgb, 147, 51, 234), 0.1);
  color: var(--ai-from);
}

.save-btn {
  min-width: 130px;
  height: 44px;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  background: linear-gradient(135deg, var(--ai-from), var(--ai-to));
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes avatarPulse {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-4px) rotate(-2deg);
  }
}
</style>
