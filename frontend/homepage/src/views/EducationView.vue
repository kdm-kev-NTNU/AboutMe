<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import type { Education, EducationData } from '../types/education'
import type { Course, CourseData } from '../types/courses'

// Import JSON data
import educationEn from '../types/education.en.json'
import educationNo from '../types/education.no.json'
import coursesEn from '../types/courses.en.json'
import coursesNo from '../types/courses.no.json'

const langStore = useLangStore()

const pageTitle = computed(() => langStore.language === 'no' ? 'Utdanning' : 'Education')
const coursesTitle = computed(() => langStore.language === 'no' ? 'Emner' : 'Courses')

// Get the appropriate data based on language
const educationData = computed(() => {
  const data: EducationData = langStore.language === 'no' ? educationNo : educationEn
  return data.education
})

const coursesData = computed(() => {
  const data: CourseData = langStore.language === 'no' ? coursesNo : coursesEn
  return data.courses
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

// Get status badge variant
const getStatusVariant = (status: string) => {
  switch (status) {
    case 'ongoing': return 'default'
    case 'completed': return 'secondary'
    case 'graduated': return 'outline'
    default: return 'secondary'
  }
}

// Get status text
const getStatusText = (status: string, language: 'en' | 'no') => {
  const statusTexts = {
    ongoing: { en: 'Ongoing', no: 'Pågående' },
    completed: { en: 'Completed', no: 'Fullført' },
    graduated: { en: 'Graduated', no: 'Uteksaminert' }
  }
  return statusTexts[status as keyof typeof statusTexts]?.[language] || status
}

// Get course status badge variant
const getCourseStatusVariant = (status: string) => {
  switch (status) {
    case 'ongoing': return 'default'
    case 'completed': return 'secondary'
    case 'planned': return 'outline'
    default: return 'secondary'
  }
}

// Get course status text
const getCourseStatusText = (status: string, language: 'en' | 'no') => {
  const statusTexts = {
    ongoing: { en: 'Ongoing', no: 'Pågående' },
    completed: { en: 'Completed', no: 'Fullført' },
    planned: { en: 'Planned', no: 'Planlagt' }
  }
  return statusTexts[status as keyof typeof statusTexts]?.[language] || status
}

// Group courses by semester
const coursesBySemester = computed(() => {
  const grouped: { [key: string]: Course[] } = {}
  
  coursesData.value.forEach(course => {
    if (!grouped[course.semester]) {
      grouped[course.semester] = []
    }
    grouped[course.semester].push(course)
  })
  
  // Sort semesters chronologically
  const sortedSemesters = Object.keys(grouped).sort((a, b) => {
    const [yearA, seasonA] = a.split('-')
    const [yearB, seasonB] = b.split('-')
    
    if (yearA !== yearB) return parseInt(yearA) - parseInt(yearB)
    return seasonA === 'Spring' || seasonA === 'Vår' ? 1 : -1
  })
  
  return sortedSemesters.map(semester => ({
    semester,
    courses: grouped[semester]
  }))
})

// Sort education by start date (most recent first) and format for display
const education = computed(() => {
  return educationData.value
    .sort((a, b) => {
      // Sort by start date descending (most recent first)
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
    })
    .map(edu => ({
      id: edu.id,
      period: formatPeriod(edu.startDate, edu.endDate, langStore.language),
      degree: edu.degree,
      field: edu.field,
      institution: edu.institution,
      description: edu.description,
      location: edu.location,
      grade: edu.grade,
      credits: edu.credits,
      status: edu.status
    }))
})
</script>

<template>
  <main class="education pt-20">
    <div class="max-w-4xl mx-auto px-8 py-8">
      <h1 class="text-3xl font-bold text-gray-800 mb-12 text-center">{{ pageTitle }}</h1>
      
      <!-- Education Cards -->
      <div class="space-y-8">
        <Card 
          v-for="edu in education" 
          :key="edu.id"
          class="hover:shadow-lg transition-shadow duration-300"
        >
          <CardHeader>
            <div class="flex flex-col space-y-3">
              <div class="flex items-center justify-between">
                <div class="text-sm font-medium text-green-600">{{ edu.period }}</div>
                <Badge v-if="edu.status" :variant="getStatusVariant(edu.status)" class="text-xs">
                  {{ getStatusText(edu.status, langStore.language) }}
                </Badge>
              </div>
              <div>
                <CardTitle class="text-xl mb-2">{{ edu.degree }} in {{ edu.field }}</CardTitle>
                <div class="flex flex-wrap gap-2 text-sm text-gray-500">
                  <span class="font-medium">{{ edu.institution }}</span>
                  <span v-if="edu.location">• {{ edu.location }}</span>
                </div>
              </div>
              <div v-if="edu.grade || edu.credits" class="flex flex-wrap gap-4 text-sm text-gray-600">
                <span v-if="edu.grade" class="flex items-center gap-1">
                  <span class="font-medium">{{ langStore.language === 'no' ? 'Karakter' : 'Grade' }}:</span>
                  <span class="px-2 py-1 bg-gray-100 rounded text-gray-700 font-medium">{{ edu.grade }}</span>
                </span>
                <span v-if="edu.credits" class="flex items-center gap-1">
                  <span class="font-medium">{{ edu.credits }} {{ langStore.language === 'no' ? 'studiepoeng' : 'credits' }}</span>
                </span>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <p class="text-gray-600 leading-relaxed">{{ edu.description }}</p>
          </CardContent>
        </Card>
      </div>

      <!-- Courses Section -->
      <div class="mt-16">
        <h2 class="text-2xl font-bold text-gray-800 mb-8 text-center">{{ coursesTitle }}</h2>
        
        <div class="space-y-12">
          <div v-for="semesterGroup in coursesBySemester" :key="semesterGroup.semester" class="space-y-4">
            <h3 class="text-lg font-semibold text-gray-700 border-b border-gray-200 pb-2">
              {{ semesterGroup.semester }}
            </h3>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Card 
                v-for="course in semesterGroup.courses" 
                :key="course.id"
                class="hover:shadow-md transition-shadow duration-200"
              >
                <CardHeader class="pb-3">
                  <div class="flex items-start justify-between">
                    <div class="flex-1">
                      <CardTitle class="text-sm font-medium text-gray-600 mb-1">
                        {{ course.courseCode }}
                      </CardTitle>
                      <h4 class="text-base font-semibold text-gray-800 leading-tight">
                        {{ course.courseName }}
                      </h4>
                    </div>
                    <Badge 
                      :variant="getCourseStatusVariant(course.status)" 
                      class="text-xs ml-2 flex-shrink-0"
                    >
                      {{ getCourseStatusText(course.status, langStore.language) }}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent class="pt-0">
                  <div class="flex items-center justify-between text-sm text-gray-600">
                    <span>{{ course.credits }} {{ langStore.language === 'no' ? 'studiepoeng' : 'credits' }}</span>
                    <span v-if="course.grade" class="font-medium text-gray-700">
                      {{ langStore.language === 'no' ? 'Karakter' : 'Grade' }}: {{ course.grade }}
                    </span>
                  </div>
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
.education {
  min-height: 100vh;
  background-color: #f9fafb;
}
</style>