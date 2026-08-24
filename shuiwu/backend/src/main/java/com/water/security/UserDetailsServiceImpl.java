package com.water.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.water.entity.*;
import com.water.mapper.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户认证信息加载
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public UserDetailsServiceImpl(SysUserMapper userMapper,
                                  SysRoleMapper roleMapper,
                                  SysUserRoleMapper userRoleMapper,
                                  SysRoleMenuMapper roleMenuMapper,
                                  SysMenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<String> roles = List.of();
        Set<String> perms = Set.of();
        if (!roleIds.isEmpty()) {
            roles = roleMapper.selectBatchIds(roleIds).stream()
                    .map(SysRole::getRoleCode).distinct().collect(Collectors.toList());
            List<Long> menuIds = roleMenuMapper.selectList(
                            new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds)).stream()
                    .map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
            if (!menuIds.isEmpty()) {
                perms = menuMapper.selectBatchIds(menuIds).stream()
                        .map(SysMenu::getPerm)
                        .filter(p -> p != null && !p.isEmpty())
                        .collect(Collectors.toSet());
            }
        }
        return new LoginUser(user, roles, perms);
    }
}