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

// Get course status badge class for custom blue shades
const getCourseStatusClass = (status: string) => {
  switch (status) {
    case 'ongoing': return 'blue-ongoing-badge'
    case 'completed': return 'blue-completed-badge'
    case 'planned': return 'blue-planned-badge'
    default: return 'blue-completed-badge'
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

  // Sort semesters in descending order (most recent first)
  const sortedSemesters = Object.keys(grouped).sort((a, b) => {
    const [yearA, seasonA] = a.split('-')
    const [yearB, seasonB] = b.split('-')

    if (yearA !== yearB) return parseInt(yearB) - parseInt(yearA) // Descending year order
    return seasonB === 'Spring' || seasonB === 'Vår' ? 1 : -1 // Spring comes after Autumn
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
    <div class="max-w-4xl mx-auto px-8 py-8 relative z-10">
      <h1 class="text-3xl font-bold text-gray-800 mb-12 text-center">{{ pageTitle }}</h1>

      <!-- Education Cards -->
      <div class="space-y-8">
        <Card
          v-for="edu in education"
          :key="edu.id"
          class="education-card hover:shadow-lg transition-all duration-300"
        >
          <CardHeader>
            <div class="flex flex-col space-y-3">
              <div class="flex items-center justify-between">
                <div class="text-sm font-medium text-blue-600">{{ edu.period }}</div>
                <Badge v-if="edu.status" :variant="getStatusVariant(edu.status)" class="text-xs blue-status-badge">
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
            <h3 class="text-lg font-semibold text-blue-700 border-b border-blue-200 pb-2">
              {{ semesterGroup.semester }}
            </h3>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Card
                v-for="course in semesterGroup.courses"
                :key="course.id"
                class="course-card hover:shadow-md transition-all duration-200"
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
                      :class="['text-xs ml-2 flex-shrink-0', getCourseStatusClass(course.status)]"
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
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
}

.education::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%),
              radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

/* Education Card Styling */
.education-card {
  position: relative;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.education-card::before {
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

.education-card:hover {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(59, 130, 246, 0.15) !important;
}

.education-card:hover::before {
  opacity: 0.1;
}

/* Course Card Styling */
.course-card {
  position: relative;
  border: 1px solid rgba(59, 130, 246, 0.1);
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(5px);
}

.course-card:hover {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.1) !important;
}

/* Blue Status Badge Styling */
.blue-status-badge {
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.05) !important;
  transition: all 0.3s ease !important;
}

.blue-status-badge:hover {
  border-color: rgba(59, 130, 246, 0.5) !important;
  background: rgba(59, 130, 246, 0.1) !important;
  color: #2563eb !important;
  transform: translateY(-1px) !important;
}

/* Blue Course Badge Styling - Different shades for each status */
.blue-ongoing-badge {
  border: 1px solid rgba(59, 130, 246, 0.4) !important;
  color: #1d4ed8 !important;
  background: rgba(59, 130, 246, 0.15) !important;
  transition: all 0.2s ease !important;
}

.blue-ongoing-badge:hover {
  border-color: rgba(59, 130, 246, 0.6) !important;
  background: rgba(59, 130, 246, 0.2) !important;
  color: #1e40af !important;
}

.blue-completed-badge {
  border: 1px solid rgba(34, 197, 94, 0.3) !important;
  color: #16a34a !important;
  background: rgba(34, 197, 94, 0.1) !important;
  transition: all 0.2s ease !important;
}

.blue-completed-badge:hover {
  border-color: rgba(34, 197, 94, 0.5) !important;
  background: rgba(34, 197, 94, 0.15) !important;
  color: #15803d !important;
}

.blue-planned-badge {
  border: 1px solid rgba(107, 114, 128, 0.3) !important;
  color: #6b7280 !important;
  background: rgba(107, 114, 128, 0.05) !important;
  transition: all 0.2s ease !important;
}

.blue-planned-badge:hover {
  border-color: rgba(107, 114, 128, 0.5) !important;
  background: rgba(107, 114, 128, 0.1) !important;
  color: #4b5563 !important;
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

h2 {
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

/* Grade styling */
span[class*="bg-gray-100"] {
  background: rgba(59, 130, 246, 0.1) !important;
  color: #3b82f6 !important;
  border: 1px solid rgba(59, 130, 246, 0.2) !important;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .education-card:hover {
    transform: translateY(-1px);
  }

  .course-card:hover {
    transform: none;
  }
}
</style>
