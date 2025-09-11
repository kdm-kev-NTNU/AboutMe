export interface WorkExperience {
  id: string
  startDate: string // ISO date string (YYYY-MM-DD)
  endDate: string | null // ISO date string or null for "Present"
  company: string
  position: string
  description: string
  location?: string
  type?: 'full-time' | 'part-time' | 'contract' | 'internship' | 'summer-job'
}

export interface WorkExperienceData {
  experiences: WorkExperience[]
}