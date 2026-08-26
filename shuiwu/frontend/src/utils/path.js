// 菜单路径解析：兼容后端返回的相对路径与绝对路径，
// 保证"菜单点击跳转"与"动态路由注册"使用完全一致的规则
export function resolveMenuPath(parent, child) {
  if (!child) return parent || ''
  if (child.startsWith('http')) return child
  if (child.startsWith('/')) return child
  const full = `${parent}/${child}`.replace(/\/+/g, '/')
  return full.startsWith('/') ? full : `/${full}`
}
