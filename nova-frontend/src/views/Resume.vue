<template>
  <div
    class="workspace-page workspace-scroll h-full min-h-0"
    @touchstart.passive="handleTouchStart"
    @touchmove.passive="handleTouchMove"
    @touchend="handleTouchEnd"
    @touchcancel="resetTouchState"
  >
    <div class="workspace-stack h-full min-h-0 flex flex-col">
      <header class="workspace-titlebar">
        <div class="workspace-titlecopy">
          <h1 class="workspace-title">简历工坊</h1>
          <p class="workspace-subtitle">
            上传 PDF 简历并设定目标岗位，系统会基于 STAR 结构生成可落地的优化版本。
          </p>
        </div>
      </header>

      <div class="workspace-shell resume-page mt-5 flex-1 min-h-0 flex flex-col overflow-hidden xl:flex-row">
        <div class="resume-mobile-switch xl:hidden shrink-0 px-3 pt-3 pb-2 border-b border-border-subtle bg-bg-surface/80 backdrop-blur">
          <div class="grid grid-cols-2 gap-2 rounded-xl bg-black/[0.03] p-1">
            <button
              class="rounded-lg px-3 py-2 text-sm font-semibold transition-colors"
              :class="resumeMobileTab === 'upload' ? 'bg-white text-text-primary shadow-sm' : 'text-text-tertiary'"
              @click="setResumeMobileTab('upload')"
            >
              上传设置
            </button>
            <button
              class="rounded-lg px-3 py-2 text-sm font-semibold transition-colors disabled:opacity-50"
              :class="resumeMobileTab === 'result' ? 'bg-white text-text-primary shadow-sm' : 'text-text-tertiary'"
              :disabled="!canOpenResumeResult"
              @click="setResumeMobileTab('result')"
            >
              分析结果
            </button>
          </div>
        </div>

        <section
          class="resume-panel resume-upload-panel resume-upload-scroll w-full min-h-0 flex flex-col border-b border-border-subtle p-4 sm:p-6 lg:p-8 xl:w-1/2 xl:border-b-0 xl:border-r"
          :class="resumeMobileTab === 'result' ? 'hidden xl:flex' : 'flex'"
        >
          <div class="mb-5 sm:mb-6 shrink-0">
            <h2 class="text-lg font-semibold text-text-primary mb-2">上传与岗位设定</h2>
            <p class="text-sm text-text-secondary">
              左侧负责上传简历与岗位设置，右侧用于查看分析结果。手机端上传后会自动切到分析页。
            </p>
          </div>

          <div class="resume-upload-scroll flex-1 min-h-0 overflow-y-auto custom-scrollbar pr-0 xl:pr-1">
            <div class="resume-upload-card card border border-border-subtle overflow-hidden">
              <div class="px-4 sm:px-5 py-3 border-b border-border-subtle bg-bg-surface">
                <span class="text-xs font-semibold text-text-secondary">简历上传（支持 PDF）</span>
              </div>

              <div
                class="p-4 sm:p-5"
                @dragover.prevent="isDragging = true"
                @dragleave.prevent="isDragging = false"
                @drop.prevent="handleDrop"
              >
                <div
                  class="resume-dropzone min-h-[240px] rounded-2xl border-2 border-dashed px-4 py-6 sm:px-6 sm:py-8 text-center transition-colors flex flex-col items-center justify-center"
                  :class="isDragging ? 'border-ai-from bg-ai-from/5' : 'border-border-subtle bg-bg-surface/72 hover:border-ai-from/50'"
                  @click="handleDropzoneClick"
                >
                  <template v-if="!selectedFile">
                    <svg class="w-12 h-12 text-text-tertiary mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                    <p class="text-sm text-text-secondary mb-1 sm:hidden">点击选择 PDF 文件</p>
                    <p class="text-sm text-text-secondary mb-1 hidden sm:block">拖拽 PDF 文件至此</p>
                    <p class="text-xs text-text-tertiary mb-4">也可以从本地文件中选择</p>
                    <button
                      type="button"
                      class="btn-ghost py-2 px-4 text-xs sm:text-sm"
                      data-picker-trigger
                      @click.stop="openFilePicker"
                    >
                      选择 PDF
                    </button>
                    <p class="mt-4 text-[11px] leading-5 text-text-tertiary max-w-[260px]">
                      手机端无需拖拽，点击整块区域或按钮都能直接唤起系统文件选择。
                    </p>
                  </template>

                  <template v-else>
                    <div class="w-16 h-16 rounded-2xl bg-ai-from/10 text-ai-from mb-4 flex items-center justify-center">
                      <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <p class="max-w-full px-2 text-sm font-medium text-text-primary break-all line-clamp-2">
                      {{ selectedFile.name }}
                    </p>
                    <p class="text-xs text-text-tertiary mt-1 mb-4">
                      {{ formatFileSize(selectedFile.size) }}
                    </p>
                    <div class="flex flex-wrap justify-center gap-2">
                      <button
                        type="button"
                        class="btn-ghost py-1.5 px-3 text-xs"
                        data-picker-trigger
                        @click.stop="openFilePicker"
                      >
                        重新选择
                      </button>
                      <button
                        type="button"
                        class="text-xs text-red-500 hover:bg-red-50 px-3 py-1.5 rounded transition-colors"
                        @click.stop="clearSelectedFile"
                      >
                        移除文件
                      </button>
                    </div>
                  </template>

                  <input
                    ref="fileInput"
                    type="file"
                    accept=".pdf,application/pdf"
                    class="resume-file-input"
                    @change="handleFileSelect"
                  />
                </div>
              </div>
            </div>

            <div class="resume-role-card mt-4 rounded-2xl border border-border-subtle bg-bg-surface p-4">
              <div class="flex items-center justify-between gap-3 mb-2">
                <label class="block text-xs font-semibold text-text-secondary">目标岗位</label>
                <span class="text-[11px] text-text-tertiary">支持自定义输入</span>
              </div>
              <div class="relative">
                <input
                  ref="roleInputRef"
                  v-model="targetRole"
                  type="text"
                  placeholder="请选择或输入目标岗位"
                  class="workspace-control w-full pr-12 py-2.5 text-sm bg-bg-surface rounded-xl border border-border-subtle outline-none focus:border-ai-from/70 transition-colors"
                  @focus="isRoleDropdownOpen = true"
                  @input="isRoleDropdownOpen = true"
                  @blur="handleRoleInputBlur"
                />
                <button
                  type="button"
                  class="absolute right-1 top-1/2 -translate-y-1/2 h-9 w-9 rounded-lg text-text-tertiary hover:bg-sky-50 transition-colors flex items-center justify-center"
                  @mousedown.prevent
                  @click="toggleRoleDropdown"
                >
                  <svg class="w-4 h-4 transition-transform" :class="isRoleDropdownOpen ? 'rotate-180' : ''" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                <div
                  v-if="isRoleDropdownOpen"
                  class="absolute z-20 left-0 right-0 top-full mt-2 max-h-44 overflow-y-auto rounded-xl border border-border-subtle bg-bg-elevated shadow-lg custom-scrollbar"
                >
                  <button
                    v-for="role in filteredRoleOptions"
                    :key="role"
                    type="button"
                    class="w-full px-3 py-2 text-left text-sm hover:bg-sky-50 transition-colors"
                    @mousedown.prevent
                    @click="selectRole(role)"
                  >
                    {{ role }}
                  </button>
                  <div v-if="!filteredRoleOptions.length" class="px-3 py-2 text-xs text-text-secondary">
                    没有匹配岗位，继续输入即可自定义。
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="hidden xl:flex shrink-0 mt-5 sm:mt-6 pt-3 sm:pt-4 justify-center">
            <button
              type="button"
              class="workspace-btn workspace-btn-primary btn-ai resume-cta w-full sm:w-2/3 flex justify-center items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!selectedFile || isStreaming || isExtracting"
              @click="optimize"
            >
              <template v-if="isExtracting">
                <LoadingDots class="bg-primary/20 px-3" />
                提取简历文本中...
              </template>
              <template v-else-if="isStreaming">
                <LoadingDots class="bg-primary/20 px-3" />
                AI 分析中...
              </template>
              <template v-else>
                <span class="text-lg">✨</span>
                <span class="tracking-widest">启动 AI STAR 引擎</span>
                <span class="text-lg">✨</span>
              </template>
            </button>
          </div>
        </section>

        <section
          class="resume-panel resume-result-panel resume-result-scroll w-full min-h-0 flex flex-col p-4 sm:p-6 lg:p-8 xl:w-1/2"
          :class="resumeMobileTab === 'upload' ? 'hidden xl:flex' : 'flex'"
        >
          <div class="flex items-center justify-between gap-3 mb-5 sm:mb-6 shrink-0">
            <div>
              <h2 class="text-xl font-display font-bold text-text-primary">分析结果</h2>
              <p class="mt-1 text-xs text-text-secondary xl:hidden">
                上传成功后会自动切到这里，左滑或点按钮可返回设置。
              </p>
            </div>
            <button
              type="button"
              class="workspace-btn workspace-btn-muted xl:hidden shrink-0 px-3 py-1.5 rounded-lg text-xs border border-black/10 bg-white text-text-secondary"
              @click="setResumeMobileTab('upload')"
            >
              返回上传
            </button>
          </div>

          <div class="xl:hidden shrink-0 mb-4">
            <button
              type="button"
              class="workspace-btn workspace-btn-primary btn-ai resume-cta w-full flex justify-center items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!selectedFile || isStreaming || isExtracting"
              @click="optimize"
            >
              <template v-if="isExtracting">
                <LoadingDots class="bg-primary/20 px-3" />
                提取简历文本中...
              </template>
              <template v-else-if="isStreaming">
                <LoadingDots class="bg-primary/20 px-3" />
                AI 分析中...
              </template>
              <template v-else>
                <span class="text-lg">✨</span>
                <span class="tracking-widest">开始分析</span>
                <span class="text-lg">✨</span>
              </template>
            </button>
          </div>

          <div v-if="selectedFile" class="xl:hidden shrink-0 mb-4 rounded-2xl border border-border-subtle bg-bg-elevated/90 px-4 py-3 shadow-card">
            <div class="text-[11px] font-semibold text-text-secondary">当前简历</div>
            <div class="mt-1 text-sm font-medium text-text-primary break-all">{{ selectedFile.name }}</div>
            <div class="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-text-secondary">
              <span class="px-2.5 py-1 rounded-full bg-black/[0.03]">{{ formatFileSize(selectedFile.size) }}</span>
              <span class="px-2.5 py-1 rounded-full bg-black/[0.03]">{{ targetRole || '未设置目标岗位' }}</span>
            </div>
          </div>

          <div id="print-area" class="resume-result-card flex-1 min-h-[320px] card overflow-y-auto w-full max-w-2xl mx-auto custom-scrollbar relative pdf-content">
            <div v-if="!content && !isStreaming" class="absolute inset-0 flex flex-col items-center justify-center text-text-tertiary px-6 text-center">
              <svg class="w-16 h-16 mb-4 opacity-40 text-ai-from" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
              </svg>
              <p class="text-sm">
                {{ selectedFile ? '简历已就位，点上方按钮开始分析。' : '先在左侧上传简历，再来这里查看优化结果。' }}
              </p>
            </div>

            <div v-if="content || isStreaming" class="p-4 sm:p-8 h-full flex flex-col gap-6 sm:gap-8 overflow-y-auto">
              <div v-if="analysisSection" class="prose prose-sm dark:prose-invert max-w-none print:hidden">
                <div class="px-4 py-3 bg-ai-from/5 rounded-xl border border-ai-from/10 border-dashed">
                  <h3 class="text-ai-from mt-0! mb-2! text-sm font-bold flex items-center gap-2">
                    <span>💡</span>
                    <span>优化建议与现状分析</span>
                  </h3>
                  <div class="text-xs leading-relaxed opacity-80">
                    <TypeWriter :text="analysisSection" :renderMarkdown="true" :isTyping="isStreaming && !resumeSection" />
                  </div>
                </div>
              </div>

              <div v-if="resumeSection" class="flex items-center gap-3 print:hidden">
                <div class="h-px flex-1 bg-border-subtle"></div>
                <span class="text-[10px] font-bold text-text-tertiary uppercase tracking-widest bg-bg-elevated px-2">
                  ✨ 优化后的简历正文 ✨
                </span>
                <div class="h-px flex-1 bg-border-subtle"></div>
              </div>

              <div
                id="resume-document"
                class="prose prose-sm md:prose-base prose-teal max-w-none prose-h3:text-ai-to prose-h3:mt-8 prose-h3:mb-4 prose-p:leading-relaxed bg-white p-3 sm:p-4 rounded shadow-sm print:shadow-none print:p-0"
              >
                <TypeWriter
                  :text="resumeSection || (isStreaming ? '正在为您重构简历，请稍候...' : content)"
                  :renderMarkdown="true"
                  :isTyping="isStreaming"
                />
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import TypeWriter from '@/components/common/TypeWriter.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'
import { useSSE } from '@/composables/useSSE'

let pdfjsModulePromise = null

const loadPdfjs = async () => {
  if (!pdfjsModulePromise) {
    pdfjsModulePromise = import('pdfjs-dist/legacy/build/pdf.mjs').then((module) => {
      module.GlobalWorkerOptions.workerSrc = 'https://cdn.jsdelivr.net/npm/pdfjs-dist@5.5.207/legacy/build/pdf.worker.min.mjs'
      return module
    })
  }
  return pdfjsModulePromise
}

const separator = '---RESUME_CONTENT_START---'
const mobileBreakpoint = 1024

const roleOptions = [
  'Java 后端开发工程师',
  '前端开发工程师',
  '客户端开发工程师',
  '算法/AI 工程师',
  '测试工程师',
  '产品经理',
  '数据分析师',
]

const targetRole = ref(roleOptions[0])
const roleInputRef = ref(null)
const isRoleDropdownOpen = ref(false)
const selectedFile = ref(null)
const isDragging = ref(false)
const isExtracting = ref(false)
const fileInput = ref(null)
const resumeMobileTab = ref('upload')

const touchStartX = ref(0)
const touchStartY = ref(0)
const touchDeltaX = ref(0)
const touchDeltaY = ref(0)

const { content, isStreaming, startStream, reset } = useSSE()

const analysisSection = computed(() => {
  if (!content.value) return ''
  if (!content.value.includes(separator)) return content.value
  return content.value.split(separator)[0].trim()
})

const resumeSection = computed(() => {
  if (!content.value || !content.value.includes(separator)) return ''
  return content.value.split(separator)[1].trim()
})

const filteredRoleOptions = computed(() => {
  const keyword = targetRole.value.trim()
  if (!keyword) return roleOptions
  return roleOptions.filter((role) => role.includes(keyword))
})

const canOpenResumeResult = computed(() => (
  !!selectedFile.value || !!content.value || isStreaming.value || isExtracting.value
))

const isResumeMobileViewport = () => (
  typeof window !== 'undefined' && window.innerWidth < mobileBreakpoint
)

const setResumeMobileTab = (tab) => {
  if (tab === 'result' && !canOpenResumeResult.value) return
  resumeMobileTab.value = tab
}

const resetTouchState = () => {
  touchStartX.value = 0
  touchStartY.value = 0
  touchDeltaX.value = 0
  touchDeltaY.value = 0
}

const handleTouchStart = (event) => {
  if (!isResumeMobileViewport()) return
  const touch = event.touches?.[0]
  if (!touch) return
  touchStartX.value = touch.clientX
  touchStartY.value = touch.clientY
  touchDeltaX.value = 0
  touchDeltaY.value = 0
}

const handleTouchMove = (event) => {
  if (!isResumeMobileViewport() || !touchStartX.value) return
  const touch = event.touches?.[0]
  if (!touch) return
  touchDeltaX.value = touch.clientX - touchStartX.value
  touchDeltaY.value = touch.clientY - touchStartY.value
}

const handleTouchEnd = () => {
  if (!isResumeMobileViewport()) {
    resetTouchState()
    return
  }

  const absDeltaX = Math.abs(touchDeltaX.value)
  const absDeltaY = Math.abs(touchDeltaY.value)

  if (absDeltaX < 72 || absDeltaX < absDeltaY * 1.2) {
    resetTouchState()
    return
  }

  if (touchDeltaX.value < 0 && resumeMobileTab.value === 'upload' && canOpenResumeResult.value) {
    resumeMobileTab.value = 'result'
  } else if (touchDeltaX.value > 0 && resumeMobileTab.value === 'result') {
    resumeMobileTab.value = 'upload'
  }

  resetTouchState()
}

const isPdfFile = (file) => {
  if (!file) return false
  const type = String(file.type || '').toLowerCase()
  const name = String(file.name || '').toLowerCase()
  return type === 'application/pdf' || name.endsWith('.pdf')
}

const formatFileSize = (size) => `${(size / 1024 / 1024).toFixed(2)} MB`

const resetFileInput = () => {
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const openFilePicker = () => {
  const input = fileInput.value
  if (!input) return
  if (typeof input.showPicker === 'function') {
    input.showPicker()
    return
  }
  input.click()
}

const handleDropzoneClick = (event) => {
  const target = event.target instanceof Element ? event.target : null
  if (target?.closest('[data-picker-trigger]')) return
  openFilePicker()
}

const clearSelectedFile = () => {
  selectedFile.value = null
  reset()
  resetFileInput()
  if (isResumeMobileViewport()) {
    resumeMobileTab.value = 'upload'
  }
}

const handleRoleInputBlur = () => {
  window.setTimeout(() => {
    isRoleDropdownOpen.value = false
  }, 120)
}

const toggleRoleDropdown = () => {
  isRoleDropdownOpen.value = !isRoleDropdownOpen.value
  if (isRoleDropdownOpen.value) {
    roleInputRef.value?.focus()
  }
}

const selectRole = (role) => {
  targetRole.value = role
  isRoleDropdownOpen.value = false
  roleInputRef.value?.focus()
}

const assignSelectedFile = (file) => {
  if (!isPdfFile(file)) {
    alert('请上传 PDF 文件')
    return
  }

  selectedFile.value = file
  reset()

  if (isResumeMobileViewport()) {
    resumeMobileTab.value = 'result'
  }
}

const handleDrop = (event) => {
  isDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (!file) return
  assignSelectedFile(file)
}

const handleFileSelect = (event) => {
  const file = event.target?.files?.[0]
  if (!file) return
  assignSelectedFile(file)
  resetFileInput()
}

const extractTextFromPDF = async (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()

  reader.onload = async () => {
    try {
      const pdfjsLib = await loadPdfjs()
      const typedArray = new Uint8Array(reader.result)
      const pdf = await pdfjsLib.getDocument(typedArray).promise
      let fullText = ''
      const maxPages = Math.min(pdf.numPages, 5)

      for (let i = 1; i <= maxPages; i += 1) {
        const page = await pdf.getPage(i)
        const textContent = await page.getTextContent()
        const pageText = textContent.items.map((item) => item.str).join(' ')
        fullText += `${pageText}\n`
      }

      resolve(fullText)
    } catch (error) {
      reject(error)
    }
  }

  reader.onerror = (error) => reject(error)
  reader.readAsArrayBuffer(file)
})

const optimize = async () => {
  if (!selectedFile.value) return

  const normalizedTargetRole = targetRole.value.trim()
  if (!normalizedTargetRole) {
    alert('请输入目标岗位')
    return
  }

  if (isResumeMobileViewport()) {
    resumeMobileTab.value = 'result'
  }

  reset()
  isExtracting.value = true

  let resumeText = ''
  try {
    resumeText = await extractTextFromPDF(selectedFile.value)
  } catch (error) {
    alert(`PDF 文本提取失败: ${error.message}`)
    isExtracting.value = false
    return
  }

  isExtracting.value = false

  if (!resumeText.trim()) {
    alert('无法从该 PDF 中提取到纯文本内容，请检查文件。')
    return
  }

  await startStream('/api/ai/resume/analyze', {
    method: 'POST',
    body: {
      resumeText: resumeText.substring(0, 15000),
      targetRole: normalizedTargetRole,
    },
  })
}

watch(
  () => selectedFile.value,
  (nextFile) => {
    if (!nextFile && isResumeMobileViewport() && !content.value && !isStreaming.value) {
      resumeMobileTab.value = 'upload'
    }
  },
)
</script>

<style scoped>
.resume-page {
  background: var(--app-shell-bg);
  min-height: 0;
}

.resume-panel {
  position: relative;
  min-height: 0;
}

.resume-upload-panel,
.resume-result-panel {
  background: var(--surface-panel-soft);
}

.dark .resume-upload-panel,
.dark .resume-result-panel {
  background: var(--surface-panel);
}

.resume-mobile-switch,
.resume-upload-scroll,
.resume-result-scroll,
.resume-dropzone {
  -webkit-overflow-scrolling: touch;
}

.resume-upload-scroll,
.resume-result-scroll {
  overscroll-behavior: contain;
  touch-action: pan-y;
}

.resume-upload-card,
.resume-role-card {
  box-shadow: var(--shadow-card);
}

.resume-upload-card {
  border-color: var(--border-subtle);
  background: var(--bg-elevated);
}

.resume-role-card {
  background: color-mix(in srgb, var(--bg-elevated) 92%, white 8%);
}

.resume-dropzone {
  cursor: pointer;
}

.resume-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.resume-result-card {
  border-color: var(--border-subtle);
  box-shadow: var(--shadow-card);
  background: var(--bg-elevated);
}

.resume-cta {
  background-image: linear-gradient(135deg, var(--ai-from), var(--ai-to));
  box-shadow: 0 14px 30px rgba(var(--primary-rgb), 0.28);
}

.resume-cta:hover {
  box-shadow: 0 18px 34px rgba(var(--primary-rgb), 0.32);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 6px;
}

.resume-page :deep([class~='hover:bg-sky-50']) {
  background: var(--accent-soft) !important;
}

.resume-page :deep([class~='bg-white']) {
  background: var(--bg-elevated) !important;
}

.resume-page :deep([class~='bg-black/[0.03]']) {
  background: color-mix(in srgb, var(--bg-elevated) 88%, var(--text-primary) 3%) !important;
}

.resume-page :deep([class~='text-red-500']) {
  color: var(--danger) !important;
}

.resume-page :deep([class~='hover:bg-red-50']) {
  background: var(--danger-soft) !important;
}

.resume-page :deep([class~='border-black/10']) {
  border-color: var(--border-soft) !important;
}

@media (max-width: 1279px) {
  .resume-page {
    overflow: visible;
  }
}

@media (max-width: 1023px) {
  .resume-upload-panel,
  .resume-result-panel {
    padding-bottom: calc(1rem + env(safe-area-inset-bottom));
  }
}

@media (max-width: 639px) {
  .resume-upload-card .resume-dropzone {
    min-height: 220px;
  }

  .resume-result-card {
    min-height: 360px;
  }

  .resume-cta {
    letter-spacing: 0.04em;
  }
}
</style>
