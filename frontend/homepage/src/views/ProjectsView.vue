<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Project, ProjectsData } from '../types/projects'

// Import JSON data
import projectsEn from '../types/projects.en.json'
import projectsNo from '../types/projects.no.json'

const langStore = useLangStore()

const pageTitle = computed(() => langStore.language === 'no' ? 'Prosjekter' : 'Projects')

// Get the appropriate data based on language
const projectsData = computed(() => {
  const data: ProjectsData = langStore.language === 'no' ? projectsNo : projectsEn
  return data.projects
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

// Get project status badge variant
const getStatusVariant = (status: string) => {
  switch (status) {
    case 'ongoing': return 'default'
    case 'completed': return 'secondary'
    case 'planned': return 'outline'
    default: return 'secondary'
  }
}

// Get project status text
const getStatusText = (status: string, language: 'en' | 'no') => {
  const statusTexts = {
    ongoing: { en: 'Ongoing', no: 'Pågående' },
    completed: { en: 'Completed', no: 'Fullført' },
    planned: { en: 'Planned', no: 'Planlagt' }
  }
  return statusTexts[status as keyof typeof statusTexts]?.[language] || status
}

// Sort projects by start date (most recent first) and format for display
const projects = computed(() => {
  return projectsData.value
    .sort((a, b) => {
      // Sort by start date descending (most recent first)
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
    })
    .map(project => ({
      id: project.id,
      projectName: project.projectName,
      projectDescription: project.projectDescription,
      courseName: project.courseName,
      period: formatPeriod(project.startDate, project.endDate, langStore.language),
      technologies: project.technologies,
      status: project.status,
      githubUrl: project.githubUrl,
      liveUrl: project.liveUrl
    }))
})
</script>

<template>
  <main class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <!-- Background overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%), radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);"></div>
    </div>
    <div class="max-w-6xl mx-auto px-8 py-8 relative z-10">
      <h1 class="text-3xl font-bold mb-12 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x">{{ pageTitle }}</h1>
      
      <!-- Projects Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Card 
          v-for="project in projects" 
          :key="project.id"
          class="relative border-2 border-transparent transition-all duration-300 bg-white/90 backdrop-blur-sm hover:border-blue-300/30 hover:bg-white/95 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/15 group"
        >
          <CardHeader>
            <div class="flex items-start justify-between mb-3">
              <div class="flex-1">
                <CardTitle class="text-xl mb-2">{{ project.projectName }}</CardTitle>
                <div class="text-sm text-blue-600 mb-2 font-medium">{{ project.period }}</div>
                <div v-if="project.courseName" class="text-sm text-gray-500 mb-2">
                  {{ langStore.language === 'no' ? 'Emne' : 'Course' }}: {{ project.courseName }}
                </div>
              </div>
              <Badge 
                :variant="getStatusVariant(project.status)" 
                class="text-xs flex-shrink-0"
              >
                {{ getStatusText(project.status, langStore.language) }}
              </Badge>
            </div>
            
            <!-- Technologies -->
            <div v-if="project.technologies" class="flex flex-wrap gap-2 mb-4">
              <Badge 
                v-for="tech in project.technologies" 
                :key="tech"
                variant="outline" 
                class="text-xs border border-blue-300/30 text-blue-600 bg-blue-50/50 hover:border-blue-300/50 hover:bg-blue-50 hover:text-blue-700 hover:-translate-y-0.5 transition-all duration-300"
              >
                {{ tech }}
              </Badge>
            </div>
          </CardHeader>
          
          <CardContent>
            <p class="text-gray-600 leading-relaxed mb-4">{{ project.projectDescription }}</p>
            
            <!-- Action Buttons -->
            <div class="flex gap-2">
              <Button 
                v-if="project.githubUrl"
                variant="outline" 
                size="sm"
                as="a"
                :href="project.githubUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="flex items-center gap-2 border-2 border-blue-300/30 text-blue-600 bg-blue-50/50 hover:border-blue-300/60 hover:bg-blue-50 hover:text-blue-700 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/20 transition-all duration-300 relative overflow-hidden"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
                {{ langStore.language === 'no' ? 'GitHub' : 'GitHub' }}
              </Button>
              <Button 
                v-if="project.liveUrl"
                variant="default" 
                size="sm"
                as="a"
                :href="project.liveUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white font-semibold hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/40 transition-all duration-300 relative overflow-hidden"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                </svg>
                {{ langStore.language === 'no' ? 'Live Demo' : 'Live Demo' }}
              </Button>
            </div>
          </CardContent>
        </Card>
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

.animate-gradient-x {
  background-size: 200% 200%;
  animation: gradient-x 3s ease-in-out infinite;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .hover\:-translate-y-0\.5:hover {
    transform: translateY(-1px);
  }
}
</style>