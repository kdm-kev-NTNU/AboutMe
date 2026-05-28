<script setup lang="ts">
import { computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MapPin } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import ProjectCardsSection from '@/components/project/ProjectCardsSection.vue'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { WorkExperienceData } from '@/types/workExperience'
import type { EducationData } from '@/types/education'
import workExperienceEn from '@/types/workExperience.en.json'
import workExperienceNo from '@/types/workExperience.no.json'
import educationEn from '@/types/education.en.json'
import educationNo from '@/types/education.no.json'

const langStore = useLangStore()
const route = useRoute()
const isNo = computed(() => langStore.language === 'no')

const pageTitle = computed(() => (isNo.value ? 'Erfaring og utdanning' : 'Experience and education'))
const workSectionTitle = computed(() => (isNo.value ? 'Arbeidserfaring' : 'Work Experience'))
const educationSectionTitle = computed(() => (isNo.value ? 'Utdanning' : 'Education'))
const projectsSectionTitle = computed(() => (isNo.value ? 'Prosjekter' : 'Projects'))

function scrollToSection(elementId: string) {
  nextTick(() => {
    document.getElementById(elementId)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function applyRouteHash(hash: string | undefined) {
  if (!hash) return
  const h = hash.replace(/^#/, '').toLowerCase()
  if (h === 'projects') {
    scrollToSection('experience-projects-section')
  }
}

watch(
  () => route.hash,
  (h) => applyRouteHash(h),
  { immediate: true },
)

const workExperienceData = computed(() => {
  const rawData = isNo.value ? workExperienceNo : workExperienceEn
  const data: WorkExperienceData = {
    experiences: rawData.experiences.map((experience) => ({
      ...experience,
      type: experience.type as 'full-time' | 'part-time' | 'contract' | 'internship' | 'summer-job' | undefined,
    })),
  }
  return data.experiences
})

const educationData = computed(() => {
  const rawData = isNo.value ? educationNo : educationEn
  const data: EducationData = {
    education: rawData.education.map((edu) => ({
      ...edu,
      status: edu.status as 'completed' | 'ongoing' | 'graduated' | undefined,
    })),
  }
  return data.education
})

const formatDate = (dateString: string | null): string => {
  if (!dateString) return isNo.value ? 'd.d.' : 'Present'
  const date = new Date(dateString)
  return new Intl.DateTimeFormat(isNo.value ? 'nb-NO' : 'en-US', { year: 'numeric', month: 'short' }).format(date)
}

const formatPeriod = (startDate: string, endDate: string | null): string => `${formatDate(startDate)} - ${formatDate(endDate)}`

const experiences = computed(() =>
  [...workExperienceData.value]
    .sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime())
    .map((exp) => ({
      id: exp.id,
      period: formatPeriod(exp.startDate, exp.endDate),
      title: exp.position,
      company: exp.company,
      description: exp.description,
      location: exp.location,
      type: exp.type,
    })),
)

const education = computed(() =>
  [...educationData.value]
    .sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime())
    .map((edu) => ({
      id: edu.id,
      period: formatPeriod(edu.startDate, edu.endDate),
      degree: edu.degree,
      field: edu.field,
      institution: edu.institution,
      description: edu.description,
      location: edu.location,
      grade: edu.grade,
      credits: edu.credits,
      status: edu.status,
    })),
)

const getStatusVariant = (status: string) => {
  if (status === 'ongoing') return 'default'
  if (status === 'completed') return 'secondary'
  if (status === 'graduated') return 'outline'
  return 'secondary'
}

const getStatusText = (status: string) => {
  const statusTexts = {
    ongoing: { en: 'Ongoing', no: 'Paagaaende' },
    completed: { en: 'Completed', no: 'Fullfort' },
    graduated: { en: 'Graduated', no: 'Uteksaminert' },
  }
  return statusTexts[status as keyof typeof statusTexts]?.[isNo.value ? 'no' : 'en'] || status
}
</script>

<template>
  <main class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 pt-20 pb-12">
    <div class="relative z-10 mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 class="text-3xl font-bold mb-12 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent">
        {{ pageTitle }}
      </h1>

      <section aria-labelledby="experience-work-heading" class="mb-14">
        <h2 id="experience-work-heading" class="text-2xl font-bold mb-8 text-center text-slate-900">{{ workSectionTitle }}</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card v-for="experience in experiences" :key="experience.id" class="bg-white/90 border border-slate-200 shadow-sm">
            <CardHeader>
              <div class="flex items-center justify-between mb-1">
                <p class="text-sm font-medium text-blue-700">{{ experience.period }}</p>
                <Badge v-if="experience.type" variant="secondary" class="text-xs">
                  {{ experience.type.replace('-', ' ') }}
                </Badge>
              </div>
              <CardTitle class="text-lg">{{ experience.title }}</CardTitle>
              <div class="flex flex-wrap gap-2 text-sm text-slate-600">
                <span>{{ experience.company }}</span>
                <span v-if="experience.location" class="inline-flex items-center gap-1">
                  <MapPin class="h-3.5 w-3.5" />
                  {{ experience.location }}
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <p class="text-sm text-slate-700 leading-relaxed">{{ experience.description }}</p>
            </CardContent>
          </Card>
        </div>
      </section>

      <section aria-labelledby="experience-education-heading" class="mb-14">
        <h2 id="experience-education-heading" class="text-2xl font-bold mb-8 text-center text-slate-900">{{ educationSectionTitle }}</h2>
        <div class="mx-auto max-w-4xl space-y-6">
          <Card v-for="edu in education" :key="edu.id" class="bg-white/90 border border-slate-200 shadow-sm">
            <CardHeader>
              <div class="flex items-center justify-between">
                <p class="text-sm font-medium text-blue-700">{{ edu.period }}</p>
                <Badge v-if="edu.status" :variant="getStatusVariant(edu.status)" class="text-xs">
                  {{ getStatusText(edu.status) }}
                </Badge>
              </div>
              <CardTitle class="text-xl">{{ edu.degree }} in {{ edu.field }}</CardTitle>
              <div class="text-sm text-slate-600">
                <span class="font-medium">{{ edu.institution }}</span>
                <span v-if="edu.location"> · {{ edu.location }}</span>
              </div>
            </CardHeader>
            <CardContent>
              <p class="text-sm text-slate-700 whitespace-pre-line">{{ edu.description }}</p>
            </CardContent>
          </Card>
        </div>
      </section>

      <section id="experience-projects-section" aria-labelledby="experience-projects-heading">
        <h2 id="experience-projects-heading" class="text-2xl font-bold mb-8 text-center text-slate-900">{{ projectsSectionTitle }}</h2>
        <ProjectCardsSection />
      </section>
    </div>
  </main>
</template>
