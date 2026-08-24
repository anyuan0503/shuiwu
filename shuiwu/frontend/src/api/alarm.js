import request from '@/utils/request'

export function pageAlarms(params) {
  return request.get('/alarm/page', { params })
}
export function listAlarmRules() {
  return request.get('/alarm/rule/list')
}
export function addAlarmRule(data) {
  return request.post('/alarm/rule', data)
}
export function updateAlarmRule(data) {
  return request.put('/alarm/rule', data)
}
export function deleteAlarmRule(id) {
  return request.delete(`/alarm/rule/${id}`)
}
export function handleAlarm(data) {
  return request.put('/alarm/handle', data)
}
export function ignoreAlarm(data) {
  return request.put('/alarm/ignore', data)
}
export function alarmSummary() {
  return request.get('/alarm/summary')
}
export function alarmTrend(params) {
  return request.get('/alarm/trend', { params })
}