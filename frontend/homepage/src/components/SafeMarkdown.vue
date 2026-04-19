<script setup lang="ts">
import { computed } from 'vue'
import { renderSafeMarkdown } from '@/utils/safeMarkdown'

const props = withDefaults(
  defineProps<{
    source: string
    /** Extra classes for the outer wrapper (e.g. text color). */
    contentClass?: string
  }>(),
  { contentClass: '' },
)

const html = computed(() => renderSafeMarkdown(props.source || ''))
</script>

<template>
  <!-- eslint-disable vue/no-v-html -- intentional: HTML is produced by markdown-it and sanitized with DOMPurify -->
  <div
    class="safe-markdown text-sm leading-relaxed whitespace-pre-wrap text-gray-700"
    :class="contentClass"
    v-html="html"
  />
</template>

<style scoped>
.safe-markdown :deep(p) {
  margin: 0 0 0.5em;
}
.safe-markdown :deep(p:last-child) {
  margin-bottom: 0;
}
.safe-markdown :deep(pre) {
  overflow-x: auto;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
  background: rgb(243 244 246);
  font-size: 0.8125rem;
}
.safe-markdown :deep(code) {
  font-size: 0.8125rem;
}
.safe-markdown :deep(p > code),
.safe-markdown :deep(li > code) {
  padding: 0.1em 0.35em;
  border-radius: 0.25rem;
  background: rgb(243 244 246);
}
.safe-markdown :deep(ul),
.safe-markdown :deep(ol) {
  margin: 0.25em 0 0.5em 1.25em;
  padding: 0;
}
.safe-markdown :deep(a) {
  text-decoration: underline;
  color: rgb(37 99 235);
}
</style>
