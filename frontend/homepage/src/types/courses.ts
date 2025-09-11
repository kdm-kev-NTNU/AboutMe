export interface Course {
  id: string
  courseCode: string
  courseName: string
  credits: number
  semester: string // e.g., "2023-Autumn", "2024-Spring"
  grade?: string
  status: 'completed' | 'ongoing' | 'planned'
}

export interface CourseData {
  courses: Course[]
}