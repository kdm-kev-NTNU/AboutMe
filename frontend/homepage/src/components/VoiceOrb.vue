<script setup lang="ts">
defineProps<{
  size?: 'sm' | 'md' | 'lg'
  active?: boolean
}>()
</script>

<template>
  <div
    class="voice-orb"
    :class="[
      size === 'sm' ? 'voice-orb--sm' : size === 'lg' ? 'voice-orb--lg' : 'voice-orb--md',
      { 'voice-orb--active': active },
    ]"
    aria-hidden="true"
  >
    <div class="voice-orb__sphere" />
    <div class="voice-orb__highlight" />
  </div>
</template>

<style scoped>
.voice-orb {
  position: relative;
  flex-shrink: 0;
}

.voice-orb--sm {
  width: 92px;
  height: 92px;
}

.voice-orb--md {
  width: 132px;
  height: 132px;
}

.voice-orb--lg {
  width: 200px;
  height: 200px;
}

.voice-orb__sphere {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(
    circle at 35% 35%,
    #60a5fa 0%,
    #2663eb 45%,
    #1d4ed8 75%,
    #1e3a8a 100%
  );
  box-shadow:
    0 0 40px rgba(38, 99, 235, 0.4),
    0 0 80px rgba(38, 99, 235, 0.2);
  animation: orb-pulse 3s ease-in-out infinite;
}

.voice-orb--active .voice-orb__sphere {
  animation: orb-pulse-active 1.5s ease-in-out infinite;
  box-shadow:
    0 0 60px rgba(38, 99, 235, 0.6),
    0 0 120px rgba(38, 99, 235, 0.3);
}

.voice-orb__highlight {
  position: absolute;
  top: 20%;
  left: 30%;
  width: 24%;
  height: 24%;
  border-radius: 50%;
  background: radial-gradient(
    circle at 50% 50%,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(255, 255, 255, 0.4) 40%,
    transparent 70%
  );
}

@keyframes orb-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.03);
    opacity: 0.95;
  }
}

@keyframes orb-pulse-active {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.06);
    opacity: 0.9;
  }
}
</style>
