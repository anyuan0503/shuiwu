package com.water.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 权限判断工具，配合 @PreAuthorize 使用
 */
@Component("ps")
public class PermissionService {

    /**
     * 判断当前用户是否拥有指定角色或权限标识
     */
    public boolean has(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (permission.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}