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
  /** Internal portfolio route for a dedicated project story page (e.g. `/projects/heathen-army`). */
  detailUrl?: string
  /** Optional thumbnail shown at the top of the project card. */
  imageUrl?: string
  /** Media slot type: static image or animated GSAP demo. */
  mediaType?: 'image' | 'demo'
  /** Registered demo id when mediaType is 'demo' (e.g. 'aboutme'). */
  demoId?: string
}

export interface ProjectsData {
  projects: Project[]
}
