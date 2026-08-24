import request from '@/utils/request'

// 登录 -> {token,user,menus}
export function login(data) {
  return request.post('/auth/login', data)
}
export function getMe() {
  return request.get('/auth/me')
}
export function logout() {
  return request.post('/auth/logout')
}
export function updatePassword(data) {
  return request.put('/auth/password', data)
}