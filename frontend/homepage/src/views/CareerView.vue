<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { MapPin } from 'lucide-vue-next'
import type { WorkExperienceData } from '../types/workExperience'
import type { EducationData } from '../types/education'

import workExperienceEn from '../types/workExperience.en.json'
import workExperienceNo from '../types/workExperience.no.json'
import educationEn from '../types/education.en.json'
import educationNo from '../types/education.no.json'

const langStore = useLangStore()

const pageTitle = computed(() =>
  langStore.language === 'no' ? 'Erfaring og utdanning' : 'Experience & education',
)
const workSectionTitle = computed(() =>
  langStore.language === 'no' ? 'Arbeidserfaring' : 'Work Experience',
)
const educationSectionTitle = computed(() =>
  langStore.language === 'no' ? 'Utdanning' : 'Education',
)

const workExperienceData = computed(() => {
  const rawData = langStore.language === 'no' ? workExperienceNo : workExperienceEn
  const data: WorkExperienceData = {
    experiences: rawData.experiences.map((experience) => ({
      ...experience,
      type: experience.type as
        | 'full-time'
        | 'part-time'
        | 'contract'
        | 'internship'
        | 'summer-job'
        | undefined,
    })),
  }
  return data.experiences
})

const educationData = computed(() => {
  const rawData = langStore.language === 'no' ? educationNo : educationEn
  const data: EducationData = {
    education: rawData.education.map((edu) => ({
      ...edu,
      status: edu.status as 'completed' | 'ongoing' | 'graduated' | undefined,
    })),
  }
  return data.education
})


const formatDate = (dateString: string | null, language: 'en' | 'no'): string => {
  if (!dateString) return language === 'no' ? 'd.d.' : 'Present'

  const date = new Date(dateString)
  const options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'short',
  }

  return new Intl.DateTimeFormat(language === 'no' ? 'nb-NO' : 'en-US', options).format(date)
}

const formatPeriod = (startDate: string, endDate: string | null, language: 'en' | 'no'): string => {
  const start = formatDate(startDate, language)
  const end = formatDate(endDate, language)
  return `${start} - ${end}`
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'ongoing':
      return 'default'
    case 'completed':
      return 'secondary'
    case 'graduated':
      return 'outline'
    default:
      return 'secondary'
  }
}

const getStatusText = (status: string, language: 'en' | 'no') => {
  const statusTexts = {
    ongoing: { en: 'Ongoing', no: 'Pågående' },
    completed: { en: 'Completed', no: 'Fullført' },
    graduated: { en: 'Graduated', no: 'Uteksaminert' },
  }
  return statusTexts[status as keyof typeof statusTexts]?.[language] || status
}


const experiences = computed(() => {
  return [...workExperienceData.value]
    .sort((a, b) => {
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
    })
    .map((exp) => ({
      id: exp.id,
      period: formatPeriod(exp.startDate, exp.endDate, langStore.language),
      title: exp.position,
      company: exp.company,
      description: exp.description,
      location: exp.location,
      type: exp.type,
    }))
})

const education = computed(() => {
  return [...educationData.value]
    .sort((a, b) => {
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
    })
    .map((edu) => ({
      id: edu.id,
      period: formatPeriod(edu.startDate, edu.endDate, langStore.language),
      degree: edu.degree,
      field: edu.field,
      institution: edu.institution,
      description: edu.description,
      location: edu.location,
      grade: edu.grade,
      credits: edu.credits,
      status: edu.status,
    }))
})
</script>

<template>
  <main id="main-content" class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <div class="absolute inset-0 pointer-events-none">
      <div
        class="absolute top-0 left-0 w-full h-full"
        style="
          background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
            radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.1) 0%, transparent 50%);
        "
      ></div>
    </div>

    <div class="relative z-10 mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <h1
        class="text-3xl font-bold mb-12 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x"
      >
        {{ pageTitle }}
      </h1>

      <!-- Work experience -->
      <section aria-labelledby="career-work-heading" class="mb-20">
        <h2
          id="career-work-heading"
          class="text-2xl font-bold mb-10 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x"
        >
          {{ workSectionTitle }}
        </h2>

        <div class="relative timeline-mobile">
          <div
            class="absolute left-1/2 transform -translate-x-1/2 w-1 h-full bg-gradient-to-b from-blue-400 via-blue-500 to-blue-600 rounded-full hidden md:block shadow-lg shadow-blue-500/30"
          ></div>

          <div class="space-y-16">
            <div
              v-for="(experience, index) in experiences"
              :key="experience.id ?? index"
              class="relative flex items-center opacity-0 translate-y-8 animate-fade-in-up group"
              :class="[
                index % 2 === 0 ? 'justify-start md:justify-start' : 'justify-end md:justify-end',
                `animation-delay-${index + 1}`,
              ]"
            >
              <div
                class="absolute left-1/2 transform -translate-x-1/2 w-6 h-6 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full border-4 border-white shadow-xl z-10 hidden md:block transition-all duration-300 group-hover:scale-125 group-hover:shadow-2xl group-hover:shadow-blue-500/40"
              >
                <div class="absolute inset-1 bg-white rounded-full opacity-20"></div>
              </div>

              <div
                class="w-full md:w-5/12 timeline-content"
                :class="index % 2 === 0 ? 'pr-0 md:pr-8' : 'pl-0 md:pl-8'"
              >
                <Card
                  class="hover:shadow-2xl transition-all duration-500 transform hover:-translate-y-2 bg-white shadow-lg overflow-hidden backdrop-blur-sm border border-white/20"
                >
                  <div class="h-1 bg-gradient-to-r from-blue-400 via-blue-500 to-blue-600"></div>

                  <CardHeader class="pb-4">
                    <div class="flex items-center justify-between mb-3">
                      <div
                        class="text-sm font-semibold bg-gradient-to-r from-blue-600 to-blue-700 bg-clip-text text-transparent"
                      >
                        {{ experience.period }}
                      </div>
                      <div class="w-2 h-2 bg-gradient-to-r from-blue-400 to-blue-500 rounded-full"></div>
                    </div>
                    <CardTitle class="text-xl font-bold text-gray-800 mb-3">{{ experience.title }}</CardTitle>
                    <div class="flex flex-wrap gap-3 mt-2">
                      <span class="text-sm text-gray-600 font-semibold bg-gray-50 px-3 py-1 rounded-full">{{
                        experience.company
                      }}</span>
                      <span
                        v-if="experience.location"
                        class="text-sm text-gray-500 bg-gray-100 px-3 py-1 rounded-full flex items-center gap-1"
                      >
                        <MapPin class="w-3 h-3" />
                        {{ experience.location }}
                      </span>
                      <Badge
                        v-if="experience.type"
                        variant="secondary"
                        class="text-xs bg-gradient-to-r from-blue-100 to-blue-200 text-blue-700 border-0"
                      >
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
      </section>

      <!-- Education -->
      <section aria-labelledby="career-education-heading" class="mb-12">
        <h2
          id="career-education-heading"
          class="text-2xl font-bold mb-10 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x"
        >
          {{ educationSectionTitle }}
        </h2>

        <div class="mx-auto max-w-4xl space-y-8">
          <Card
            v-for="edu in education"
            :key="edu.id"
            class="relative border-2 border-transparent transition-all duration-300 bg-white/90 backdrop-blur-sm hover:border-blue-300/30 hover:bg-white/95 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/15 group"
          >
            <CardHeader>
              <div class="flex flex-col space-y-3">
                <div class="flex items-center justify-between">
                  <div class="text-sm font-medium text-blue-600">{{ edu.period }}</div>
                  <Badge
                    v-if="edu.status"
                    :variant="getStatusVariant(edu.status)"
                    class="text-xs border border-blue-300/30 text-blue-600 bg-blue-50/50 hover:border-blue-300/50 hover:bg-blue-50 hover:text-blue-700 hover:-translate-y-0.5 transition-all duration-300"
                  >
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
                    <span class="px-2 py-1 bg-blue-50 border border-blue-200 rounded text-blue-600 font-medium">{{
                      edu.grade
                    }}</span>
                  </span>
                  <span v-if="edu.credits" class="flex items-center gap-1">
                    <span class="font-medium"
                      >{{ edu.credits }} {{ langStore.language === 'no' ? 'studiepoeng' : 'credits' }}</span
                    >
                  </span>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p class="text-gray-600 leading-relaxed whitespace-pre-line">{{ edu.description }}</p>
            </CardContent>
          </Card>
        </div>
      </section>

    </div>
  </main>
</template>

<style scoped>
@keyframes gradient-x {
  0%,
  100% {
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

.animation-delay-1 {
  animation-delay: 0.1s;
}
.animation-delay-2 {
  animation-delay: 0.2s;
}
.animation-delay-3 {
  animation-delay: 0.3s;
}
.animation-delay-4 {
  animation-delay: 0.4s;
}
.animation-delay-5 {
  animation-delay: 0.5s;
}

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

  .hover\:-translate-y-0\.5:hover {
    transform: translateY(-1px);
  }
}

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
