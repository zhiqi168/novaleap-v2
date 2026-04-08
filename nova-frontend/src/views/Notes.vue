
<template>
  <div class="h-full relative overflow-hidden notes-shell workspace-page workspace-shell sm:mx-4">
    <div class="relative z-10 h-full flex flex-col lg:flex-row">
      <aside class="w-full lg:w-[430px] xl:w-[460px] border-b lg:border-b-0 lg:border-r border-border-subtle bg-bg-surface backdrop-blur-xl flex flex-col h-[48%] lg:h-full shrink-0">
        <div class="px-4 pt-4 pb-3 border-b border-border-subtle">
          <div class="flex items-center justify-between gap-2">
            <div>
              <h2 class="text-2xl leading-none font-bold text-text-primary">灵感手记</h2>
              <p class="mt-1 text-sm text-text-secondary">沉淀思考，记录成长路径</p>
            </div>
            <div class="flex items-center gap-2">
              <button class="workspace-btn workspace-btn-muted px-3 py-1.5 rounded-lg text-xs border border-black/10 bg-white/90 text-slate-600 hover:bg-black/[0.03] transition-colors" @click="loadCurrentNotes">刷新</button>
              <button
                class="workspace-btn px-3 py-1.5 rounded-lg text-xs border transition-colors"
                :class="canSubmitNote ? 'border-ai-from/35 text-ai-from bg-ai-from/10 hover:bg-ai-from/16' : 'border-black/10 text-slate-400 cursor-not-allowed'"
                @click="openSubmitDialog"
              >
                投稿手记
              </button>
            </div>
          </div>

          <div class="mt-3 relative">
            <input
              v-model="searchQuery"
              @keyup.enter="loadCurrentNotes"
              type="text"
              class="workspace-control w-full rounded-xl border border-black/10 bg-black/[0.02] py-2.5 pl-10 pr-4 text-sm outline-none transition-all focus:border-ai-from/30 focus:bg-white focus:ring-2 focus:ring-ai-from/20"
              placeholder="搜索手记标题..."
            />
            <svg class="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>

          <div class="mt-3 flex items-center gap-2 text-xs">
            <button
              class="workspace-chip px-2.5 py-1 rounded-full border transition-colors"
              :class="isViewingMine ? 'border-black/10 text-slate-500 bg-white/70 hover:bg-white' : 'border-indigo-200 text-indigo-600 bg-indigo-50'"
              @click="switchNoteMode('published')"
            >
              已发布
            </button>
            <button
              class="px-2.5 py-1 rounded-full border transition-colors"
              :class="isViewingMine ? 'border-ai-from/35 text-ai-from bg-ai-from/10' : 'border-black/10 text-slate-500 bg-white/70 hover:bg-white'"
              @click="switchNoteMode('mine')"
            >
              我的投稿
            </button>
          </div>

          <div v-if="isViewingMine" class="mt-2 flex items-center gap-1.5 text-xs">
            <button
              v-for="item in mineStatusOptions"
              :key="item.value"
              class="workspace-chip px-2 py-1 rounded-full border transition-colors"
              :class="mineStatusFilter === item.value ? 'border-sky-200 text-sky-600 bg-sky-50' : 'border-black/10 text-slate-500 bg-white/70 hover:bg-white'"
              @click="setMineStatusFilter(item.value)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="mt-3 flex items-center justify-between text-xs text-slate-500">
            <span>{{ notesCountText }}</span>
            <span v-if="activeNote">当前阅读 #{{ activeNote.id }}</span>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto px-2 py-2 custom-scrollbar">
          <div v-if="loading" class="workspace-empty p-8 text-center text-sm text-slate-500">手记加载中...</div>
          <div v-else-if="!notes.length" class="p-8 text-center text-sm text-slate-500">暂无手记内容</div>

          <button
            v-for="note in notes"
            :key="note.id"
            class="workspace-list-item w-full text-left rounded-2xl mb-2 border p-3 transition-all duration-200 note-item"
            :class="activeNote?.id === note.id ? 'workspace-list-item-active border-ai-from/35 bg-ai-from/8 shadow-[0_14px_28px_-20px_rgba(99,102,241,0.55)]' : 'border-black/8 bg-white/80 hover:bg-white hover:border-black/14'"
            @click="selectNote(note)"
          >
            <div class="flex gap-3">
              <div class="w-14 h-14 rounded-xl shrink-0 grid place-items-center border border-black/8 bg-gradient-to-br from-slate-50 to-white text-[28px] shadow-sm">{{ note.emoji }}</div>

              <div class="min-w-0 flex-1">
                <h3 class="note-title text-[20px] font-bold leading-tight text-slate-800" :class="{ 'text-indigo-600': activeNote?.id === note.id }">{{ note.title }}</h3>
                <p class="note-summary mt-1 text-xs text-slate-500">{{ note.summary || '这篇手记暂未设置摘要。' }}</p>
                <div v-if="isViewingMine" class="mt-2 flex items-center justify-end gap-1.5">
                  <button
                    class="px-2 py-1 rounded-md text-[11px] border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100 transition-colors"
                    @click.stop="openEditDialogFor(note)"
                  >
                    编辑
                  </button>
                  <button
                    class="px-2 py-1 rounded-md text-[11px] border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 transition-colors disabled:opacity-60"
                    :disabled="deletingNote"
                    @click.stop="deleteNote(note)"
                  >
                    删除
                  </button>
                </div>
                <div class="mt-2 flex items-center justify-between text-[11px] text-slate-500">
                  <span class="inline-flex items-center gap-1.5">
                    <span>{{ note.date }}</span>
                    <span v-if="isViewingMine" class="px-1.5 py-0.5 rounded-full border text-[10px]" :class="statusClass(note.status)">{{ statusText(note.status) }}</span>
                    <span v-if="isViewingMine && Number(note.status) === 2" class="text-rose-600 max-w-[160px] truncate" :title="note.rejectReason || '管理端未填写原因'">
                      原因：{{ note.rejectReason || '管理端未填写原因' }}
                    </span>
                  </span>
                  <span class="inline-flex items-center gap-2.5">
                    <span class="inline-flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                      {{ note.views }}
                    </span>
                    <span class="inline-flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                      {{ note.likeCount || 0 }}
                    </span>
                    <span class="inline-flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 4v-4z" /></svg>
                      {{ note.commentCount || 0 }}
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </button>
        </div>
      </aside>

      <section class="flex-1 overflow-y-auto custom-scrollbar">
        <template v-if="activeNote">
          <div class="max-w-[1020px] mx-auto px-5 md:px-8 xl:px-10 py-7 space-y-5">
            <header class="rounded-3xl border border-border-subtle bg-bg-surface backdrop-blur-xl p-5 md:p-7 shadow-card">
              <div class="text-xs text-text-secondary tracking-wide">手记 / {{ activeNote.category }}</div>
              <h1 class="mt-2 text-2xl md:text-3xl leading-tight font-bold text-text-primary">{{ activeNote.title }}</h1>
              <div class="mt-4 flex flex-wrap items-center gap-2 text-xs">
                <span class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">{{ activeNote.author || 'Nova 学员' }}</span>
                <span class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">{{ activeNote.date }}</span>
                <span class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">{{ activeNote.words }} 字</span>
                <span class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">浏览 {{ activeNote.views }}</span>
                <span class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">点赞 {{ activeNote.likeCount || 0 }}</span>
                <span class="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">评论 {{ activeNote.commentCount || 0 }}</span>
                <span v-if="isViewingMine" class="px-2.5 py-1 rounded-full border text-xs" :class="statusClass(activeNote.status)">状态：{{ statusText(activeNote.status) }}</span>
                <span
                  v-if="isViewingMine && Number(activeNote.status) === 2"
                  class="px-2.5 py-1 rounded-full border border-rose-200 bg-rose-50 text-rose-700"
                >
                  失败原因：{{ activeNote.rejectReason || '管理端未填写原因' }}
                </span>
              </div>

              <div class="mt-5 flex flex-wrap items-center gap-2">
                <button
                  class="px-3.5 py-2 rounded-xl text-sm border border-ai-from/30 bg-ai-from/10 text-ai-from hover:bg-ai-from/15 transition-colors"
                  @click="generateSummary"
                  :disabled="isAiGenerating"
                >
                  {{ isAiGenerating ? 'Nova 正在提炼中...' : '生成 Nova AI 智能摘要' }}
                </button>
                <button
                  v-if="isViewingMine"
                  class="px-3.5 py-2 rounded-xl text-sm border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100 transition-colors"
                  @click="openEditDialogFor(activeNote)"
                  :disabled="!activeNote?.id"
                >
                  编辑手记
                </button>
                <button
                  v-if="isViewingMine"
                  class="px-3.5 py-2 rounded-xl text-sm border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 transition-colors disabled:opacity-60"
                  @click="deleteNote(activeNote)"
                  :disabled="deletingNote || !activeNote?.id"
                >
                  {{ deletingNote ? '删除中...' : '删除手记' }}
                </button>
              </div>
            </header>

            <AiSummaryBlock v-if="aiSummaries.length || isAiGenerating" class="animate-fade-in-up">
              <div v-if="isAiGenerating" class="flex items-center gap-3 text-slate-600">
                <LoadingDots />
                <span>Nova 正在提炼核心要点...</span>
              </div>
              <ul v-else class="space-y-3">
                <li v-for="(item, idx) in aiSummaries" :key="idx" class="flex gap-3">
                  <span class="shrink-0 w-5 h-5 rounded-full bg-ai-from/20 text-ai-from flex items-center justify-center text-xs font-bold">{{ idx + 1 }}</span>
                  <span class="leading-relaxed text-slate-700">{{ item }}</span>
                </li>
              </ul>
            </AiSummaryBlock>

            <article class="relative rounded-3xl border border-border-subtle bg-bg-elevated backdrop-blur-xl shadow-card overflow-hidden">
              <div class="pointer-events-none absolute inset-0 z-0">
                <div class="absolute -top-10 right-8 h-32 w-32 rounded-full bg-indigo-200/10 blur-3xl"></div>
                <div class="absolute -bottom-12 left-14 h-36 w-36 rounded-full bg-cyan-200/10 blur-3xl"></div>
              </div>
              <div class="relative z-10 px-6 py-7 md:px-10 md:py-9 min-h-[52vh]">
                <div class="prose prose-slate dark:prose-invert max-w-none text-[16px] leading-8 prose-shell">
                  <TypeWriter :text="activeNote.content" :renderMarkdown="true" :isTyping="false" />
                </div>
              </div>
              <div class="note-fab">
                <button class="note-action" :class="{ 'note-action-liked': activeNote.likedByMe }" :disabled="likingNote || !canInteractActiveNote || authStore.isGuest" @click="toggleLike">
                  <span>{{ activeNote.likedByMe ? '❤️' : '🤍' }}</span>
                  <span>{{ activeNote.likeCount || 0 }}</span>
                </button>

                <button class="note-action" :disabled="!canInteractActiveNote" @click="openComments">
                  <span>💬</span>
                  <span>{{ activeNote.commentCount || 0 }}</span>
                </button>
              </div>
            </article>
          </div>
        </template>

        <div v-else class="h-full grid place-items-center text-slate-500">
          <div class="text-center px-6">
            <div class="w-24 h-24 rounded-2xl bg-white/80 border border-black/8 shadow-sm mx-auto mb-4 grid place-items-center">
              <svg class="w-11 h-11 opacity-35" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </div>
            <p class="text-xl font-semibold text-slate-700">请选择一篇手记开始阅读</p>
            <p class="mt-1 text-sm text-slate-500">左侧支持搜索和快速切换，阅读数据会实时同步。</p>
          </div>
        </div>
      </section>
    </div>

    <div v-if="showSubmitDialog" class="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm flex items-center justify-center p-4" @click.self="closeSubmitDialog">
      <div class="w-full max-w-2xl bg-bg-card rounded-2xl border border-border-soft shadow-2xl">
        <div class="px-5 py-4 border-b border-black/8 flex items-center justify-between">
          <h3 class="text-base font-semibold text-slate-800">{{ isEditingNote ? '编辑手记（保存后自动复审）' : '投稿手记（AI 自动审核）' }}</h3>
          <button class="p-1.5 rounded hover:bg-black/5 text-slate-500" @click="closeSubmitDialog">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div class="px-5 py-4 space-y-3">
          <input v-model="submitForm.title" type="text" class="workspace-control w-full px-3 py-2 rounded-lg border border-border-soft bg-bg-surface text-sm outline-none focus:ring-2 focus:ring-ai-from/25" placeholder="请输入手记标题" />
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <input v-model="submitForm.category" type="text" class="w-full px-3 py-2 rounded-lg border border-border-soft bg-bg-surface text-sm outline-none focus:ring-2 focus:ring-ai-from/25" placeholder="分类（如 后端）" />
            <div class="rounded-lg border border-border-soft bg-bg-surface px-2 py-1.5">
              <div class="text-[11px] text-slate-500">封面图标</div>
              <div class="mt-1 flex items-center gap-1.5 flex-wrap">
                <button
                  v-for="emoji in noteEmojiOptions"
                  :key="emoji"
                  type="button"
                  class="w-8 h-8 rounded-md border text-base leading-none transition-colors"
                  :class="submitForm.emoji === emoji ? 'border-ai-from bg-ai-from/10' : 'border-black/10 hover:bg-black/[0.03]'"
                  @click="submitForm.emoji = emoji"
                >
                  {{ emoji }}
                </button>
              </div>
            </div>
            <input v-model="submitForm.summary" type="text" class="w-full px-3 py-2 rounded-lg border border-border-soft bg-bg-surface text-sm outline-none focus:ring-2 focus:ring-ai-from/25" placeholder="摘要（可选）" />
          </div>
          <textarea v-model="submitForm.content" rows="10" class="workspace-control w-full px-3 py-2 rounded-lg border border-border-soft bg-bg-surface text-sm outline-none resize-y focus:ring-2 focus:ring-ai-from/25" placeholder="正文内容（支持 Markdown）"></textarea>
          <p class="text-xs text-slate-500">{{ isEditingNote ? '保存后会重新进行违禁词审核：通过则立即展示，不通过会返回具体失败原因。' : '提交后会先进行违禁词审核：通过则立即展示，不通过会返回具体失败原因。' }}</p>
        </div>

        <div class="px-5 py-4 border-t border-black/8 flex justify-end gap-2">
          <button class="workspace-btn workspace-btn-muted px-3 py-2 rounded-lg border border-black/10 text-sm hover:bg-black/[0.03]" @click="closeSubmitDialog">取消</button>
          <button class="workspace-btn workspace-btn-primary px-3 py-2 rounded-lg text-sm text-white bg-gradient-to-r from-ai-from to-ai-to disabled:opacity-60" :disabled="submittingNote" @click="submitNote">{{ submittingNote ? (isEditingNote ? '保存中...' : '提交中...') : (isEditingNote ? '保存修改' : '提交审核') }}</button>
        </div>
      </div>
    </div>
    <div v-if="commentPanelVisible" class="fixed inset-0 z-[1200] bg-black/25 backdrop-blur-[2px]" @click.self="closeComments">
      <aside class="absolute right-0 top-0 h-full w-full max-w-[430px] bg-bg-card backdrop-blur-xl shadow-2xl border-l border-border-soft flex flex-col">
        <header class="px-5 py-4 border-b border-black/5 flex items-center justify-between">
          <div>
            <h3 class="text-base font-bold text-slate-800">手记评论</h3>
            <p class="text-xs text-slate-500 mt-1 line-clamp-1">{{ activeNote?.title || '' }}</p>
          </div>
          <button class="workspace-btn workspace-btn-muted px-2.5 py-1 rounded-md bg-black/5 hover:bg-black/10 text-sm" @click="closeComments">关闭</button>
        </header>

        <div class="flex-1 overflow-y-auto px-5 py-4 custom-scrollbar">
          <div v-if="loadingComments" class="text-sm text-slate-500 py-8 text-center">评论加载中...</div>
          <div v-else-if="comments.length === 0" class="text-sm text-slate-500 py-8 text-center">还没有评论，来做第一个留言的人吧。</div>
          <div v-else class="space-y-3">
            <div v-for="comment in comments" :key="comment.id" class="rounded-xl bg-black/[0.03] border border-black/[0.05] p-3">
              <div class="flex items-center justify-between gap-2 mb-1">
                <div class="text-sm font-semibold text-slate-800">
                  {{ comment.nickname || comment.username || '用户' }}
                  <span v-if="comment.mine" class="ml-1 text-[10px] px-1.5 py-0.5 rounded bg-ai-from/15 text-ai-from">我</span>
                </div>
                <div class="text-[11px] text-slate-500">{{ formatDateTime(comment.createdAt) }}</div>
              </div>
              <p class="text-sm text-slate-700 leading-relaxed break-words">{{ comment.content }}</p>
            </div>
          </div>
        </div>

        <footer class="px-5 py-4 border-t border-black/5">
          <div v-if="authStore.isGuest" class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2 mb-3">游客账号仅可浏览评论，发布评论请先使用正式账号登录。</div>
          <textarea
            v-model="newCommentContent"
            :disabled="!authStore.isLoggedIn || authStore.isGuest || submittingComment"
            maxlength="300"
            rows="3"
            placeholder="写下你的评论..."
            class="workspace-control w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25 resize-none disabled:bg-gray-50 disabled:text-gray-400"
          ></textarea>
          <div class="mt-3 flex items-center justify-between">
            <span class="text-xs text-slate-500">{{ newCommentContent.length }}/300</span>
            <button
              :disabled="submittingComment || !newCommentContent.trim() || !authStore.isLoggedIn || authStore.isGuest"
              class="workspace-btn workspace-btn-primary px-3 py-2 rounded-lg text-sm text-white bg-gradient-to-r from-ai-from to-ai-to disabled:opacity-50"
              @click="submitComment"
            >
              {{ submittingComment ? '发送中...' : '发送评论' }}
            </button>
          </div>
        </footer>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import AiSummaryBlock from '@/components/common/AiSummaryBlock.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'
import TypeWriter from '@/components/common/TypeWriter.vue'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const notes = ref([])
const loading = ref(false)
const searchQuery = ref('')
const activeNote = ref(null)
const aiSummaries = ref([])
const isAiGenerating = ref(false)
const listMode = ref('published')
const mineStatusFilter = ref('')

const showSubmitDialog = ref(false)
const submittingNote = ref(false)
const submitDialogMode = ref('create')
const editingNoteId = ref(null)
const deletingNote = ref(false)
const noteEmojiOptions = ['📘', '📗', '📙', '📕', '📝', '💡', '🧠', '🔧']
const submitForm = reactive({
  title: '',
  category: '技术手记',
  emoji: noteEmojiOptions[0],
  summary: '',
  content: '',
})

const likingNote = ref(false)
const commentPanelVisible = ref(false)
const comments = ref([])
const loadingComments = ref(false)
const submittingComment = ref(false)
const newCommentContent = ref('')

let viewSyncTimer = null

const canSubmitNote = computed(() => authStore.isLoggedIn && !authStore.isGuest)
const isViewingMine = computed(() => listMode.value === 'mine')
const canInteractActiveNote = computed(() => !!activeNote.value?.id)
const isEditingNote = computed(() => submitDialogMode.value === 'edit' && !!editingNoteId.value)

const notesCountText = computed(() => (isViewingMine.value
  ? `共 ${notes.value.length} 篇我的投稿`
  : `共 ${notes.value.length} 篇已发布手记`))

const mineStatusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: '0' },
  { label: '已通过', value: '1' },
  { label: '审核失败', value: '2' },
]

const statusText = (status) => {
  if (status === 0 || status === '0') return '待审核'
  if (status === 1 || status === '1') return '已通过'
  if (status === 2 || status === '2') return '审核失败'
  return '未知'
}

const statusClass = (status) => {
  if (status === 0 || status === '0') return 'border-amber-200 bg-amber-50 text-amber-700'
  if (status === 1 || status === '1') return 'border-emerald-200 bg-emerald-50 text-emerald-700'
  if (status === 2 || status === '2') return 'border-rose-200 bg-rose-50 text-rose-700'
  return 'border-slate-200 bg-slate-50 text-slate-600'
}

const pickRejectReason = (item) => {
  const candidates = [
    item?.rejectReason,
    item?.reject_reason,
    item?.reason,
    item?.auditReason,
    item?.failReason,
    item?.fail_reason,
  ]
  const matched = candidates.find((val) => String(val || '').trim())
  return String(matched || '').trim()
}

const toNoteView = (item) => {
  const createdAt = item?.createdAt ? new Date(item.createdAt) : new Date()
  const content = String(item?.content || '')
  return {
    id: item?.id,
    title: item?.title || '未命名手记',
    summary: item?.summary || '',
    date: createdAt.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }),
    views: Number(item?.viewCount || 0),
    words: Number(item?.wordCount ?? content.length),
    emoji: item?.emoji || '📘',
    category: item?.category || '技术手记',
    content,
    author: item?.author || 'Nova 学员',
    status: item?.status ?? 1,
    likeCount: Number(item?.likeCount || 0),
    commentCount: Number(item?.commentCount || 0),
    likedByMe: !!item?.likedByMe,
    rejectReason: pickRejectReason(item),
  }
}

const buildPublishedNotesUrl = () => {
  let url = '/api/notes?page=1&size=80'
  const keyword = searchQuery.value.trim()
  if (keyword) {
    url += `&keyword=${encodeURIComponent(keyword)}`
  }
  return url
}

const buildMineNotesUrl = () => {
  let url = '/api/notes/mine?page=1&size=80'
  if (mineStatusFilter.value !== '') {
    url += `&status=${encodeURIComponent(mineStatusFilter.value)}`
  }
  return url
}

const hydrateNoteDetail = async (noteId) => {
  if (!noteId) return
  try {
    const res = await api.get(`/api/notes/${noteId}`)
    if (res.code !== 200 || !res.data) return
    const detail = toNoteView(res.data)
    activeNote.value = detail
    patchNoteInList(detail)
  } catch (_) {
    // noop
  }
}

const applyNotesPayload = (res) => {
  notes.value = (res.data?.records || []).map(toNoteView)
  if ((!activeNote.value || !notes.value.find((n) => n.id === activeNote.value.id)) && notes.value.length) {
    activeNote.value = notes.value[0]
    if (!activeNote.value.content) {
      void hydrateNoteDetail(activeNote.value.id)
    }
  }
}
const loadPublishedNotes = async () => {
  loading.value = true
  try {
    const res = await api.get(buildPublishedNotesUrl())
    if (res.code === 200) {
      applyNotesPayload(res)
    } else {
      notes.value = []
    }
  } catch (_) {
    notes.value = []
  } finally {
    loading.value = false
  }
}

const loadMineNotes = async () => {
  loading.value = true
  try {
    const res = await api.get(buildMineNotesUrl())
    if (res.code === 200) {
      const keyword = searchQuery.value.trim().toLowerCase()
      if (!keyword) {
        applyNotesPayload(res)
      } else {
        const filteredRecords = (res.data?.records || []).filter((item) => String(item?.title || '').toLowerCase().includes(keyword))
        applyNotesPayload({ ...res, data: { ...(res.data || {}), records: filteredRecords } })
      }
    } else {
      notes.value = []
      if (res.code === 403) {
        alert('游客账号不支持查看投稿中心。')
      }
    }
  } catch (e) {
    notes.value = []
    if (e?.message) {
      alert(e.message)
    }
  } finally {
    loading.value = false
  }
}

const loadCurrentNotes = async () => {
  if (isViewingMine.value) {
    await loadMineNotes()
    return
  }
  await loadPublishedNotes()
}

const switchNoteMode = async (mode) => {
  if (mode === listMode.value) return
  if (mode === 'mine') {
    if (!authStore.isLoggedIn) {
      alert('请先登录后查看我的投稿。')
      return
    }
    if (authStore.isGuest) {
      alert('游客账号不支持查看投稿中心。')
      return
    }
  }
  listMode.value = mode
  activeNote.value = null
  aiSummaries.value = []
  closeComments()
  await loadCurrentNotes()
}

const setMineStatusFilter = async (status) => {
  if (mineStatusFilter.value === status) return
  mineStatusFilter.value = status
  if (isViewingMine.value) {
    activeNote.value = null
    aiSummaries.value = []
    closeComments()
    await loadMineNotes()
  }
}

const syncViewCounts = async () => {
  if (!notes.value.length || isViewingMine.value) return
  try {
    const res = await api.get(buildPublishedNotesUrl())
    if (res.code !== 200) return
    const fresh = (res.data?.records || []).map(toNoteView)
    const viewMap = new Map(fresh.map((n) => [n.id, n.views]))
    notes.value = notes.value.map((n) => (viewMap.has(n.id) ? { ...n, views: viewMap.get(n.id) } : n))
    if (activeNote.value && viewMap.has(activeNote.value.id)) {
      activeNote.value = { ...activeNote.value, views: viewMap.get(activeNote.value.id) }
    }
  } catch (_) {
    // noop
  }
}

const patchNoteInList = (next) => {
  if (!next?.id) return
  notes.value = notes.value.map((item) => (item.id === next.id ? { ...item, ...next } : item))
}

const toggleLike = async () => {
  if (!activeNote.value?.id || likingNote.value) return
  if (!authStore.isLoggedIn) {
    alert('请先登录后再点赞。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能点赞，请先登录正式账号。')
    return
  }

  likingNote.value = true
  try {
    const res = await api.post(`/api/notes/${activeNote.value.id}/like`, {})
    if (res.code !== 200) {
      throw new Error(res.msg || '点赞失败')
    }
    const next = {
      id: activeNote.value.id,
      likedByMe: !!res.data?.liked,
      likeCount: Number(res.data?.likeCount || 0),
    }
    activeNote.value = { ...activeNote.value, ...next }
    patchNoteInList(next)
  } catch (e) {
    alert(e.message || '点赞失败，请稍后重试')
  } finally {
    likingNote.value = false
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const dt = new Date(dateTime)
  if (Number.isNaN(dt.getTime())) return ''
  return dt.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const loadComments = async (noteId) => {
  if (!noteId) return
  loadingComments.value = true
  try {
    const res = await api.get(`/api/notes/${noteId}/comments`)
    if (res.code !== 200) {
      throw new Error(res.msg || '评论加载失败')
    }
    comments.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    comments.value = []
    alert(e.message || '评论加载失败')
  } finally {
    loadingComments.value = false
  }
}

const openComments = async () => {
  if (!activeNote.value?.id) return
  commentPanelVisible.value = true
  newCommentContent.value = ''
  await loadComments(activeNote.value.id)
}

const closeComments = () => {
  commentPanelVisible.value = false
  comments.value = []
  newCommentContent.value = ''
}

const submitComment = async () => {
  if (!activeNote.value?.id) return
  if (!authStore.isLoggedIn) {
    alert('请先登录后评论。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能评论，请先登录正式账号。')
    return
  }
  if (!newCommentContent.value.trim()) return

  submittingComment.value = true
  try {
    const res = await api.post(`/api/notes/${activeNote.value.id}/comments`, {
      content: newCommentContent.value.trim(),
    })
    if (res.code !== 200) {
      throw new Error(res.msg || '评论发送失败')
    }
    comments.value = [res.data, ...comments.value]
    newCommentContent.value = ''
    const next = {
      id: activeNote.value.id,
      commentCount: Number(activeNote.value.commentCount || 0) + 1,
    }
    activeNote.value = { ...activeNote.value, ...next }
    patchNoteInList(next)
  } catch (e) {
    alert(e.message || '评论发送失败，请稍后重试')
  } finally {
    submittingComment.value = false
  }
}

const selectNote = (note) => {
  const isCurrentSelected = String(activeNote.value?.id ?? '') === String(note?.id ?? '')
  activeNote.value = note
  aiSummaries.value = []
  isAiGenerating.value = false
  if (commentPanelVisible.value) {
    closeComments()
  }

  const viewApi = isCurrentSelected || isViewingMine.value || Number(note.status) !== 1
    ? api.get(`/api/notes/${note.id}`)
    : api.post(`/api/notes/${note.id}/view`)

  viewApi
    .then((res) => {
      if (res.code !== 200 || !res.data) return
      const detail = toNoteView(res.data)
      activeNote.value = detail
      notes.value = notes.value.map((n) => (n.id === detail.id ? detail : n))
    })
    .catch(() => {})
}

const generateSummary = async () => {
  if (!activeNote.value) return
  isAiGenerating.value = true
  aiSummaries.value = []
  try {
    const res = await api.post('/api/ai/notes/summarize', {
      title: activeNote.value.title,
      content: activeNote.value.content,
    })
    if (res.code !== 200) {
      throw new Error(res.msg || 'AI 摘要生成失败')
    }
    aiSummaries.value = Array.isArray(res.data)
      ? res.data.filter((item) => typeof item === 'string' && item.trim())
      : []
    if (!aiSummaries.value.length) {
      throw new Error('AI 未返回有效摘要')
    }
  } catch (e) {
    alert(e.message || 'AI 摘要生成失败，请稍后重试。')
  } finally {
    isAiGenerating.value = false
  }
}
const resetSubmitForm = () => {
  submitForm.title = ''
  submitForm.category = '技术手记'
  submitForm.emoji = noteEmojiOptions[0]
  submitForm.summary = ''
  submitForm.content = ''
}

const fillSubmitFormByNote = (note) => {
  submitForm.title = note?.title || ''
  submitForm.category = note?.category || '技术手记'
  submitForm.emoji = noteEmojiOptions.includes(note?.emoji) ? note.emoji : noteEmojiOptions[0]
  submitForm.summary = note?.summary || ''
  submitForm.content = note?.content || ''
}

const openSubmitDialog = () => {
  if (!authStore.isLoggedIn) {
    alert('请先登录后再投稿手记。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能投稿，请注册/登录正式账号。')
    return
  }
  submitDialogMode.value = 'create'
  editingNoteId.value = null
  resetSubmitForm()
  showSubmitDialog.value = true
}

const openEditDialogFor = async (note) => {
  if (!isViewingMine.value || !note?.id) return
  if (!authStore.isLoggedIn) {
    alert('请先登录后再编辑手记。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能编辑手记，请注册/登录正式账号。')
    return
  }

  let editableNote = note
  if (!editableNote.content) {
    try {
      const res = await api.get(`/api/notes/${note.id}`)
      if (res.code === 200 && res.data) {
        editableNote = toNoteView(res.data)
        patchNoteInList(editableNote)
      }
    } catch (_) {
      // noop
    }
  }

  activeNote.value = editableNote
  submitDialogMode.value = 'edit'
  editingNoteId.value = editableNote.id
  fillSubmitFormByNote(editableNote)
  showSubmitDialog.value = true
}

const closeSubmitDialog = () => {
  showSubmitDialog.value = false
  submitDialogMode.value = 'create'
  editingNoteId.value = null
}

const submitNote = async () => {
  if (!submitForm.title.trim()) {
    alert('标题不能为空。')
    return
  }
  if (!submitForm.content.trim()) {
    alert('正文不能为空。')
    return
  }

  submittingNote.value = true
  try {
    const selectedEmoji = noteEmojiOptions.includes(submitForm.emoji)
      ? submitForm.emoji
      : noteEmojiOptions[0]
    const payload = {
      title: submitForm.title.trim(),
      content: submitForm.content,
      category: submitForm.category || '技术手记',
      emoji: selectedEmoji,
      summary: submitForm.summary || '',
    }
    const editingId = editingNoteId.value
    const editingNow = !!(isEditingNote.value && editingId)
    const res = editingNow
      ? await api.put(`/api/notes/${editingId}`, payload)
      : await api.post('/api/notes', payload)

    if (res.code !== 200) {
      alert(res.msg || (editingNow ? '保存失败，请稍后重试。' : '投稿失败，请稍后重试。'))
      return
    }

    closeSubmitDialog()
    resetSubmitForm()
    const status = Number(res.data?.status)
    if (status === 2) {
      const reason = pickRejectReason(res.data)
      alert(`${editingNow ? '保存完成，但审核未通过。' : '投稿未通过审核。'}${reason ? `原因：${reason}` : ''}`)
    } else if (status === 1) {
      alert(editingNow ? '保存成功，内容审核通过并已更新。' : '投稿成功，内容审核通过，已对外展示。')
    } else {
      alert(editingNow ? '保存成功，内容已提交复审。' : '投稿成功，已进入待审核队列。')
    }
    listMode.value = 'mine'
    if (!editingNow) {
      mineStatusFilter.value = ''
    }
    await loadCurrentNotes()
  } catch (e) {
    alert(e.message || '操作失败，请稍后重试。')
  } finally {
    submittingNote.value = false
  }
}

const deleteNote = async (note) => {
  if (!isViewingMine.value || !note?.id || deletingNote.value) return
  if (!authStore.isLoggedIn) {
    alert('请先登录后再删除手记。')
    return
  }
  if (authStore.isGuest) {
    alert('游客账号不能删除手记，请注册/登录正式账号。')
    return
  }
  const noteTitle = String(note.title || '这篇手记').trim()
  const confirmed = window.confirm(`确认删除《${noteTitle}》吗？删除后不可恢复。`)
  if (!confirmed) return

  deletingNote.value = true
  try {
    const res = await api.delete(`/api/notes/${note.id}`)
    if (res.code !== 200) {
      throw new Error(res.msg || '删除失败，请稍后重试。')
    }
    if (activeNote.value?.id === note.id) {
      closeComments()
      aiSummaries.value = []
    }
    alert('手记已删除。')
    await loadCurrentNotes()
  } catch (e) {
    alert(e.message || '删除失败，请稍后重试。')
  } finally {
    deletingNote.value = false
  }
}

onMounted(async () => {
  await loadCurrentNotes()
  viewSyncTimer = setInterval(syncViewCounts, 5000)
})

onUnmounted(() => {
  if (viewSyncTimer) clearInterval(viewSyncTimer)
})
</script>

<style scoped>
.notes-shell {
  background: var(--app-shell-bg);
}

.notes-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(56px);
}

.notes-glow-a {
  top: -96px;
  left: 12%;
  width: 42vw;
  height: 16rem;
  background: var(--module-glow-a);
}

.notes-glow-b {
  top: 18%;
  right: 12%;
  width: 36vw;
  height: 18rem;
  background: var(--module-glow-c);
}

.notes-glow-c {
  bottom: -96px;
  left: 30%;
  width: 48vw;
  height: 15rem;
  background: var(--module-glow-b);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 999px;
}

.note-title,
.note-summary {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-title {
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.note-summary {
  -webkit-line-clamp: 2;
  line-clamp: 2;
  line-height: 1.45;
}

.note-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  background: var(--bg-elevated);
  transition: all 0.2s ease;
}

.note-action:hover:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--primary);
  background: var(--accent-soft);
}

.note-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.note-action-liked {
  border-color: var(--danger-border);
  color: var(--danger);
  background: var(--danger-soft);
}

.note-fab {
  position: absolute;
  right: 20px;
  bottom: 18px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 8px;
}

.prose-shell :deep(p) {
  line-height: 1.9;
  margin-bottom: 1rem;
}

.prose-shell :deep(h1),
.prose-shell :deep(h2),
.prose-shell :deep(h3) {
  color: var(--text-primary);
  margin-top: 1.5rem;
  margin-bottom: 0.8rem;
}

.notes-shell :deep([class~='text-slate-500']),
.notes-shell :deep([class~='text-slate-600']) {
  color: var(--text-secondary) !important;
}

.notes-shell :deep([class~='text-slate-700']),
.notes-shell :deep([class~='text-slate-800']) {
  color: var(--text-primary) !important;
}

.notes-shell :deep([class~='text-indigo-600']),
.notes-shell :deep([class~='text-sky-600']),
.notes-shell :deep([class~='text-sky-700']) {
  color: var(--primary) !important;
}

.notes-shell :deep([class~='text-rose-600']),
.notes-shell :deep([class~='text-rose-700']) {
  color: var(--danger) !important;
}

.notes-shell :deep([class~='bg-white/90']),
.notes-shell :deep([class~='bg-white/80']),
.notes-shell :deep([class~='bg-white/70']),
.notes-shell :deep([class~='bg-slate-100']),
.notes-shell :deep([class~='bg-slate-50']),
.notes-shell :deep([class~='bg-black/[0.03]']),
.notes-shell :deep([class~='bg-black/[0.02]']) {
  background: var(--bg-elevated) !important;
}

.notes-shell :deep([class~='bg-indigo-50']),
.notes-shell :deep([class~='bg-sky-50']) {
  background: var(--accent-soft) !important;
}

.notes-shell :deep([class~='bg-rose-50']) {
  background: var(--danger-soft) !important;
}

.notes-shell :deep([class~='border-black/10']),
.notes-shell :deep([class~='border-black/8']),
.notes-shell :deep([class~='border-black/[0.05]']),
.notes-shell :deep([class~='border-black/[0.03]']),
.notes-shell :deep([class~='border-black/[0.02]']) {
  border-color: var(--border-soft) !important;
}

.notes-shell :deep([class~='border-indigo-200']),
.notes-shell :deep([class~='border-sky-200']) {
  border-color: var(--accent-border) !important;
}

.notes-shell :deep([class~='border-rose-200']),
.notes-shell :deep([class~='border-rose-100']) {
  border-color: var(--danger-border) !important;
}
</style>
