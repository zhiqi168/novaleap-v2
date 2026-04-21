
<template>
  <div class="h-full relative overflow-hidden notes-shell workspace-page workspace-shell sm:mx-4">
    <div
      class="relative z-10 h-full flex flex-col lg:flex-row"
      @touchstart.passive="handleTouchStart"
      @touchmove.passive="handleTouchMove"
      @touchend="handleTouchEnd"
      @touchcancel="resetTouchState"
    >
      <div class="lg:hidden shrink-0 px-3 pt-3 pb-2 border-b border-border-subtle bg-bg-surface/80 backdrop-blur">
        <div class="grid grid-cols-2 gap-2 rounded-xl bg-black/[0.03] p-1">
          <button
            class="rounded-lg px-3 py-2 text-sm font-semibold transition-colors"
            :class="mobileNoteTab === 'list' ? 'bg-bg-elevated text-text-primary shadow-sm' : 'text-text-tertiary'"
            @click="setMobileNoteTab('list')"
          >
            手记列表
          </button>
          <button
            class="rounded-lg px-3 py-2 text-sm font-semibold transition-colors"
            :class="mobileNoteTab === 'detail' ? 'bg-bg-elevated text-text-primary shadow-sm' : 'text-text-tertiary'"
            @click="setMobileNoteTab('detail')"
          >
            手记详情
          </button>
        </div>
      </div>

      <aside
        class="w-full lg:w-[430px] xl:w-[460px] border-b lg:border-b-0 lg:border-r border-border-subtle bg-bg-surface backdrop-blur-xl flex-col shrink-0"
        :class="mobileNoteTab === 'detail' ? 'hidden lg:flex lg:h-full' : 'flex h-full lg:h-full'"
      >
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

          <div class="mt-3 flex items-center text-xs text-slate-500">
            <span>{{ notesCountText }}</span>
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

      <section
        class="flex-1 overflow-y-auto custom-scrollbar min-h-0"
        :class="mobileNoteTab === 'list' ? 'hidden lg:block' : 'block'"
      >
        <template v-if="activeNote">
          <div class="max-w-[1020px] mx-auto px-5 md:px-8 xl:px-10 py-7 space-y-5">
            <header class="rounded-3xl border border-border-subtle bg-bg-surface backdrop-blur-xl p-5 md:p-7 shadow-card">
              <button
                class="lg:hidden inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold border border-border-subtle bg-bg-elevated text-text-secondary hover:bg-bg-soft transition-colors"
                @click="setMobileNoteTab('list')"
              >
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 18l-6-6 6-6" />
                </svg>
                返回列表
              </button>
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
                <div v-if="noteDetailLoading && !activeNote.content" class="py-3">
                  <LoadingDots />
                </div>
                <div v-else class="prose prose-slate dark:prose-invert max-w-none text-[16px] leading-8 prose-shell">
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

    <transition name="submit-note-fade">
      <div v-if="showSubmitDialog" class="submit-note-overlay" @click.self="closeSubmitDialog">
        <div class="submit-note-shell">
          <div class="submit-note-dialog">
          <div class="submit-note-header">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="submit-note-title">投稿手记</h3>
                <span class="submit-note-badge">发布审核</span>
              </div>
              <p class="submit-note-subtitle">
                {{ isEditingNote ? '保存后会重新进行内容校验与发布审核，展示内容会同步更新。' : '提交后会先进行内容校验与发布审核，通过后即可公开展示。' }}
              </p>
            </div>
            <button class="submit-note-close" @click="closeSubmitDialog">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>

          <div class="submit-note-body custom-scrollbar">
            <input
              v-model="submitForm.title"
              type="text"
              class="submit-note-input submit-note-title-input"
              placeholder="请输入手记标题"
            />

            <div class="grid grid-cols-1 gap-3 md:grid-cols-[1fr_1.18fr_1fr]">
              <label class="submit-note-card submit-note-card-compact">
                <div class="submit-note-card-head">
                  <span class="submit-note-label">分类</span>
                  <span class="submit-note-meta">自定义</span>
                </div>
                <input
                  v-model="submitForm.category"
                  type="text"
                  class="submit-note-card-input"
                  placeholder="例如：技术手记"
                />
              </label>

              <div
                ref="emojiPickerRef"
                class="submit-note-card submit-note-icon-card"
                @mouseenter="cancelEmojiPopoverClose"
                @mouseleave="scheduleEmojiPopoverClose"
              >
                <div class="flex items-center justify-between gap-3">
                  <div class="submit-note-card-head">
                    <span class="submit-note-label">封面图标</span>
                    <span class="submit-note-meta">选择封面</span>
                  </div>
                  <span class="submit-note-icon-preview">{{ submitForm.emoji }}</span>
                </div>
                <div class="submit-note-icon-grid">
                  <button
                    v-for="emoji in primaryEmojiOptions"
                    :key="emoji"
                    type="button"
                    class="submit-note-icon"
                    :class="{ 'submit-note-icon-active': submitForm.emoji === emoji }"
                    @click="submitForm.emoji = emoji"
                  >
                    {{ emoji }}
                  </button>
                  <button
                    type="button"
                    class="submit-note-icon submit-note-icon-more"
                    :class="{ 'submit-note-icon-more-active': showMoreEmojiPanel }"
                    title="更多图标"
                    aria-label="更多图标"
                    @mouseenter="openEmojiPopover"
                    @click="handleEmojiPopoverTrigger"
                  >
                    <span class="submit-note-icon-more-plus">+</span>
                  </button>
                </div>
                <transition name="submit-note-pop">
                  <div v-if="showMoreEmojiPanel" class="submit-note-icon-popover">
                    <div class="submit-note-icon-popover-grid">
                      <button
                        v-for="emoji in moreEmojiOptions"
                        :key="`more-${emoji}`"
                        type="button"
                        class="submit-note-icon submit-note-icon-pop"
                        :class="{ 'submit-note-icon-active': submitForm.emoji === emoji }"
                        @click="submitForm.emoji = emoji; showMoreEmojiPanel = false"
                      >
                        {{ emoji }}
                      </button>
                    </div>
                  </div>
                </transition>
              </div>

              <label class="submit-note-card submit-note-card-compact">
                <div class="submit-note-card-head">
                  <span class="submit-note-label">摘要</span>
                  <span class="submit-note-meta">可选</span>
                </div>
                <input
                  v-model="submitForm.summary"
                  type="text"
                  class="submit-note-card-input"
                  placeholder="用一句话概括这篇内容"
                />
              </label>
            </div>

            <div class="submit-note-editor">
              <div class="flex items-center justify-between gap-3">
                <span class="submit-note-label">正文内容</span>
                <span class="submit-note-hint">支持 Markdown</span>
              </div>
              <textarea
                v-model="submitForm.content"
                rows="10"
                class="submit-note-textarea"
                placeholder="写下你的思考、经验、方法或一瞬间的灵感..."
              ></textarea>
            </div>

            <div class="submit-note-footnote">
              {{ isEditingNote ? '保存后会重新进行内容规范检测；若命中不适宜发布的内容，会给出调整提示。' : '提交前会先进行内容规范检测；若命中违禁词或不适宜公开展示的内容，会提示修改。' }}
            </div>
          </div>

          <div class="submit-note-footer">
            <button class="submit-note-btn submit-note-btn-secondary" @click="closeSubmitDialog">取消</button>
            <button class="submit-note-btn submit-note-btn-primary" :disabled="submittingNote" @click="submitNote">{{ submittingNote ? (isEditingNote ? '保存中...' : '提交中...') : (isEditingNote ? '保存修改' : '提交审核') }}</button>
          </div>
          </div>
        </div>
      </div>
    </transition>
    <div v-if="commentPanelVisible" class="fixed inset-0 z-[1200] bg-black/25 backdrop-blur-[2px]" @click.self="closeComments">
      <aside class="note-comment-drawer absolute right-0 top-0 h-full w-full max-w-[430px] bg-bg-card backdrop-blur-xl shadow-2xl border-l border-border-soft flex flex-col">
        <button
          class="note-comment-mobile-close"
          type="button"
          @click="closeComments"
          aria-label="关闭评论"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        <header class="note-comment-header px-5 py-4 border-b border-black/5 flex items-center justify-between">
          <div>
            <h3 class="text-base font-bold text-slate-800">手记评论</h3>
            <p class="text-xs text-slate-500 mt-1 line-clamp-1">{{ activeNote?.title || '' }}</p>
          </div>
          <button class="note-comment-desktop-close workspace-btn workspace-btn-muted px-2.5 py-1 rounded-md bg-black/5 hover:bg-black/10 text-sm" @click="closeComments">关闭</button>
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
            class="workspace-control w-full border border-border-subtle bg-bg-surface text-text-primary rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25 resize-none disabled:bg-bg-elevated disabled:text-text-tertiary"
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
import { useResourceCacheStore } from '@/stores/resourceCache'

const authStore = useAuthStore()
const resourceCacheStore = useResourceCacheStore()
const NOTE_RUNTIME_NAMESPACE = 'notes'
const NOTE_LIST_CACHE_TTL = 2 * 60 * 1000
const NOTE_DETAIL_CACHE_TTL = 10 * 60 * 1000

const notes = ref([])
const loading = ref(false)
const noteDetailLoading = ref(false)
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
const showMoreEmojiPanel = ref(false)
const emojiPickerRef = ref(null)
const primaryEmojiOptions = ['📘', '📝', '🧠', '🧰', '📊', '🎯', '🌱']
const moreEmojiOptions = ['📚', '✅', '🧩', '✍️', '📖', '🗒️', '🧪', '🗂', '🪝', '🔍', '📡', '💡', '✨', '🌙', '💭', '🎨', '🌷', '🏗', '☕', '🗓', '📎', '🚨', '🪴']
const noteEmojiOptions = Array.from(new Set([
  ...primaryEmojiOptions,
  ...moreEmojiOptions,
]))
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
const mobileNoteTab = ref('list')

const buildNotesListCacheKey = () => JSON.stringify({
  mode: listMode.value,
  keyword: searchQuery.value.trim(),
  status: mineStatusFilter.value,
})

const buildNotesListSnapshot = () => ({
  records: notes.value.map((item) => ({ ...item })),
  activeNoteId: activeNote.value?.id || null,
  mobileNoteTab: mobileNoteTab.value,
})

const applyNotesListSnapshot = (snapshot) => {
  notes.value = Array.isArray(snapshot?.records) ? snapshot.records.map((item) => ({ ...item })) : []
  const activeId = Number(snapshot?.activeNoteId || 0)
  activeNote.value = activeId > 0
    ? notes.value.find((item) => Number(item?.id || 0) === activeId) || null
    : null
  if (!activeNote.value && notes.value.length) {
    activeNote.value = notes.value[0]
  }
  noteDetailLoading.value = !!(activeNote.value?.id && !activeNote.value?.content)
  mobileNoteTab.value = snapshot?.mobileNoteTab || (activeNote.value ? 'detail' : 'list')
}

const persistNotesListSnapshot = () => {
  resourceCacheStore.writeList(NOTE_RUNTIME_NAMESPACE, buildNotesListCacheKey(), buildNotesListSnapshot())
}

const persistNoteDetailSnapshot = (note = activeNote.value) => {
  const noteId = Number(note?.id || 0)
  if (!noteId) return
  resourceCacheStore.writeDetail(NOTE_RUNTIME_NAMESPACE, noteId, { ...note })
}

const touchState = reactive({
  startX: 0,
  deltaX: 0,
  active: false,
})
const noteViewFloorMap = ref(new Map())

let emojiPopoverCloseTimer = null

const canSubmitNote = computed(() => authStore.isLoggedIn && !authStore.isGuest)
const isViewingMine = computed(() => listMode.value === 'mine')
const canInteractActiveNote = computed(() => !!activeNote.value?.id)
const isEditingNote = computed(() => submitDialogMode.value === 'edit' && !!editingNoteId.value)

const isNotesMobileViewport = () => typeof window !== 'undefined' && window.innerWidth < 1024

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

const readLocalNoteViewFloor = (noteId) => {
  const id = Number(noteId)
  if (!Number.isInteger(id) || id <= 0) return null
  return noteViewFloorMap.value.get(id) ?? null
}

const rememberLocalNoteViewCount = (noteId, viewCount) => {
  const id = Number(noteId)
  const nextCount = Number(viewCount)
  if (!Number.isInteger(id) || id <= 0 || !Number.isFinite(nextCount)) return
  const nextMap = new Map(noteViewFloorMap.value)
  nextMap.set(id, Math.max(0, nextCount))
  noteViewFloorMap.value = nextMap
}

const applyLocalNoteViewFloor = (note) => {
  if (!note?.id) return note
  const localFloor = readLocalNoteViewFloor(note.id)
  if (localFloor == null) return note
  return {
    ...note,
    views: Math.max(Number(note.views || 0), localFloor),
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

const hydrateNoteDetail = async (noteId, options = {}) => {
  const { force = false } = options
  if (!noteId) return
  if (Number(activeNote.value?.id || 0) === Number(noteId)) {
    noteDetailLoading.value = !activeNote.value?.content || force
  }
  if (!force) {
    const cached = resourceCacheStore.readDetail(NOTE_RUNTIME_NAMESPACE, noteId, NOTE_DETAIL_CACHE_TTL)
    if (cached) {
      const detail = applyLocalNoteViewFloor(cached)
      rememberLocalNoteViewCount(detail.id, detail.views)
      activeNote.value = detail
      patchNoteInList(detail)
      persistNoteDetailSnapshot(detail)
      if (Number(activeNote.value?.id || 0) === Number(noteId)) {
        noteDetailLoading.value = false
      }
      return
    }
  }
  try {
    const detail = await resourceCacheStore.loadDetail(
      NOTE_RUNTIME_NAMESPACE,
      noteId,
      async () => {
        const res = await api.get(`/api/notes/${noteId}`)
        if (res.code !== 200 || !res.data) {
          throw new Error(res.msg || '加载手记详情失败')
        }
        return applyLocalNoteViewFloor(toNoteView(res.data))
      },
      { ttlMs: NOTE_DETAIL_CACHE_TTL, force },
    )
    rememberLocalNoteViewCount(detail.id, detail.views)
    activeNote.value = detail
    patchNoteInList(detail)
    persistNoteDetailSnapshot(detail)
  } catch (_) {
    // noop
  } finally {
    if (Number(activeNote.value?.id || 0) === Number(noteId)) {
      noteDetailLoading.value = false
    }
  }
}

const applyNotesPayload = (payload) => {
  notes.value = Array.isArray(payload?.records)
    ? payload.records.map((item) => ({ ...item }))
    : (payload.data?.records || []).map(toNoteView).map(applyLocalNoteViewFloor)
  if ((!activeNote.value || !notes.value.find((n) => n.id === activeNote.value.id)) && notes.value.length) {
    activeNote.value = notes.value[0]
    if (!activeNote.value.content) {
      void hydrateNoteDetail(activeNote.value.id)
    }
  } else if (!notes.value.length) {
    activeNote.value = null
    noteDetailLoading.value = false
  }

  if (isNotesMobileViewport()) {
    mobileNoteTab.value = activeNote.value ? mobileNoteTab.value : 'list'
  }
  persistNotesListSnapshot()
}
const loadPublishedNotes = async (options = {}) => {
  const { force = false } = options
  const cacheKey = buildNotesListCacheKey()
  if (!force) {
    const cached = resourceCacheStore.readList(NOTE_RUNTIME_NAMESPACE, cacheKey, NOTE_LIST_CACHE_TTL)
    if (cached) {
      applyNotesListSnapshot(cached)
      if (activeNote.value && !activeNote.value.content) {
        void hydrateNoteDetail(activeNote.value.id)
      }
      return
    }
  }
  loading.value = true
  try {
    const snapshot = await resourceCacheStore.loadList(
      NOTE_RUNTIME_NAMESPACE,
      cacheKey,
      async () => {
        const res = await api.get(buildPublishedNotesUrl())
        if (res.code !== 200) {
          throw new Error(res.msg || '加载手记列表失败')
        }
        const records = (res.data?.records || []).map(toNoteView).map(applyLocalNoteViewFloor)
        return {
          records,
          activeNoteId: activeNote.value?.id || records[0]?.id || null,
          mobileNoteTab: mobileNoteTab.value,
        }
      },
      { ttlMs: NOTE_LIST_CACHE_TTL, force },
    )
    applyNotesListSnapshot(snapshot)
    if (activeNote.value && !activeNote.value.content) {
      void hydrateNoteDetail(activeNote.value.id)
    }
  } catch (_) {
    notes.value = []
  } finally {
    loading.value = false
  }
}

const loadMineNotesLegacy = async () => {
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

const loadMineNotes = async (options = {}) => {
  const { force = false } = options
  const cacheKey = buildNotesListCacheKey()
  if (!force) {
    const cached = resourceCacheStore.readList(NOTE_RUNTIME_NAMESPACE, cacheKey, NOTE_LIST_CACHE_TTL)
    if (cached) {
      applyNotesListSnapshot(cached)
      if (activeNote.value && !activeNote.value.content) {
        void hydrateNoteDetail(activeNote.value.id)
      }
      return
    }
  }
  loading.value = true
  try {
    const snapshot = await resourceCacheStore.loadList(
      NOTE_RUNTIME_NAMESPACE,
      cacheKey,
      async () => {
        const res = await api.get(buildMineNotesUrl())
        if (res.code !== 200) {
          throw new Error(res.msg || '加载我的手记失败')
        }
        const keyword = searchQuery.value.trim().toLowerCase()
        const filteredRecords = !keyword
          ? (res.data?.records || [])
          : (res.data?.records || []).filter((item) => String(item?.title || '').toLowerCase().includes(keyword))
        const records = filteredRecords.map(toNoteView).map(applyLocalNoteViewFloor)
        return {
          records,
          activeNoteId: activeNote.value?.id || records[0]?.id || null,
          mobileNoteTab: mobileNoteTab.value,
        }
      },
      { ttlMs: NOTE_LIST_CACHE_TTL, force },
    )
    applyNotesListSnapshot(snapshot)
    if (activeNote.value && !activeNote.value.content) {
      void hydrateNoteDetail(activeNote.value.id)
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

const loadCurrentNotes = async (options = {}) => {
  if (isViewingMine.value) {
    await loadMineNotes(options)
    return
  }
  await loadPublishedNotes(options)
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
  mobileNoteTab.value = 'list'
  closeComments()
  await loadCurrentNotes()
}

const setMineStatusFilter = async (status) => {
  if (mineStatusFilter.value === status) return
  mineStatusFilter.value = status
  if (isViewingMine.value) {
    activeNote.value = null
    aiSummaries.value = []
    mobileNoteTab.value = 'list'
    closeComments()
    await loadMineNotes()
  }
}

const setMobileNoteTab = (tab) => {
  if (tab === 'detail' && !activeNote.value) return
  mobileNoteTab.value = tab
}

const resetTouchState = () => {
  touchState.startX = 0
  touchState.deltaX = 0
  touchState.active = false
}

const handleTouchStart = (event) => {
  if (!isNotesMobileViewport() || !event.touches?.length) return
  const interactive = event.target?.closest?.('button, input, textarea, select, a')
  if (interactive) {
    resetTouchState()
    return
  }
  touchState.startX = event.touches[0].clientX
  touchState.deltaX = 0
  touchState.active = true
}

const handleTouchMove = (event) => {
  if (!touchState.active || !event.touches?.length) return
  touchState.deltaX = event.touches[0].clientX - touchState.startX
}

const handleTouchEnd = () => {
  if (!touchState.active) return
  const absDeltaX = Math.abs(touchState.deltaX)
  if (absDeltaX < 56) {
    resetTouchState()
    return
  }
  if (touchState.deltaX < 0 && mobileNoteTab.value === 'list' && activeNote.value) {
    mobileNoteTab.value = 'detail'
  } else if (touchState.deltaX > 0 && mobileNoteTab.value === 'detail') {
    mobileNoteTab.value = 'list'
  }
  resetTouchState()
}

const patchNoteInList = (next) => {
  if (!next?.id) return
  const patched = applyLocalNoteViewFloor(next)
  notes.value = notes.value.map((item) => (item.id === patched.id ? { ...item, ...patched } : item))
  persistNotesListSnapshot()
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
    persistNoteDetailSnapshot(activeNote.value)
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
    persistNoteDetailSnapshot(activeNote.value)
    patchNoteInList(next)
  } catch (e) {
    alert(e.message || '评论发送失败，请稍后重试')
  } finally {
    submittingComment.value = false
  }
}

const selectNote = (note) => {
  const isCurrentSelected = String(activeNote.value?.id ?? '') === String(note?.id ?? '')
  activeNote.value = applyLocalNoteViewFloor(note)
  persistNoteDetailSnapshot(activeNote.value)
  noteDetailLoading.value = !activeNote.value?.content
  aiSummaries.value = []
  isAiGenerating.value = false
  if (isNotesMobileViewport()) {
    mobileNoteTab.value = 'detail'
  }
  if (commentPanelVisible.value) {
    closeComments()
  }

  if (!isCurrentSelected && !isViewingMine.value && Number(note?.status) === 1) {
    const nextViews = Number(activeNote.value?.views || note?.views || 0) + 1
    rememberLocalNoteViewCount(note.id, nextViews)
    activeNote.value = { ...activeNote.value, views: nextViews }
    patchNoteInList({ id: note.id, views: nextViews })
  }

  const viewApi = isCurrentSelected || isViewingMine.value || Number(note.status) !== 1
    ? resourceCacheStore.loadDetail(
      NOTE_RUNTIME_NAMESPACE,
      note.id,
      async () => {
        const res = await api.get(`/api/notes/${note.id}`)
        if (res.code !== 200 || !res.data) {
          throw new Error(res.msg || '加载手记详情失败')
        }
        return applyLocalNoteViewFloor(toNoteView(res.data))
      },
      { ttlMs: NOTE_DETAIL_CACHE_TTL },
    )
    : api.post(`/api/notes/${note.id}/view`)

  viewApi
    .then((res) => {
      const detail = res?.data
        ? applyLocalNoteViewFloor(toNoteView(res.data))
        : applyLocalNoteViewFloor(res)
      rememberLocalNoteViewCount(detail.id, detail.views)
      activeNote.value = detail
      patchNoteInList(detail)
      persistNoteDetailSnapshot(detail)
      if (Number(activeNote.value?.id || 0) === Number(detail.id || 0)) {
        noteDetailLoading.value = false
      }
    })
    .catch(() => {
      if (Number(activeNote.value?.id || 0) === Number(note?.id || 0)) {
        noteDetailLoading.value = false
      }
    })
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

const cancelEmojiPopoverClose = () => {
  if (emojiPopoverCloseTimer) {
    clearTimeout(emojiPopoverCloseTimer)
    emojiPopoverCloseTimer = null
  }
}

const closeEmojiPopover = () => {
  cancelEmojiPopoverClose()
  showMoreEmojiPanel.value = false
}

const openEmojiPopover = () => {
  cancelEmojiPopoverClose()
  showMoreEmojiPanel.value = true
}

const scheduleEmojiPopoverClose = () => {
  cancelEmojiPopoverClose()
  emojiPopoverCloseTimer = setTimeout(() => {
    showMoreEmojiPanel.value = false
    emojiPopoverCloseTimer = null
  }, 180)
}

const handleEmojiPopoverTrigger = () => {
  if (showMoreEmojiPanel.value) return
  openEmojiPopover()
}

const handleEmojiPopoverOutside = (event) => {
  if (!showMoreEmojiPanel.value) return
  const root = emojiPickerRef.value
  if (!root) return
  const target = event.target
  if (target instanceof Node && root.contains(target)) return
  closeEmojiPopover()
}

const handleEmojiPopoverEscape = (event) => {
  if (event.key !== 'Escape') return
  if (!showMoreEmojiPanel.value) return
  closeEmojiPopover()
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
  showMoreEmojiPanel.value = false
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
  showMoreEmojiPanel.value = false
  fillSubmitFormByNote(editableNote)
  showSubmitDialog.value = true
}

const closeSubmitDialog = () => {
  showSubmitDialog.value = false
  submitDialogMode.value = 'create'
  editingNoteId.value = null
  showMoreEmojiPanel.value = false
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
    resourceCacheStore.invalidateLists(NOTE_RUNTIME_NAMESPACE)
    if (editingId) {
      resourceCacheStore.invalidateDetail(NOTE_RUNTIME_NAMESPACE, editingId)
    }
    await loadCurrentNotes({ force: true })
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
    resourceCacheStore.invalidateLists(NOTE_RUNTIME_NAMESPACE)
    resourceCacheStore.invalidateDetail(NOTE_RUNTIME_NAMESPACE, note.id)
    await loadCurrentNotes({ force: true })
  } catch (e) {
    alert(e.message || '删除失败，请稍后重试。')
  } finally {
    deletingNote.value = false
  }
}

onMounted(async () => {
  await loadCurrentNotes()
  document.addEventListener('pointerdown', handleEmojiPopoverOutside, true)
  document.addEventListener('keydown', handleEmojiPopoverEscape)
})

onUnmounted(() => {
  cancelEmojiPopoverClose()
  document.removeEventListener('pointerdown', handleEmojiPopoverOutside, true)
  document.removeEventListener('keydown', handleEmojiPopoverEscape)
})
</script>

<style scoped>
.notes-shell {
  background: var(--app-shell-bg);
}

.note-comment-drawer {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0)),
    var(--surface-panel-soft);
}

.note-comment-header {
  position: sticky;
  top: 0;
  z-index: 5;
  background: color-mix(in srgb, var(--surface-panel-soft) 92%, transparent);
  backdrop-filter: blur(18px);
}

.note-comment-mobile-close {
  position: absolute;
  top: 12px;
  right: 14px;
  z-index: 6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: color-mix(in srgb, var(--bg-elevated) 92%, transparent);
  color: var(--text-secondary);
  box-shadow: var(--shadow-card);
}

.note-comment-mobile-close:active {
  transform: scale(0.96);
}

.note-comment-desktop-close {
  display: none;
}

@media (min-width: 1024px) {
  .note-comment-mobile-close {
    display: none;
  }

  .note-comment-desktop-close {
    display: inline-flex;
  }
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
.dark .submit-note-card,
.dark .submit-note-icon,
.dark .submit-note-icon-preview,
.dark .submit-note-icon-popover,
.dark .submit-note-city-row {
  box-shadow: 0 24px 54px rgba(0, 0, 0, 0.28);
}

.dark .submit-note-header,
.dark .submit-note-footer {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0));
}

.dark .submit-note-close,
.dark .submit-note-icon,
.dark .submit-note-icon-preview {
  background: rgba(32, 26, 45, 0.88);
  color: var(--submit-text-secondary);
}

.dark .submit-note-card:hover,
.dark .submit-note-icon:hover,
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
  max-width: 700px;
  max-height: calc(100dvh - 56px);
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
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--submit-divider);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
}

.submit-note-title {
  font-size: 23px;
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
  margin-top: 8px;
  font-size: 12.5px;
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
  padding: 16px 22px 18px;
}

.submit-note-input,
.submit-note-card-input,
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
    background 0.2s var(--submit-ease),
    transform 0.2s var(--submit-ease);
}

.submit-note-input::placeholder,
.submit-note-card-input::placeholder,
.submit-note-textarea::placeholder {
  color: var(--submit-text-placeholder);
}

.submit-note-input:hover,
.submit-note-card-input:hover,
.submit-note-textarea:hover {
  border-color: var(--submit-border-soft);
  background: rgba(252, 252, 254, 0.96);
}

.submit-note-input:focus,
.submit-note-card-input:focus,
.submit-note-textarea:focus {
  outline: none;
  border-color: var(--submit-brand-border);
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.84),
    0 0 0 4px rgba(231, 154, 176, 0.08),
    0 16px 30px -28px rgba(168, 191, 220, 0.28);
}

.submit-note-title-input {
  min-height: 46px;
  padding: 0 16px;
  border-radius: 18px;
  font-size: 14px;
}

.submit-note-card {
  display: flex;
  min-height: 112px;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid var(--submit-border-light);
  border-radius: 20px;
  background: var(--submit-bg-card);
  padding: 12px 14px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 14px 26px -28px rgba(43, 47, 56, 0.12);
  transition:
    border-color 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    transform 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease);
}

.submit-note-card:hover {
  border-color: var(--submit-border-soft);
  background: rgba(255, 255, 255, 0.7);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 20px 34px -30px rgba(43, 47, 56, 0.16);
  transform: translateY(-1px);
}

.submit-note-card-compact {
  gap: 10px;
}

.submit-note-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.submit-note-label {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--submit-text-secondary);
  letter-spacing: 0.02em;
}

.submit-note-meta {
  font-size: 11px;
  color: var(--submit-text-tertiary);
}

.submit-note-card-input {
  min-height: 42px;
  border-radius: 16px;
  padding: 0 14px;
  font-size: 14px;
}

.submit-note-icon-preview {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--submit-brand-border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 10px 20px -18px rgba(168, 191, 220, 0.2);
}

.submit-note-icon-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 6px;
  margin-top: 10px;
  align-items: stretch;
}

.submit-note-icon {
  position: relative;
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 38px;
  aspect-ratio: 1 / 1;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--submit-border-light);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--submit-text-secondary);
  font-size: 18px;
  line-height: 1;
  transition:
    transform 0.2s var(--submit-ease),
    border-color 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 18px -18px rgba(43, 47, 56, 0.14);
}

.submit-note-icon:hover {
  transform: translateY(-1px);
  border-color: var(--submit-border-soft);
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.84),
    0 14px 22px -18px rgba(168, 191, 220, 0.22);
}

.submit-note-icon:active {
  transform: translateY(0);
}

.submit-note-icon-active {
  border-color: var(--submit-brand-border);
  background: var(--submit-brand-soft);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 0 0 5px rgba(231, 154, 176, 0.14),
    0 14px 24px -18px rgba(231, 154, 176, 0.28);
  color: var(--submit-text-primary);
  transform: translateY(-1px) scale(1.05);
}

.submit-note-icon-active::after {
  content: '';
  position: absolute;
  right: 3px;
  top: 3px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--submit-brand);
  box-shadow:
    0 0 0 2px rgba(255, 255, 255, 0.94),
    0 0 0 5px rgba(231, 154, 176, 0.08);
}

.submit-note-icon-more {
  min-height: 38px;
  padding: 0;
  gap: 0;
  font-size: 13px;
  font-weight: 600;
  aspect-ratio: 1 / 1;
}

.submit-note-icon-more-plus {
  font-size: 18px;
  line-height: 1;
}

.submit-note-icon-more-active {
  border-color: var(--submit-brand-border);
  background: rgba(255, 255, 255, 0.94);
}

.submit-note-icon-card {
  position: relative;
}

.submit-note-icon-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 5;
  width: min(332px, calc(100vw - 72px));
  padding: 12px;
  border: 1px solid var(--submit-border-soft);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    0 20px 40px -30px rgba(43, 47, 56, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
}

.submit-note-icon-popover::before {
  content: '';
  position: absolute;
  top: -7px;
  right: 24px;
  width: 14px;
  height: 14px;
  border-top: 1px solid var(--submit-border-soft);
  border-left: 1px solid var(--submit-border-soft);
  background: rgba(255, 255, 255, 0.94);
  transform: rotate(45deg);
}

.submit-note-icon-popover::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: -12px;
  height: 14px;
}

.submit-note-icon-popover-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.submit-note-icon-pop {
  width: 100%;
}

.submit-note-editor {
  margin-top: 14px;
  border: 1px solid var(--submit-border-soft);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(247, 248, 252, 0.74));
  padding: 14px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 18px 34px -30px rgba(43, 47, 56, 0.14);
  transition:
    border-color 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease);
}

.submit-note-editor:hover {
  border-color: var(--submit-border-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(248, 249, 253, 0.8));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.84),
    0 22px 38px -30px rgba(43, 47, 56, 0.16);
}

.submit-note-hint {
  font-size: 12px;
  color: var(--submit-text-tertiary);
}

.submit-note-textarea {
  min-height: 226px;
  margin-top: 10px;
  resize: vertical;
  border-radius: 18px;
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.75;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.84),
    0 16px 30px -28px rgba(47, 42, 40, 0.2);
}

.submit-note-footnote {
  margin-top: 16px;
  padding: 0 2px;
  color: var(--submit-text-tertiary);
  font-size: 12px;
  line-height: 1.7;
}

.submit-note-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px 16px;
  border-top: 1px solid var(--submit-divider);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.48));
}

.submit-note-btn {
  min-width: 94px;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 600;
  transition:
    transform 0.2s var(--submit-ease),
    box-shadow 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease),
    border-color 0.2s var(--submit-ease),
    opacity 0.2s var(--submit-ease);
}

.submit-note-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.submit-note-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-note-btn-secondary {
  border: 1px solid var(--submit-border-soft);
  background: rgba(255, 255, 255, 0.72);
  color: var(--submit-text-secondary);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.submit-note-btn-secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.96);
  border-color: var(--submit-border-soft);
  box-shadow: 0 14px 24px -22px rgba(43, 47, 56, 0.16);
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

.submit-note-btn-primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 14px 24px -18px rgba(255, 169, 181, 0.32);
}

.dark .submit-note-input:hover,
.dark .submit-note-card-input:hover,
.dark .submit-note-textarea:hover,
.dark .submit-note-card:hover,
.dark .submit-note-editor:hover,
.dark .submit-note-icon-popover,
.dark .submit-note-editor,
.dark .submit-note-btn-secondary,
.dark .submit-note-format-example {
  background: rgba(34, 28, 47, 0.92);
}

.dark .submit-note-input:focus,
.dark .submit-note-card-input:focus,
.dark .submit-note-textarea:focus {
  background: rgba(40, 33, 55, 0.98);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 0 0 4px rgba(215, 191, 220, 0.12),
    0 16px 30px -28px rgba(0, 0, 0, 0.42);
}

.dark .submit-note-icon-popover::before {
  background: rgba(34, 28, 47, 0.96);
}

.dark .submit-note-btn-secondary {
  color: var(--submit-text-secondary);
  border-color: var(--submit-border-soft);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.dark .submit-note-btn-secondary:hover:not(:disabled) {
  background: rgba(42, 35, 58, 0.96);
}

.submit-note-pop-enter-active,
.submit-note-pop-leave-active {
  transition: opacity 0.16s ease;
}

.submit-note-pop-enter-from,
.submit-note-pop-leave-to {
  opacity: 0;
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

@media (max-width: 768px) {
  .submit-note-overlay {
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    padding:
      max(86px, calc(env(safe-area-inset-top) + 76px))
      10px
      max(28px, calc(env(safe-area-inset-bottom) + 22px));
  }

  .submit-note-shell {
    align-items: flex-start;
    min-height: 100%;
    padding-bottom: calc(env(safe-area-inset-bottom) + 12px);
  }

  .submit-note-dialog {
    max-width: min(100%, 640px);
    width: min(100%, calc(100vw - 20px));
    max-height: calc(100svh - 128px - env(safe-area-inset-bottom));
    margin-bottom: calc(env(safe-area-inset-bottom) + 8px);
    border-radius: 22px;
  }

  .submit-note-header,
  .submit-note-body,
  .submit-note-footer {
    padding-left: 14px;
    padding-right: 14px;
  }

  .submit-note-header {
    padding-top: 16px;
    padding-bottom: 14px;
  }

  .submit-note-title {
    font-size: 21px;
  }

  .submit-note-subtitle {
    font-size: 12px;
    line-height: 1.6;
  }

  .submit-note-body {
    padding-top: 14px;
    padding-bottom: 16px;
  }

  .submit-note-footer {
    flex-wrap: wrap;
    padding-top: 12px;
    padding-bottom: calc(18px + env(safe-area-inset-bottom));
    position: relative;
    z-index: 1;
  }

  .submit-note-close {
    flex-shrink: 0;
  }

  .submit-note-btn {
    flex: 1 1 auto;
    min-height: 42px;
  }

  .submit-note-title-input {
    min-height: 44px;
    font-size: 15px;
  }

  .submit-note-card {
    min-height: 100px;
    padding: 11px 12px;
  }

  .submit-note-textarea {
    min-height: 200px;
  }

  .submit-note-icon-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 7px;
  }

  .submit-note-card-head {
    align-items: flex-start;
  }

  .submit-note-icon-popover {
    width: min(320px, calc(100vw - 40px));
    right: -8px;
  }

  .submit-note-icon-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 7px;
  }
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
