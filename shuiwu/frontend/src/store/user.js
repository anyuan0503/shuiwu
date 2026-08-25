import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, getMe } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('sw_token') || '',
    user: null, // {id,username,realName,roleCode,roleName}
    menus: [] // 后端返回的菜单树
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    realName: (s) => (s.user && (s.user.realName || s.user.username)) || '用户',
    roleCode: (s) => (s.user && s.user.roleCode) || '',
    roleName: (s) => (s.user && s.user.roleName) || ''
  },
  actions: {
    async login(payload) {
      const res = await apiLogin(payload)
      const data = res.data || {}
      this.token = data.token
      this.user = data.user || null
      this.menus = data.menus || []
      localStorage.setItem('sw_token', this.token)
      return data
    },
    async fetchMe() {
      try {
        const res = await getMe()
        this.user = res.data || null
        // 刷新页面后恢复动态路由菜单
        this.menus = (res.data && res.data.menus) || []
      } catch (e) {
        /* ignore */
      }
    },
    async logout() {
      try {
        await apiLogout()
      } catch (e) {
        /* ignore */
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('sw_token')
    }
  }
})