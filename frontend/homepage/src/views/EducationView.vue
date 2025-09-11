<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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
  const rawData = langStore.language === 'no' ? educationNo : educationEn
  const data: EducationData = {
    education: rawData.education.map(edu => ({
      ...edu,
      status: edu.status as 'completed' | 'ongoing' | 'graduated' | undefined
    }))
  }
  return data.education
})

const coursesData = computed(() => {
  const rawData = langStore.language === 'no' ? coursesNo : coursesEn
  const data: CourseData = {
    courses: rawData.courses.map(course => ({
      ...course,
      status: course.status as 'completed' | 'ongoing' | 'planned'
    }))
  }
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

// Convert \n characters to HTML line breaks
const formatDescription = (text: string) => {
  return text.replace(/\n/g, '<br>')
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
  <main class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <!-- Background overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full bg-gradient-radial from-blue-500/8 via-transparent to-transparent" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%), radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);"></div>
    </div>
    <div class="max-w-4xl mx-auto px-8 py-8 relative z-10">
      <h1 class="text-3xl font-bold mb-12 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x">{{ pageTitle }}</h1>

      <!-- Education Cards -->
      <div class="space-y-8">
        <Card
          v-for="edu in education"
          :key="edu.id"
          class="relative border-2 border-transparent transition-all duration-300 bg-white/90 backdrop-blur-sm hover:border-blue-300/30 hover:bg-white/95 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/15 group"
        >
          <CardHeader>
            <div class="flex flex-col space-y-3">
              <div class="flex items-center justify-between">
                <div class="text-sm font-medium text-blue-600">{{ edu.period }}</div>
                <Badge v-if="edu.status" :variant="getStatusVariant(edu.status)" class="text-xs border border-blue-300/30 text-blue-600 bg-blue-50/50 hover:border-blue-300/50 hover:bg-blue-50 hover:text-blue-700 hover:-translate-y-0.5 transition-all duration-300">
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
                  <span class="px-2 py-1 bg-blue-50 border border-blue-200 rounded text-blue-600 font-medium">{{ edu.grade }}</span>
                </span>
                <span v-if="edu.credits" class="flex items-center gap-1">
                  <span class="font-medium">{{ edu.credits }} {{ langStore.language === 'no' ? 'studiepoeng' : 'credits' }}</span>
                </span>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <p class="text-gray-600 leading-relaxed" v-html="formatDescription(edu.description)"></p>
          </CardContent>
        </Card>
      </div>

      <!-- Courses Section -->
      <div class="mt-16">
        <h2 class="text-2xl font-bold mb-8 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x">{{ coursesTitle }}</h2>

        <div class="space-y-12">
          <div v-for="semesterGroup in coursesBySemester" :key="semesterGroup.semester" class="space-y-4">
            <h3 class="text-lg font-semibold text-blue-700 border-b border-blue-200 pb-2">
              {{ semesterGroup.semester }}
            </h3>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Card
                v-for="course in semesterGroup.courses"
                :key="course.id"
                class="relative border border-blue-100 transition-all duration-200 bg-white/80 backdrop-blur-sm hover:border-blue-300/30 hover:bg-white/90 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/10"
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
                      :class="[
                        'text-xs ml-2 flex-shrink-0 transition-all duration-200',
                        course.status === 'ongoing' ? 'border border-blue-400/40 text-blue-800 bg-blue-100/50 hover:border-blue-400/60 hover:bg-blue-100 hover:text-blue-900' :
                        course.status === 'completed' ? 'border border-green-300/30 text-green-700 bg-green-50/50 hover:border-green-300/50 hover:bg-green-50 hover:text-green-800' :
                        'border border-gray-300/30 text-gray-600 bg-gray-50/50 hover:border-gray-300/50 hover:bg-gray-50 hover:text-gray-700'
                      ]"
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
