import request from '@/utils/request'

export function nlsql(data) {
  return request.post('/ai/nlsql', data, { showError: false })
}
export function pageAiLog(params) {
  return request.get('/ai/log/page', { params })
}
export function clean(data) {
  return request.post('/ai/clean', data)
}
export function anomaly(params) {
  return request.get('/ai/anomaly', { params })
}