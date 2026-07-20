<template>
  <div
    class="question-bank-shell workspace-shell h-full min-h-0 flex flex-col md:flex-row overflow-hidden sm:mx-4"
    @touchstart.passive="handleTouchStart"
    @touchmove.passive="handleTouchMove"
    @touchend="handleTouchEnd"
    @touchcancel="resetTouchState"
  >
    <div class="md:hidden shrink-0 px-3 pt-3 pb-2 border-b border-border-subtle bg-bg-surface/80 backdrop-blur">
      <div class="grid grid-cols-2 gap-2 rounded-xl bg-black/[0.03] p-1">
        <button
          class="rounded-lg px-3 py-2 text-sm font-semibold transition-colors"
          :class="mobileTab === 'list' ? 'bg-white text-text-primary shadow-sm' : 'text-text-tertiary'"
          @click="mobileTab = 'list'"
        >
          题目列表
        </button>
        <button
          class="rounded-lg px-3 py-2 text-sm font-semibold transition-colors"
          :class="mobileTab === 'detail' ? 'bg-white text-text-primary shadow-sm' : 'text-text-tertiary'"
          @click="mobileTab = 'detail'"
        >
          题目详情
        </button>
      </div>
    </div>

    <aside
      class="w-full h-full min-h-0 md:w-[430px] xl:w-[460px] border-b md:border-b-0 md:border-r border-border-subtle bg-bg-surface backdrop-blur-xl flex-col"
      :class="mobileTab === 'detail' ? 'hidden md:flex' : 'flex'"
    >
      <div class="shrink-0 px-4 py-4 border-b border-border-subtle">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="text-2xl font-bold text-text-primary">拾光题库</h2>
            <p class="text-sm text-text-secondary mt-1">官方题库 + 自定义导入题库 + AI 深度解析</p>
          </div>
          <button
            class="workspace-btn shrink-0 inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold border transition-colors"
            :class="authStore.isGuest
              ? 'border-black/10 bg-black/[0.03] text-text-tertiary cursor-not-allowed'
              : 'border-ai-from/20 bg-ai-from/10 text-ai-from hover:bg-ai-from/15'"
            :disabled="authStore.isGuest"
            @click="openImportDialog"
          >
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 16V4m0 0l-4 4m4-4l4 4M4 20h16" />
            </svg>
            导入题库
          </button>
        </div>

        <div class="mt-3 flex items-center gap-2">
          <div class="relative flex-1">
            <input
              v-model="searchQuery"
              type="text"
              class="workspace-control w-full pl-9 pr-3 py-2 rounded-xl border border-border-subtle bg-bg-elevated text-sm focus:outline-none focus:ring-2 focus:ring-ai-from/25"
              placeholder="搜索题目..."
              @keyup.enter="handleSearch"
            />
            <svg class="w-4 h-4 text-text-tertiary absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <button
            class="workspace-btn workspace-btn-muted px-3 py-2 rounded-xl text-sm border border-border-subtle bg-bg-surface hover:bg-bg-elevated text-text-primary"
            @click="handleSearch"
          >
            搜索
          </button>
        </div>

        <div class="mt-2 grid grid-cols-3 gap-2">
          <select
            v-model="difficultyFilter"
            class="workspace-control w-full rounded-xl border border-border-subtle bg-bg-surface px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25"
          >
            <option value="all">全部难度</option>
            <option value="1">简单</option>
            <option value="2">中等</option>
            <option value="3">困难</option>
          </select>
          <select
            v-model="currentCategory"
            class="workspace-control w-full rounded-xl border border-border-subtle bg-bg-surface px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25"
          >
            <option
              v-for="cat in categories"
              :key="cat.id"
              :value="cat.id"
            >
              {{ cat.name }}
            </option>
          </select>
          <button
            class="workspace-btn workspace-btn-primary w-full rounded-xl px-3 py-2 text-sm font-semibold text-white bg-gradient-to-r from-ai-from to-ai-to hover:opacity-95 disabled:opacity-55"
            @click="drawRandomQuestion"
            :disabled="randomLoading || !isCurrentBankReady"
          >
            {{ randomLoading ? '抽题中...' : 'AI 抽题' }}
          </button>
        </div>

        <div class="mt-4 rounded-2xl border border-border-subtle bg-bg-elevated p-3">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-text-primary">我的题库</p>
              <p class="text-[11px] text-text-tertiary mt-1">导入 `TXT` 后会先进入审核，审核通过再正式入库展示。</p>
            </div>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="workspace-btn workspace-btn-muted text-[11px] px-2.5 py-1 rounded-lg border border-border-subtle bg-bg-surface text-text-secondary hover:bg-bg-elevated"
                @click="bankPanelCollapsed = !bankPanelCollapsed"
              >
                {{ bankPanelCollapsed ? '展开题库区' : '收起题库区' }}
              </button>
              <span v-if="authStore.isGuest" class="text-[11px] text-amber-700 bg-amber-50 border border-amber-100 px-2 py-1 rounded-full">
                游客不可导入
              </span>
            </div>
          </div>

          <div v-if="bankPanelCollapsed" class="mt-3 text-[11px] text-text-tertiary">
            当前：{{ currentBank ? currentBank.name : '官方题库' }} · 自定义题库 {{ customBanks.length }} 个
          </div>

          <div v-else class="mt-3">
            <div class="mb-2 flex items-center justify-end gap-1">
              <button
                class="nav-arrow-btn"
                :disabled="!canScrollBankLeft"
                @click="scrollBankNav(-1)"
                aria-label="向左滚动题库"
              >
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 18l-6-6 6-6" />
                </svg>
              </button>
              <button
                class="nav-arrow-btn"
                :disabled="!canScrollBankRight"
                @click="scrollBankNav(1)"
                aria-label="向右滚动题库"
              >
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 6l6 6-6 6" />
                </svg>
              </button>
            </div>

            <div
              ref="bankScroller"
              class="flex gap-2 overflow-x-auto no-scrollbar pb-1 bank-scroll-area"
              @scroll="updateBankNavState"
              @wheel="handleBankWheel"
            >
              <button
                class="bank-card"
                :class="!currentBankId ? 'bank-card-active' : ''"
                @click="selectBank(null)"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="text-xs font-semibold text-text-primary">官方题库</span>
                  <span class="bank-status bank-status-approved">已上架</span>
                </div>
                <p class="mt-2 text-[11px] text-text-tertiary leading-5">
                  默认题库，随时可刷题和 AI 抽题。
                </p>
              </button>

              <button
                v-for="bank in customBanks"
                :key="bank.id"
                class="bank-card"
                :class="currentBankId === bank.id ? 'bank-card-active' : ''"
                @click="selectBank(bank)"
              >
                <div class="flex items-start justify-between gap-2">
                  <div class="min-w-0">
                    <p class="text-xs font-semibold text-text-primary truncate">{{ bank.name }}</p>
                    <p class="text-[10px] text-text-tertiary mt-1 truncate">{{ bank.originalFileName }}</p>
                  </div>
                  <button
                    class="shrink-0 rounded-lg border border-border-subtle bg-bg-surface p-1 text-text-tertiary hover:text-text-primary"
                    title="重命名题库"
                    @click.stop="openRenameDialog(bank)"
                  >
                    <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5M16.5 3.5a2.121 2.121 0 113 3L12 14l-4 1 1-4 7.5-7.5z" />
                    </svg>
                  </button>
                </div>

                <div class="mt-2 flex items-center gap-2 flex-wrap">
                  <span class="bank-status" :class="bankStatusClass(bank.status)">{{ bankStatusText(bank.status) }}</span>
                  <span class="text-[10px] px-2 py-0.5 rounded-full bg-black/[0.04] text-text-tertiary">
                    {{ bankQuestionCount(bank) }} 题
                  </span>
                </div>

                <p class="mt-2 text-[11px] text-text-tertiary leading-5 line-clamp-2">
                  {{ bankListDescription(bank) }}
                </p>
              </button>

              <div
                v-if="!authStore.isGuest && customBanks.length === 0"
                class="bank-empty-card"
              >
                <p class="text-xs font-semibold text-text-primary">还没有自定义题库</p>
                <p class="mt-2 text-[11px] text-text-tertiary leading-5">上传 TXT 文件后，会在这里显示审核状态和可用题库。</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div ref="questionListRef" class="question-list-scroll custom-scrollbar flex-1 min-h-0 overflow-y-auto px-2 py-2">
        <div class="mb-2 flex items-center justify-between px-1 text-[11px] text-text-tertiary">
          <span>共 {{ totalQuestions }} 题 · 当前 10 题/页</span>
          <span v-if="questions.length > 0 && totalQuestions > questions.length">可翻页查看更多</span>
        </div>

        <div v-if="loading" class="space-y-2">
          <div v-for="i in 6" :key="i" class="rounded-xl border border-black/10 bg-white/70 p-3 animate-pulse">
            <div class="h-3 bg-black/8 rounded w-4/5"></div>
            <div class="h-3 bg-black/8 rounded w-1/2 mt-2"></div>
          </div>
        </div>

        <div v-else-if="questions.length === 0" class="workspace-empty h-full flex items-center justify-center text-sm text-text-tertiary text-center px-4 leading-6">
          {{ listEmptyTip }}
        </div>

        <div v-else class="space-y-2">
          <button
            v-for="q in questions"
            :key="q.id"
            :data-qid="q.id"
            class="workspace-list-item w-full text-left rounded-2xl border p-3 transition-all"
            :class="activeQuestion?.id === q.id
              ? 'workspace-list-item-active border-ai-from/35 bg-ai-from/8 shadow-[0_14px_28px_-20px_rgba(99,102,241,0.55)]'
              : 'border-black/8 bg-white/80 hover:bg-white hover:border-black/14'"
            @click="openQuestion(q)"
          >
            <div class="flex items-center gap-2 flex-wrap">
              <span
                v-if="isDoneQuestion(q.id)"
                class="text-[10px] px-1.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700 font-semibold"
              >
                已做
              </span>
              <span
                v-if="q.sourceType === 'CUSTOM'"
                class="text-[10px] px-1.5 py-0.5 rounded-full bg-sky-100 text-sky-700 font-semibold"
              >
                自定义
              </span>
            </div>
            <h3 class="question-title mt-2 font-semibold text-text-primary" :class="{ 'text-indigo-600': activeQuestion?.id === q.id }">{{ q.title }}</h3>
            <div class="mt-2 flex items-center gap-2 text-[11px] text-text-tertiary flex-wrap">
              <span>{{ categoryLabel(q.category) }}</span>
              <span>·</span>
              <span>{{ difficultyLabel(q.difficulty) }}</span>
              <span>·</span>
              <span>浏览 {{ q.viewCount || 0 }}</span>
              <span v-if="currentBank" class="text-ai-from">· {{ currentBank.name }}</span>
            </div>
          </button>
        </div>

        <div v-if="!loading && totalPages > 1" class="mt-4 flex items-center justify-center gap-2 text-xs">
          <button
            class="workspace-btn workspace-btn-muted px-2.5 py-1 rounded border border-border-subtle bg-bg-surface disabled:opacity-40"
            :disabled="currentPage <= 1"
            @click="changePage(currentPage - 1)"
          >
            上一页
          </button>
          <span class="text-text-tertiary">{{ currentPage }} / {{ totalPages }}</span>
          <button
            class="workspace-btn workspace-btn-muted px-2.5 py-1 rounded border border-border-subtle bg-bg-surface text-text-primary disabled:opacity-40"
            :disabled="currentPage >= totalPages"
            @click="changePage(currentPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </aside>

    <section
      class="question-detail-surface question-detail-scroll custom-scrollbar flex-1 min-h-0 overflow-y-auto bg-bg-base"
      :class="mobileTab === 'list' ? 'hidden md:block' : 'block'"
    >
      <div v-if="!activeQuestion" class="h-full grid place-items-center text-text-tertiary">
        <div v-if="currentBank && !isApprovedBank(currentBank)" class="question-empty-card max-w-lg rounded-3xl border border-black/8 bg-white/82 px-8 py-9 text-center shadow-sm">
          <p class="text-xs tracking-[0.26em] uppercase text-text-tertiary">Custom Bank</p>
          <h3 class="mt-3 text-3xl font-display font-bold text-text-primary">{{ currentBank.name }}</h3>
          <span class="inline-flex mt-4 px-3 py-1 rounded-full text-xs font-semibold" :class="bankStatusClass(currentBank.status)">
            {{ bankStatusText(currentBank.status) }}
          </span>
          <p class="mt-4 text-sm leading-7 text-text-secondary">{{ bankPanelDescription(currentBank) }}</p>
          <p v-if="currentBank.rejectReason" class="mt-3 text-xs text-rose-600">
            驳回原因：{{ currentBank.rejectReason }}
          </p>
        </div>

        <div v-else class="question-empty-wrap text-center px-6">
          <div class="question-empty-icon w-24 h-24 rounded-2xl bg-white/80 border border-black/8 shadow-sm mx-auto mb-4 grid place-items-center">
            <svg class="w-11 h-11 opacity-35" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 6h16M4 12h16M4 18h10" />
            </svg>
          </div>
          <p class="text-xl font-semibold text-slate-700">请先从左侧选择题目，或点击 AI 抽题</p>
        </div>
      </div>

      <div v-else class="question-detail-stack min-h-full px-5 md:px-8 xl:px-10 py-7 max-w-[1020px] mx-auto">
        <div class="flex items-center flex-wrap gap-2">
          <button
            class="workspace-btn workspace-btn-muted md:hidden text-xs px-2.5 py-1 rounded-lg border border-black/10 bg-white text-text-secondary"
            @click="mobileTab = 'list'"
          >
            返回列表
          </button>
          <span class="text-xs px-2 py-1 rounded bg-black/[0.05] text-text-secondary">{{ categoryLabel(activeQuestion.category) }}</span>
          <span class="text-xs px-2 py-1 rounded bg-black/[0.05] text-text-secondary">{{ difficultyLabel(activeQuestion.difficulty) }}</span>
          <span class="text-xs px-2 py-1 rounded bg-black/[0.05] text-text-secondary">
            {{ currentBank ? currentBank.name : '官方题库' }}
          </span>
          <button
            class="workspace-btn ml-auto text-xs px-3 py-1.5 rounded-lg border transition-colors"
            :class="isDoneQuestion(activeQuestion.id)
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-ai-from/35 bg-ai-from/8 text-ai-from hover:bg-ai-from/14'"
            @click="openDoneConfirmDialog(activeQuestion.id)"
          >
            {{ isDoneQuestion(activeQuestion.id) ? '已计入做题' : '完成本题' }}
          </button>
        </div>

        <h1 class="mt-4 text-2xl font-bold text-text-primary leading-tight">{{ activeQuestion.title }}</h1>

        <article class="question-answer-card mt-4 rounded-3xl border border-border-subtle bg-bg-elevated backdrop-blur-xl p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-3 flex-wrap">
            <div class="text-sm font-semibold text-text-primary">参考答案</div>
            <button
              class="workspace-btn workspace-btn-muted text-xs px-2.5 py-1 rounded-lg border border-black/10 bg-white hover:bg-black/[0.03]"
              @click="loadDbAnswer(activeQuestion.id, { preserveExisting: false, force: true })"
              :disabled="dbAnswerLoading"
            >
              {{ dbAnswerLoading ? '读取中...' : '刷新答案' }}
            </button>
            <span v-if="dbAnswerSource" class="text-[11px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
              来源：{{ dbAnswerSource }}
            </span>
          </div>
          <div class="answer-body">
            <div v-if="dbAnswerLoading && !dbAnswer" class="py-3"><LoadingDots /></div>
            <p v-if="dbAnswerLoading && dbAnswerSlow" class="mb-3 text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-xl px-3 py-2">
              参考答案接口正在变慢，线上环境可能在排队或波动中。
            </p>
            <div v-else class="prose prose-sm max-w-none text-text-secondary">
              <TypeWriter :text="answerDisplayText" :renderMarkdown="true" :isTyping="false" />
            </div>
          </div>
        </article>

        <div class="mt-4">
          <button
            class="workspace-btn workspace-btn-primary px-4 py-2 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-ai-from to-ai-to shadow-md hover:opacity-95"
            @click="startAiExplanation"
          >
            AI 深度解析
          </button>
        </div>

        <article v-if="aiStarted" class="question-ai-card mt-4 rounded-3xl border border-black/8 bg-white/84 backdrop-blur-xl p-6 shadow-sm">
          <div class="text-sm font-semibold text-text-primary mb-3">AI 回答</div>
          <div class="prose prose-sm max-w-none text-text-secondary">
            <TypeWriter :text="aiContent || ''" :renderMarkdown="true" :isTyping="aiStreaming" />
            <div v-if="aiStreaming && !aiContent" class="py-2"><LoadingDots /></div>
          </div>
          <p v-if="safeAiErrorText" class="mt-2 text-xs text-danger">AI 请求失败：{{ safeAiErrorText }}</p>
        </article>

        <p v-if="doneTip" class="mt-3 text-xs text-emerald-700">{{ doneTip }}</p>
      </div>
    </section>

    <transition name="submit-note-fade">
      <div v-if="uploadDialogVisible" class="submit-note-overlay" @click.self="closeImportDialog">
        <div class="submit-note-shell">
          <div class="submit-note-dialog">
            <div class="submit-note-header">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <h3 class="submit-note-title">导入题库</h3>
                  <span class="submit-note-badge">发布审核</span>
                </div>
                <p class="submit-note-subtitle">
                  仅支持 `.txt`，提交后会先进入后台审核，通过后即可对外展示。
                </p>
              </div>
              <button class="submit-note-close" @click="closeImportDialog">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>

            <div class="submit-note-body custom-scrollbar">
              <input
                v-model.trim="uploadForm.name"
                type="text"
                class="submit-note-input submit-note-title-input"
                placeholder="请输入题库名称，不填则默认使用文件名"
              />

              <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
                <label class="submit-note-card submit-note-card-compact">
                  <div class="submit-note-card-head">
                    <span class="submit-note-label">分类</span>
                    <span class="submit-note-meta">选择分类</span>
                  </div>
                  <select
                    v-model="uploadForm.category"
                    class="submit-note-card-input"
                  >
                    <option v-for="cat in categories.filter((item) => item.id !== 'all')" :key="cat.id" :value="cat.id">
                      {{ cat.name }}
                    </option>
                  </select>
                </label>

                <label class="submit-note-card submit-note-card-compact">
                  <div class="submit-note-card-head">
                    <span class="submit-note-label">默认难度</span>
                    <span class="submit-note-meta">选择难度</span>
                  </div>
                  <select
                    v-model.number="uploadForm.difficulty"
                    class="submit-note-card-input"
                  >
                    <option :value="1">简单</option>
                    <option :value="2">中等</option>
                    <option :value="3">困难</option>
                  </select>
                </label>
              </div>

              <label class="submit-note-card">
                <div class="submit-note-card-head">
                  <span class="submit-note-label">题库文件</span>
                  <span class="submit-note-meta">上传文件</span>
                </div>
                <div class="submit-note-file-drop">
                  <input
                    ref="uploadInput"
                    type="file"
                    accept=".txt,text/plain"
                    class="submit-note-file-input"
                    @change="handleUploadFileChange"
                  />
                  <div class="submit-note-file-icon">
                    <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                  </div>
                  <p class="submit-note-file-text">
                    {{ uploadFile ? uploadFile.name : '点击或将文件拖拽到此处上传' }}
                  </p>
                  <p class="submit-note-file-hint">仅支持 txt，建议控制在 5MB 以内</p>
                </div>
              </label>

              <div class="submit-note-card">
                <div class="flex items-center justify-between gap-3">
                  <span class="submit-note-label">格式要求（严格）</span>
                  <button
                    class="submit-note-btn submit-note-btn-secondary text-xs px-2 py-1"
                    @click="downloadTxtTemplate"
                  >
                    下载模板
                  </button>
                </div>
                <p class="submit-note-format-desc">
                  每题两段：第一行是题目，下一行起是答案。题目与题目之间空一行。
                </p>
                <pre class="submit-note-format-example">题目：什么是索引失效？
答案：对索引列做函数计算、隐式类型转换等都可能导致索引失效。

题目：什么是缓存穿透？
答案：请求的数据不存在，缓存和数据库都没有，导致请求直接打到数据库。</pre>
              </div>
            </div>

            <div class="submit-note-footer">
              <button class="submit-note-btn submit-note-btn-secondary" @click="closeImportDialog">取消</button>
              <button
                class="submit-note-btn submit-note-btn-primary"
                :disabled="uploadingBank"
                @click="submitImport"
              >
                {{ uploadingBank ? '提交中...' : '提交审核' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <div
      v-if="renameDialogVisible"
      class="fixed inset-0 z-[180] bg-transparent flex items-center justify-center p-4"
      @click.self="closeRenameDialog"
    >
      <div class="w-full max-w-md rounded-3xl border border-border-subtle bg-[#f4f5fb] dark:bg-[#221c30] p-6 shadow-2xl">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-lg font-semibold text-text-primary">重命名题库</h3>
            <p class="text-xs text-text-tertiary mt-1">修改后会立即更新你的题库列表显示。</p>
          </div>
          <button class="rounded-lg p-1.5 hover:bg-black/[0.04] text-text-tertiary" @click="closeRenameDialog">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <label class="block mt-5">
          <span class="text-xs text-text-tertiary">题库名称</span>
          <input
            v-model.trim="renameForm.name"
            type="text"
            class="workspace-control mt-1.5 w-full rounded-xl border border-black/10 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25"
            placeholder="请输入新的题库名称"
          />
        </label>

        <div class="mt-6 flex justify-end gap-2">
          <button class="workspace-btn workspace-btn-muted px-3 py-2 rounded-xl border border-black/10 text-sm hover:bg-black/[0.03]" @click="closeRenameDialog">
            取消
          </button>
          <button
            class="workspace-btn workspace-btn-primary px-3 py-2 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-ai-from to-ai-to disabled:opacity-60"
            :disabled="renamingBank"
            @click="submitRename"
          >
            {{ renamingBank ? '保存中...' : '保存名称' }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="doneConfirmVisible"
      class="fixed inset-0 z-[180] bg-transparent flex items-center justify-center p-4"
      @click.self="closeDoneConfirmDialog"
    >
      <div class="w-full max-w-sm rounded-3xl border border-border-subtle bg-[#f4f5fb] dark:bg-[#221c30] p-6 shadow-2xl">
        <h3 class="text-lg font-semibold text-text-primary">是否真会了？</h3>
        <p class="mt-2 text-sm text-text-secondary leading-6">
          确认后会把本题记入你的做题记录，并同步到排行榜统计。
        </p>
        <div class="mt-6 flex justify-end gap-2">
          <button
            class="workspace-btn workspace-btn-muted px-3 py-2 rounded-xl border border-black/10 text-sm hover:bg-black/[0.03]"
            :disabled="doneConfirmLoading"
            @click="closeDoneConfirmDialog"
          >
            再看看
          </button>
          <button
            class="workspace-btn workspace-btn-primary px-4 py-2 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-ai-from to-ai-to disabled:opacity-60"
            :disabled="doneConfirmLoading"
            @click="confirmMarkQuestionDone"
          >
            {{ doneConfirmLoading ? '提交中...' : '是' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TypeWriter from '@/components/common/TypeWriter.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'
import { fireConfetti, resetConfetti } from '@/composables/useConfetti'
import { useSSE } from '@/composables/useSSE'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'
import { useResourceCacheStore } from '@/stores/resourceCache'

const questionListRef = ref(null)
const authStore = useAuthStore()
const resourceCacheStore = useResourceCacheStore()
const route = useRoute()
const router = useRouter()
const QUESTION_RUNTIME_NAMESPACE = 'question-bank'
const QUESTION_ANSWER_NAMESPACE = 'question-bank-answer'
const QUESTION_META_NAMESPACE = 'question-bank-meta'
const QUESTION_LIST_CACHE_TTL = 2 * 60 * 1000
const QUESTION_ANSWER_CACHE_TTL = 30 * 60 * 1000
const QUESTION_CATEGORY_CACHE_TTL = 30 * 60 * 1000
const QUESTION_BANK_CACHE_TTL = 60 * 1000
const QUESTION_ANSWER_TIMEOUT_MS = 2500
const QUESTION_ANSWER_SLOW_MS = 1200
const AI_EXPLAIN_TIMEOUT_MS = 20000
const AI_EXPLAIN_FIRST_CHUNK_TIMEOUT_MS = 5000
const {
  content: aiContent,
  isStreaming: aiStreaming,
  error: aiError,
  startStream,
  reset: resetAi,
} = useSSE()

const builtinCategoryNameMap = Object.freeze({
  java: 'Java 核心',
  spring: 'Spring 生态',
  db: '数据存储',
  redis: 'Redis',
  algo: '算法',
  network: '计算机网络',
  'system-design': '系统设计',
  arch: '架构设计',
  linux: 'Linux',
})
const categoryCodeAliasMap = Object.freeze({
  database: 'db',
  '数据库': 'db',
  algorithm: 'algo',
  '算法': 'algo',
  '计算机网络': 'network',
  'system-sign': 'system-design',
  'system_sign': 'system-design',
  'system design': 'system-design',
  '系统设计': 'system-design',
  architecture: 'arch',
  '架构': 'arch',
  '架构设计': 'arch',
})
const fallbackCategoryOptions = Object.entries(builtinCategoryNameMap).map(([code, name]) => ({ code, name }))
const categories = ref([{ id: 'all', name: '全部分类' }])

const loading = ref(false)
const randomLoading = ref(false)
const mobileTab = ref('list')
const currentCategory = ref('all')
const difficultyFilter = ref('all')
const searchQuery = ref('')
const questions = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const totalQuestions = ref(0)
const activeQuestion = ref(null)
const pendingHighlightId = ref(0)
const aiStarted = ref(false)
const doneQuestionIds = ref(new Set())
const pendingDoneQuestionIds = ref(new Set())
const doneTip = ref('')
const doneConfirmVisible = ref(false)
const doneConfirmLoading = ref(false)
const pendingDoneQuestionId = ref(null)
const questionViewFloorMap = ref(new Map())

const dbAnswer = ref('')
const dbAnswerSource = ref('')
const dbAnswerLoading = ref(false)
const dbAnswerSlow = ref(false)
const bankPanelCollapsed = ref(true)

const customBanks = ref([])
const currentBankId = ref(null)

const buildQuestionListCacheKey = (page = currentPage.value) => JSON.stringify({
  page: Number(page || 1),
  size: 10,
  category: currentCategory.value,
  difficulty: difficultyFilter.value,
  keyword: searchQuery.value.trim(),
  bankId: currentBankId.value || 0,
})

const buildQuestionListSnapshot = (page = currentPage.value) => ({
  records: questions.value.map((item) => ({ ...item })),
  currentPage: Number(page || currentPage.value || 1),
  totalPages: Number(totalPages.value || 1),
  totalQuestions: Number(totalQuestions.value || 0),
})

const applyQuestionListSnapshot = (snapshot) => {
  questions.value = Array.isArray(snapshot?.records) ? snapshot.records.map((item) => ({ ...item })) : []
  currentPage.value = Number(snapshot?.currentPage || 1)
  totalPages.value = Math.max(1, Number(snapshot?.totalPages || 1))
  totalQuestions.value = Math.max(0, Number(snapshot?.totalQuestions || 0))
}

const persistQuestionListSnapshot = (page = currentPage.value) => {
  resourceCacheStore.writeList(
    QUESTION_RUNTIME_NAMESPACE,
    buildQuestionListCacheKey(page),
    buildQuestionListSnapshot(page),
  )
}

const persistQuestionDetailSnapshot = (question = activeQuestion.value) => {
  const qid = Number(question?.id || 0)
  if (!qid) return
  resourceCacheStore.writeDetail(QUESTION_RUNTIME_NAMESPACE, qid, { ...question })
}

const normalizeQuestionRecord = (question) => ({
  ...question,
  category: normalizeCategoryCode(question?.category),
  tags: Array.isArray(question?.tags)
    ? [...question.tags]
    : (typeof question?.tags === 'string'
        ? question.tags.split(',').map((item) => item.trim()).filter(Boolean)
        : []),
})

const readCachedQuestionAnswerSnapshot = (questionId) => {
  const qid = Number(questionId || 0)
  if (!Number.isInteger(qid) || qid <= 0) return null
  return resourceCacheStore.readDetail(QUESTION_ANSWER_NAMESPACE, qid, QUESTION_ANSWER_CACHE_TTL)
}

const writeCachedQuestionAnswerSnapshot = (questionId, snapshot) => {
  const qid = Number(questionId || 0)
  if (!Number.isInteger(qid) || qid <= 0 || !snapshot) return
  resourceCacheStore.writeDetail(QUESTION_ANSWER_NAMESPACE, qid, snapshot)
}

const applyQuestionAnswerSnapshot = (snapshot) => {
  dbAnswer.value = sanitizeOfficialAnswer(snapshot?.answer || '')
  dbAnswerSource.value = String(snapshot?.source || '').trim()
}

const uploadDialogVisible = ref(false)
const uploadingBank = ref(false)
const uploadInput = ref(null)
const uploadFile = ref(null)
const uploadForm = reactive({
  name: '',
  category: 'java',
  difficulty: 2,
})

const renameDialogVisible = ref(false)
const renamingBank = ref(false)
const renameBankId = ref(null)
const renameForm = reactive({
  name: '',
})

const bankScroller = ref(null)
const canScrollBankLeft = ref(false)
const canScrollBankRight = ref(false)

const touchState = reactive({
  startX: 0,
  startY: 0,
  deltaX: 0,
  deltaY: 0,
  ignore: false,
})

const currentBank = computed(() => customBanks.value.find((item) => item.id === currentBankId.value) || null)
const isCurrentBankReady = computed(() => !currentBank.value || Number(currentBank.value.status) === 1)
const safeAiError = computed(() => {
  const raw = String(aiError.value || '').trim()
  if (!raw) return ''
  if (/https?:\/\//i.test(raw)) {
    return 'AI 服务暂时不可用，请稍后重试。'
  }
  return raw
})
const listEmptyTip = computed(() => {
  if (currentBank.value?.status === 0) {
    return '该题库已提交审核，审核通过后题目会自动出现在这里。'
  }
  if (currentBank.value?.status === 2) {
    return currentBank.value.rejectReason
      ? `审核未通过：${currentBank.value.rejectReason}`
      : '该题库暂未通过审核，请修改后重新上传。'
  }
  return '当前条件下暂无题目'
})

const safeAiErrorText = computed(() => {
  const raw = String(safeAiError.value || '').trim()
  if (!raw) return ''
  if (/failed to fetch|networkerror|load failed|fetch failed/i.test(raw)) {
    return 'AI 服务连接失败，通常是线上网关或后端 AI 服务暂时不可用，请稍后重试。'
  }
  if (/timeout|timed out|超时/i.test(raw)) {
    return 'AI 服务响应超时，线上链路偏慢时会出现这个提示，请稍后重试。'
  }
  return raw
})
const answerDisplayText = computed(() => {
  if (dbAnswer.value) return dbAnswer.value
  if (dbAnswerSlow.value) {
    return '参考答案加载较慢，线上接口可能正在波动。你可以稍后点击“刷新答案”重试。'
  }
  return '当前题目尚未配置参考答案'
})

const categoryLabel = (category) => {
  const code = normalizeCategoryCode(category)
  if (!code) return '未分类'
  return categories.value.find((c) => c.id === code)?.name || code
}
const difficultyLabel = (level) => ({ 1: '简单', 2: '中等', 3: '困难' }[Number(level)] || '未知')
const isQuestionDonePending = (id) => pendingDoneQuestionIds.value.has(Number(id))
const isDoneQuestion = (id) => isQuestionDonePending(id) || doneQuestionIds.value.has(Number(id))
const isApprovedBank = (bank) => Number(bank?.status) === 1
const bankQuestionCount = (bank) => Number(bank?.importedQuestionCount || bank?.questionCount || 0)
const bankStatusText = (status) => {
  if (Number(status) === 0) return '待审核'
  if (Number(status) === 1) return '已通过'
  if (Number(status) === 2) return '已驳回'
  return '未知'
}
const bankStatusClass = (status) => {
  if (Number(status) === 0) return 'bank-status-pending'
  if (Number(status) === 1) return 'bank-status-approved'
  if (Number(status) === 2) return 'bank-status-rejected'
  return 'bank-status-default'
}
const bankListDescription = (bank) => {
  if (Number(bank?.status) === 0) {
    return `已解析 ${bankQuestionCount(bank)} 题，等待管理员审核。`
  }
  if (Number(bank?.status) === 2) {
    return bank?.rejectReason || '审核未通过，可调整内容后重新上传。'
  }
  return `已入库 ${bankQuestionCount(bank)} 题，当前分类：${categoryLabel(bank?.category)}。`
}
const bankPanelDescription = (bank) => {
  if (Number(bank?.status) === 0) {
    return '这份题库正在等待后台审核。审核通过后，题目会自动写入数据库，并可以像官方题库一样刷题、抽题和 AI 解析。'
  }
  if (Number(bank?.status) === 2) {
    return '这份题库暂时没有通过审核。你可以根据驳回原因重新整理文件后再次上传，也可以先修改题库名称保留记录。'
  }
  return '这份题库已经可用，直接从左侧选择题目开始练习即可。'
}

const normalizeCategoryCode = (value) => {
  const text = String(value || '').trim()
  if (!text) return ''
  const compact = text.replace(/_/g, '-').replace(/\s+/g, '-')
  const lowered = compact.toLowerCase()
  return categoryCodeAliasMap[text] || categoryCodeAliasMap[lowered] || lowered
}
const normalizeCategoryDisplayName = (code, name = '') => {
  const normalizedCode = normalizeCategoryCode(code)
  if (!normalizedCode) return ''
  const builtin = builtinCategoryNameMap[normalizedCode]
  if (builtin) return builtin

  const text = String(name || '').trim()
  if (!text) return normalizedCode
  const onlyQuestionMarks = /^\?+$/.test(text)
  if (text.includes('\uFFFD') || onlyQuestionMarks) {
    return normalizedCode
  }
  return text
}
const defaultImportCategory = () => categories.value.find((item) => item.id !== 'all')?.id || 'java'
const hasCategoryCode = (code) => categories.value.some((item) => item.id === code)
const isMobileViewport = () => window.innerWidth < 768
const QUESTION_DONE_CONFETTI_DURATION_MS = 3 * 1000
let questionDoneConfettiIntervalId = 0

const stopQuestionDoneConfetti = () => {
  if (questionDoneConfettiIntervalId) {
    clearInterval(questionDoneConfettiIntervalId)
    questionDoneConfettiIntervalId = 0
  }
  resetConfetti()
}

const randomInRange = (min, max) => Math.random() * (max - min) + min

const celebrateQuestionDone = () => {
  stopQuestionDoneConfetti()

  const duration = QUESTION_DONE_CONFETTI_DURATION_MS
  const animationEnd = Date.now() + duration
  const defaults = {
    startVelocity: 30,
    spread: 360,
    ticks: 60,
    zIndex: 1200,
  }

  questionDoneConfettiIntervalId = window.setInterval(() => {
    const timeLeft = animationEnd - Date.now()
    if (timeLeft <= 0) {
      stopQuestionDoneConfetti()
      return
    }

    const particleCount = 50 * (timeLeft / duration)
    fireConfetti({
      ...defaults,
      particleCount,
      origin: { x: randomInRange(0.1, 0.3), y: Math.random() - 0.2 },
    })
    fireConfetti({
      ...defaults,
      particleCount,
      origin: { x: randomInRange(0.7, 0.9), y: Math.random() - 0.2 },
    })
  }, 250)
}

const applyCategoryOptions = (rows) => {
  const next = [{ id: 'all', name: '全部分类' }]
  const seen = new Set(['all'])
  for (const row of rows) {
    const code = normalizeCategoryCode(row?.code)
    const name = String(row?.name || row?.code || '').trim()
    if (!code || seen.has(code)) continue
    seen.add(code)
    next.push({
      id: code,
      name: normalizeCategoryDisplayName(code, name),
    })
  }
  categories.value = next
}

const syncCategorySelectionAfterOptionsLoaded = () => {
  if (!hasCategoryCode(currentCategory.value)) {
    currentCategory.value = 'all'
  }
  if (!hasCategoryCode(uploadForm.category) || uploadForm.category === 'all') {
    uploadForm.category = defaultImportCategory()
  }
}

const clearAnswerPanels = () => {
  dbAnswer.value = ''
  dbAnswerSource.value = ''
  dbAnswerSlow.value = false
  doneTip.value = ''
  aiStarted.value = false
  resetAi()
}

const readQuestionViewFloor = (questionId) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return null
  return questionViewFloorMap.value.get(qid) ?? null
}

const rememberQuestionViewCount = (questionId, viewCount) => {
  const qid = Number(questionId)
  const nextCount = Number(viewCount)
  if (!Number.isInteger(qid) || qid <= 0 || !Number.isFinite(nextCount)) return
  const nextMap = new Map(questionViewFloorMap.value)
  nextMap.set(qid, Math.max(0, nextCount))
  questionViewFloorMap.value = nextMap
}

const applyQuestionViewFloor = (question) => {
  if (!question?.id) return question
  const localFloor = readQuestionViewFloor(question.id)
  if (localFloor == null) return question
  return {
    ...question,
    viewCount: Math.max(Number(question.viewCount || 0), localFloor),
  }
}

const updateDoneQuestionSet = (updater) => {
  const next = new Set(doneQuestionIds.value)
  updater(next)
  doneQuestionIds.value = next
}

const updatePendingDoneQuestionSet = (updater) => {
  const next = new Set(pendingDoneQuestionIds.value)
  updater(next)
  pendingDoneQuestionIds.value = next
}

const patchListQuestionViewCount = (questionId, computeNext) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return
  questions.value = questions.value.map((item) => {
    if (Number(item?.id) !== qid) return item
    const current = Number(item?.viewCount || 0)
    const next = Number(computeNext(current))
    const safeNext = Number.isFinite(next) ? Math.max(0, next) : current
    rememberQuestionViewCount(qid, safeNext)
    return {
      ...item,
      viewCount: safeNext,
    }
  })
  persistQuestionListSnapshot()
}

const bumpLocalQuestionViewCount = (questionId) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return
  patchListQuestionViewCount(qid, (current) => current + 1)
  if (Number(activeQuestion.value?.id) === qid) {
    const nextViews = Number(activeQuestion.value?.viewCount || 0) + 1
    rememberQuestionViewCount(qid, nextViews)
    activeQuestion.value = {
      ...activeQuestion.value,
      viewCount: nextViews,
    }
    persistQuestionDetailSnapshot(activeQuestion.value)
  }
}

const syncQuestionViewCountFromServer = (questionId, viewCount) => {
  const qid = Number(questionId)
  const count = Number(viewCount)
  if (!Number.isInteger(qid) || qid <= 0 || !Number.isFinite(count)) return
  rememberQuestionViewCount(qid, count)
  patchListQuestionViewCount(qid, (current) => Math.max(current, count))
  if (Number(activeQuestion.value?.id) === qid) {
    const current = Number(activeQuestion.value?.viewCount || 0)
    activeQuestion.value = {
      ...activeQuestion.value,
      viewCount: Math.max(current, count),
    }
    persistQuestionDetailSnapshot(activeQuestion.value)
  }
}

const markQuestionViewed = async (questionId) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return
  try {
    const res = await api.post(`/api/questions/${qid}/view`)
    if (res.code === 200 && res.data && Number.isFinite(Number(res.data.viewCount))) {
      syncQuestionViewCountFromServer(qid, Number(res.data.viewCount))
      return
    }
  } catch (_) {
    // no-op
  }
  // fallback for temporary network issues
  bumpLocalQuestionViewCount(qid)
}

const sanitizeOfficialAnswer = (text) => {
  if (!text) return ''
  return String(text)
    .replace(/^\s*\[(数据库标准答案|标准答案)\]\s*/g, '')
    .replace(/^数据库标准答案[:：]?\s*/g, '')
    .replace(/^标准答案[:：]?\s*/g, '')
    .trim()
}

const syncCategoryToSelectedBank = () => {
  if (currentBank.value && isApprovedBank(currentBank.value) && currentBank.value.category) {
    const normalized = normalizeCategoryCode(currentBank.value.category)
    currentCategory.value = hasCategoryCode(normalized) ? normalized : 'all'
  }
}

const resetTouchState = () => {
  touchState.startX = 0
  touchState.startY = 0
  touchState.deltaX = 0
  touchState.deltaY = 0
  touchState.ignore = false
}

const hasBlockingOverlay = () => uploadDialogVisible.value || renameDialogVisible.value || doneConfirmVisible.value

const handleTouchStart = (event) => {
  if (!isMobileViewport() || hasBlockingOverlay()) {
    resetTouchState()
    return
  }
  const target = event.target instanceof Element ? event.target : null
  touchState.ignore = !!target?.closest?.('input, textarea, select, .bank-scroll-area, [data-swipe-lock]')
  const touch = event.touches?.[0]
  if (!touch) return
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.deltaX = 0
  touchState.deltaY = 0
}

const handleTouchMove = (event) => {
  if (touchState.ignore) return
  const touch = event.touches?.[0]
  if (!touch) return
  touchState.deltaX = touch.clientX - touchState.startX
  touchState.deltaY = touch.clientY - touchState.startY
}

const handleTouchEnd = () => {
  if (!isMobileViewport() || touchState.ignore) {
    resetTouchState()
    return
  }
  const absX = Math.abs(touchState.deltaX)
  const absY = Math.abs(touchState.deltaY)
  if (absX < 72 || absX < absY * 1.2) {
    resetTouchState()
    return
  }
  if (touchState.deltaX < 0 && mobileTab.value === 'list' && activeQuestion.value) {
    mobileTab.value = 'detail'
  } else if (touchState.deltaX > 0 && mobileTab.value === 'detail') {
    mobileTab.value = 'list'
  }
  resetTouchState()
}

const updateBankNavState = () => {
  const el = bankScroller.value
  if (!el) return
  canScrollBankLeft.value = el.scrollLeft > 2
  canScrollBankRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 2
}

const scrollBankNav = (direction) => {
  const el = bankScroller.value
  if (!el) return
  el.scrollBy({
    left: direction * 180,
    behavior: 'smooth',
  })
  window.setTimeout(updateBankNavState, 220)
}

const handleBankWheel = (event) => {
  const el = bankScroller.value
  if (!el || el.scrollWidth <= el.clientWidth) return
  if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return
  event.preventDefault()
  el.scrollLeft += event.deltaY
  updateBankNavState()
}

const fetchCategoryOptions = async ({ force = false } = {}) => {
  if (!force) {
    const cached = resourceCacheStore.readList(QUESTION_META_NAMESPACE, 'categories', QUESTION_CATEGORY_CACHE_TTL)
    if (Array.isArray(cached) && cached.length > 0) {
      applyCategoryOptions(cached)
      syncCategorySelectionAfterOptionsLoaded()
      return
    }
  }
  try {
    const res = await api.get('/api/questions/categories')
    if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
      applyCategoryOptions(res.data)
      resourceCacheStore.writeList(QUESTION_META_NAMESPACE, 'categories', res.data)
      syncCategorySelectionAfterOptionsLoaded()
      return
    }
  } catch (_) {
    // ignore and fallback
  }

  applyCategoryOptions(fallbackCategoryOptions)
  resourceCacheStore.writeList(QUESTION_META_NAMESPACE, 'categories', fallbackCategoryOptions)
  syncCategorySelectionAfterOptionsLoaded()
}

const fetchCustomBanks = async ({ force = false } = {}) => {
  if (authStore.isGuest) {
    customBanks.value = []
    currentBankId.value = null
    return
  }
  if (!force) {
    const cached = resourceCacheStore.readList(QUESTION_META_NAMESPACE, 'custom-banks', QUESTION_BANK_CACHE_TTL)
    if (Array.isArray(cached)) {
      customBanks.value = cached
      if (currentBankId.value && !customBanks.value.some((item) => item.id === currentBankId.value)) {
        currentBankId.value = null
      }
      return
    }
  }
  try {
    const res = await api.get('/api/question-banks/mine?page=1&size=50')
    if (res.code === 200) {
      customBanks.value = (res.data?.records || []).map((bank) => ({
        ...bank,
        category: normalizeCategoryCode(bank?.category),
      }))
      resourceCacheStore.writeList(QUESTION_META_NAMESPACE, 'custom-banks', customBanks.value)
      if (currentBankId.value && !customBanks.value.some((item) => item.id === currentBankId.value)) {
        currentBankId.value = null
      }
      return
    }
  } catch (_) {
    // ignore bank list errors on page load
  }
  customBanks.value = []
}

const fetchQuestions = async (page = currentPage.value, options = {}) => {
  const { force = false } = options
  if (currentBank.value && !isApprovedBank(currentBank.value)) {
    questions.value = []
    currentPage.value = 1
    totalPages.value = 1
    totalQuestions.value = 0
    activeQuestion.value = null
    return
  }

  const cacheKey = buildQuestionListCacheKey(page)
  if (!force) {
    const cachedSnapshot = resourceCacheStore.readList(QUESTION_RUNTIME_NAMESPACE, cacheKey, QUESTION_LIST_CACHE_TTL)
    if (cachedSnapshot) {
      applyQuestionListSnapshot(cachedSnapshot)
      syncActiveQuestionAfterListLoaded()
      return
    }
  }

  loading.value = true
  try {
    const snapshot = await resourceCacheStore.loadList(
      QUESTION_RUNTIME_NAMESPACE,
      cacheKey,
      async () => {
        const params = new URLSearchParams()
        params.set('page', String(page))
        params.set('size', '10')
        if (currentCategory.value !== 'all') params.set('category', currentCategory.value)
        if (difficultyFilter.value !== 'all') params.set('difficulty', difficultyFilter.value)
        if (searchQuery.value.trim()) params.set('keyword', searchQuery.value.trim())
        if (currentBankId.value) params.set('bankId', String(currentBankId.value))

        const res = await api.get(`/api/questions?${params.toString()}`)
        if (res.code !== 200) {
          throw new Error(res.msg || '加载题库失败')
        }
        return {
          records: (res.data?.records || []).map(normalizeQuestionRecord).map(applyQuestionViewFloor),
          currentPage: Number(page || 1),
          totalPages: Math.max(1, Number(res.data?.pages || 1)),
          totalQuestions: Math.max(0, Number(res.data?.total || 0)),
        }
      },
      { ttlMs: QUESTION_LIST_CACHE_TTL, force },
    )
    applyQuestionListSnapshot(snapshot)
    syncActiveQuestionAfterListLoaded()
  } catch (_) {
    questions.value = []
    totalPages.value = 1
    totalQuestions.value = 0
    activeQuestion.value = null
    clearAnswerPanels()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchQuestions(1)
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  fetchQuestions(page)
}

const selectBank = (bank) => {
  currentBankId.value = bank?.id ?? null
  if (bank && isApprovedBank(bank) && bank.category) {
    const normalized = normalizeCategoryCode(bank.category)
    currentCategory.value = hasCategoryCode(normalized) ? normalized : 'all'
  }
  activeQuestion.value = null
  if (window.innerWidth < 768) {
    mobileTab.value = 'list'
  }
  clearAnswerPanels()
}

let latestAnswerRequestId = 0

const prefetchQuestionAnswer = async (questionId, options = {}) => {
  const { force = false } = options
  const qid = Number(questionId || 0)
  if (!Number.isInteger(qid) || qid <= 0) return null
  try {
    const snapshot = await resourceCacheStore.loadDetail(
      QUESTION_ANSWER_NAMESPACE,
      qid,
      async () => {
        const res = await api.get(`/api/questions/${qid}/answer`, {
          timeoutMs: QUESTION_ANSWER_TIMEOUT_MS,
          retryCount: 0,
        })
        if (res.code !== 200 || !res.data) {
          throw new Error(res.msg || '加载题目答案失败')
        }
        return {
          id: qid,
          answer: sanitizeOfficialAnswer(res.data.answer),
          source: res.data.source || '',
        }
      },
      { ttlMs: QUESTION_ANSWER_CACHE_TTL, force },
    )
    writeCachedQuestionAnswerSnapshot(qid, snapshot)
    return snapshot
  } catch (_) {
    return null
  }
}

const warmQuestionAnswers = (questionRows = []) => {
  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
  if (connection?.saveData) return
  if (/(^|[^a-z])(slow-?2g|2g)($|[^a-z])/i.test(String(connection?.effectiveType || ''))) return
  const ids = questionRows
    .map((item) => Number(item?.id || 0))
    .filter((qid, index, arr) => Number.isInteger(qid) && qid > 0 && arr.indexOf(qid) === index)
    .slice(0, 1)
  for (const qid of ids) {
    void prefetchQuestionAnswer(qid)
  }
}

const syncActiveQuestionAfterListLoaded = () => {
  if (!questions.value.length) {
    activeQuestion.value = null
    clearAnswerPanels()
    return
  }

  // 有 pending highlight 时不自动选中第一题，让 handleHighlight 接管
  if (pendingHighlightId.value > 0) {
    return
  }

  const activeId = Number(activeQuestion.value?.id || 0)
  const matched = questions.value.find((item) => Number(item?.id || 0) === activeId)
  if (matched) {
    activeQuestion.value = {
      ...matched,
      ...activeQuestion.value,
      id: matched.id,
      category: matched.category,
      tags: matched.tags,
    }
    persistQuestionDetailSnapshot(activeQuestion.value)
    if (!dbAnswer.value && !dbAnswerLoading.value) {
      void loadDbAnswer(activeId, { preserveExisting: true })
    }
    warmQuestionAnswers([matched, ...questions.value.filter((item) => Number(item?.id || 0) !== activeId)])
    return
  }

  const firstQuestion = questions.value[0]
  activeQuestion.value = firstQuestion
  persistQuestionDetailSnapshot(activeQuestion.value)
  clearAnswerPanels()
  const firstQuestionId = Number(firstQuestion?.id || 0)
  if (firstQuestionId > 0) {
    void loadDbAnswer(firstQuestionId, { preserveExisting: false })
    warmQuestionAnswers(questions.value)
  }
}

const scrollToQuestionInList = (questionId) => {
  if (!questionListRef.value) return
  const el = questionListRef.value.querySelector(`[data-qid="${questionId}"]`)
  if (el) {
    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }
}

const openQuestion = async (question) => {
  const nextQuestion = applyQuestionViewFloor(normalizeQuestionRecord(question || {}))
  const qid = Number(nextQuestion?.id || 0)
  const isSameQuestion = qid > 0 && Number(activeQuestion.value?.id || 0) === qid
  if (isSameQuestion) {
    scrollToQuestionInList(qid)
    if (!dbAnswer.value && !dbAnswerLoading.value) {
      void loadDbAnswer(qid, { preserveExisting: true })
    }
    const latest = questions.value.find((item) => Number(item?.id || 0) === qid)
    if (latest && activeQuestion.value) {
      activeQuestion.value = {
        ...activeQuestion.value,
        viewCount: Number(latest.viewCount || activeQuestion.value.viewCount || 0),
      }
    }
    if (window.innerWidth < 768) {
      mobileTab.value = 'detail'
    }
    return
  }

  clearAnswerPanels()
  activeQuestion.value = nextQuestion
  // 滚动到选中题目在列表中的位置
  scrollToQuestionInList(qid)
  persistQuestionDetailSnapshot(activeQuestion.value)
  void markQuestionViewed(qid)
  if (window.innerWidth < 768) {
    mobileTab.value = 'detail'
  }
  void loadDbAnswer(qid, { preserveExisting: false })
  warmQuestionAnswers([nextQuestion, ...questions.value.filter((item) => Number(item?.id || 0) !== qid)])
}

const loadDbAnswer = async (questionId, options = {}) => {
  const { preserveExisting = true, force = false } = options
  const qid = Number(questionId || 0)
  if (!Number.isInteger(qid) || qid <= 0) return
  const reqId = ++latestAnswerRequestId
  const previousAnswer = dbAnswer.value
  const previousSource = dbAnswerSource.value
  let slowTimer = 0
  if (!force) {
    const cached = readCachedQuestionAnswerSnapshot(qid)
    if (cached) {
      if (reqId !== latestAnswerRequestId) return
      applyQuestionAnswerSnapshot(cached)
      dbAnswerLoading.value = false
      dbAnswerSlow.value = false
      return
    }
  }
  if (!preserveExisting) {
    dbAnswer.value = ''
    dbAnswerSource.value = ''
  }
  dbAnswerSlow.value = false
  dbAnswerLoading.value = true
  slowTimer = window.setTimeout(() => {
    if (reqId === latestAnswerRequestId && dbAnswerLoading.value) {
      dbAnswerSlow.value = true
    }
  }, QUESTION_ANSWER_SLOW_MS)
  try {
    const snapshot = await prefetchQuestionAnswer(qid, { force })
    if (reqId !== latestAnswerRequestId) return
    if (snapshot) {
      applyQuestionAnswerSnapshot(snapshot)
      dbAnswerSlow.value = false
      return
    }
    if (preserveExisting) {
      dbAnswer.value = previousAnswer
      dbAnswerSource.value = previousSource
      dbAnswerSlow.value = false
      return
    }
    dbAnswer.value = ''
    dbAnswerSource.value = ''
    dbAnswerSlow.value = true
  } catch (_) {
    if (reqId !== latestAnswerRequestId) return
    if (preserveExisting) {
      dbAnswer.value = previousAnswer
      dbAnswerSource.value = previousSource
      dbAnswerSlow.value = false
      return
    }
    dbAnswer.value = ''
    dbAnswerSource.value = ''
    dbAnswerSlow.value = true
  } finally {
    if (slowTimer) {
      window.clearTimeout(slowTimer)
    }
    if (reqId === latestAnswerRequestId) {
      dbAnswerLoading.value = false
    }
  }
}

const drawRandomQuestion = async () => {
  if (currentBank.value && !isApprovedBank(currentBank.value)) {
    alert(bankPanelDescription(currentBank.value))
    return
  }

  randomLoading.value = true
  try {
    const params = new URLSearchParams()
    if (currentCategory.value !== 'all') params.set('category', currentCategory.value)
    if (difficultyFilter.value !== 'all') params.set('difficulty', difficultyFilter.value)
    if (currentBankId.value) params.set('bankId', String(currentBankId.value))

    const query = params.toString()
    const res = await api.get(`/api/questions/random${query ? `?${query}` : ''}`)
    if (res.code === 200 && res.data) {
      await openQuestion(res.data)
      return
    }
    alert(res.msg || '当前条件下暂无可抽取题目')
  } catch (e) {
    alert(e.message || '抽题失败，请稍后重试')
  } finally {
    randomLoading.value = false
  }
}

const loadDoneFromServer = async () => {
  if (authStore.isGuest) {
    doneQuestionIds.value = new Set()
    pendingDoneQuestionIds.value = new Set()
    return
  }
  try {
    const res = await api.get('/api/leaderboard/question-done')
    if (res.code === 200 && Array.isArray(res.data)) {
      doneQuestionIds.value = new Set(
        res.data.map((v) => Number(v)).filter((v) => Number.isInteger(v) && v > 0),
      )
      pendingDoneQuestionIds.value = new Set(
        [...pendingDoneQuestionIds.value].filter((id) => !doneQuestionIds.value.has(id)),
      )
    }
  } catch (_) {
    doneQuestionIds.value = new Set()
  }
}

const openDoneConfirmDialog = (questionId) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return

  if (authStore.isGuest) {
    doneTip.value = '游客模式不参与做题统计，登录后可计入排行榜。'
    return
  }
  if (isQuestionDonePending(qid)) {
    doneTip.value = '本题正在同步做题记录，请稍候。'
    return
  }
  if (isDoneQuestion(qid)) {
    doneTip.value = '这道题已经记录过做题次数。'
    return
  }

  pendingDoneQuestionId.value = qid
  doneConfirmVisible.value = true
}

const closeDoneConfirmDialog = () => {
  if (doneConfirmLoading.value) return
  doneConfirmVisible.value = false
  pendingDoneQuestionId.value = null
}

const applyQuestionDoneSuccess = (questionId) => {
  updatePendingDoneQuestionSet((next) => next.delete(questionId))
  updateDoneQuestionSet((next) => next.add(questionId))
  doneTip.value = '宸茶褰曟湰棰橈紝鎺掕姒滄暟鎹細鍚屾鏇存柊銆?'
  doneTip.value = '已记录本题，排行榜数据会同步更新。'
  celebrateQuestionDone()
}

const confirmMarkQuestionDone = async () => {
  const qid = Number(pendingDoneQuestionId.value)
  if (!Number.isInteger(qid) || qid <= 0) {
    closeDoneConfirmDialog()
    return
  }
  if (isQuestionDonePending(qid)) {
    return
  }

  doneConfirmVisible.value = false
  pendingDoneQuestionId.value = null
  doneConfirmLoading.value = true
  updatePendingDoneQuestionSet((next) => next.add(qid))
  updateDoneQuestionSet((next) => next.add(qid))
  let didSyncQuestionDone = false
  doneTip.value = '正在记录本题...'
  try {
    const res = await api.post('/api/leaderboard/question-done', { questionId: qid })
    if (res.code !== 200) {
      throw new Error(res.msg || '上报失败')
    }
    applyQuestionDoneSuccess(qid)
    didSyncQuestionDone = true
    doneTip.value = '已记录本题，排行榜数据会同步更新。'
    doneConfirmVisible.value = false
    pendingDoneQuestionId.value = null
  } catch (_) {
    doneTip.value = '记录失败，请稍后再试。'
  } finally {
    if (!didSyncQuestionDone) {
      updatePendingDoneQuestionSet((next) => next.delete(qid))
      updateDoneQuestionSet((next) => next.delete(qid))
    }
    doneConfirmLoading.value = false
  }
}

const markQuestionDone = async (questionId) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return

  if (authStore.isGuest) {
    doneTip.value = '游客模式不参与做题统计，登录后可计入排行榜。'
    return
  }
  if (isDoneQuestion(qid)) {
    doneTip.value = '这道题已经记录过做题次数。'
    return
  }

  try {
    const res = await api.post('/api/leaderboard/question-done', { questionId: qid })
    if (res.code !== 200) {
      throw new Error(res.msg || '上报失败')
    }
    applyQuestionDoneSuccess(qid)
    doneTip.value = '已记录本题，排行榜数据会同步更新。'
  } catch (_) {
    doneTip.value = '记录失败，请稍后再试。'
  }
}

const startAiExplanation = async () => {
  if (!activeQuestion.value?.id) return
  aiStarted.value = true
  resetAi()
  await startStream(`/api/ai/question/${activeQuestion.value.id}/explain`, {
    timeoutMs: AI_EXPLAIN_TIMEOUT_MS,
    firstChunkTimeoutMs: AI_EXPLAIN_FIRST_CHUNK_TIMEOUT_MS,
    retryCount: 1,
  })
}

const resetImportForm = () => {
  uploadForm.name = ''
  uploadForm.category = defaultImportCategory()
  uploadForm.difficulty = 2
  uploadFile.value = null
  if (uploadInput.value) {
    uploadInput.value.value = ''
  }
}

const openImportDialog = () => {
  if (authStore.isGuest) {
    alert('游客模式不支持导入自定义题库。')
    return
  }
  uploadDialogVisible.value = true
}

const closeImportDialog = () => {
  uploadDialogVisible.value = false
  uploadingBank.value = false
  resetImportForm()
}

const handleUploadFileChange = (event) => {
  const file = event.target?.files?.[0]
  if (file && !/\.txt$/i.test(file.name || '')) {
    alert('仅支持 txt 文件导入。')
    if (uploadInput.value) {
      uploadInput.value.value = ''
    }
    uploadFile.value = null
    return
  }
  uploadFile.value = file || null
}

const submitImport = async () => {
  if (!uploadFile.value) {
    alert('请先选择题库文件。')
    return
  }
  if (!/\.txt$/i.test(uploadFile.value.name || '')) {
    alert('仅支持 txt 文件导入。')
    return
  }

  uploadingBank.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    if (uploadForm.name.trim()) formData.append('name', uploadForm.name.trim())
    formData.append('category', uploadForm.category)
    formData.append('difficulty', String(uploadForm.difficulty))

    const res = await api.upload('/api/question-banks/import', formData)
    if (res.code !== 200) {
      throw new Error(res.msg || '题库提交失败')
    }

    closeImportDialog()
    resourceCacheStore.invalidateNamespace(QUESTION_META_NAMESPACE)
    resourceCacheStore.invalidateNamespace(QUESTION_RUNTIME_NAMESPACE)
    resourceCacheStore.invalidateNamespace(QUESTION_ANSWER_NAMESPACE)
    await fetchCustomBanks({ force: true })
    if (res.data?.id) {
      currentBankId.value = res.data.id
    }
    await fetchQuestions(1, { force: true })
    alert('题库已提交审核，审核通过后会自动入库。')
  } catch (e) {
    const message = String(e?.message || '')
    if (message.includes('Failed to fetch')) {
      alert('导入失败：当前无法连接后端服务，请检查服务是否在线后重试。')
    } else {
      alert(message || '题库提交失败')
    }
  } finally {
    uploadingBank.value = false
  }
}

const downloadTxtTemplate = () => {
  const template = [
    '题目：请解释数据库索引失效的常见原因。',
    '答案：函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。',
    '',
    '题目：请说明缓存穿透和缓存击穿的区别。',
    '答案：缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。',
  ].join('\n')
  const blob = new Blob([template], { type: 'text/plain;charset=utf-8' })
  const href = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = '题库导入模板.txt'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(href)
}

const openRenameDialog = (bank) => {
  renameBankId.value = bank.id
  renameForm.name = bank.name || ''
  renameDialogVisible.value = true
}

const closeRenameDialog = () => {
  renameDialogVisible.value = false
  renamingBank.value = false
  renameBankId.value = null
  renameForm.name = ''
}

const submitRename = async () => {
  if (!renameBankId.value) return
  if (!renameForm.name.trim()) {
    alert('题库名称不能为空。')
    return
  }

  renamingBank.value = true
  try {
    const res = await api.put(`/api/question-banks/${renameBankId.value}`, {
      name: renameForm.name.trim(),
    })
    if (res.code !== 200) {
      throw new Error(res.msg || '重命名失败')
    }
    closeRenameDialog()
    resourceCacheStore.invalidateNamespace(QUESTION_META_NAMESPACE)
    resourceCacheStore.invalidateNamespace(QUESTION_ANSWER_NAMESPACE)
    await fetchCustomBanks({ force: true })
  } catch (e) {
    alert(e.message || '重命名失败')
  } finally {
    renamingBank.value = false
  }
}

watch(
  [currentCategory, difficultyFilter, currentBankId, () => currentBank.value?.status],
  () => {
    fetchQuestions(1)
  },
)

watch(
  [bankPanelCollapsed, () => customBanks.value.length],
  async () => {
    if (bankPanelCollapsed.value) return
    await nextTick()
    updateBankNavState()
  },
)

const handleWindowResize = () => {
  if (bankPanelCollapsed.value) return
  updateBankNavState()
}

const refreshQuestionBankData = async () => {
  const bootstrapTasks = [
    fetchCategoryOptions().then(() => {
      syncCategoryToSelectedBank()
    }),
    fetchCustomBanks(),
    loadDoneFromServer(),
  ]
  await fetchQuestions(currentPage.value || 1)
  await Promise.allSettled(bootstrapTasks)
}

/** 处理 URL 中的 highlight 参数，定位到指定题目 */
const handleHighlight = async () => {
  const highlightId = route.query.highlight
  if (!highlightId) return
  const targetId = Number(highlightId)
  if (targetId <= 0) return
  pendingHighlightId.value = targetId
  const found = questions.value.find(q => Number(q.id) === targetId)
  if (found) {
    await openQuestion(found)
  } else {
    try {
      const res = await api.get(`/api/questions/${targetId}`)
      if (res.code === 200 && res.data) {
        const q = normalizeQuestionRecord(res.data)
        questions.value.unshift(q)
        await nextTick()
        await openQuestion(q)
      }
    } catch (_) {}
  }
  pendingHighlightId.value = 0
  await router.replace({ query: {} })
}

onMounted(async () => {
  await refreshQuestionBankData()
  await nextTick()
  await handleHighlight()
  updateBankNavState()
  window.addEventListener('resize', handleWindowResize)
})

// 监听 highlight 参数变化，每次路由变化时重新处理高亮
watch(() => route.query.highlight, (newHighlight) => {
  if (newHighlight) {
    nextTick(() => handleHighlight())
  }
})

onUnmounted(() => {
  stopQuestionDoneConfetti()
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<style scoped>
.question-bank-shell {
  background: var(--app-shell-bg);
  min-height: 0;
}

.question-list-scroll,
.question-detail-scroll {
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  touch-action: pan-y;
}

.question-detail-surface {
  background:
    radial-gradient(circle at 72% 14%, color-mix(in srgb, var(--module-glow-a) 44%, transparent), transparent 36%),
    radial-gradient(circle at 22% 78%, color-mix(in srgb, var(--module-glow-c) 40%, transparent), transparent 34%),
    var(--bg-base);
}

.question-detail-stack {
  display: flex;
  flex-direction: column;
  padding-bottom: clamp(22px, 4vh, 44px);
}

.question-answer-card,
.question-ai-card,
.question-empty-card {
  border-color: var(--border-soft);
  background: color-mix(in srgb, var(--bg-elevated) 92%, white 8%);
  box-shadow: var(--shadow-base);
}

.question-answer-card {
  min-height: clamp(300px, 44vh, 480px);
  display: flex;
  flex-direction: column;
}

.answer-body {
  flex: 1;
}

.question-empty-wrap {
  max-width: 560px;
}

.question-empty-icon {
  background: color-mix(in srgb, var(--bg-elevated) 88%, white 12%);
  border-color: var(--border-soft);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 999px;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.question-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.35;
}

.nav-arrow-btn {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--bg-elevated);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  transition: all 0.2s ease;
}

.nav-arrow-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.nav-arrow-btn:not(:disabled):hover {
  background: var(--accent-soft);
  color: var(--primary);
}

.bank-scroll-area {
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-x;
}

.bank-card,
.bank-empty-card {
  min-width: 196px;
  max-width: 196px;
  min-height: 122px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: var(--surface-panel-soft);
  padding: 14px;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.bank-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px -18px rgba(15, 23, 42, 0.35);
}

.bank-card-active {
  border-color: var(--accent-border);
  box-shadow: 0 16px 28px -22px rgba(var(--primary-rgb), 0.45);
  background:
    radial-gradient(circle at top right, var(--module-glow-a), transparent 40%),
    var(--surface-panel);
}

.bank-status {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
}

.bank-status-approved {
  background: var(--success-soft);
  color: color-mix(in srgb, var(--success) 72%, var(--text-primary));
}

.bank-status-pending {
  background: var(--warning-soft);
  color: color-mix(in srgb, var(--warning) 72%, var(--text-primary));
}

.bank-status-rejected {
  background: var(--danger-soft);
  color: color-mix(in srgb, var(--danger) 72%, var(--text-primary));
}

.bank-status-default {
  background: var(--bg-soft);
  color: var(--text-secondary);
}

.question-bank-shell :deep([class~='bg-slate-50']),
.question-bank-shell :deep([class~='bg-slate-100']),
.question-bank-shell :deep([class~='bg-black/[0.03]']),
.question-bank-shell :deep([class~='bg-black/[0.02]']),
.question-bank-shell :deep([class~='bg-white']),
.question-bank-shell :deep([class~='bg-white/84']),
.question-bank-shell :deep([class~='bg-white/82']),
.question-bank-shell :deep([class~='bg-white/80']),
.question-bank-shell :deep([class~='bg-white/70']) {
  background: var(--bg-elevated) !important;
}

.question-bank-shell :deep([class~='text-slate-400']),
.question-bank-shell :deep([class~='text-slate-500']),
.question-bank-shell :deep([class~='text-slate-600']) {
  color: var(--text-secondary) !important;
}

.question-bank-shell :deep([class~='text-slate-700']),
.question-bank-shell :deep([class~='text-slate-800']) {
  color: var(--text-primary) !important;
}

.question-bank-shell :deep([class~='text-amber-700']) {
  color: color-mix(in srgb, var(--warning) 76%, var(--text-primary)) !important;
}

.question-bank-shell :deep([class~='bg-amber-50']) {
  background: var(--warning-soft) !important;
}

.question-bank-shell :deep([class~='border-amber-100']),
.question-bank-shell :deep([class~='border-gray-200']),
.question-bank-shell :deep([class~='border-slate-200']),
.question-bank-shell :deep([class~='border-black/10']),
.question-bank-shell :deep([class~='border-black/8']),
.question-bank-shell :deep([class~='border-black/[0.05]']),
.question-bank-shell :deep([class~='border-black/[0.03]']),
.question-bank-shell :deep([class~='border-black/[0.02]']) {
  border-color: var(--border-soft) !important;
}

.dark .question-answer-card,
.dark .question-ai-card,
.dark .question-empty-card {
  background:
    radial-gradient(circle at 92% 100%, color-mix(in srgb, var(--module-glow-a) 36%, transparent), transparent 38%),
    color-mix(in srgb, var(--bg-elevated) 94%, #0f172a 6%);
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
.dark .submit-note-file-drop,
.dark .submit-note-format-example {
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

.dark .submit-note-card:hover,
.dark .submit-note-close:hover,
.dark .submit-note-file-drop:hover {
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
    background 0.2s var(--submit-ease);
  border-radius: 16px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
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
  background: rgba(255, 255, 255, 0.94);
}

.submit-note-input:focus,
.submit-note-card-input:focus,
.submit-note-textarea:focus {
  border-color: var(--submit-brand-border);
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 12px 28px -24px rgba(231, 154, 176, 0.28),
    0 0 0 4px rgba(231, 154, 176, 0.1);
}

.submit-note-title-input {
  font-size: 16px;
  font-weight: 600;
  min-height: 48px;
  padding: 0 18px;
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

.submit-note-file-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px dashed var(--submit-border-soft);
  border-radius: 18px;
  background: var(--submit-bg-soft);
  padding: 24px;
  cursor: pointer;
  transition:
    border-color 0.2s var(--submit-ease),
    background 0.2s var(--submit-ease);
}

.submit-note-file-drop:hover {
  border-color: var(--submit-brand-border);
  background: rgba(255, 255, 255, 0.74);
}

.submit-note-file-input {
  pointer-events: none;
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.submit-note-file-icon {
  color: var(--submit-text-tertiary);
}

.submit-note-file-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--submit-text-primary);
  text-align: center;
}

.submit-note-file-hint {
  font-size: 12px;
  color: var(--submit-text-tertiary);
}

.submit-note-format-desc {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--submit-text-secondary);
}

.submit-note-format-example {
  margin-top: 10px;
  border-radius: 12px;
  border: 1px solid var(--submit-border-light);
  background: rgba(255, 255, 255, 0.78);
  padding: 14px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--submit-text-secondary);
  overflow-x: auto;
  white-space: pre-wrap;
}

.submit-note-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px 16px;
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

.dark .submit-note-input:hover,
.dark .submit-note-card-input:hover,
.dark .submit-note-textarea:hover,
.dark .submit-note-card:hover,
.dark .submit-note-file-drop:hover,
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

.dark .submit-note-btn-secondary {
  color: var(--submit-text-secondary);
  border-color: var(--submit-border-soft);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.dark .submit-note-btn-secondary:hover:not(:disabled) {
  background: rgba(42, 35, 58, 0.96);
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
  .question-bank-shell > aside,
  .question-bank-shell > section {
    min-height: 0;
  }

  .question-detail-stack {
    padding: 20px 16px 28px;
  }

  .bank-card,
  .bank-empty-card {
    min-width: 168px;
    max-width: 168px;
    min-height: 116px;
    padding: 12px;
  }

  .bank-card:hover {
    transform: none;
    box-shadow: none;
  }

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

  .submit-note-file-drop {
    padding: 18px 14px;
  }

  .submit-note-card-head {
    align-items: flex-start;
  }

  .submit-note-format-example {
    max-height: 180px;
  }
}
</style>
