<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
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
  <main class="work-experience pt-20">
    <div class="max-w-6xl mx-auto px-8 py-8">
      <h1 class="text-3xl font-bold text-gray-800 mb-12 text-center">{{ pageTitle }}</h1>
      
      <!-- Timeline Container -->
      <div class="relative timeline-mobile">
        <!-- Vertical Line with Gradient -->
        <div class="absolute left-1/2 transform -translate-x-1/2 w-1 h-full bg-gradient-to-b from-indigo-500 via-purple-500 to-pink-500 rounded-full timeline-line hidden md:block shadow-lg"></div>
        
        <!-- Timeline Items -->
        <div class="space-y-16">
          <div 
            v-for="(experience, index) in experiences" 
            :key="index"
            class="relative flex items-center timeline-item group"
            :class="index % 2 === 0 ? 'justify-start md:justify-start' : 'justify-end md:justify-end'"
          >
            <!-- Timeline Dot with Enhanced Styling -->
            <div class="absolute left-1/2 transform -translate-x-1/2 w-6 h-6 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full border-4 border-white shadow-xl z-10 timeline-dot hidden md:block transition-all duration-300 group-hover:scale-125 group-hover:shadow-2xl">
              <div class="absolute inset-1 bg-white rounded-full opacity-20"></div>
            </div>
            
            <!-- Content Card with Enhanced Styling -->
            <div 
              class="w-full md:w-5/12 timeline-content"
              :class="index % 2 === 0 ? 'pr-0 md:pr-8' : 'pl-0 md:pl-8'"
            >
              <Card class="hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-2 bg-white border-0 shadow-lg overflow-hidden">
                <!-- Gradient Header -->
                <div class="h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500"></div>
                
                <CardHeader class="pb-4">
                  <div class="flex items-center justify-between mb-3">
                    <div class="text-sm font-semibold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
                      {{ experience.period }}
                    </div>
                    <div class="w-2 h-2 bg-gradient-to-r from-indigo-400 to-purple-400 rounded-full"></div>
                  </div>
                  <CardTitle class="text-xl font-bold text-gray-800 mb-3">{{ experience.title }}</CardTitle>
                  <div class="flex flex-wrap gap-3 mt-2">
                    <span class="text-sm text-gray-600 font-semibold bg-gray-50 px-3 py-1 rounded-full">{{ experience.company }}</span>
                    <span v-if="experience.location" class="text-sm text-gray-500 bg-gray-100 px-3 py-1 rounded-full">📍 {{ experience.location }}</span>
                    <Badge v-if="experience.type" variant="secondary" class="text-xs bg-gradient-to-r from-indigo-100 to-purple-100 text-indigo-700 border-0">
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
.work-experience {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
}

.work-experience::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

/* Timeline Animations */
.timeline-item {
  opacity: 0;
  transform: translateY(30px);
  animation: fadeInUp 0.6s ease-out forwards;
}

.timeline-item:nth-child(1) { animation-delay: 0.1s; }
.timeline-item:nth-child(2) { animation-delay: 0.2s; }
.timeline-item:nth-child(3) { animation-delay: 0.3s; }
.timeline-item:nth-child(4) { animation-delay: 0.4s; }
.timeline-item:nth-child(5) { animation-delay: 0.5s; }

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Enhanced Timeline Line */
.timeline-line {
  background: linear-gradient(180deg, 
    #6366f1 0%, 
    #8b5cf6 25%, 
    #a855f7 50%, 
    #ec4899 75%, 
    #f97316 100%
  );
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.3);
}

/* Timeline Dot Enhancements */
.timeline-dot {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.8),
              0 4px 20px rgba(99, 102, 241, 0.4);
}

.timeline-dot::before {
  content: '';
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.timeline-item:hover .timeline-dot::before {
  opacity: 0.2;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.2; }
  50% { transform: scale(1.2); opacity: 0.1; }
}

/* Card Enhancements */
.timeline-content .card {
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* Mobile Timeline - Enhanced */
@media (max-width: 768px) {
  .timeline-mobile {
    display: block;
    padding-left: 2rem;
  }
  
  .timeline-mobile .timeline-item {
    display: block;
    margin-bottom: 3rem;
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
  
  .timeline-mobile .timeline-line {
    left: 1rem;
    transform: none;
    width: 2px;
    background: linear-gradient(180deg, #6366f1, #8b5cf6, #a855f7);
  }
  
  .timeline-mobile .timeline-dot {
    left: 0.5rem;
    transform: none;
    width: 1rem;
    height: 1rem;
  }
  
  .work-experience {
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  }
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .timeline-mobile {
    padding-left: 1.5rem;
  }
  
  .timeline-mobile .timeline-line {
    left: 0.75rem;
  }
  
  .timeline-mobile .timeline-dot {
    left: 0.25rem;
  }
}
</style>