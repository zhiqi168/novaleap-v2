<template>
  <div class="wish-wall-shell workspace-page h-full flex flex-col relative overflow-hidden select-none">
    <div class="absolute top-0 left-0 right-0 p-3 sm:p-6 md:p-8 z-[20] pointer-events-none">
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
            class="absolute wish-note will-change-transform"
            :class="`wish-note--${wish._decor}`"
            :style="getWishStyle(wish)"
          >
            <article class="wish-note-card">
              <div class="wish-note-noise"></div>
              <div v-if="wish._decor === 'pin' || wish._decor === 'pin-sticker'" class="wish-note-pin">✦</div>
              <div v-if="wish._decor === 'tape' || wish._decor === 'tape-sticker'" class="wish-note-tape"></div>
              <div
                v-if="wish._decor === 'sticker' || wish._decor === 'pin-sticker' || wish._decor === 'tape-sticker'"
                class="wish-note-sticker"
              >
                {{ wish._sticker }}
              </div>

              <div class="wish-note-inner">
                <div class="wish-note-meta">
                  <div class="wish-note-source">
                    <span class="wish-note-source-mark">
                      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M12 3l1.7 4.5L18 9.2l-4 1.6L12 15l-2-4.2-4-1.6 4.3-1.7L12 3z" fill="currentColor" />
                      </svg>
                    </span>
                    <span class="wish-note-source-label">{{ wish.city || '云端坐标' }}</span>
                  </div>
                  <div class="wish-note-date">{{ formatDate(wish.createdAt) }}</div>
                </div>

                <div class="wish-note-body">
                  <p class="wish-note-content wish-content">
                    {{ wish.content }}
                  </p>
                </div>

                <div class="wish-note-footer">
                  <div class="wish-note-tag">
                    {{ translateEmotion(wish.emotion) }}
                  </div>

                  <div class="flex items-center gap-2 pointer-events-auto">
                    <button
                      :disabled="authStore.isGuest || !!likingMap[wish.id]"
                      class="wish-action"
                      :class="{ 'wish-action-liked': wish.likedByMe }"
                      @click.stop="toggleLike(wish)"
                    >
                      <span class="wish-action-icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none">
                          <path d="M12 20.4l-1.1-1C5.3 14.2 2 11.3 2 7.8 2 5.3 4 3.3 6.5 3.3c1.4 0 2.8.7 3.7 1.8.9-1.1 2.3-1.8 3.7-1.8C16.4 3.3 18.4 5.3 18.4 7.8c0 3.5-3.3 6.4-8.9 11.6l-1.1 1z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                      </span>
                      <span>{{ wish.likeCount || 0 }}</span>
                    </button>
                    <button class="wish-action" @click.stop="openComments(wish)">
                      <span class="wish-action-icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none">
                          <path d="M6.2 16.8c-1.8-1.2-2.9-3.1-2.9-5.3 0-4 3.8-7.2 8.5-7.2s8.5 3.2 8.5 7.2-3.8 7.2-8.5 7.2c-1.1 0-2.2-.2-3.2-.6L4.8 20l1.4-3.2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                      </span>
                      <span>{{ wish.commentCount || 0 }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </article>
        </div>
      </div>
    </div>
    </div>

    <div class="absolute bottom-4 sm:bottom-10 left-1/2 -translate-x-1/2 z-[30] md:z-[220] w-[calc(100vw-1.5rem)] sm:w-auto flex justify-center">
      <button
        @click="openSubmitDialog"
        :disabled="authStore.isGuest"
        class="wish-publish-btn workspace-btn workspace-btn-primary w-full sm:w-auto backdrop-blur-xl px-6 sm:px-8 py-3.5 sm:py-4 rounded-full shadow-modal ring-1 ring-black/5 flex items-center justify-center gap-3 text-white font-bold hover:scale-105 transition-all active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:scale-100"
      >
        <span class="text-xl text-ai-from">✨</span>
        发布星愿
        <span class="text-xl text-amber-400">✨</span>
      </button>
    </div>

<transition name="submit-note-fade">
      <div v-if="showSubmitDialog" class="submit-note-overlay" @click.self="showSubmitDialog = false">
        <div class="submit-note-shell">
          <div class="submit-note-dialog">
            <div class="submit-note-header">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <h3 class="submit-note-title">许下星愿</h3>
                  <span class="submit-note-badge">审核发布</span>
                </div>
                <p class="submit-note-subtitle">
                  写下你的技术梦想、职业目标或学习心愿。
                </p>
              </div>
              <button class="submit-note-close" @click="showSubmitDialog = false">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>

            <div class="submit-note-body custom-scrollbar">
              <div class="submit-note-star-header">
                <div class="submit-note-star-icon">
                  <svg fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                  </svg>
                </div>
                <div class="submit-note-star-text">
                  <h4>向星空许愿</h4>
                  <p>写下你的愿望，星星会帮你实现</p>
                </div>
              </div>

              <textarea
                v-model="newWishContent"
                rows="4"
                maxlength="200"
                placeholder="我想成为..."
                class="submit-note-textarea"
              />

              <div class="submit-note-char-count">
                {{ newWishContent.length }}/200
              </div>

              <div class="submit-note-city-row">
                <svg class="submit-note-city-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                </svg>
                <input
                  v-model="newWishCity"
                  type="text"
                  maxlength="40"
                  placeholder="你来自哪座城市？"
                  class="submit-note-city-input"
                />
              </div>
            </div>

            <div class="submit-note-footer">
              <button class="submit-note-btn submit-note-btn-secondary" @click="showSubmitDialog = false">
                取消
              </button>
              <button
                class="submit-note-btn submit-note-btn-primary"
                :disabled="submitting || !newWishContent.trim()"
                @click="submitWish"
              >
                {{ submitting ? '提交中...' : '发布星愿' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <div
      v-if="commentPanelVisible"
      class="fixed inset-0 z-[1200] bg-black/25 backdrop-blur-[2px] md:top-[91px] md:rounded-t-[28px] md:overflow-hidden md:bg-transparent md:backdrop-blur-none"
      @click.self="closeComments"
    >
      <aside class="wish-comment-drawer workspace-panel absolute right-0 bottom-0 w-full h-[75vh] max-w-[430px] rounded-t-3xl bg-bg-surface backdrop-blur-xl shadow-2xl border-t border-border-subtle flex flex-col md:absolute md:right-0 md:top-0 md:h-full md:w-full md:max-w-[430px] md:rounded-none md:rounded-t-[28px] md:rounded-l-3xl md:border-t-0 md:border-l md:border-border-subtle md:shadow-none">
        <button
          class="wish-comment-mobile-close"
          type="button"
          @click="closeComments"
          aria-label="关闭评论"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        <header class="wish-comment-header px-5 py-4 border-b border-border-subtle flex items-center justify-between relative">
          <h3 class="text-base font-bold text-text-primary">星愿评论</h3>
          <button
            class="wish-comment-desktop-close workspace-btn workspace-btn-muted px-2.5 py-1 rounded-md bg-black/5 hover:bg-black/10 text-sm"
            type="button"
            @click="closeComments"
            aria-label="关闭评论"
          >
            关闭
          </button>
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
                class="workspace-btn workspace-btn-muted px-3 py-2 rounded-lg text-sm border border-border-subtle bg-bg-surface hover:bg-bg-elevated text-text-primary"
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
import { useAutoPageRefresh } from '@/composables/useAutoPageRefresh'
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
const viewportHeight = ref(typeof window !== 'undefined' ? window.innerHeight : 720)

const activeWish = computed(() => wishes.value.find((w) => w.id === activeWishId.value) || null)

const noteThemes = [
  {
    surface: 'linear-gradient(160deg, rgba(255, 251, 248, 0.98) 0%, rgba(255, 238, 243, 0.97) 52%, rgba(255, 227, 235, 0.98) 100%)',
    glow: 'rgba(255, 221, 232, 0.82)',
    glowSoft: 'rgba(255, 244, 248, 0.92)',
    border: 'rgba(255, 255, 255, 0.78)',
    highlight: 'rgba(255, 255, 255, 0.86)',
    shadow: 'rgba(198, 166, 175, 0.22)',
    shadowStrong: 'rgba(184, 151, 161, 0.34)',
    text: '#46383f',
    meta: '#8d7c84',
    date: '#a3959b',
    tagBg: 'rgba(255, 250, 252, 0.68)',
    tagBorder: 'rgba(234, 207, 216, 0.96)',
    tagText: '#8d6676',
    actionBg: 'rgba(255, 255, 255, 0.56)',
    actionBgHover: 'rgba(255, 255, 255, 0.84)',
    actionBorder: 'rgba(236, 209, 219, 0.92)',
    actionText: '#755e69',
    actionShadow: 'rgba(205, 173, 183, 0.2)',
    pinBg: 'linear-gradient(135deg, #fff1f6 0%, #f6bed0 100%)',
    pinShadow: 'rgba(214, 161, 181, 0.34)',
    stickerBg: 'linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 236, 242, 0.96) 100%)',
    stickerText: '#9f7384',
    tapeBg: 'linear-gradient(180deg, rgba(255, 255, 255, 0.58) 0%, rgba(255, 236, 242, 0.46) 100%)',
    tapeBorder: 'rgba(240, 214, 223, 0.88)',
    divider: 'rgba(145, 124, 133, 0.1)',
    noiseOpacity: '0.15',
  },
  {
    surface: 'linear-gradient(160deg, rgba(247, 252, 255, 0.98) 0%, rgba(232, 242, 249, 0.97) 54%, rgba(223, 235, 247, 0.98) 100%)',
    glow: 'rgba(219, 236, 249, 0.86)',
    glowSoft: 'rgba(241, 248, 253, 0.94)',
    border: 'rgba(255, 255, 255, 0.82)',
    highlight: 'rgba(255, 255, 255, 0.9)',
    shadow: 'rgba(150, 176, 196, 0.22)',
    shadowStrong: 'rgba(134, 164, 186, 0.34)',
    text: '#38434c',
    meta: '#748694',
    date: '#8f9eaa',
    tagBg: 'rgba(249, 252, 255, 0.72)',
    tagBorder: 'rgba(198, 216, 229, 0.96)',
    tagText: '#647c8e',
    actionBg: 'rgba(255, 255, 255, 0.58)',
    actionBgHover: 'rgba(255, 255, 255, 0.86)',
    actionBorder: 'rgba(201, 219, 232, 0.94)',
    actionText: '#556c7c',
    actionShadow: 'rgba(155, 182, 199, 0.2)',
    pinBg: 'linear-gradient(135deg, #eef8ff 0%, #b7d5ea 100%)',
    pinShadow: 'rgba(156, 188, 209, 0.34)',
    stickerBg: 'linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgba(233, 244, 252, 0.98) 100%)',
    stickerText: '#6b8598',
    tapeBg: 'linear-gradient(180deg, rgba(255, 255, 255, 0.6) 0%, rgba(228, 241, 249, 0.48) 100%)',
    tapeBorder: 'rgba(205, 222, 233, 0.88)',
    divider: 'rgba(101, 123, 139, 0.1)',
    noiseOpacity: '0.14',
  },
  {
    surface: 'linear-gradient(160deg, rgba(251, 248, 255, 0.98) 0%, rgba(238, 232, 247, 0.97) 52%, rgba(229, 223, 243, 0.98) 100%)',
    glow: 'rgba(229, 220, 245, 0.84)',
    glowSoft: 'rgba(246, 242, 252, 0.94)',
    border: 'rgba(255, 255, 255, 0.8)',
    highlight: 'rgba(255, 255, 255, 0.88)',
    shadow: 'rgba(168, 156, 194, 0.22)',
    shadowStrong: 'rgba(151, 138, 181, 0.34)',
    text: '#433d52',
    meta: '#82798f',
    date: '#9d93aa',
    tagBg: 'rgba(252, 250, 255, 0.7)',
    tagBorder: 'rgba(218, 209, 235, 0.95)',
    tagText: '#76688f',
    actionBg: 'rgba(255, 255, 255, 0.58)',
    actionBgHover: 'rgba(255, 255, 255, 0.84)',
    actionBorder: 'rgba(219, 210, 235, 0.92)',
    actionText: '#6a607c',
    actionShadow: 'rgba(179, 166, 206, 0.2)',
    pinBg: 'linear-gradient(135deg, #f4efff 0%, #cbbfe8 100%)',
    pinShadow: 'rgba(180, 167, 214, 0.34)',
    stickerBg: 'linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgba(239, 233, 249, 0.98) 100%)',
    stickerText: '#7d6d99',
    tapeBg: 'linear-gradient(180deg, rgba(255, 255, 255, 0.58) 0%, rgba(237, 232, 247, 0.46) 100%)',
    tapeBorder: 'rgba(220, 212, 235, 0.88)',
    divider: 'rgba(119, 108, 145, 0.1)',
    noiseOpacity: '0.14',
  },
  {
    surface: 'linear-gradient(160deg, rgba(255, 251, 246, 0.98) 0%, rgba(252, 239, 224, 0.97) 52%, rgba(249, 228, 206, 0.98) 100%)',
    glow: 'rgba(250, 229, 203, 0.82)',
    glowSoft: 'rgba(255, 246, 235, 0.94)',
    border: 'rgba(255, 255, 255, 0.78)',
    highlight: 'rgba(255, 255, 255, 0.86)',
    shadow: 'rgba(200, 172, 145, 0.22)',
    shadowStrong: 'rgba(185, 156, 129, 0.34)',
    text: '#4b3d35',
    meta: '#907d72',
    date: '#ac988b',
    tagBg: 'rgba(255, 250, 244, 0.7)',
    tagBorder: 'rgba(236, 214, 190, 0.95)',
    tagText: '#8a6b58',
    actionBg: 'rgba(255, 255, 255, 0.58)',
    actionBgHover: 'rgba(255, 255, 255, 0.84)',
    actionBorder: 'rgba(236, 213, 190, 0.92)',
    actionText: '#7a614f',
    actionShadow: 'rgba(204, 176, 148, 0.18)',
    pinBg: 'linear-gradient(135deg, #fff5ea 0%, #f0c8a2 100%)',
    pinShadow: 'rgba(218, 180, 136, 0.34)',
    stickerBg: 'linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgba(252, 240, 227, 0.98) 100%)',
    stickerText: '#92715f',
    tapeBg: 'linear-gradient(180deg, rgba(255, 255, 255, 0.58) 0%, rgba(251, 238, 223, 0.46) 100%)',
    tapeBorder: 'rgba(239, 218, 196, 0.9)',
    divider: 'rgba(133, 109, 91, 0.1)',
    noiseOpacity: '0.13',
  },
  {
    surface: 'linear-gradient(160deg, rgba(245, 252, 250, 0.98) 0%, rgba(228, 243, 239, 0.97) 54%, rgba(217, 235, 231, 0.98) 100%)',
    glow: 'rgba(220, 238, 233, 0.82)',
    glowSoft: 'rgba(240, 249, 246, 0.94)',
    border: 'rgba(255, 255, 255, 0.8)',
    highlight: 'rgba(255, 255, 255, 0.88)',
    shadow: 'rgba(148, 180, 171, 0.22)',
    shadowStrong: 'rgba(132, 164, 155, 0.34)',
    text: '#374541',
    meta: '#718682',
    date: '#8b9f9b',
    tagBg: 'rgba(248, 253, 252, 0.72)',
    tagBorder: 'rgba(201, 223, 217, 0.95)',
    tagText: '#607976',
    actionBg: 'rgba(255, 255, 255, 0.58)',
    actionBgHover: 'rgba(255, 255, 255, 0.86)',
    actionBorder: 'rgba(204, 226, 220, 0.92)',
    actionText: '#556c67',
    actionShadow: 'rgba(160, 191, 183, 0.18)',
    pinBg: 'linear-gradient(135deg, #eef9f6 0%, #bcdcd3 100%)',
    pinShadow: 'rgba(158, 193, 184, 0.34)',
    stickerBg: 'linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgba(230, 244, 240, 0.98) 100%)',
    stickerText: '#6b8781',
    tapeBg: 'linear-gradient(180deg, rgba(255, 255, 255, 0.6) 0%, rgba(227, 243, 238, 0.46) 100%)',
    tapeBorder: 'rgba(208, 227, 222, 0.88)',
    divider: 'rgba(103, 128, 121, 0.1)',
    noiseOpacity: '0.13',
  },
]

const noteDecorations = ['pin', 'pin', 'pin-sticker', 'tape', 'sticker']
const noteStickers = ['✦', '☾', '☁', '✧', '❋']

const isMobile = computed(() => viewportWidth.value < 768)
const cardWidth = computed(() => (isMobile.value ? 216 : 272))
const cardHeight = computed(() => (isMobile.value ? 168 : 188))
const cardPadding = computed(() => (isMobile.value ? 14 : 24))
const enableCardFloat = computed(() => false)

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
  viewportHeight.value = window.innerHeight
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

const parseWishTime = (value) => {
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

const buildCenteredSequence = (radius, positiveFirst = false) => {
  const values = [0]
  for (let offset = 1; offset <= radius; offset += 1) {
    values.push(positiveFirst ? offset : -offset)
    values.push(positiveFirst ? -offset : offset)
  }
  return values.filter((value) => Math.abs(value) <= radius)
}

const buildRingCoords = (radius) => {
  if (radius === 0) {
    return [{ col: 0, row: 0 }]
  }

  const top = buildCenteredSequence(radius).map((col) => ({ col, row: -radius }))
  const right = buildCenteredSequence(radius)
    .filter((row) => row !== -radius)
    .map((row) => ({ col: radius, row }))
  const bottom = buildCenteredSequence(radius, true)
    .filter((col) => col !== radius)
    .map((col) => ({ col, row: radius }))
  const left = buildCenteredSequence(radius, true)
    .filter((row) => row !== radius && row !== -radius)
    .map((row) => ({ col: -radius, row }))

  const ring = []
  const maxLen = Math.max(top.length, right.length, bottom.length, left.length)
  for (let i = 0; i < maxLen; i += 1) {
    if (top[i]) ring.push(top[i])
    if (right[i]) ring.push(right[i])
    if (bottom[i]) ring.push(bottom[i])
    if (left[i]) ring.push(left[i])
  }
  return ring
}

const buildSlots = (count) => {
  if (!count) return []

  const viewportWidthSafe = Math.max(window.innerWidth || 0, 360)
  const viewportHeightSafe = Math.max(window.innerHeight || 0, 640)
  const topSafe = isMobile.value ? 136 : 180
  const bottomSafe = isMobile.value ? 128 : 152
  const gapX = isMobile.value ? 8 : 12
  const gapY = isMobile.value ? 8 : 12
  const stepX = cardWidth.value + gapX
  const stepY = cardHeight.value + gapY
  const usableHeight = Math.max(cardHeight.value, viewportHeightSafe - topSafe - bottomSafe)
  const centerX = Math.round((viewportWidthSafe - cardWidth.value) / 2)
  const centerY = Math.round(topSafe + (usableHeight - cardHeight.value) / 2)
  const slots = []

  for (let radius = 0; slots.length < count; radius += 1) {
    const ringCoords = buildRingCoords(radius)
    for (const coord of ringCoords) {
      if (slots.length >= count) break
      slots.push({
        x: centerX + coord.col * stepX,
        y: centerY + coord.row * stepY,
        index: slots.length,
      })
    }
  }

  return slots
}

const sortWishesForLayout = (rows) => {
  return [...rows].sort((a, b) => {
    const timeDiff = parseWishTime(a.createdAt) - parseWishTime(b.createdAt)
    if (timeDiff !== 0) return timeDiff
    return hashId(a.id) - hashId(b.id)
  })
}

const getWishStyle = (wish) => ({
  left: `${wish._x}px`,
  top: `${wish._y}px`,
  width: `${cardWidth.value}px`,
  height: `${cardHeight.value}px`,
  '--wish-rotate': `${wish._rotation}deg`,
  '--wish-hover-rotate': `${wish._hoverRotation}deg`,
  '--note-bg': wish._theme.surface,
  '--note-glow': wish._theme.glow,
  '--note-glow-soft': wish._theme.glowSoft,
  '--note-border': wish._theme.border,
  '--note-highlight': wish._theme.highlight,
  '--note-shadow': wish._theme.shadow,
  '--note-shadow-strong': wish._theme.shadowStrong,
  '--note-text': wish._theme.text,
  '--note-meta': wish._theme.meta,
  '--note-date': wish._theme.date,
  '--note-tag-bg': wish._theme.tagBg,
  '--note-tag-border': wish._theme.tagBorder,
  '--note-tag-text': wish._theme.tagText,
  '--note-action-bg': wish._theme.actionBg,
  '--note-action-bg-hover': wish._theme.actionBgHover,
  '--note-action-border': wish._theme.actionBorder,
  '--note-action-text': wish._theme.actionText,
  '--note-action-shadow': wish._theme.actionShadow,
  '--note-pin-bg': wish._theme.pinBg,
  '--note-pin-shadow': wish._theme.pinShadow,
  '--note-sticker-bg': wish._theme.stickerBg,
  '--note-sticker-text': wish._theme.stickerText,
  '--note-tape-bg': wish._theme.tapeBg,
  '--note-tape-border': wish._theme.tapeBorder,
  '--note-divider': wish._theme.divider,
  '--note-noise-opacity': wish._theme.noiseOpacity,
})

const applyWishList = (rows) => {
  const normalizedRows = (Array.isArray(rows) ? rows : []).filter((row) => row?.id)
  const orderedRows = sortWishesForLayout(normalizedRows)
  const existingMap = new Map(wishes.value.map((item) => [item.id, item]))
  const slots = buildSlots(orderedRows.length)

  wishes.value = orderedRows.map((row, index) => {
    const existing = existingMap.get(row.id)
    const slotIndex = index
    const slot = slots[Math.min(slotIndex, slots.length - 1)]
    const rotationSeed = hashId(`rotation-${row.id}`)
    const rotationEnabled = rotationSeed % 6 >= 4
    const rotationBase = (((rotationSeed >> 3) % 12) - 6) * 0.18
    const rotation = rotationEnabled ? Number(Math.max(-1.2, Math.min(1, rotationBase || 0.42)).toFixed(2)) : 0
    const theme = existing?._theme || noteThemes[hashId(`theme-${row.id}`) % noteThemes.length]
    const decor = existing?._decor || noteDecorations[hashId(`decor-${row.id}`) % noteDecorations.length]
    const sticker = existing?._sticker || noteStickers[hashId(`sticker-${row.id}`) % noteStickers.length]
    return {
      ...row,
      likeCount: Number(row.likeCount || 0),
      commentCount: Number(row.commentCount || 0),
      likedByMe: !!row.likedByMe,
      _x: slot?.x ?? 0,
      _y: slot?.y ?? 0,
      _slotIndex: slotIndex,
      _theme: theme,
      _decor: decor,
      _sticker: sticker,
      _rotation: existing?._rotation ?? rotation,
      _hoverRotation: existing?._hoverRotation ?? Number((rotation * 0.22).toFixed(2)),
      _floatLift: existing?._floatLift ?? (4 + (hashId(`lift-${row.id}`) % 3)),
      _floatTilt: existing?._floatTilt ?? Number((((hashId(`tilt-${row.id}`) % 7) - 3) * 0.05).toFixed(2)),
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

useAutoPageRefresh(async () => {
  await fetchWishes()
  if (activeWishId.value) {
    await loadComments(activeWishId.value)
  }
}, {
  throttleMs: 5000,
})

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

watch([viewportWidth, viewportHeight], () => {
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
    transform: translate3d(0, 0, 0) rotate(var(--wish-rotate));
  }
  100% {
    transform: translate3d(0, calc(var(--wish-float-lift) * -1), 0) rotate(calc(var(--wish-rotate) + var(--wish-float-tilt)));
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

.wish-comment-drawer {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0)),
    var(--surface-panel-soft);
}

.wish-comment-header {
  position: sticky;
  top: 0;
  z-index: 5;
  background: color-mix(in srgb, var(--surface-panel-soft) 92%, transparent);
  backdrop-filter: blur(18px);
}

.wish-comment-mobile-close {
  position: absolute;
  right: 16px;
  top: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-secondary);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  flex-shrink: 0;
}

.wish-comment-mobile-close:active {
  transform: scale(0.96);
}

.wish-comment-desktop-close {
  display: none;
}

@media (min-width: 768px) {
  .wish-comment-mobile-close {
    display: none;
  }

  .wish-comment-desktop-close {
    display: inline-flex;
  }
}

.wish-note {
  z-index: 1;
  transform: rotate(var(--wish-rotate));
  transform-origin: 50% 14%;
  transition:
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    z-index 0.26s ease,
    filter 0.34s ease;
  filter: saturate(0.98);
}

.wish-note:hover {
  z-index: 60;
  animation-play-state: paused;
  transform: translate3d(0, -6px, 0) rotate(var(--wish-hover-rotate)) !important;
  filter: saturate(1.01);
}

.wish-note-card {
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: 28px 28px 24px 24px;
  border: 1px solid color-mix(in srgb, var(--note-border) 82%, rgba(255, 255, 255, 0.88));
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.28) 0%, rgba(255, 255, 255, 0) 24%),
    radial-gradient(circle at 12% 8%, rgba(255, 255, 255, 0.34) 0%, rgba(255, 255, 255, 0) 28%),
    radial-gradient(circle at 86% 16%, color-mix(in srgb, var(--note-glow) 78%, white 22%) 0%, rgba(255, 255, 255, 0) 28%),
    var(--note-bg);
  box-shadow:
    0 10px 20px -16px var(--note-shadow),
    0 20px 38px -28px var(--note-shadow-strong),
    inset 0 1px 0 var(--note-highlight),
    inset 0 -14px 24px -24px rgba(255, 255, 255, 0.54);
  transition:
    box-shadow 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.34s ease,
    background 0.34s ease;
}

.wish-note:hover .wish-note-card {
  box-shadow:
    0 14px 26px -18px var(--note-shadow),
    0 26px 46px -28px var(--note-shadow-strong),
    inset 0 1px 0 var(--note-highlight),
    inset 0 -16px 26px -24px rgba(255, 255, 255, 0.64);
}

.wish-note-card::before,
.wish-note-card::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.wish-note-card::before {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.02) 16%, rgba(255, 255, 255, 0) 38%),
    radial-gradient(circle at 70% 16%, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0) 22%);
  mix-blend-mode: screen;
  opacity: 0.92;
}

.wish-note-card::after {
  inset: 1px;
  border-radius: 27px 27px 23px 23px;
  border-top: 1px solid rgba(255, 255, 255, 0.28);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  opacity: 0.64;
}

.wish-note-noise {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(rgba(255, 255, 255, 0.32) 0.4px, transparent 0.55px),
    radial-gradient(rgba(114, 104, 110, 0.03) 0.35px, transparent 0.5px),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0));
  background-position: 0 0, 7px 9px, 0 0;
  background-size: 13px 13px, 17px 17px, 100% 100%;
  opacity: var(--note-noise-opacity);
  mix-blend-mode: soft-light;
}

.wish-note-inner {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 17px 17px 15px;
}

.wish-note-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 9px;
  margin-bottom: 11px;
}

.wish-note-source {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.wish-note-source-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.28);
  color: var(--note-tag-text);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.66);
  flex-shrink: 0;
  opacity: 0.88;
}

.wish-note-source-mark svg {
  width: 8px;
  height: 8px;
}

.wish-note-source-label {
  min-width: 0;
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--note-meta);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.86;
}

.wish-note-date {
  flex-shrink: 0;
  font-size: 8.5px;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: var(--note-meta);
  font-variant-numeric: tabular-nums;
  opacity: 0.72;
}

.wish-note-body {
  flex: 1;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.wish-note-content {
  margin: 0;
  color: var(--note-text);
  font-size: 15px;
  font-weight: 560;
  line-height: 1.72;
  letter-spacing: -0.008em;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.14);
}

.wish-note-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px solid color-mix(in srgb, var(--note-divider) 78%, rgba(255, 255, 255, 0.24));
}

.wish-note-tag {
  display: inline-flex;
  align-items: center;
  max-width: 86px;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.06)), var(--note-tag-bg);
  border: 1px solid var(--note-tag-border);
  color: var(--note-tag-text);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.03em;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.58),
    0 6px 12px -14px var(--note-shadow);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wish-note-pin,
.wish-note-tape,
.wish-note-sticker {
  position: absolute;
  z-index: 50;
  pointer-events: none;
}

.wish-note-pin {
  top: 11px;
  left: 50%;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--note-pin-bg);
  color: rgba(255, 255, 255, 0.92);
  font-size: 8px;
  transform: translateX(-50%) rotate(-6deg);
  box-shadow:
    0 8px 14px -12px var(--note-pin-shadow),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.wish-note-tape {
  top: 10px;
  right: 20px;
  width: 44px;
  height: 14px;
  border-radius: 5px;
  border: 1px solid var(--note-tape-border);
  background: var(--note-tape-bg);
  box-shadow: 0 6px 12px -12px rgba(118, 111, 120, 0.18);
  transform: rotate(7deg);
  opacity: 0.78;
}

.wish-note-tape::before {
  content: '';
  position: absolute;
  inset: 2px 6px;
  border-top: 1px dashed rgba(255, 255, 255, 0.26);
  border-bottom: 1px dashed rgba(255, 255, 255, 0.12);
  opacity: 0.34;
}

.wish-note-sticker {
  right: 14px;
  bottom: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: var(--note-sticker-bg);
  color: var(--note-sticker-text);
  font-size: 10px;
  box-shadow:
    0 8px 14px -16px var(--note-shadow-strong),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  opacity: 0.86;
}

.wish-content {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wish-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 38px;
  min-height: 24px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.26), rgba(255, 255, 255, 0.04)), var(--note-action-bg);
  border: 1px solid var(--note-action-border);
  color: var(--note-action-text);
  font-size: 9.5px;
  font-weight: 500;
  padding: 0 7px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.54),
    0 6px 12px -14px var(--note-action-shadow);
  transition:
    transform 0.24s ease,
    background 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease,
    color 0.24s ease;
}

.wish-action:hover {
  transform: translateY(-1px);
  background: var(--note-action-bg-hover);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.64),
    0 8px 14px -14px var(--note-action-shadow);
}

.wish-action:active {
  transform: translateY(0) scale(0.96);
}

.dark .wish-action {
  background: var(--note-action-bg);
  border: 1px solid var(--note-action-border);
}

.wish-action:hover {
  transform: translateY(-1px);
  background: var(--note-action-bg-hover);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 12px 18px -18px var(--note-action-shadow);
}

.wish-action:active {
  transform: translateY(0) scale(0.965);
}

.wish-action-liked {
  color: color-mix(in srgb, var(--note-tag-text) 72%, #d47c9b 28%);
  border-color: color-mix(in srgb, var(--note-tag-border) 78%, rgba(234, 190, 208, 0.96));
  background: color-mix(in srgb, var(--note-action-bg-hover) 82%, rgba(255, 244, 248, 0.82));
}

.wish-action-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.wish-action-icon svg {
  width: 11px;
  height: 11px;
}

.wish-publish-btn {
  box-shadow:
    0 20px 34px -26px rgba(221, 162, 180, 0.34),
    0 10px 18px -16px rgba(221, 162, 180, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.28s ease;
}

.wish-publish-btn:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow:
    0 24px 38px -26px rgba(221, 162, 180, 0.36),
    0 14px 20px -16px rgba(221, 162, 180, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.24);
  filter: saturate(1.02);
}

.wish-publish-btn:active {
  transform: translateY(0) scale(0.982);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 8px;
}

.submit-note-overlay {
  --submit-bg-page: #f6f7fb;
  --submit-bg-modal: rgba(255, 255, 255, 0.78);
  --submit-bg-card: rgba(255, 255, 255, 0.62);
  --submit-bg-input: rgba(250, 250, 252, 0.9);
  --submit-bg-soft: rgba(246, 247, 251, 0.86);
  --submit-border-light: rgba(202, 207, 216, 0.28);
  --submit-border-soft: rgba(214, 218, 226, 0.42);
  --submit-divider: rgba(225, 228, 235, 0.72);
  --submit-text-primary: #2b2f38;
  --submit-text-secondary: #6f7684;
  --submit-text-tertiary: #a2a8b5;
  --submit-text-placeholder: #b7bcc8;
  --submit-brand: #e79ab0;
  --submit-brand-hover: #dc89a3;
  --submit-brand-soft: rgba(231, 154, 176, 0.12);
  --submit-brand-border: rgba(231, 154, 176, 0.22);
  --submit-accent-blue: #a8bfdc;
  --submit-accent-lilac: #c8c2dc;
  --submit-accent-silver: #d8dde7;
  --submit-ease: cubic-bezier(0.22, 1, 0.36, 1);
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(96, 103, 116, 0.24);
}

.dark .submit-note-overlay {
  --submit-bg-page: #15111e;
  --submit-bg-modal: rgba(22, 18, 32, 0.88);
  --submit-bg-card: rgba(34, 28, 47, 0.8);
  --submit-bg-input: rgba(28, 22, 40, 0.94);
  --submit-bg-soft: rgba(22, 18, 32, 0.92);
  --submit-border-light: rgba(255, 255, 255, 0.08);
  --submit-border-soft: rgba(255, 255, 255, 0.14);
  --submit-divider: rgba(255, 255, 255, 0.08);
  --submit-text-primary: #f3eefb;
  --submit-text-secondary: #c0b5d0;
  --submit-text-tertiary: #958aa7;
  --submit-text-placeholder: #7d738f;
  --submit-brand: #d7bfdc;
  --submit-brand-hover: #e6c6d8;
  --submit-brand-soft: rgba(215, 191, 220, 0.14);
  --submit-brand-border: rgba(215, 191, 220, 0.22);
  --submit-accent-blue: #9ab1d5;
  --submit-accent-lilac: #b8aad7;
  --submit-accent-silver: #9e9bb7;
  background: rgba(9, 7, 15, 0.58);
}

.dark .submit-note-dialog,
.dark .submit-note-city-row {
  box-shadow: 0 24px 54px rgba(0, 0, 0, 0.28);
}

.dark .submit-note-header,
.dark .submit-note-footer {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0));
}

.dark .submit-note-close {
  background: rgba(32, 26, 45, 0.88);
  color: var(--submit-text-secondary);
}

.dark .submit-note-close:hover {
  background: rgba(38, 31, 52, 0.94);
}

.submit-note-shell {
  display: flex;
  min-height: 100%;
  width: 100%;
  align-items: center;
  justify-content: center;
}

.submit-note-dialog {
  width: 100%;
  max-width: 760px;
  max-height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: 28px;
  background: var(--submit-bg-page);
  box-shadow: 0 28px 80px -42px rgba(43, 47, 56, 0.24), 0 18px 36px -30px rgba(43, 47, 56, 0.12);
  will-change: transform, opacity;
}

.submit-note-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--submit-divider);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
}

.submit-note-title {
  font-size: 25px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--submit-text-primary);
}

.submit-note-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border: 1px solid var(--submit-brand-border);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(231, 154, 176, 0.13), rgba(231, 154, 176, 0.08));
  color: #b37790;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.submit-note-subtitle {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--submit-text-secondary);
}

.submit-note-close {
  display: inline-flex;
  height: 36px;
  width: 36px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--submit-border-light);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.48);
  color: var(--submit-text-tertiary);
  transition:
    background 0.2s var(--submit-ease),
    color 0.2s var(--submit-ease),
    transform 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    border-color 0.2s var(--submit-ease);
}

.submit-note-close:hover {
  background: rgba(255, 255, 255, 0.88);
  color: var(--submit-text-secondary);
  border-color: var(--submit-border-soft);
  transform: translateY(-1px);
  box-shadow: 0 10px 22px -18px rgba(43, 47, 56, 0.22);
}

.submit-note-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 24px 20px;
}

.submit-note-input,
.submit-note-textarea {
  width: 100%;
  border: 1px solid var(--submit-border-light);
  background: var(--submit-bg-input);
  color: var(--submit-text-primary);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 12px 24px -26px rgba(43, 47, 56, 0.16);
  transition:
    border-color 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease);
  border-radius: 16px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
}

.submit-note-input {
  min-height: 48px;
  margin-top: 14px;
}

.submit-note-star-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  margin-bottom: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(248, 154, 165, 0.1), rgba(248, 154, 165, 0.05));
}

.submit-note-star-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f89aa8, #f6a4b1);
  color: white;
  box-shadow: 0 8px 16px -8px rgba(248, 154, 165, 0.4);
}

.submit-note-star-icon svg {
  width: 22px;
  height: 22px;
}

.submit-note-star-text h4 {
  font-size: 15px;
  font-weight: 700;
  color: var(--submit-text-primary);
  margin: 0;
  line-height: 1.3;
}

.submit-note-star-text p {
  font-size: 12px;
  color: var(--submit-text-secondary);
  margin: 4px 0 0;
}

.submit-note-char-count {
  text-align: right;
  margin-top: 8px;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--submit-text-tertiary);
}

.submit-note-city-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  padding: 0 14px;
  border: 1px solid var(--submit-border-light);
  border-radius: 14px;
  background: var(--submit-bg-input);
}

.submit-note-city-icon {
  width: 18px;
  height: 18px;
  color: var(--submit-text-tertiary);
}

.submit-note-city-input {
  flex: 1;
  min-height: 44px;
  border: none;
  background: transparent;
  color: var(--submit-text-primary);
  font-size: 14px;
  outline: none;
}

.submit-note-textarea {
  min-height: 120px;
  padding: 14px 16px;
  resize: none;
  line-height: 1.6;
}

.submit-note-input::placeholder,
.submit-note-textarea::placeholder {
  color: var(--submit-text-placeholder);
}

.submit-note-input:hover,
.submit-note-textarea:hover {
  border-color: var(--submit-border-soft);
  background: rgba(255, 255, 255, 0.94);
}

.submit-note-input:focus,
.submit-note-textarea:focus {
  border-color: var(--submit-brand-border);
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 12px 28px -24px rgba(231, 154, 176, 0.28),
    0 0 0 4px rgba(231, 154, 176, 0.1);
}

.submit-note-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid var(--submit-divider);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.16));
}

.submit-note-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  min-width: 96px;
  border-radius: 14px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  transition:
    transform 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease);
  cursor: pointer;
}

.submit-note-btn-secondary {
  border: 1px solid var(--submit-border-light);
  background: rgba(255, 255, 255, 0.64);
  color: var(--submit-text-secondary);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 18px -16px rgba(43, 47, 56, 0.14);
}

.submit-note-btn-secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.88);
  border-color: var(--submit-border-soft);
  transform: translateY(-1px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 14px 24px -18px rgba(43, 47, 56, 0.18);
}

.submit-note-btn-primary {
  border: 1px solid rgba(255, 169, 181, 0.44);
  background: linear-gradient(135deg, #ffa9b5, #f6a4b1);
  color: #fff;
  box-shadow:
    0 18px 30px -22px rgba(255, 169, 181, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.submit-note-btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff9ead, #f19fad);
  box-shadow: 0 22px 36px -22px rgba(255, 169, 181, 0.4);
}

.submit-note-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-note-fade-enter-active,
.submit-note-fade-leave-active {
  transition: opacity 0.34s var(--submit-ease), backdrop-filter 0.34s var(--submit-ease);
}

.submit-note-fade-enter-active .submit-note-dialog,
.submit-note-fade-leave-active .submit-note-dialog {
  transition:
    opacity 0.34s var(--submit-ease),
    transform 0.34s var(--submit-ease),
    box-shadow 0.34s var(--submit-ease);
}

.submit-note-fade-enter-from,
.submit-note-fade-leave-to {
  opacity: 0;
  backdrop-filter: blur(0);
}

.submit-note-fade-enter-from .submit-note-dialog,
.submit-note-fade-leave-to .submit-note-dialog {
  opacity: 0;
  transform: translateY(16px) scale(0.985);
  box-shadow: 0 10px 30px -24px rgba(43, 47, 56, 0.1);
}

@media (max-width: 767px) {
  .wish-note-card {
    border-radius: 24px 24px 20px 20px;
  }

  .wish-note-card::after {
    border-radius: 23px 23px 19px 19px;
  }

  .wish-note-inner {
    padding: 15px 14px 13px;
  }

  .wish-note-meta {
    margin-bottom: 9px;
  }

  .wish-note-source-mark {
    width: 15px;
    height: 15px;
  }

  .wish-note-source-mark svg {
    width: 7px;
    height: 7px;
  }

  .wish-note-source-label,
  .wish-note-date {
    font-size: 9px;
  }

  .wish-note-tag {
    min-height: 23px;
    padding: 0 9px;
    max-width: 82px;
  }

  .wish-note-sticker {
    width: 20px;
    height: 20px;
    right: 13px;
    bottom: 42px;
    font-size: 9px;
  }

  .submit-note-overlay {
    padding: 12px;
  }

  .submit-note-dialog {
    max-height: calc(100vh - 24px);
    border-radius: 24px;
  }

  .submit-note-header,
  .submit-note-body,
  .submit-note-footer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .submit-note-footer {
    flex-wrap: wrap;
  }

  .submit-note-btn {
    flex: 1 1 auto;
  }

  .wish-content {
    -webkit-line-clamp: 3;
    line-clamp: 3;
    font-size: 14px;
    line-height: 1.58;
  }

  .wish-action {
    font-size: 10px;
    min-width: 40px;
    min-height: 26px;
    padding: 0 8px;
  }

  .wish-action-icon svg {
    width: 10px;
    height: 10px;
  }
}
</style>
