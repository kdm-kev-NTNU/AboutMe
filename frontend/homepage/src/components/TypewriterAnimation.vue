<template>
  <div class="typewriter-container">
    <vue-markdown 
      v-if="displayedText" 
      :source="displayedText" 
      class="text-sm leading-relaxed whitespace-pre-wrap"
      :class="textClass"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import VueMarkdown from 'vue-markdown-render'

interface Props {
  text: string
  speed?: number // milliseconds per character
  textClass?: string
  autoStart?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  speed: 30,
  textClass: '',
  autoStart: true
})

const displayedText = ref('')
const isTyping = ref(false)
const currentIndex = ref(0)

const emit = defineEmits<{
  finished: []
  scroll: []
}>()

const startTyping = () => {
  if (!props.text) return
  
  displayedText.value = ''
  currentIndex.value = 0
  isTyping.value = true
  
  const typeNextCharacter = () => {
    if (currentIndex.value < props.text.length) {
      displayedText.value += props.text[currentIndex.value]
      currentIndex.value++
      
      // Emit scroll event every few characters to keep up with the typing
      if (currentIndex.value % 5 === 0) {
        emit('scroll')
      }
      
      setTimeout(typeNextCharacter, props.speed)
    } else {
      isTyping.value = false
      emit('finished')
      emit('scroll') // Final scroll when typing is complete
    }
  }
  
  typeNextCharacter()
}

const reset = () => {
  displayedText.value = ''
  currentIndex.value = 0
  isTyping.value = false
}

// Watch for text changes and restart animation
watch(() => props.text, (newText) => {
  if (newText && props.autoStart) {
    reset()
    nextTick(() => {
      startTyping()
    })
  }
}, { immediate: true })

// Expose methods for external control
defineExpose({
  startTyping,
  reset
})
</script>

<style scoped>
.typewriter-container {
  display: inline-block;
}
</style>