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
  <main class="projects pt-20">
    <div class="max-w-6xl mx-auto px-8 py-8">
      <h1 class="text-3xl font-bold text-gray-800 mb-12 text-center">{{ pageTitle }}</h1>
      
      <!-- Projects Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Card 
          v-for="project in projects" 
          :key="project.id"
          class="project-card hover:shadow-lg transition-all duration-300"
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
                class="text-xs blue-tech-badge"
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
                class="flex items-center gap-2 blue-outline-button"
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
                class="flex items-center gap-2 blue-primary-button"
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
.projects {
  min-height: 100vh;
  background-color: #f9fafb;
}

/* Project Card Styling */
.project-card {
  position: relative;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.project-card::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #3b82f6, #2563eb, #1d4ed8);
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.project-card:hover {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(59, 130, 246, 0.15) !important;
}

.project-card:hover::before {
  opacity: 0.1;
}

/* Blue Outline Button (GitHub) */
.blue-outline-button {
  border: 2px solid rgba(59, 130, 246, 0.3) !important;
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.05) !important;
  transition: all 0.3s ease !important;
  position: relative !important;
  overflow: hidden !important;
}

.blue-outline-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.blue-outline-button:hover {
  border-color: rgba(59, 130, 246, 0.6) !important;
  background: rgba(59, 130, 246, 0.1) !important;
  color: #2563eb !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2) !important;
}

.blue-outline-button:hover::before {
  left: 100%;
}

/* Blue Primary Button (Live Demo) */
.blue-primary-button {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  transition: all 0.3s ease !important;
  position: relative !important;
  overflow: hidden !important;
}

.blue-primary-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.blue-primary-button:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4) !important;
}

.blue-primary-button:hover::before {
  left: 100%;
}

.blue-primary-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.3) !important;
}

/* Blue Technology Badges */
.blue-tech-badge {
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.05) !important;
  transition: all 0.3s ease !important;
}

.blue-tech-badge:hover {
  border-color: rgba(59, 130, 246, 0.5) !important;
  background: rgba(59, 130, 246, 0.1) !important;
  color: #2563eb !important;
  transform: translateY(-1px) !important;
}

/* Page Title Styling */
h1 {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-size: 200% 200%;
  animation: gradientShift 3s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .project-card:hover {
    transform: translateY(-1px);
  }
  
  .blue-outline-button:hover,
  .blue-primary-button:hover {
    transform: none !important;
  }
}
</style>