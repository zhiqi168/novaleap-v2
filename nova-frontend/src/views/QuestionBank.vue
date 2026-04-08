<template>
  <div class="question-bank-shell workspace-shell h-full flex flex-col md:flex-row overflow-hidden sm:mx-4">
    <div class="md:hidden px-3 pt-3 pb-2 border-b border-border-subtle bg-bg-surface/80 backdrop-blur">
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
      class="w-full md:w-[430px] xl:w-[460px] border-b md:border-b-0 md:border-r border-border-subtle bg-bg-surface backdrop-blur-xl flex-col"
      :class="mobileTab === 'detail' ? 'hidden md:flex' : 'flex'"
    >
      <div class="px-4 py-4 border-b border-border-subtle">
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

      <div class="flex-1 overflow-y-auto px-2 py-2 custom-scrollbar">
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
            class="workspace-list-item w-full text-left rounded-2xl border p-3 transition-all"
            :class="activeQuestion?.id === q.id
              ? 'workspace-list-item-active border-ai-from/35 bg-ai-from/8 shadow-[0_14px_28px_-20px_rgba(99,102,241,0.55)]'
              : 'border-black/8 bg-white/80 hover:bg-white hover:border-black/14'"
            @click="openQuestion(q)"
          >
            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-xs px-2 py-1 rounded-md bg-black/[0.04] text-text-tertiary">#{{ q.id }}</span>
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
      class="flex-1 overflow-y-auto bg-bg-base custom-scrollbar"
      :class="mobileTab === 'list' ? 'hidden md:block' : 'block'"
    >
      <div v-if="!activeQuestion" class="h-full grid place-items-center text-text-tertiary">
        <div v-if="currentBank && !isApprovedBank(currentBank)" class="max-w-lg rounded-3xl border border-black/8 bg-white/82 px-8 py-9 text-center shadow-sm">
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

        <div v-else class="text-center px-6">
          <div class="w-24 h-24 rounded-2xl bg-white/80 border border-black/8 shadow-sm mx-auto mb-4 grid place-items-center">
            <svg class="w-11 h-11 opacity-35" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 6h16M4 12h16M4 18h10" />
            </svg>
          </div>
          <p class="text-xl font-semibold text-slate-700">请先从左侧选择题目，或点击 AI 抽题</p>
        </div>
      </div>

      <div v-else class="px-5 md:px-8 xl:px-10 py-7 max-w-[1020px] mx-auto">
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

        <article class="mt-5 rounded-3xl border border-border-subtle bg-bg-elevated backdrop-blur-xl p-6 shadow-sm">
          <div class="text-sm font-semibold text-text-primary mb-3">题目内容</div>
          <div class="prose prose-sm max-w-none text-text-secondary">
            <TypeWriter :text="activeQuestion.content || '暂无题目内容'" :renderMarkdown="true" :isTyping="false" />
          </div>
        </article>

        <article class="mt-4 rounded-3xl border border-border-subtle bg-bg-elevated backdrop-blur-xl p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-3 flex-wrap">
            <div class="text-sm font-semibold text-text-primary">参考答案</div>
            <button
              class="workspace-btn workspace-btn-muted text-xs px-2.5 py-1 rounded-lg border border-black/10 bg-white hover:bg-black/[0.03]"
              @click="loadDbAnswer(activeQuestion.id)"
              :disabled="dbAnswerLoading"
            >
              {{ dbAnswerLoading ? '读取中...' : '刷新答案' }}
            </button>
            <span v-if="dbAnswerSource" class="text-[11px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
              来源：{{ dbAnswerSource }}
            </span>
          </div>
          <div v-if="dbAnswerLoading" class="py-3"><LoadingDots /></div>
          <div v-else class="prose prose-sm max-w-none text-text-secondary">
            <TypeWriter :text="dbAnswer || '当前题目尚未配置参考答案'" :renderMarkdown="true" :isTyping="false" />
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

        <article v-if="aiStarted" class="mt-4 rounded-3xl border border-black/8 bg-white/84 backdrop-blur-xl p-6 shadow-sm">
          <div class="text-sm font-semibold text-text-primary mb-3">AI 回答</div>
          <div class="prose prose-sm max-w-none text-text-secondary">
            <TypeWriter :text="aiContent || ''" :renderMarkdown="true" :isTyping="aiStreaming" />
            <div v-if="aiStreaming && !aiContent" class="py-2"><LoadingDots /></div>
          </div>
          <p v-if="safeAiError" class="mt-2 text-xs text-danger">AI 请求失败：{{ safeAiError }}</p>
        </article>

        <p v-if="doneTip" class="mt-3 text-xs text-emerald-700">{{ doneTip }}</p>
      </div>
    </section>

    <div v-if="uploadDialogVisible" class="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="w-full max-w-lg rounded-3xl border border-border-subtle bg-bg-surface p-6 shadow-2xl">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-lg font-semibold text-text-primary">导入自定义题库</h3>
            <p class="text-xs text-text-tertiary mt-1">仅支持 `.txt`，提交后会先进入后台审核。</p>
          </div>
          <button class="rounded-lg p-1.5 hover:bg-black/[0.04] text-text-tertiary" @click="closeImportDialog">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="mt-5 space-y-4">
          <label class="block">
            <span class="text-xs text-text-tertiary">题库名称</span>
            <input
              v-model.trim="uploadForm.name"
              type="text"
              class="workspace-control mt-1.5 w-full rounded-xl border border-black/10 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25"
              placeholder="不填则默认使用文件名"
            />
          </label>

          <div class="grid grid-cols-2 gap-3">
            <label class="block">
              <span class="text-xs text-text-tertiary">分类</span>
              <select
                v-model="uploadForm.category"
                class="workspace-control mt-1.5 w-full rounded-xl border border-black/10 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25"
              >
                <option v-for="cat in categories.filter((item) => item.id !== 'all')" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </label>
            <label class="block">
              <span class="text-xs text-text-tertiary">默认难度</span>
              <select
                v-model.number="uploadForm.difficulty"
                class="workspace-control mt-1.5 w-full rounded-xl border border-black/10 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ai-from/25"
              >
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select>
            </label>
          </div>

          <label class="block">
            <span class="text-xs text-text-tertiary">题库文件</span>
            <div class="mt-1.5 rounded-2xl border border-dashed border-black/12 bg-black/[0.02] p-4">
              <input
                ref="uploadInput"
                type="file"
                accept=".txt,text/plain"
                class="workspace-control block w-full text-sm text-text-secondary file:mr-3 file:rounded-lg file:border-0 file:bg-ai-from/10 file:px-3 file:py-2 file:text-xs file:font-semibold file:text-ai-from hover:file:bg-ai-from/15"
                @change="handleUploadFileChange"
              />
              <p class="mt-2 text-[11px] text-text-tertiary">
                {{ uploadFile ? `已选择：${uploadFile.name}` : '仅支持 txt，建议控制在 5MB 以内。' }}
              </p>
            </div>
          </label>

          <div class="rounded-2xl border border-black/8 bg-black/[0.02] p-3.5">
            <div class="flex items-center justify-between gap-3">
              <p class="text-xs font-semibold text-text-primary">格式要求（严格）</p>
              <button
                class="workspace-btn workspace-btn-muted text-[11px] px-2 py-1 rounded-lg border border-black/10 bg-white hover:bg-black/[0.03]"
                @click="downloadTxtTemplate"
              >
                下载模板
              </button>
            </div>
            <p class="mt-2 text-[11px] leading-6 text-text-secondary">
              每题两段：第一行是题目，下一行起是答案。题目与题目之间空一行。
            </p>
            <pre class="mt-2 rounded-lg bg-white border border-black/8 p-2.5 text-[11px] leading-6 text-text-secondary overflow-x-auto">题目：什么是索引失效？
答案：对索引列做函数计算、隐式类型转换等都可能导致索引失效。

题目：什么是缓存穿透？
答案：请求的数据不存在，缓存和数据库都没有，导致请求直接打到数据库。</pre>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-2">
          <button class="workspace-btn workspace-btn-muted px-3 py-2 rounded-xl border border-black/10 text-sm hover:bg-black/[0.03]" @click="closeImportDialog">
            取消
          </button>
          <button
            class="workspace-btn workspace-btn-primary px-3 py-2 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-ai-from to-ai-to disabled:opacity-60"
            :disabled="uploadingBank"
            @click="submitImport"
          >
            {{ uploadingBank ? '提交中...' : '提交审核' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="renameDialogVisible" class="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="w-full max-w-md rounded-3xl border border-border-subtle bg-bg-surface p-6 shadow-2xl">
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
      class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4"
      @click.self="closeDoneConfirmDialog"
    >
      <div class="w-full max-w-sm rounded-3xl border border-border-subtle bg-bg-surface p-6 shadow-2xl">
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
import TypeWriter from '@/components/common/TypeWriter.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'
import { useSSE } from '@/composables/useSSE'
import { api } from '@/composables/useRequest'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
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
const aiStarted = ref(false)
const doneQuestionIds = ref(new Set())
const doneTip = ref('')
const doneConfirmVisible = ref(false)
const doneConfirmLoading = ref(false)
const pendingDoneQuestionId = ref(null)

const dbAnswer = ref('')
const dbAnswerSource = ref('')
const dbAnswerLoading = ref(false)
const bankPanelCollapsed = ref(true)

const customBanks = ref([])
const currentBankId = ref(null)

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

const categoryLabel = (category) => {
  const code = normalizeCategoryCode(category)
  if (!code) return '未分类'
  return categories.value.find((c) => c.id === code)?.name || code
}
const difficultyLabel = (level) => ({ 1: '简单', 2: '中等', 3: '困难' }[Number(level)] || '未知')
const isDoneQuestion = (id) => doneQuestionIds.value.has(Number(id))
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
  doneTip.value = ''
  aiStarted.value = false
  resetAi()
}

const patchListQuestionViewCount = (questionId, computeNext) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return
  questions.value = questions.value.map((item) => {
    if (Number(item?.id) !== qid) return item
    const current = Number(item?.viewCount || 0)
    const next = Number(computeNext(current))
    return {
      ...item,
      viewCount: Number.isFinite(next) ? Math.max(0, next) : current,
    }
  })
}

const bumpLocalQuestionViewCount = (questionId) => {
  const qid = Number(questionId)
  if (!Number.isInteger(qid) || qid <= 0) return
  patchListQuestionViewCount(qid, (current) => current + 1)
  if (Number(activeQuestion.value?.id) === qid) {
    activeQuestion.value = {
      ...activeQuestion.value,
      viewCount: Number(activeQuestion.value?.viewCount || 0) + 1,
    }
  }
}

const syncQuestionViewCountFromServer = (questionId, viewCount) => {
  const qid = Number(questionId)
  const count = Number(viewCount)
  if (!Number.isInteger(qid) || qid <= 0 || !Number.isFinite(count)) return
  patchListQuestionViewCount(qid, (current) => Math.max(current, count))
  if (Number(activeQuestion.value?.id) === qid) {
    const current = Number(activeQuestion.value?.viewCount || 0)
    activeQuestion.value = {
      ...activeQuestion.value,
      viewCount: Math.max(current, count),
    }
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

const fetchCategoryOptions = async () => {
  try {
    const res = await api.get('/api/questions/categories')
    if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
      applyCategoryOptions(res.data)
      syncCategorySelectionAfterOptionsLoaded()
      return
    }
  } catch (_) {
    // ignore and fallback
  }

  applyCategoryOptions(fallbackCategoryOptions)
  syncCategorySelectionAfterOptionsLoaded()
}

const fetchCustomBanks = async () => {
  if (authStore.isGuest) {
    customBanks.value = []
    currentBankId.value = null
    return
  }
  try {
    const res = await api.get('/api/question-banks/mine?page=1&size=50')
    if (res.code === 200) {
      customBanks.value = (res.data?.records || []).map((bank) => ({
        ...bank,
        category: normalizeCategoryCode(bank?.category),
      }))
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

const fetchQuestions = async (page = currentPage.value) => {
  if (currentBank.value && !isApprovedBank(currentBank.value)) {
    questions.value = []
    currentPage.value = 1
    totalPages.value = 1
    totalQuestions.value = 0
    activeQuestion.value = null
    return
  }

  loading.value = true
  try {
    const params = new URLSearchParams()
    params.set('page', String(page))
    params.set('size', '10')
    if (currentCategory.value !== 'all') params.set('category', currentCategory.value)
    if (difficultyFilter.value !== 'all') params.set('difficulty', difficultyFilter.value)
    if (searchQuery.value.trim()) params.set('keyword', searchQuery.value.trim())
    if (currentBankId.value) params.set('bankId', String(currentBankId.value))

    const res = await api.get(`/api/questions?${params.toString()}`)
    if (res.code !== 200) {
      questions.value = []
      totalPages.value = 1
      totalQuestions.value = 0
      activeQuestion.value = null
      clearAnswerPanels()
      return
    }

    questions.value = (res.data?.records || []).map((q) => ({
      ...q,
      category: normalizeCategoryCode(q?.category),
      tags: typeof q.tags === 'string' ? q.tags.split(',').map((t) => t.trim()).filter(Boolean) : [],
    }))
    currentPage.value = page
    totalPages.value = Math.max(1, Number(res.data?.pages || 1))
    totalQuestions.value = Math.max(0, Number(res.data?.total || 0))
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

const hydrateQuestionDetail = async (questionId, fallbackQuestion = null) => {
  const qid = Number(questionId || 0)
  if (!Number.isInteger(qid) || qid <= 0) {
    activeQuestion.value = fallbackQuestion || null
    return
  }
  try {
    const res = await api.get(`/api/questions/${qid}?_t=${Date.now()}`, { cache: 'no-store' })
    if (res.code === 200 && res.data) {
      const latestListQuestion = questions.value.find((item) => Number(item?.id || 0) === qid)
      const mergedViewCount = Math.max(
        Number(res.data.viewCount || 0),
        Number(latestListQuestion?.viewCount || 0),
        Number(fallbackQuestion?.viewCount || 0),
        Number(activeQuestion.value?.id || 0) === qid ? Number(activeQuestion.value?.viewCount || 0) : 0,
      )
      activeQuestion.value = {
        ...res.data,
        tags: typeof res.data.tags === 'string' ? res.data.tags.split(',').map((t) => t.trim()).filter(Boolean) : [],
        viewCount: Number.isFinite(mergedViewCount) ? mergedViewCount : Number(res.data.viewCount || 0),
      }
      return
    }
  } catch (_) {
    // fall back to list payload
  }
  const latestListQuestion = questions.value.find((item) => Number(item?.id || 0) === qid)
  activeQuestion.value = latestListQuestion || fallbackQuestion || null
}

const syncActiveQuestionAfterListLoaded = () => {
  if (!questions.value.length) {
    activeQuestion.value = null
    clearAnswerPanels()
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
    return
  }

  const firstQuestion = questions.value[0]
  activeQuestion.value = firstQuestion
  clearAnswerPanels()
  const firstQuestionId = Number(firstQuestion?.id || 0)
  if (firstQuestionId > 0) {
    void hydrateQuestionDetail(firstQuestionId, firstQuestion)
    void loadDbAnswer(firstQuestionId, { preserveExisting: false })
  }
}

const openQuestion = async (question) => {
  const qid = Number(question?.id || 0)
  const isSameQuestion = qid > 0 && Number(activeQuestion.value?.id || 0) === qid
  if (isSameQuestion) {
    // Align with Notes module: clicking the already selected item does not increase view count.
    await hydrateQuestionDetail(qid, question)
    if (!dbAnswer.value && !dbAnswerLoading.value) {
      loadDbAnswer(qid, { preserveExisting: true })
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
  await markQuestionViewed(qid)
  await hydrateQuestionDetail(qid, question)
  if (window.innerWidth < 768) {
    mobileTab.value = 'detail'
  }
  loadDbAnswer(qid, { preserveExisting: false })
}

const loadDbAnswer = async (questionId, options = {}) => {
  const { preserveExisting = true } = options
  const reqId = ++latestAnswerRequestId
  const previousAnswer = dbAnswer.value
  const previousSource = dbAnswerSource.value
  dbAnswerLoading.value = true
  try {
    const res = await api.get(`/api/questions/${questionId}/answer`)
    if (reqId !== latestAnswerRequestId) return
    if (res.code === 200 && res.data) {
      dbAnswer.value = sanitizeOfficialAnswer(res.data.answer)
      dbAnswerSource.value = res.data.source || (currentBank.value ? currentBank.value.name : '官方题库')
      return
    }
    if (preserveExisting) {
      dbAnswer.value = previousAnswer
      dbAnswerSource.value = previousSource
      return
    }
    dbAnswer.value = ''
    dbAnswerSource.value = ''
  } catch (_) {
    if (reqId !== latestAnswerRequestId) return
    if (preserveExisting) {
      dbAnswer.value = previousAnswer
      dbAnswerSource.value = previousSource
      return
    }
    dbAnswer.value = ''
    dbAnswerSource.value = ''
  } finally {
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
    return
  }
  try {
    const res = await api.get('/api/leaderboard/question-done')
    if (res.code === 200 && Array.isArray(res.data)) {
      doneQuestionIds.value = new Set(
        res.data.map((v) => Number(v)).filter((v) => Number.isInteger(v) && v > 0),
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

const confirmMarkQuestionDone = async () => {
  const qid = Number(pendingDoneQuestionId.value)
  if (!Number.isInteger(qid) || qid <= 0) {
    closeDoneConfirmDialog()
    return
  }

  doneConfirmLoading.value = true
  try {
    const res = await api.post('/api/leaderboard/question-done', { questionId: qid })
    if (res.code !== 200) {
      throw new Error(res.msg || '上报失败')
    }
    doneQuestionIds.value.add(qid)
    doneTip.value = '已记录本题，排行榜数据会同步更新。'
    doneConfirmVisible.value = false
    pendingDoneQuestionId.value = null
  } catch (_) {
    doneTip.value = '记录失败，请稍后再试。'
  } finally {
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
    doneQuestionIds.value.add(qid)
    doneTip.value = '已记录本题，排行榜数据会同步更新。'
  } catch (_) {
    doneTip.value = '记录失败，请稍后再试。'
  }
}

const startAiExplanation = async () => {
  if (!activeQuestion.value?.id) return
  aiStarted.value = true
  resetAi()
  await startStream(`/api/ai/question/${activeQuestion.value.id}/explain`)
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
    await fetchCustomBanks()
    if (res.data?.id) {
      currentBankId.value = res.data.id
    }
    await fetchQuestions(1)
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
    await fetchCustomBanks()
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

onMounted(async () => {
  await fetchCategoryOptions()
  await fetchCustomBanks()
  syncCategoryToSelectedBank()
  await fetchQuestions(1)
  await loadDoneFromServer()
  await nextTick()
  updateBankNavState()
  window.addEventListener('resize', handleWindowResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<style scoped>
.question-bank-shell {
  background: var(--app-shell-bg);
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
.question-bank-shell :deep([class~='bg-black/[0.03]']) {
  background: var(--bg-elevated) !important;
}

.question-bank-shell :deep([class~='text-slate-400']),
.question-bank-shell :deep([class~='text-slate-500']),
.question-bank-shell :deep([class~='text-slate-600']),
.question-bank-shell :deep([class~='text-slate-700']),
.question-bank-shell :deep([class~='text-slate-800']) {
  color: var(--text-secondary) !important;
}

.question-bank-shell :deep([class~='text-amber-700']) {
  color: color-mix(in srgb, var(--warning) 76%, var(--text-primary)) !important;
}

.question-bank-shell :deep([class~='bg-amber-50']) {
  background: var(--warning-soft) !important;
}

.question-bank-shell :deep([class~='border-amber-100']),
.question-bank-shell :deep([class~='border-gray-200']),
.question-bank-shell :deep([class~='border-slate-200']) {
  border-color: var(--border-soft) !important;
}

@media (max-width: 767px) {
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
}
</style>
