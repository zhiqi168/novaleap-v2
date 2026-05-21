<template>
  <div class="typewriter-container relative">
    <div
      v-if="renderMarkdown"
      class="markdown-body text-text-primary"
      :class="{ typing: isTyping }"
      v-html="renderedHtml"
    ></div>
    <div
      v-else
      class="whitespace-pre-wrap text-text-primary leading-relaxed"
      :class="{ typing: isTyping }"
      v-html="formattedText"
    ></div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  text: {
    type: String,
    default: '',
  },
  renderMarkdown: {
    type: Boolean,
    default: true,
  },
  isTyping: {
    type: Boolean,
    default: false,
  },
})

const displayedText = ref('')
let currentIndex = 0
let timer = null
let streamSession = false

const sanitizeHtml = (unsafeHtml) => {
  const raw = String(unsafeHtml || '')
  if (!raw) return ''
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'code', 'pre', 'span', 'div', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'hr'],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class'],
  })
}

const normalizeMarkdown = (input) => {
  const raw = String(input || '')
  if (!raw) return ''

  return raw
    .replace(/\r\n?/g, '\n')
    // Ensure heading tokens are separated from previous text blocks.
    .replace(/([^\n])(\s*#{2,6}\s*\d+\.)/g, '$1\n\n$2')
    // Ensure there is a space after heading markers: "##1." -> "## 1."
    .replace(/(^|\n)(#{2,6})([^\s#])/g, '$1$2 $3')
    .replace(/\n{3,}/g, '\n\n')
}

const stopTyping = () => {
  if (!timer) return
  clearInterval(timer)
  timer = null
}

const resetTypingState = () => {
  currentIndex = 0
  displayedText.value = ''
}

const startTyping = () => {
  if (timer) return
  timer = setInterval(() => {
    const source = String(props.text || '')
    if (currentIndex >= source.length) {
      displayedText.value = source
      if (!props.isTyping) {
        stopTyping()
        streamSession = false
      }
      return
    }

    const remaining = source.length - currentIndex
    const step = Math.max(1, Math.ceil(remaining / 22))
    currentIndex = Math.min(source.length, currentIndex + step)
    displayedText.value = source.slice(0, currentIndex)
  }, 24)
}

watch(
  () => props.isTyping,
  (typing, prevTyping) => {
    const source = String(props.text || '')
    if (typing) {
      // New streaming round: text usually resets to empty first.
      if (!streamSession || !prevTyping || source.length < currentIndex || source.length === 0) {
        resetTypingState()
      }
      streamSession = true
      startTyping()
      return
    }

    if (streamSession) {
      if (currentIndex < source.length) {
        startTyping()
        return
      }
      stopTyping()
      streamSession = false
      displayedText.value = source
      currentIndex = source.length
      return
    }

    stopTyping()
    displayedText.value = source
    currentIndex = source.length
  },
  { immediate: true },
)

watch(
  () => props.text,
  (newText, oldText) => {
    const source = String(newText || '')
    const prev = String(oldText || '')

    if (!props.isTyping && !streamSession) {
      displayedText.value = source
      currentIndex = source.length
      return
    }

    // If upstream content restarted (or was truncated), reset cursor.
    if (source.length < currentIndex || (prev && !source.startsWith(prev))) {
      resetTypingState()
    }
    startTyping()
  },
  { immediate: true },
)

onUnmounted(() => {
  stopTyping()
})

const renderedHtml = computed(() => {
  if (!displayedText.value) return ''
  const html = marked.parse(normalizeMarkdown(displayedText.value))
  return sanitizeHtml(html)
})

const formattedText = computed(() => {
  if (!displayedText.value) return ''
  return sanitizeHtml(displayedText.value.replace(/\n/g, '<br>'))
})
</script>

<style scoped>
.typing::after {
  content: '|';
  display: inline-block;
  vertical-align: bottom;
  color: var(--ai-from);
  animation: blink-cursor 0.8s step-end infinite;
  margin-left: 2px;
}

.markdown-body.typing > *:last-child::after {
  content: '|';
  display: inline-block;
  vertical-align: bottom;
  color: var(--ai-from);
  animation: blink-cursor 0.8s step-end infinite;
  margin-left: 2px;
}

.markdown-body.typing::after {
  display: none;
}

@keyframes blink-cursor {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}
</style>
