<template>
  <div class="wish-wall-shell workspace-page h-full flex flex-col relative overflow-hidden select-none">
    <div class="absolute top-0 left-0 right-0 p-3 sm:p-6 md:p-8 z-[30] md:z-[100] pointer-events-none">
      <div class="workspace-titlebar wish-titlebar max-w-[360px] sm:max-w-3xl w-[76vw] sm:w-auto p-3 sm:p-4 inline-block shadow-sm ring-1 ring-border-subtle">
        <h1 class="text-2xl font-bold text-text-primary flex items-center gap-3 mb-2">
          星愿墙
          <span class="px-2 py-0.5 rounded text-xs bg-bg-elevated/40 text-text-secondary">Live</span>
        </h1>
        <p class="text-sm text-text-secondary leading-relaxed mt-1">
          人总要有梦想，万一实现了呢！快来记录你的美好星愿吧！
        </p>
      </div>
    </div>

    <div
      class="flex-1 relative z-10 w-full h-full overflow-hidden cursor-grab active:cursor-grabbing bg-[radial-gradient(var(--border-subtle)_1px,transparent_1px)] [background-size:20px_20px]"
      @mousedown="startPan"
      @mousemove="handleGlobalMove"
      @mouseup="endAllGestures"
      @mouseleave="endAllGestures"
      @touchstart="startPanTouch"
      @touchmove="handleGlobalTouch"
      @touchend="endAllGestures"
    >
      <div
        class="absolute top-0 left-0 w-full h-full transform origin-center will-change-transform"
        :style="{ transform: `translate3d(${panX}px, ${panY}px, 0)` }"
      >
        <div class="wish-layer">
          <div
            v-for="wish in wishes"
            :key="wish.id"
            class="absolute p-5 rounded-2xl border border-white/20 backdrop-blur-md shadow-xl flex flex-col justify-between overflow-hidden hover:shadow-2xl transition-all duration-200 hover:z-[60] will-change-transform"
            :class="{ 'z-[100] scale-105 shadow-2xl': draggingWishId === wish.id }"
            :style="{
              background: wish._color,
              left: `${wish._x}px`,
              top: `${wish._y}px`,
              width: `${cardWidth}px`,
              height: `${cardHeight}px`,
              animation: (enableCardFloat && draggingWishId !== wish.id) ? `float ${3 / wish._speed}s ease-in-out infinite alternate` : 'none',
              cursor: draggingWishId === wish.id ? 'grabbing' : 'grab'
            }"
            @mousedown.stop="startDragWish($event, wish)"
            @touchstart.stop="startDragWishTouch($event, wish)"
          >
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-1.5">
                <div class="w-5 h-5 rounded-full bg-white/45 flex items-center justify-center text-[10px]">✨</div>
                <div class="text-[10px] font-semibold text-black/60">{{ wish.city || '云端坐标' }}</div>
              </div>
              <div class="text-[10px] text-black/40 font-mono">{{ formatDate(wish.createdAt) }}</div>
            </div>

            <div class="flex-1 mt-1">
              <p class="text-black/80 font-medium text-sm leading-relaxed wish-content">
                {{ wish.content }}
              </p>
            </div>

            <div class="mt-3 pt-2 border-t border-black/[0.05] flex items-center justify-between gap-2">
              <div class="px-2 py-0.5 bg-white/45 backdrop-blur-sm rounded-md text-[10px] font-bold text-black/60">
                {{ translateEmotion(wish.emotion) }}
              </div>

              <div class="flex items-center gap-2 pointer-events-auto">
                <button
                  :disabled="authStore.isGuest || !!likingMap[wish.id]"
                  class="wish-action"
                  :class="{ 'wish-action-liked': wish.likedByMe }"
                  @click.stop="toggleLike(wish)"
                >
                  <span>{{ wish.likedByMe ? '❤️' : '🤍' }}</span>
                  <span>{{ wish.likeCount || 0 }}</span>
                </button>
                <button class="wish-action" @click.stop="openComments(wish)">
                  <span>💬</span>
                  <span>{{ wish.commentCount || 0 }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="absolute bottom-4 sm:bottom-10 left-1/2 -translate-x-1/2 z-[30] md:z-[220] w-[calc(100vw-1.5rem)] sm:w-auto flex justify-center">
      <button
        @click="openSubmitDialog"
        :disabled="authStore.isGuest"
        class="workspace-btn workspace-btn-primary w-full sm:w-auto backdrop-blur-xl px-6 sm:px-8 py-3.5 sm:py-4 rounded-full shadow-modal ring-1 ring-black/5 flex items-center justify-center gap-3 text-white font-bold hover:scale-105 transition-all active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:scale-100"
      >
        <span class="text-xl text-ai-from">✨</span>
        发布星愿
        <span class="text-xl text-amber-400">✨</span>
      </button>
    </div>

    <div
      v-if="showSubmitDialog"
      class="fixed inset-0 z-[2000] flex items-center justify-center bg-black/30 backdrop-blur-sm px-4"
      @click.self="showSubmitDialog = false"
    >
      <div class="workspace-shell bg-bg-elevated rounded-2xl shadow-2xl p-8 max-w-md w-full border border-border-subtle">
        <h2 class="text-xl font-bold text-text-primary mb-4">许下你的星愿 ✨</h2>
        <textarea
          v-model="newWishContent"
          rows="4"
          maxlength="200"
          placeholder="写下你的技术梦想、职业目标或学习心愿..."
          class="workspace-control w-full border border-border-subtle bg-bg-surface rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-ai-from/30 focus:border-ai-from outline-none resize-none mb-4 custom-scrollbar text-text-primary placeholder:text-text-tertiary"
        ></textarea>
        <input
          v-model="newWishCity"
          type="text"
          maxlength="40"
          placeholder="你所在的城市（可选）"
          class="workspace-control w-full border border-border-subtle bg-bg-surface rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-ai-from/30 focus:border-ai-from outline-none mb-4 text-text-primary placeholder:text-text-tertiary"
        />
        <div class="flex gap-3">
          <button
            @click="showSubmitDialog = false"
            class="workspace-btn workspace-btn-muted flex-1 px-4 py-3 border border-border-subtle bg-bg-surface rounded-xl text-sm font-medium text-text-secondary hover:bg-bg-elevated transition-colors"
          >
            取消
          </button>
          <button
            @click="submitWish"
            :disabled="submitting || !newWishContent.trim()"
            class="workspace-btn workspace-btn-primary flex-1 px-4 py-3 bg-gradient-to-r from-ai-from to-ai-to text-white rounded-xl text-sm font-medium hover:opacity-90 transition-colors disabled:opacity-50"
          >
            {{ submitting ? '提交中...' : '发布星愿' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="commentPanelVisible"
      class="fixed inset-0 z-[2100] bg-black/25 backdrop-blur-[2px]"
      @click.self="closeComments"
    >
      <aside class="workspace-panel absolute right-0 top-0 h-full w-full max-w-[430px] rounded-none sm:rounded-l-3xl bg-bg-surface backdrop-blur-xl shadow-2xl border-l border-border-subtle flex flex-col">
        <header class="px-5 py-4 border-b border-border-subtle flex items-center justify-between">
          <div>
            <h3 class="text-base font-bold text-text-primary">星愿评论</h3>
            <p class="text-xs text-text-secondary mt-1">{{ activeWish?.content || '' }}</p>
          </div>
          <button class="workspace-btn workspace-btn-muted px-2.5 py-1 rounded-md bg-black/5 hover:bg-black/10 text-sm" @click="closeComments">关闭</button>
        </header>

        <div class="flex-1 overflow-y-auto px-5 py-4 custom-scrollbar">
          <div v-if="loadingComments" class="workspace-empty text-sm text-text-secondary py-8 text-center">评论加载中...</div>
          <div v-else-if="comments.length === 0" class="workspace-empty text-sm text-text-secondary py-8 text-center">还没有评论，来做第一个留言的人吧。</div>
          <div v-else class="space-y-3">
            <div v-for="comment in comments" :key="comment.id" class="workspace-list-item rounded-xl bg-black/[0.03] border border-black/[0.05] p-3">
              <div class="flex items-center justify-between gap-2 mb-1">
                <div class="text-sm font-semibold text-text-primary">
                  {{ comment.nickname || comment.username || '用户' }}
                  <span v-if="comment.mine" class="ml-1 text-[10px] px-1.5 py-0.5 rounded bg-ai-from/15 text-ai-from">我</span>
                </div>
                <div class="text-[11px] text-text-secondary">{{ formatDateTime(comment.createdAt) }}</div>
              </div>
              <p class="text-sm text-text-primary leading-relaxed break-words">{{ comment.content }}</p>
            </div>
          </div>
        </div>

        <footer class="px-5 py-4 border-t border-black/5">
          <div v-if="authStore.isGuest" class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2 mb-3">
            游客账号仅可浏览，点赞和评论请先注册。
          </div>
          <textarea
            v-model="newCommentContent"
            :disabled="authStore.isGuest || submittingComment"
            maxlength="300"
            rows="3"
            placeholder="写下你的评论..."
            class="workspace-control w-full border border-border-subtle bg-bg-surface rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25 resize-none disabled:bg-bg-elevated disabled:text-text-tertiary text-text-primary"
          ></textarea>
          <div class="mt-3 flex items-center justify-between">
            <span class="text-xs text-text-secondary">{{ newCommentContent.length }}/300</span>
            <div class="flex items-center gap-2">
              <button
                v-if="authStore.isGuest"
                class="workspace-btn workspace-btn-muted px-3 py-2 rounded-lg text-sm border border-gray-200 hover:bg-gray-50"
                @click="goRegister"
              >
                去注册
              </button>
              <button
                v-else
                :disabled="submittingComment || !newCommentContent.trim()"
                class="workspace-btn workspace-btn-primary px-3 py-2 rounded-lg text-sm text-white bg-gradient-to-r from-ai-from to-ai-to disabled:opacity-50"
                @click="submitComment"
              >
                {{ submittingComment ? '发送中...' : '发送评论' }}
              </button>
            </div>
          </div>
        </footer>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/composables/useRequest'
import { getVisitorId } from '@/services/analytics'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const panX = ref(0)
const panY = ref(0)
const isPanning = ref(false)
const draggingWishId = ref(null)
const lastMouseX = ref(0)
const lastMouseY = ref(0)

const showSubmitDialog = ref(false)
const submitting = ref(false)
const newWishContent = ref('')
const newWishCity = ref('')

const wishes = ref([])
const likingMap = reactive({})
let pollInterval = null

const commentPanelVisible = ref(false)
const activeWishId = ref(null)
const comments = ref([])
const loadingComments = ref(false)
const submittingComment = ref(false)
const newCommentContent = ref('')
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1280)

const activeWish = computed(() => wishes.value.find((w) => w.id === activeWishId.value) || null)

const colors = [
  'linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%)',
  'linear-gradient(120deg, #e0c3fc 0%, #8ec5fc 100%)',
  'linear-gradient(120deg, #fccb90 0%, #d57eeb 100%)',
  'linear-gradient(to top, #accbee 0%, #e7f0fd 100%)',
  'linear-gradient(to right, #cfd9df 0%, #e2ebf0 100%)',
  'linear-gradient(to top, #fff1eb 0%, #ace0f9 100%)',
  'linear-gradient(135deg, #ffd3a5 0%, #fd6585 100%)',
]

const isMobile = computed(() => viewportWidth.value < 768)
const cardWidth = computed(() => (isMobile.value ? 216 : 272))
const cardHeight = computed(() => (isMobile.value ? 168 : 190))
const cardPadding = computed(() => (isMobile.value ? 14 : 24))
const enableCardFloat = computed(() => !isMobile.value && wishes.value.length <= 24)

const translateEmotion = (emotion) => {
  const map = {
    happy: '欣喜雀跃',
    hopeful: '充满信念',
    confused: '有些迷茫',
    anxious: '焦虑不安',
    determined: '长风破浪',
    confident: '自信满满',
  }
  return map[emotion] || '平静'
}

const formatDate = (value) => {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return d.toLocaleDateString()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return d.toLocaleString()
}

const startPan = (e) => {
  isPanning.value = true
  draggingWishId.value = null
  lastMouseX.value = e.clientX
  lastMouseY.value = e.clientY
}

const startDragWish = (e, wish) => {
  draggingWishId.value = wish.id
  lastMouseX.value = e.clientX
  lastMouseY.value = e.clientY
}

const handleGlobalMove = (e) => {
  if (isPanning.value) {
    handlePan(e)
  } else if (draggingWishId.value) {
    const dx = e.clientX - lastMouseX.value
    const dy = e.clientY - lastMouseY.value
    const wish = wishes.value.find(w => w.id === draggingWishId.value)
    if (wish) {
      wish._x += dx
      wish._y += dy
    }
    lastMouseX.value = e.clientX
    lastMouseY.value = e.clientY
  }
}

const handlePan = (e) => {
  const dx = e.clientX - lastMouseX.value
  const dy = e.clientY - lastMouseY.value
  panX.value += dx
  panY.value += dy
  lastMouseX.value = e.clientX
  lastMouseY.value = e.clientY
}

const startPanTouch = (e) => {
  if (e.touches.length !== 1) return
  isPanning.value = true
  draggingWishId.value = null
  lastMouseX.value = e.touches[0].clientX
  lastMouseY.value = e.touches[0].clientY
}

const startDragWishTouch = (e, wish) => {
  if (e.touches.length !== 1) return
  draggingWishId.value = wish.id
  lastMouseX.value = e.touches[0].clientX
  lastMouseY.value = e.touches[0].clientY
}

const handleGlobalTouch = (e) => {
  if (e.touches.length !== 1) return
  if (isPanning.value) {
    handlePanTouch(e)
  } else if (draggingWishId.value) {
    const dx = e.touches[0].clientX - lastMouseX.value
    const dy = e.touches[0].clientY - lastMouseY.value
    const wish = wishes.value.find(w => w.id === draggingWishId.value)
    if (wish) {
      wish._x += dx
      wish._y += dy
    }
    lastMouseX.value = e.touches[0].clientX
    lastMouseY.value = e.touches[0].clientY
  }
}

const handlePanTouch = (e) => {
  const dx = e.touches[0].clientX - lastMouseX.value
  const dy = e.touches[0].clientY - lastMouseY.value
  panX.value += dx
  panY.value += dy
  lastMouseX.value = e.touches[0].clientX
  lastMouseY.value = e.touches[0].clientY
}

const endAllGestures = () => {
  isPanning.value = false
  draggingWishId.value = null
}

const handleResize = () => {
  viewportWidth.value = window.innerWidth
}

const hashId = (id) => {
  const str = String(id ?? '')
  let h = 2166136261
  for (let i = 0; i < str.length; i += 1) {
    h ^= str.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

const buildSlots = (count) => {
  const viewportWidthSafe = Math.max(window.innerWidth || 0, 360)
  const viewportHeightSafe = Math.max(window.innerHeight || 0, 640)
  const topSafe = isMobile.value ? 132 : 178
  const leftSafe = isMobile.value ? 10 : 24

  // Keep cards dense but non-overlapping: base step is card size + minimal safety gap.
  const minGapX = isMobile.value ? 8 : 12
  const minGapY = isMobile.value ? 10 : 14
  const stepX = cardWidth.value + minGapX
  const stepY = cardHeight.value + minGapY
  const densityBase = isMobile.value ? 1.08 : 1.12
  const aspect = viewportWidthSafe / viewportHeightSafe
  const targetSlots = Math.max(Math.ceil(count * densityBase), 16)
  const cols = Math.max(1, Math.ceil(Math.sqrt(targetSlots * aspect)))
  const rows = Math.max(1, Math.ceil(targetSlots / cols))
  const total = rows * cols

  const freeX = Math.max(1, stepX - cardWidth.value)
  const freeY = Math.max(1, stepY - cardHeight.value)
  const jitterX = Math.max(0, Math.min(freeX * 0.4, isMobile.value ? 3 : 4.5))
  const jitterY = Math.max(0, Math.min(freeY * 0.4, isMobile.value ? 3 : 4.5))
  const slots = []
  for (let i = 0; i < total; i += 1) {
    const col = i % cols
    const row = Math.floor(i / cols)
    const stagger = row % 2 === 0 ? 0 : Math.min(stepX * 0.14, cardWidth.value * 0.18)
    const driftX = (Math.random() - 0.5) * jitterX
    const driftY = (Math.random() - 0.5) * jitterY + Math.sin((col + row) * 0.75) * (isMobile.value ? 2 : 3)
    slots.push({
      x: leftSafe + col * stepX + stagger + driftX,
      y: topSafe + row * stepY + driftY,
      index: i,
    })
  }
  return slots
}

const allocateSlots = (rows, existingMap, totalSlots) => {
  const used = new Set()
  const slotByWishId = new Map()
  const safeTotal = Math.max(1, totalSlots)

  for (const row of rows) {
    const previous = existingMap.get(row.id)
    const prevIndex = Number(previous?._slotIndex)
    if (Number.isInteger(prevIndex) && prevIndex >= 0 && prevIndex < safeTotal && !used.has(prevIndex)) {
      used.add(prevIndex)
      slotByWishId.set(row.id, prevIndex)
    }
  }

  for (const row of rows) {
    if (slotByWishId.has(row.id)) continue
    let index = hashId(row.id) % safeTotal
    for (let attempts = 0; attempts < safeTotal; attempts += 1) {
      if (!used.has(index)) {
        used.add(index)
        slotByWishId.set(row.id, index)
        break
      }
      index = (index + 1) % safeTotal
    }
  }

  return slotByWishId
}

const applyWishList = (rows) => {
  const normalizedRows = (Array.isArray(rows) ? rows : []).filter((row) => row?.id)
  const existingMap = new Map(wishes.value.map((item) => [item.id, item]))
  const slots = buildSlots(normalizedRows.length)
  const slotByWishId = allocateSlots(normalizedRows, existingMap, slots.length)

  wishes.value = normalizedRows.map((row) => {
    const existing = existingMap.get(row.id)
    const slotIndex = slotByWishId.get(row.id) ?? 0
    const slot = slots[Math.min(slotIndex, slots.length - 1)]
    return {
      ...row,
      likeCount: Number(row.likeCount || 0),
      commentCount: Number(row.commentCount || 0),
      likedByMe: !!row.likedByMe,
      _x: slot?.x ?? 0,
      _y: slot?.y ?? 0,
      _slotIndex: slotIndex,
      _color: existing?._color || colors[hashId(row.id) % colors.length],
      _speed: existing?._speed || (0.85 + (hashId(`speed-${row.id}`) % 50) / 100),
    }
  })
}

const fetchWishes = async () => {
  const visitorId = getVisitorId()
  const res = await api.get(`/api/wishes?visitorId=${encodeURIComponent(visitorId)}`)
  if (res.code !== 200) {
    throw new Error(res.msg || 'Failed to load wishes')
  }
  applyWishList(res.data || [])
}

const openSubmitDialog = () => {
  if (!authStore.isLoggedIn) {
    alert('请先登录后再发布星愿。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能发布星愿，请先登录正式账号。')
    return
  }
  showSubmitDialog.value = true
}

const submitWish = async () => {
  if (!authStore.isLoggedIn) {
    alert('请先登录后再发布星愿。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能发布星愿，请先登录正式账号。')
    return
  }
  if (!newWishContent.value.trim()) return
  submitting.value = true
  try {
    const res = await api.post('/api/wishes', {
      content: newWishContent.value.trim(),
      city: newWishCity.value.trim() || undefined,
    })
    if (res.code !== 200) {
      throw new Error(res.msg || 'Failed to submit wish')
    }
    newWishContent.value = ''
    newWishCity.value = ''
    showSubmitDialog.value = false
    await fetchWishes()
  } catch (e) {
    alert(e.message || '发布失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const toggleLike = async (wish) => {
  if (!wish?.id || likingMap[wish.id]) return
  if (authStore.isGuest) {
    alert('游客账号不可点赞，请先注册')
    return
  }
  likingMap[wish.id] = true
  try {
    const res = await api.post(`/api/wishes/${wish.id}/like`, {
      visitorId: getVisitorId(),
    })
    if (res.code !== 200) {
      throw new Error(res.msg || 'Failed to like')
    }
    wish.likedByMe = !!res.data?.liked
    wish.likeCount = Number(res.data?.likeCount || 0)
  } catch (e) {
    alert(e.message || '点赞失败，请稍后重试')
  } finally {
    likingMap[wish.id] = false
  }
}

const openComments = async (wish) => {
  activeWishId.value = wish.id
  commentPanelVisible.value = true
  newCommentContent.value = ''
  await loadComments(wish.id)
}

const closeComments = () => {
  commentPanelVisible.value = false
  activeWishId.value = null
  comments.value = []
  newCommentContent.value = ''
}

const loadComments = async (wishId) => {
  if (!wishId) return
  loadingComments.value = true
  try {
    const res = await api.get(`/api/wishes/${wishId}/comments`)
    if (res.code !== 200) {
      throw new Error(res.msg || 'Failed to load comments')
    }
    comments.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    comments.value = []
    alert(e.message || '加载评论失败')
  } finally {
    loadingComments.value = false
  }
}

const goRegister = () => {
  router.push('/register')
}

const submitComment = async () => {
  if (!activeWish.value?.id) return
  if (authStore.isGuest) {
    alert('游客账号不可评论，请先登录注册')
    return
  }
  if (!newCommentContent.value.trim()) return

  submittingComment.value = true
  try {
    const res = await api.post(`/api/wishes/${activeWish.value.id}/comments`, {
      content: newCommentContent.value.trim(),
    })
    if (res.code !== 200) {
      throw new Error(res.msg || 'Failed to submit comment')
    }
    const created = res.data
    comments.value = [created, ...comments.value]
    newCommentContent.value = ''
    activeWish.value.commentCount = Number(activeWish.value.commentCount || 0) + 1
  } catch (e) {
    alert(e.message || '评论发送失败')
  } finally {
    submittingComment.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize, { passive: true })
  try {
    await fetchWishes()
  } catch (e) {
    alert(e.message || '星愿墙加载失败')
  }
  pollInterval = setInterval(async () => {
    try {
      await fetchWishes()
      if (activeWishId.value) {
        await loadComments(activeWishId.value)
      }
    } catch (_) {
      // keep UI stable when polling fails
    }
  }, 12000)
})

watch(viewportWidth, () => {
  if (!wishes.value.length) return
  applyWishList(wishes.value)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
@keyframes float {
  0% {
    transform: translateY(0) rotate(-1deg);
  }
  100% {
    transform: translateY(-10px) rotate(1deg);
  }
}

.wish-wall-shell {
  background: var(--app-shell-bg);
}

.wish-wall-shell :deep([class~='bg-white/90']),
.wish-wall-shell :deep([class~='bg-white/75']),
.wish-wall-shell :deep([class~='bg-bg-elevated']),
.wish-wall-shell :deep([class~='bg-bg-surface']) {
  background: var(--surface-panel-soft) !important;
}

.wish-wall-shell :deep([class~='bg-black/5']),
.wish-wall-shell :deep([class~='bg-black/10']),
.wish-wall-shell :deep([class~='bg-black/[0.03]']) {
  background: var(--bg-soft) !important;
}

.wish-wall-shell :deep([class~='text-slate-500']),
.wish-wall-shell :deep([class~='text-slate-400']),
.wish-wall-shell :deep([class~='text-slate-600']),
.wish-wall-shell :deep([class~='text-gray-400']) {
  color: var(--text-tertiary) !important;
}

.wish-wall-shell :deep([class~='border-gray-200']),
.wish-wall-shell :deep([class~='border-black/5']),
.wish-wall-shell :deep([class~='border-black/[0.05]']) {
  border-color: var(--border-soft) !important;
}

.wish-content {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wish-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: 999px;
  background: var(--bg-soft);
  border: 1px solid var(--border-soft);
  color: var(--text-primary);
  font-size: 12px;
  padding: 3px 8px;
  transition: all 0.2s ease;
}

.dark .wish-action {
  background: var(--bg-soft);
  border: 1px solid var(--border-soft);
}

.wish-action:hover {
  transform: translateY(-1px);
  background: var(--bg-elevated);
}

.wish-action-liked {
  color: var(--danger);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 8px;
}

@media (max-width: 767px) {
  .wish-content {
    -webkit-line-clamp: 3;
    line-clamp: 3;
    font-size: 13px;
    line-height: 1.35rem;
  }

  .wish-action {
    font-size: 11px;
    padding: 2px 7px;
  }
}
</style>
