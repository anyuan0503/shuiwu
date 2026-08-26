// 菜单路径解析：兼容后端返回的相对路径与绝对路径，
// 保证"菜单点击跳转"与"动态路由注册"使用完全一致的规则
export function resolveMenuPath(parent, child) {
  if (!child) return parent || ''
  if (child.startsWith('http')) return child
  if (child.startsWith('/')) return child
  const full = `${parent}/${child}`.replace(/\/+/g, '/')
  return full.startsWith('/') ? full : `/${full}`
}

// 将后端返回的扁平菜单构建为树：
// 无 component 的"目录"节点按 path 前缀收纳其叶子（如 /system -> /system/user），
// 使侧边栏能正确渲染子菜单，目录本身不再作为可点击菜单项导致 404
export function buildMenuTree(menus) {
  const items = (menus || []).map((m) => ({ ...m, children: [] }))
  const dirs = items.filter((m) => !m.component)
  const childIds = new Set()
  for (const dir of dirs) {
    for (const item of items) {
      if (item === dir) continue
      if (item.path && dir.path && item.path.startsWith(`${dir.path}/`)) {
        dir.children.push(item)
        childIds.add(item.id)
      }
    }
  }
  return items.filter((m) => !childIds.has(m.id))
}
