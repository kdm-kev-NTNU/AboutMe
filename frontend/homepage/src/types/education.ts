export interface Education {
  id: string
  startDate: string // ISO date string (YYYY-MM-DD)
  endDate: string | null // ISO date string or null for "Present"
  institution: string
  degree: string
  field: string
  description: string
  location?: string
  grade?: string
  credits?: number
  status?: 'completed' | 'ongoing' | 'graduated'
}

export interface EducationData {
  education: Education[]
}