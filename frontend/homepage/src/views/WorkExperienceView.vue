<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { MapPin } from 'lucide-vue-next'
import type { WorkExperience, WorkExperienceData } from '../types/workExperience'

// Import JSON data
import workExperienceEn from '../types/workExperience.en.json'
import workExperienceNo from '../types/workExperience.no.json'

const langStore = useLangStore()

const pageTitle = computed(() => langStore.language === 'no' ? 'Arbeidserfaring' : 'Work Experience')

// Get the appropriate data based on language
const workExperienceData = computed(() => {
  const data: WorkExperienceData = langStore.language === 'no' ? workExperienceNo : workExperienceEn
  return data.experiences
})

// Format date for display
const formatDate = (dateString: string | null, language: 'en' | 'no'): string => {
  if (!dateString) return language === 'no' ? 'd.d.' : 'Present'
  
  const date = new Date(dateString)
  const options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'short'
  }
  
  return new Intl.DateTimeFormat(language === 'no' ? 'nb-NO' : 'en-US', options).format(date)
}

// Format period string
const formatPeriod = (startDate: string, endDate: string | null, language: 'en' | 'no'): string => {
  const start = formatDate(startDate, language)
  const end = formatDate(endDate, language)
  return `${start} - ${end}`
}

// Sort experiences by start date (most recent first) and format for display
const experiences = computed(() => {
  return workExperienceData.value
    .sort((a, b) => {
      // Sort by start date descending (most recent first)
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
    })
    .map(exp => ({
      id: exp.id,
      period: formatPeriod(exp.startDate, exp.endDate, langStore.language),
      title: exp.position,
      company: exp.company,
      description: exp.description,
      location: exp.location,
      type: exp.type
    }))
})
</script>

<template>
  <main class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <!-- Background overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.1) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.1) 0%, transparent 50%);"></div>
    </div>
    <div class="max-w-6xl mx-auto px-8 py-8 relative z-10">
      <h1 class="text-3xl font-bold mb-12 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x">{{ pageTitle }}</h1>
      
      <!-- Timeline Container -->
      <div class="relative timeline-mobile">
        <!-- Vertical Line with Gradient -->
        <div class="absolute left-1/2 transform -translate-x-1/2 w-1 h-full bg-gradient-to-b from-blue-400 via-blue-500 to-blue-600 rounded-full hidden md:block shadow-lg shadow-blue-500/30"></div>
        
        <!-- Timeline Items -->
        <div class="space-y-16">
          <div 
            v-for="(experience, index) in experiences" 
            :key="index"
            class="relative flex items-center opacity-0 translate-y-8 animate-fade-in-up group"
            :class="[
              index % 2 === 0 ? 'justify-start md:justify-start' : 'justify-end md:justify-end',
              `animation-delay-${index + 1}`
            ]"
          >
            <!-- Timeline Dot with Enhanced Styling -->
            <div class="absolute left-1/2 transform -translate-x-1/2 w-6 h-6 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full border-4 border-white shadow-xl z-10 hidden md:block transition-all duration-300 group-hover:scale-125 group-hover:shadow-2xl group-hover:shadow-blue-500/40">
              <div class="absolute inset-1 bg-white rounded-full opacity-20"></div>
            </div>
            
            <!-- Content Card with Enhanced Styling -->
            <div 
              class="w-full md:w-5/12 timeline-content"
              :class="index % 2 === 0 ? 'pr-0 md:pr-8' : 'pl-0 md:pl-8'"
            >
              <Card class="hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-2 bg-white border-0 shadow-lg overflow-hidden backdrop-blur-sm border border-white/20">
                <!-- Gradient Header -->
                <div class="h-1 bg-gradient-to-r from-blue-400 via-blue-500 to-blue-600"></div>
                
                <CardHeader class="pb-4">
                  <div class="flex items-center justify-between mb-3">
                    <div class="text-sm font-semibold bg-gradient-to-r from-blue-600 to-blue-700 bg-clip-text text-transparent">
                      {{ experience.period }}
                    </div>
                    <div class="w-2 h-2 bg-gradient-to-r from-blue-400 to-blue-500 rounded-full"></div>
                  </div>
                  <CardTitle class="text-xl font-bold text-gray-800 mb-3">{{ experience.title }}</CardTitle>
                  <div class="flex flex-wrap gap-3 mt-2">
                    <span class="text-sm text-gray-600 font-semibold bg-gray-50 px-3 py-1 rounded-full">{{ experience.company }}</span>
                    <span v-if="experience.location" class="text-sm text-gray-500 bg-gray-100 px-3 py-1 rounded-full flex items-center gap-1">
                      <MapPin class="w-3 h-3" />
                      {{ experience.location }}
                    </span>
                    <Badge v-if="experience.type" variant="secondary" class="text-xs bg-gradient-to-r from-blue-100 to-blue-200 text-blue-700 border-0">
                      {{ experience.type.replace('-', ' ') }}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent class="pt-0">
                  <p class="text-gray-700 leading-relaxed text-sm">{{ experience.description }}</p>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
@keyframes gradient-x {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-gradient-x {
  background-size: 200% 200%;
  animation: gradient-x 3s ease-in-out infinite;
}

.animate-fade-in-up {
  animation: fadeInUp 0.6s ease-out forwards;
}

.animation-delay-1 { animation-delay: 0.1s; }
.animation-delay-2 { animation-delay: 0.2s; }
.animation-delay-3 { animation-delay: 0.3s; }
.animation-delay-4 { animation-delay: 0.4s; }
.animation-delay-5 { animation-delay: 0.5s; }

/* Mobile Timeline - Enhanced */
@media (max-width: 768px) {
  .timeline-mobile {
    display: block;
    padding-left: 2rem;
  }
  
  .timeline-mobile .animate-fade-in-up {
    opacity: 1;
    transform: none;
    animation: none;
  }
  
  .timeline-mobile .timeline-content {
    width: 100%;
    margin-left: 0;
    margin-right: 0;
    padding-left: 0;
  }
  
  .timeline-mobile .absolute.left-1\/2 {
    left: 1rem;
    transform: none;
    width: 2px;
  }
  
  .timeline-mobile .w-6.h-6 {
    left: 0.5rem;
    transform: none;
    width: 1rem;
    height: 1rem;
  }
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .timeline-mobile {
    padding-left: 1.5rem;
  }
  
  .timeline-mobile .absolute.left-1\/2 {
    left: 0.75rem;
  }
  
  .timeline-mobile .w-6.h-6 {
    left: 0.25rem;
  }
}
</style>