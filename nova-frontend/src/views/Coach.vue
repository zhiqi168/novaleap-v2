<template>
  <div class="h-full relative overflow-hidden coach-surface workspace-page workspace-scroll">
    <div class="workspace-stack relative z-10 h-full flex flex-col">
      <header class="pb-3">
        <div class="workspace-titlebar coach-titlebar-inner px-4 py-3 sm:px-5 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 class="text-2xl font-bold text-text-primary leading-tight">知跃陪练</h1>
            <p class="text-sm text-text-secondary mt-1">技术问题、状态困惑、日常想法都可以聊，我会给你可执行支持</p>
          </div>

          <div class="flex items-center gap-2 flex-wrap">
            <button
              type="button"
              class="workspace-btn workspace-btn-muted px-3 py-1.5 rounded-lg text-xs"
              @click="newConversation"
            >
              新建对话
            </button>
            <button
              type="button"
              class="workspace-btn workspace-btn-muted px-3 py-1.5 rounded-lg text-xs"
              @click="loadHistory"
            >
              刷新历史
            </button>
            <button
              type="button"
              class="workspace-btn workspace-btn-danger px-3 py-1.5 rounded-lg text-xs"
              @click="clearHistory"
            >
              清空历史
            </button>
          </div>
        </div>
      </header>

      <div
        ref="chatContainer"
        class="flex-1 overflow-y-auto custom-scrollbar pb-48 scroll-smooth"
      >
        <div
          class="mx-auto max-w-[1080px]"
          :class="showStarter ? 'pt-14 sm:pt-16' : 'pt-6 sm:pt-8'"
        >
          <section v-if="showStarter" class="mb-8 grid grid-cols-1 md:grid-cols-3 gap-3">
            <button
              v-for="item in quickPrompts"
              :key="item"
              class="workspace-panel text-left rounded-2xl p-4 hover:border-ai-from/35 hover:bg-bg-elevated transition-colors"
              @click="useQuickPrompt(item)"
            >
              <p class="text-xs text-text-secondary">快速开始</p>
              <p class="mt-1.5 text-sm text-text-primary leading-relaxed">{{ item }}</p>
            </button>
          </section>

          <div class="flex flex-col space-y-6">
            <template v-for="(msg, index) in messages" :key="index">
              <div v-if="msg.role === 'user'" class="flex max-w-[920px] gap-3.5 ml-auto flex-row-reverse">
                <div class="avatar-shell user-avatar">
                  <span v-if="isEmoji(displayAvatar)" class="emoji-avatar">{{ displayAvatar }}</span>
                  <img v-else :src="displayAvatar" alt="user-avatar" class="w-full h-full object-cover" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="coach-user-bubble text-white p-4 sm:p-5 rounded-[24px] rounded-tr-[9px]">
                    <img
                      v-if="msg.image"
                      :src="msg.image"
                      class="w-full max-w-sm aspect-auto object-cover rounded-xl mb-2.5 ring-1 ring-white/28 shadow-sm"
                    />
                    <p class="text-sm leading-relaxed text-white/95 whitespace-pre-wrap">{{ msg.content }}</p>
                  </div>
                </div>
              </div>

              <div v-else class="flex max-w-[940px] gap-3.5">
                <div class="avatar-shell ai-avatar">
                  <img
                    :src="coachAvatarSrc"
                    alt="ai-avatar"
                    class="w-full h-full object-cover"
                    loading="eager"
                    decoding="async"
                  />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="bg-bg-elevated backdrop-blur-xl p-4 sm:p-5 rounded-[24px] rounded-tl-[9px] shadow-[0_22px_50px_-36px_rgba(15,23,42,0.42)] ring-1 ring-border-light border border-border-subtle">
                    <div class="prose prose-sm max-w-none prose-p:leading-relaxed dark:prose-invert">
                      <TypeWriter :text="msg.content" :renderMarkdown="true" :isTyping="msg.isTyping" />
                    </div>

                    <!-- 题目卡片 -->
                    <div v-if="msg.questions && msg.questions.length > 0" class="mt-4 space-y-2">
                      <div class="text-xs font-semibold text-text-secondary mb-1">相关题目：</div>
                      <div
                        v-for="q in msg.questions"
                        :key="q.id"
                        class="rounded-xl border border-ai-from/20 bg-white dark:bg-bg-card p-3 cursor-pointer hover:shadow-md transition-all hover:border-ai-from/40 active:scale-[0.98]"
                        @click="goToQuestion(q.id)"
                      >
                        <div class="flex items-center gap-2">
                          <span class="text-[11px] px-1.5 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 font-medium">{{ q.category || '未分类' }}</span>
                          <span class="text-[11px] px-1.5 py-0.5 rounded-full font-medium" :class="difficultyBadgeClass(q.difficulty)">
                            {{ ['未知', '简单', '中等', '困难'][q.difficulty] || '未知' }}
                          </span>
                        </div>
                        <p class="mt-1.5 text-sm font-medium text-text-primary leading-snug">{{ q.title }}</p>
                      </div>
                    </div>

                    <!-- 笔记卡片 -->
                    <div v-if="msg.notes && msg.notes.length > 0" class="mt-3 space-y-2">
                      <div class="text-xs font-semibold text-text-secondary mb-1">相关笔记：</div>
                      <div
                        v-for="n in msg.notes"
                        :key="n.id"
                        class="rounded-xl border border-amber-200/60 bg-amber-50/60 dark:bg-amber-900/20 dark:border-amber-700/30 p-3 cursor-pointer hover:shadow-md transition-all hover:border-amber-300/80 active:scale-[0.98]"
                        @click="goToNote(n.id)"
                      >
                        <p class="mt-0 text-sm font-medium text-text-primary leading-snug">{{ n.title }}</p>
                        <p v-if="n.summary" class="mt-1 text-xs text-text-tertiary line-clamp-2">{{ n.summary }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <div class="h-20 sm:h-24"></div>
          </div>
        </div>
      </div>

      <div class="absolute left-0 right-0 bottom-0 px-4 sm:px-6 lg:px-10 pb-6 lg:pb-8">
        <div class="max-w-[1080px] mx-auto">
          <div class="workspace-shell relative flex items-end gap-3 rounded-[28px] border border-border-subtle bg-bg-surface backdrop-blur-xl px-3 sm:px-4 py-2 shadow-float">
            <div
              v-if="uploadImage"
              class="absolute -top-16 left-4 bg-bg-card backdrop-blur-sm p-1 rounded-xl shadow-md ring-1 ring-border-soft flex items-center gap-1.5 z-10 animate-fade-in"
            >
              <img :src="uploadImage" class="w-12 h-12 object-cover rounded-lg" />
              <button @click="uploadImage = null" class="coach-inline-icon p-1 transition-colors">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="onFileChange" />

            <button
              class="coach-upload-trigger p-2 mb-1 rounded-full transition-colors shrink-0"
              @click="fileInput?.click()"
            >
              <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </button>

            <textarea
              v-model="inputMessage"
              @keydown.enter.prevent="sendMessage"
              @paste="handlePaste"
              rows="1"
              class="flex-1 w-full py-3.5 sm:py-4 bg-transparent outline-none text-text-primary text-sm sm:text-base resize-none max-h-32 min-h-[54px] custom-scrollbar placeholder:text-text-tertiary"
              :placeholder="inputPlaceholder"
            ></textarea>

            <div class="flex items-center gap-2 mb-1.5 shrink-0">
              <button
                class="p-2 rounded-full transition-colors hidden sm:block"
                :class="isRecording ? 'coach-record-trigger coach-record-trigger-active animate-pulse' : 'coach-record-trigger'"
                @click="toggleSpeech"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                </svg>
              </button>

              <button
                class="workspace-btn workspace-btn-primary coach-send-btn text-white px-5 sm:px-6 py-2.5 rounded-xl sm:rounded-2xl font-medium transition-transform active:scale-95 flex items-center gap-2 mr-1 disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="(!inputMessage.trim() && !uploadImage) && !isBusy"
                @click="isBusy ? abort() : sendMessage()"
              >
                <template v-if="isBusy">
                  停止生成
                </template>
                <template v-else>
                  发送
                  <svg class="w-4 h-4 hidden sm:block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                  </svg>
                </template>
              </button>
            </div>
          </div>

          <div class="coach-disclaimer text-center mt-3 text-[10px]">AI 可能会产生偏差，请批判性参考建议。</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TypeWriter from '@/components/common/TypeWriter.vue'
import coachAvatarSrc from '@/assets/logo.png'
import { useAutoPageRefresh } from '@/composables/useAutoPageRefresh'
import { useSSE } from '@/composables/useSSE'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const chatContainer = ref(null)
const fileInput = ref(null)
const inputMessage = ref('')
const chatMode = ref('buddy')
const uploadImage = ref(null)
const isRecording = ref(false)

const messages = ref([buildWelcome()])
const { content, isStreaming, error, startStream, abort, reset } = useSSE()
const loading = ref(false)
const isBusy = computed(() => isStreaming.value || loading.value)

let recognition = null

const displayAvatar = computed(() => authStore.avatar || '🥳')
const showStarter = computed(() => messages.value.length <= 1 && !isBusy.value)
const currentTopic = computed(() => '技术成长与日常聊天')
const inputPlaceholder = computed(() => '输入你的技术问题、项目难点，或聊聊近况...(Enter 发送)')
const quickPrompts = computed(() => [
  '帮我设计一个高并发系统的分层架构',
  '给我做一轮项目复盘式面试追问',
  '今天有点焦虑，帮我梳理下怎么恢复状态',
  '我总觉得自己进步慢，给我一个可执行的周计划',
  '这段方案哪里风险最大？怎么验证？',
])

function isEmoji(value) {
  return typeof value === 'string' && !value.startsWith('http') && !value.startsWith('data:image/')
}

function buildWelcome() {
  return {
    role: 'ai',
    content: '你好，我是知跃。技术问题和日常状态都可以聊，我会先接住你，再给你可执行的下一步。',
    isTyping: false,
  }
}

function useQuickPrompt(text) {
  inputMessage.value = text
}

const readAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (event) => resolve(event.target?.result || null)
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })

const compressImage = async (file) => {
  const dataUrl = await readAsDataUrl(file)
  if (!dataUrl || typeof dataUrl !== 'string') {
    return null
  }

  return await new Promise((resolve) => {
    const image = new Image()
    image.onload = () => {
      const maxSide = 1280
      const ratio = Math.min(1, maxSide / Math.max(image.width, image.height))
      const width = Math.max(1, Math.floor(image.width * ratio))
      const height = Math.max(1, Math.floor(image.height * ratio))
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const context = canvas.getContext('2d')
      if (!context) {
        resolve(dataUrl)
        return
      }
      context.drawImage(image, 0, 0, width, height)
      resolve(canvas.toDataURL('image/jpeg', 0.86))
    }
    image.onerror = () => resolve(dataUrl)
    image.src = dataUrl
  })
}

const onFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  uploadImage.value = await compressImage(file)
}

const handlePaste = async (event) => {
  const items = event.clipboardData?.items || []
  for (const item of items) {
    if (!item.type.includes('image')) continue
    const file = item.getAsFile()
    if (!file) break
    uploadImage.value = await compressImage(file)
    break
  }
}

const toggleSpeech = () => {
  if (isRecording.value) {
    recognition?.stop()
    return
  }

  const Speech = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!Speech) {
    alert('当前浏览器或环境不支持语音识别。')
    return
  }

  recognition = new Speech()
  recognition.continuous = false
  recognition.interimResults = true
  recognition.lang = 'zh-CN'

  recognition.onstart = () => {
    isRecording.value = true
  }
  recognition.onresult = (e) => {
    inputMessage.value = Array.from(e.results)
      .map((result) => result[0].transcript)
      .join('')
  }
  recognition.onerror = () => {
    isRecording.value = false
  }
  recognition.onend = () => {
    isRecording.value = false
  }
  recognition.start()
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

watch(content, (newVal) => {
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg?.role === 'ai') {
    lastMsg.content = newVal || '我在整理思路...'
    scrollToBottom()
  }
})

watch(isStreaming, (streaming) => {
  if (streaming) return
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg?.role === 'ai') {
    lastMsg.isTyping = false
  }
})

watch(error, (err) => {
  if (!err) return
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg?.role === 'ai') {
    lastMsg.content = `${lastMsg.content || ''}\n\n> [请求失败：${err}]`
    lastMsg.isTyping = false
  }
})

const sendMessage = async () => {
  const msg = inputMessage.value.trim()
  if ((!msg) || isBusy.value) {
    return
  }

  messages.value.push({
    role: 'user',
    content: msg,
    image: null,
  })
  inputMessage.value = ''
  await scrollToBottom()

  messages.value.push({
    role: 'ai',
    content: '让我想想...',
    isTyping: true,
    questions: [],
    notes: [],
  })
  await scrollToBottom()

  loading.value = true
  try {
    const res = await api.post('/api/ai/agent/chat', { message: msg })
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg?.role === 'ai' && res?.code === 200) {
      lastMsg.content = res.data?.answer || '抱歉，没得到有效回复。'
      lastMsg.questions = res.data?.questions || []
      lastMsg.notes = res.data?.notes || []
      // 保持 isTyping = true，让 TypeWriter 自动逐字显示
      // 用户看到打字效果后，等文本全部显示完毕会自动停止光标闪烁
    } else if (lastMsg?.role === 'ai') {
      lastMsg.content = '请求失败，请稍后重试。'
    }
  } catch (e) {
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg?.role === 'ai') {
      lastMsg.content = '请求失败，请稍后重试。'
    }
  }
  loading.value = false
  await scrollToBottom()
  await scrollToBottom()
}

const goToQuestion = (questionId) => {
  router.push({ name: 'QuestionBank', query: { highlight: questionId } })
}

const goToNote = (noteId) => {
  router.push({ name: 'Notes', query: { highlight: noteId } })
}

const difficultyBadgeClass = (diff) => {
  if (diff === 1) return 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300'
  if (diff === 2) return 'bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300'
  if (diff === 3) return 'bg-rose-100 dark:bg-rose-900/40 text-rose-700 dark:text-rose-300'
  return 'bg-gray-100 dark:bg-gray-800 text-gray-500'
}

const loadHistory = async () => {
  try {
    const res = await api.get('/api/ai/coach/history?limit=40')
    if (res.code !== 200 || !Array.isArray(res.data)) {
      return
    }

    if (res.data.length === 0) {
      if (messages.value.length === 0) {
        messages.value = [buildWelcome()]
      }
      return
    }
    const restored = []
    const ordered = [...res.data].reverse()
    for (const row of ordered) {
      // Current backend format: one record per message { role, content, ... }
      const role = String(row?.role || '').trim().toLowerCase()
      const content = String(row?.content || '').trim()
      if ((role === 'user' || role === 'assistant' || role === 'ai') && content) {
        restored.push({
          role: role === 'user' ? 'user' : 'ai',
          content,
          image: null,
          isTyping: false,
          questions: row.questions || [],
          notes: row.notes || [],
        })
        continue
      }

      // Legacy fallback: pair format { message, answer, hasImage }
      const userText = String(row?.message || '').trim()
      const aiText = String(row?.answer || '').trim()
      if (userText) {
        restored.push({ role: 'user', content: userText, image: null })
      }
      if (aiText) {
        restored.push({ role: 'ai', content: aiText, isTyping: false })
      }
    }

    if (restored.length === 0) {
      messages.value = [buildWelcome()]
      return
    }

    messages.value = [
      {
        role: 'ai',
        content: '已恢复最近对话记录，我们可以接着聊。',
        isTyping: false,
      },
      ...restored,
    ]
    await scrollToBottom()
  } catch (_) {
    // keep current panel usable when history fetch fails
  }
}

const newConversation = async () => {
  if (isStreaming.value) {
    abort()
  }
  try {
    await api.post('/api/ai/coach/session/new')
  } catch (_) {
    // keep the panel usable even when new-session API is temporarily unavailable
  }
  reset()
  messages.value = [buildWelcome()]
  inputMessage.value = ''
  uploadImage.value = null
  await scrollToBottom()
}

const clearHistory = async () => {
  if (!confirm('确定要清空当前对话历史吗？')) {
    return
  }
  try {
    await api.delete('/api/ai/coach/history')
  } catch (_) {
    // ignore clear failure
  }
  messages.value = [buildWelcome()]
  inputMessage.value = ''
  uploadImage.value = null
}

useAutoPageRefresh(async () => {
  if (!isStreaming.value) {
    await loadHistory()
  }
}, {
  throttleMs: 5000,
})

onMounted(() => {
  loadHistory()
})

onUnmounted(() => {
  abort()
})
</script>

<style scoped>
.coach-surface {
  background: var(--app-shell-bg);
}

.dark .coach-surface {
  background: var(--app-shell-bg);
}

.coach-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(56px);
}

.coach-glow-a {
  left: -96px;
  top: -80px;
  width: 320px;
  height: 320px;
  background: var(--module-glow-a);
}

.coach-glow-b {
  right: -100px;
  top: -70px;
  width: 380px;
  height: 380px;
  background: var(--module-glow-c);
}

@keyframes coachFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(12px); }
}

.coach-float {
  animation: coachFloat 8s ease-in-out infinite;
}

.coach-float-delayed {
  animation: coachFloat 10s ease-in-out infinite reverse;
}

.mode-pill {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
  transition: all 0.2s ease;
}

.mode-pill:hover {
  color: var(--text-primary);
  border-color: var(--accent-border);
}

.mode-pill.active {
  color: var(--primary);
  border-color: var(--accent-border);
  background: var(--accent-soft);
}

.avatar-shell {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--border-soft);
  box-shadow: 0 10px 20px -15px rgba(15, 23, 42, 0.5);
  display: grid;
  place-items: center;
}

.user-avatar {
  background: var(--surface-panel-soft);
}

.ai-avatar {
  background: var(--accent-soft);
}

.emoji-avatar {
  font-size: 26px;
  line-height: 1;
}

.coach-user-bubble {
  background: linear-gradient(135deg, var(--ai-from), var(--ai-to));
  box-shadow: 0 20px 44px -30px rgba(var(--primary-rgb), 0.68);
}

.coach-inline-icon {
  color: var(--text-tertiary);
}

.coach-inline-icon:hover {
  color: var(--danger);
}

.coach-upload-trigger {
  color: var(--text-tertiary);
}

.coach-upload-trigger:hover {
  color: var(--primary);
  background: var(--accent-soft);
}

.coach-record-trigger {
  color: var(--text-tertiary);
}

.coach-record-trigger:hover {
  color: var(--text-primary);
}

.coach-record-trigger-active {
  color: var(--danger);
  background: var(--danger-soft);
}

.coach-send-btn {
  background: linear-gradient(135deg, var(--ai-from), var(--ai-to));
  box-shadow: 0 14px 28px -16px rgba(var(--primary-rgb), 0.75);
}

.coach-disclaimer {
  color: var(--text-tertiary);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 999px;
}
</style>
