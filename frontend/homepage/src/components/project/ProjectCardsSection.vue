<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useLangStore } from '@/stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { ProjectsData } from '@/types/projects'
import projectsEn from '@/types/projects.en.json'
import projectsNo from '@/types/projects.no.json'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const projectsData = computed(() => {
  const rawData = isNo.value ? projectsNo : projectsEn
  const data: ProjectsData = {
    projects: rawData.projects.map((project) => ({
      ...project,
      status: project.status as 'completed' | 'ongoing' | 'planned',
    })),
  }
  return data.projects
})

const formatDate = (dateString: string | null): string => {
  if (!dateString) return isNo.value ? 'd.d.' : 'Present'
  const date = new Date(dateString)
  return new Intl.DateTimeFormat(isNo.value ? 'nb-NO' : 'en-US', { year: 'numeric', month: 'short' }).format(date)
}

const projects = computed(() =>
  [...projectsData.value]
    .sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime())
    .map((project) => ({
      ...project,
      period: `${formatDate(project.startDate)} - ${formatDate(project.endDate)}`,
    })),
)

const getStatusVariant = (status: string) => {
  if (status === 'ongoing') return 'default'
  if (status === 'completed') return 'secondary'
  if (status === 'planned') return 'outline'
  return 'secondary'
}

const getStatusText = (status: string) => {
  const statusTexts = {
    ongoing: { en: 'Ongoing', no: 'Paagaaende' },
    completed: { en: 'Completed', no: 'Fullfort' },
    planned: { en: 'Planned', no: 'Planlagt' },
  }
  return statusTexts[status as keyof typeof statusTexts]?.[isNo.value ? 'no' : 'en'] || status
}
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <Card v-for="project in projects" :key="project.id" class="bg-white/90 border border-slate-200 shadow-sm">
      <CardHeader>
        <div class="flex items-start justify-between gap-3 mb-2">
          <div>
            <CardTitle class="text-xl">{{ project.projectName }}</CardTitle>
            <p class="text-sm text-blue-700 mt-1">{{ project.period }}</p>
          </div>
          <Badge :variant="getStatusVariant(project.status)" class="text-xs">
            {{ getStatusText(project.status) }}
          </Badge>
        </div>
        <div v-if="project.technologies" class="flex flex-wrap gap-2">
          <Badge v-for="tech in project.technologies" :key="tech" variant="outline" class="text-xs">{{ tech }}</Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p class="text-sm text-slate-700 leading-relaxed mb-4">{{ project.projectDescription }}</p>
        <div class="flex flex-wrap gap-2">
          <Button v-if="project.githubUrl" as="a" :href="project.githubUrl" target="_blank" rel="noopener noreferrer" variant="outline" size="sm">
            GitHub
          </Button>
          <Button v-if="project.liveUrl" as="a" :href="project.liveUrl" target="_blank" rel="noopener noreferrer" size="sm">
            Live demo
          </Button>
          <Button v-if="project.detailUrl" as-child variant="outline" size="sm">
            <RouterLink :to="project.detailUrl">
              {{ isNo ? 'Detaljer' : 'Details' }}
            </RouterLink>
          </Button>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
