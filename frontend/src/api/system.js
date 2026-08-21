import request from '@/utils/request'

export function pageUsers(params) {
  return request.get('/system/user/page', { params })
}
export function addUser(data) {
  return request.post('/system/user', data)
}
export function updateUser(data) {
  return request.put('/system/user', data)
}
export function updateUserStatus(id, status) {
  return request.put(`/system/user/${id}/status`, { status })
}
export function deleteUser(id) {
  return request.delete(`/system/user/${id}`)
}
export function listRoles() {
  return request.get('/system/role/list')
}
export function menuTree() {
  return request.get('/system/menu/tree')
}
export function sysStat() {
  return request.get('/system/stat')
}