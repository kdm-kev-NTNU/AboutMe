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
        <!-- Vertical Line -->
        <div class="absolute left-1/2 transform -translate-x-1/2 w-0.5 h-full bg-gray-300 timeline-line hidden md:block"></div>
        
        <!-- Timeline Items -->
        <div class="space-y-12">
          <div 
            v-for="(experience, index) in experiences" 
            :key="index"
            class="relative flex items-center timeline-item"
            :class="index % 2 === 0 ? 'justify-start md:justify-start' : 'justify-end md:justify-end'"
          >
            <!-- Timeline Dot -->
            <div class="absolute left-1/2 transform -translate-x-1/2 w-4 h-4 bg-blue-500 rounded-full border-4 border-white shadow-lg z-10 timeline-dot hidden md:block"></div>
            
            <!-- Content Card -->
            <div 
              class="w-full md:w-5/12 timeline-content"
              :class="index % 2 === 0 ? 'pr-0 md:pr-8' : 'pl-0 md:pl-8'"
            >
              <Card class="hover:shadow-lg transition-shadow duration-300">
                <CardHeader>
                  <div class="text-sm font-medium text-blue-600 mb-2">{{ experience.period }}</div>
                  <CardTitle class="text-lg">{{ experience.title }}</CardTitle>
                  <div class="flex flex-wrap gap-2 mt-2">
                    <span class="text-sm text-gray-500 font-medium">{{ experience.company }}</span>
                    <span v-if="experience.location" class="text-sm text-gray-400">• {{ experience.location }}</span>
                    <Badge v-if="experience.type" variant="secondary" class="text-xs">
                      {{ experience.type.replace('-', ' ') }}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <p class="text-gray-600 leading-relaxed">{{ experience.description }}</p>
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
  background-color: #f9fafb;
}

/* Mobile Timeline - Stack vertically */
@media (max-width: 768px) {
  .timeline-mobile {
    display: block;
  }
  
  .timeline-mobile .timeline-item {
    display: block;
    margin-bottom: 2rem;
  }
  
  .timeline-mobile .timeline-content {
    width: 100%;
    margin-left: 0;
    margin-right: 0;
    padding-left: 2rem;
  }
  
  .timeline-mobile .timeline-line {
    left: 1rem;
    transform: none;
  }
  
  .timeline-mobile .timeline-dot {
    left: 0.5rem;
    transform: none;
  }
}
</style>