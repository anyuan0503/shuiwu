import request from '@/utils/request'

export function pageDevices(params) {
  return request.get('/device/page', { params })
}
export function listDevices() {
  return request.get('/device/list')
}
export function getDevice(id) {
  return request.get(`/device/${id}`)
}
export function addDevice(data) {
  return request.post('/device', data)
}
export function updateDevice(data) {
  return request.put('/device', data)
}
export function deleteDevice(id) {
  return request.delete(`/device/${id}`)
}
export function onlineCount() {
  return request.get('/device/onlineCount')
}