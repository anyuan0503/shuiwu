import request from '@/utils/request'

export function realtime() {
  return request.get('/monitor/realtime')
}
export function trend(params) {
  return request.get('/monitor/trend', { params })
}
export function monitorStat() {
  return request.get('/monitor/stat')
}