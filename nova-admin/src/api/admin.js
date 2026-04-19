import { api } from '@/composables/useRequest'

// ===== Dashboard =====
export const getDashboard = () => api.get('/api/admin/dashboard')
export const getSystemMonitor = () => api.get('/api/admin/system-monitor')
export const getVisitorRecords = (page = 1, size = 20, keyword = '', actorType = '') => {
  let url = `/api/admin/visitor-records?page=${page}&size=${size}`
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`
  if (actorType) url += `&actorType=${encodeURIComponent(actorType)}`
  return api.get(url)
}

// ===== Users =====
export const getUserList = (page = 1, size = 10, keyword = '', role = '') => {
  let url = `/api/admin/users?page=${page}&size=${size}`
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`
  if (role) url += `&role=${encodeURIComponent(role)}`
  return api.get(url)
}

export const getUserDetail = (id) => api.get(`/api/admin/users/${id}`)
export const createUser = (data) => api.post('/api/admin/users', data)
export const updateUser = (id, data) => api.put(`/api/admin/users/${id}`, data)
export const deleteUser = (id) => api.delete(`/api/admin/users/${id}`)

// ===== Questions =====
export const getQuestionList = (page = 1, size = 10, category = '', difficulty = '', keyword = '') => {
  let url = `/api/admin/questions?page=${page}&size=${size}`
  if (category) url += `&category=${encodeURIComponent(category)}`
  if (difficulty !== '' && difficulty !== null) url += `&difficulty=${encodeURIComponent(difficulty)}`
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`
  return api.get(url)
}

export const getQuestionDetail = (id) => api.get(`/api/admin/questions/${id}`)
export const createQuestion = (data) => api.post('/api/admin/questions', data)
export const updateQuestion = (id, data) => api.put(`/api/admin/questions/${id}`, data)
export const deleteQuestion = (id) => api.delete(`/api/admin/questions/${id}`)
export const getQuestionCategoryList = () => api.get('/api/admin/question-categories')
export const createQuestionCategory = (data) => api.post('/api/admin/question-categories', data)
export const deleteQuestionCategory = (code) => api.delete(`/api/admin/question-categories/${encodeURIComponent(code)}`)
export const getCustomQuestionBankList = (page = 1, size = 10, status = '', keyword = '') => {
  let url = `/api/admin/question-banks?page=${page}&size=${size}`
  if (status !== '' && status !== null) url += `&status=${encodeURIComponent(status)}`
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`
  return api.get(url)
}
export const auditCustomQuestionBank = (id, data) => api.put(`/api/admin/question-banks/${id}/audit`, data)
export const importOfficialQuestionBank = (formData) => api.upload('/api/admin/question-banks/official/import', formData)

// ===== Notes =====
export const getNoteList = (page = 1, size = 10, keyword = '', category = '', status = '') => {
  let url = `/api/admin/notes?page=${page}&size=${size}`
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`
  if (category) url += `&category=${encodeURIComponent(category)}`
  if (status !== '' && status !== null) url += `&status=${encodeURIComponent(status)}`
  return api.get(url)
}

export const getNoteDetail = (id) => api.get(`/api/admin/notes/${id}`)
export const createNote = (data) => api.post('/api/admin/notes', data)
export const updateNote = (id, data) => api.put(`/api/admin/notes/${id}`, data)
export const deleteNote = (id) => api.delete(`/api/admin/notes/${id}`)
export const auditNoteStatus = (id, status, reason = '') => {
  let url = `/api/admin/notes/${id}/status?status=${encodeURIComponent(status)}`
  if (reason) {
    url += `&reason=${encodeURIComponent(reason)}`
  }
  return api.put(url, {
    status,
    rejectReason: status === 2 ? reason : '',
  })
}

// ===== Wishes =====
export const getWishList = (page = 1, size = 10, status = '') => {
  let url = `/api/admin/wishes?page=${page}&size=${size}`
  if (status !== '' && status !== null) url += `&status=${encodeURIComponent(status)}`
  return api.get(url)
}

export const getWishDetail = (id) => api.get(`/api/admin/wishes/${id}`)
export const createWish = (data) => api.post('/api/admin/wishes', data)
export const updateWish = (id, data) => api.put(`/api/admin/wishes/${id}`, data)
export const deleteWish = (id) => api.delete(`/api/admin/wishes/${id}`)
export const updateWishStatus = (id, status) => api.put(`/api/admin/wishes/${id}/status?status=${status}`)
export const getWishQueueStats = () => api.get('/api/admin/wishes/queue/stats')
export const getDeadLetterWishes = (size = 20) => api.get(`/api/admin/wishes/dead-letter?size=${size}`)
export const retryWishReview = (id) => api.post(`/api/admin/wishes/${id}/retry`)

// ===== Auth =====
export const adminLogin = (data) => api.post('/api/admin/auth/login', data)
