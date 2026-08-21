import request from '@/utils/request'

export function pageReports(params) {
  return request.get('/report/page', { params })
}
export function generateReport(data) {
  return request.post('/report/generate', data)
}
export function reportSummary() {
  return request.get('/report/summary')
}
export function downloadReport(id) {
  return request.get(`/report/${id}/download`, { responseType: 'blob' })
}