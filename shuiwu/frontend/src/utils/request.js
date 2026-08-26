import axios from 'axios'
import router from '@/router'

// 统一 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 20000
})

let loadingInstance = null
let loadingCount = 0

function showLoading() {
  loadingCount++
  if (loadingCount === 1 && !loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: false,
      text: '数据处理中...',
      background: 'rgba(6,10,26,0.6)',
      customClass: 'global-loading'
    })
  }
}
function hideLoading() {
  if (loadingCount > 0) loadingCount--
  if (loadingCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

// 请求拦截：附带 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sw_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 不拦截文件流下载
    if (config.silent !== true && config.responseType !== 'blob') {
      showLoading()
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：ResCode 处理
service.interceptors.response.use(
  (response) => {
    hideLoading()
    // 文件流
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res && res.code === 401) {
      localStorage.removeItem('sw_token')
      router.push('/login')
      return Promise.reject(new Error(res.message || '未认证'))
    }
    if (res && res.code !== 200) {
      if (response.config.showError !== false) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    hideLoading()
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('sw_token')
      router.push('/login')
    } else if (error.response && error.response.status === 403) {
      ElMessage.error('无操作权限')
    } else if (error.response && error.response.status === 404) {
      ElMessage.error('接口不存在')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service