export interface Project {
  id: string
  projectName: string
  projectDescription: string
  courseName?: string
  startDate: string // ISO date string (YYYY-MM-DD)
  endDate: string | null // ISO date string or null for "Present"
  technologies?: string[]
  status: 'completed' | 'ongoing' | 'planned'
  githubUrl?: string
  liveUrl?: string
}

export interface ProjectsData {
  projects: Project[]
}