<template>
  <div class="workspace-page workspace-scroll h-full">
    <div class="workspace-stack h-full flex flex-col">
      <header class="workspace-titlebar">
        <div class="workspace-titlecopy">
          <h1 class="workspace-title">简历工坊</h1>
          <p class="workspace-subtitle">
            上传 PDF 简历并设定目标岗位，系统会基于 STAR 结构生成可落地的优化版本。
          </p>
        </div>
      </header>

      <div class="workspace-shell resume-page mt-5 flex-1 min-h-0 flex flex-col md:flex-row relative overflow-hidden">
    
    <!-- 左侧：编写区域 -->
    <div class="resume-panel resume-upload-panel w-full md:w-1/2 h-1/2 md:h-full flex flex-col border-b md:border-b-0 md:border-r border-border-subtle p-6 lg:p-8">
      <div class="mb-6">
        <h2 class="text-lg font-semibold text-text-primary mb-2">上传与岗位设定</h2>
        <p class="text-sm text-text-secondary pr-4">
          左侧完成简历上传与目标岗位设置，右侧将实时呈现优化结果与最终简历正文。
        </p>
      </div>

      <div class="resume-upload-card flex-1 flex flex-col card overflow-hidden border border-border-subtle ring-0 ring-offset-0 transition-all group">
        <div class="px-5 py-3 border-b border-border-subtle flex justify-between items-center bg-bg-surface shrink-0">
          <span class="text-xs font-semibold text-text-secondary">简历上传 (支持 PDF)</span>
        </div>
        
        <!-- Dropzone -->
        <div 
          class="flex-1 w-full p-5 bg-transparent flex flex-col justify-center items-center relative"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="handleDrop"
        >
           <div class="absolute inset-4 border-2 border-dashed rounded-xl transition-colors flex flex-col items-center justify-center p-6 text-center"
                :class="isDragging ? 'border-ai-from bg-ai-from/5' : 'border-border-subtle bg-bg-surface/72 hover:border-ai-from/50'">
             
             <template v-if="!selectedFile">
               <svg class="w-12 h-12 text-text-tertiary mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
               </svg>
               <p class="text-sm text-text-secondary mb-1">拖拽 PDF 文件至此</p>
               <p class="text-xs text-text-tertiary mb-4">或者</p>
               <button @click="$refs.fileInput.click()" class="btn-ghost py-1.5 px-4 text-xs">浏览文件</button>
             </template>
             <template v-else>
               <div class="w-16 h-16 bg-ai-from/10 rounded-lg flex items-center justify-center mb-4 text-ai-from">
                 <svg class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                   <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                 </svg>
               </div>
               <p class="text-sm font-medium text-text-primary line-clamp-1 break-all px-4">{{ selectedFile.name }}</p>
               <p class="text-xs text-text-tertiary mt-1 mb-4">{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</p>
               <div class="flex gap-2">
                 <button @click="$refs.fileInput.click()" class="btn-ghost py-1 px-3 text-xs">重新选择</button>
                 <button @click="selectedFile = null" class="text-xs text-red-500 hover:bg-red-50 px-3 py-1 rounded transition-colors">移除</button>
               </div>
             </template>
             <input type="file" ref="fileInput" accept=".pdf" class="hidden" @change="handleFileSelect" />
           </div>
        </div>
        
        <!-- 目标职位输入 -->
        <div class="resume-role-bar p-3 shrink-0 border-t border-black/5 z-10">
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-text-secondary">目标岗位：</span>
            <input
              ref="roleInputRef"
              v-model="targetRole"
              type="text"
              placeholder="请选择或输入目标岗位（支持自定义）"
              class="workspace-control w-full pl-[72px] pr-12 py-2 bg-bg-surface rounded border border-border-subtle text-sm outline-none focus:border-ai-from/70 transition-colors"
              @focus="isRoleDropdownOpen = true"
              @input="isRoleDropdownOpen = true"
              @blur="handleRoleInputBlur"
            />
            <button
              type="button"
              class="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-8 rounded-md text-text-tertiary hover:bg-sky-50 transition-colors flex items-center justify-center"
              @mousedown.prevent
              @click="toggleRoleDropdown"
            >
              <svg
                class="w-4 h-4 transition-transform"
                :class="isRoleDropdownOpen ? 'rotate-180' : ''"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            <div
              v-if="isRoleDropdownOpen"
              class="absolute z-20 left-0 right-0 bottom-full mb-1 max-h-44 overflow-y-auto rounded-lg border border-border-subtle bg-bg-elevated shadow-lg"
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
                无匹配岗位，继续输入即可自定义
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部触发按钮 -->
      <div class="shrink-0 mt-6 pt-4 flex justify-center">
        <button 
          @click="optimize"
          :disabled="!selectedFile || isStreaming || isExtracting"
          class="workspace-btn workspace-btn-primary btn-ai resume-cta w-full sm:w-2/3 flex justify-center items-center gap-2 group disabled:opacity-50 disabled:cursor-not-allowed"
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
    </div>

    <!-- 右侧：优化结果区域 -->
    <div class="resume-panel resume-result-panel w-full md:w-1/2 h-1/2 md:h-full p-6 lg:p-8 flex flex-col relative">
      <div class="flex justify-between items-center mb-6 shrink-0">
        <h2 class="text-xl font-display font-bold text-text-primary">优化结果</h2>
      </div>

      <div class="resume-result-card flex-1 card overflow-y-auto w-full max-w-2xl mx-auto custom-scrollbar relative pdf-content" id="print-area">
        
        <!-- 空状态 -->
        <div v-if="!content && !isStreaming" class="absolute inset-0 flex flex-col items-center justify-center text-text-tertiary">
          <svg class="w-16 h-16 mb-4 opacity-40 text-ai-from" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
          </svg>
          <p class="text-sm border-b border-transparent">左侧上传简历，右侧见证奇迹</p>
        </div>

        <!-- 渲染结果：分块显示 -->
        <div v-if="content || isStreaming" class="p-8 h-full flex flex-col gap-8 overflow-y-auto">
          <!-- 建议部分 (仅屏幕显示) -->
          <div v-if="analysisSection" class="prose prose-sm dark:prose-invert max-w-none print:hidden">
            <div class="px-4 py-3 bg-ai-from/5 rounded-xl border border-ai-from/10 border-dashed">
              <h3 class="text-ai-from mt-0! mb-2! text-sm font-bold flex items-center gap-2">
                <span>💡</span> 优化建议与现状分析
              </h3>
              <div class="text-xs leading-relaxed opacity-80">
                <TypeWriter :text="analysisSection" :renderMarkdown="true" :isTyping="isStreaming && !resumeSection" />
              </div>
            </div>
          </div>
          
          <!-- 分界线 -->
          <div v-if="resumeSection" class="flex items-center gap-3 print:hidden">
            <div class="h-px flex-1 bg-border-subtle"></div>
            <span class="text-[10px] font-bold text-text-tertiary uppercase tracking-widest bg-bg-elevated px-2">✨ 优化后的简历正文 ✨</span>
            <div class="h-px flex-1 bg-border-subtle"></div>
          </div>

          <!-- 简历正文 (打印目标) -->
          <div id="resume-document" class="prose prose-sm md:prose-base prose-teal max-w-none prose-h3:text-ai-to prose-h3:mt-8 prose-h3:mb-4 prose-p:leading-relaxed bg-white p-2 sm:p-4 rounded shadow-sm print:shadow-none print:p-0">
             <TypeWriter 
               :text="resumeSection || (isStreaming ? '正在为您重构简历，请稍候...' : content)" 
               :renderMarkdown="true" 
               :isTyping="isStreaming" 
             />
          </div>
        </div>
      </div>
    </div>
  </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf.mjs'
import TypeWriter from '@/components/common/TypeWriter.vue'
import LoadingDots from '@/components/common/LoadingDots.vue'
import { useSSE } from '@/composables/useSSE'

// 初始化 pdfjs worker
pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdn.jsdelivr.net/npm/pdfjs-dist@5.5.207/legacy/build/pdf.worker.min.mjs`

const roleOptions = [
  'Java 后端开发工程师',
  '前端开发工程师',
  '客户端开发工程师',
  '算法/AI 工程师',
  '测试工程师',
  '产品经理',
  '数据分析师'
]
const targetRole = ref(roleOptions[0])
const roleInputRef = ref(null)
const isRoleDropdownOpen = ref(false)
const selectedFile = ref(null)
const isDragging = ref(false)
const isExtracting = ref(false)
const fileInput = ref(null)

const { content, isStreaming, startStream, reset } = useSSE()

// 内容拆分逻辑
const separator = '---RESUME_CONTENT_START---'
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
  if (!keyword) {
    return roleOptions
  }
  return roleOptions.filter((role) => role.includes(keyword))
})

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

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer?.files[0]
  if (file && file.type === 'application/pdf') {
    selectedFile.value = file
  } else {
    alert('请上传 PDF 文件')
  }
}

const handleFileSelect = async (e) => {
  const file = e.target.files[0]
  if (file && file.type === 'application/pdf') {
    selectedFile.value = file
  }
}

const extractTextFromPDF = async (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = async () => {
      try {
        const typedarray = new Uint8Array(reader.result)
        const pdf = await pdfjsLib.getDocument(typedarray).promise
        let fullText = ''
        
        // 提取前 5 页文本
        const maxPages = Math.min(pdf.numPages, 5)
        for (let i = 1; i <= maxPages; i++) {
          const page = await pdf.getPage(i)
          const textContent = await page.getTextContent()
          const pageText = textContent.items.map(item => item.str).join(' ')
          fullText += pageText + '\n'
        }
        resolve(fullText)
      } catch (err) {
        reject(err)
      }
    }
    reader.onerror = (err) => reject(err)
    reader.readAsArrayBuffer(file)
  })
}

const optimize = async () => {
  if (!selectedFile.value) return
  const normalizedTargetRole = targetRole.value.trim()
  if (!normalizedTargetRole) {
    alert('请输入目标岗位')
    return
  }
  reset()
  
  isExtracting.value = true
  let resumeText = ''
  try {
    resumeText = await extractTextFromPDF(selectedFile.value)
  } catch (error) {
    alert('PDF 文本提取失败: ' + error.message)
    isExtracting.value = false
    return
  }
  isExtracting.value = false
  
  if (!resumeText.trim()) {
    alert('无法从该 PDF 中提取到纯文本内容，请检查文件。')
    return
  }
  
  // 启动后端 SSE
  await startStream('/api/ai/resume/analyze', {
    method: 'POST',
    body: {
      resumeText: resumeText.substring(0, 15000), // 扩容至 15000，确保多页 PDF 内容完整
      targetRole: normalizedTargetRole
    }
  })
}
</script>

<style scoped>
.resume-page {
  background: var(--app-shell-bg);
}

.resume-panel {
  position: relative;
}

.resume-upload-panel {
  background: var(--surface-panel-soft);
}

.dark .resume-upload-panel {
  background: var(--surface-panel);
}

.resume-result-panel {
  background: var(--surface-panel-soft);
}

.dark .resume-result-panel {
  background: var(--surface-panel);
}

.resume-upload-card {
  border-color: var(--border-subtle);
  box-shadow: var(--shadow-card);
  background: var(--bg-elevated);
}

.resume-role-bar {
  background: var(--bg-surface);
  border-top: 1px solid var(--border-subtle);
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

.resume-page :deep([class~='text-red-500']) {
  color: var(--danger) !important;
}

.resume-page :deep([class~='hover:bg-red-50']) {
  background: var(--danger-soft) !important;
}
</style>
