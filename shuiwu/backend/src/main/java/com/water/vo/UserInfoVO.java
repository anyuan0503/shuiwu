package com.water.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    // 刷新页面时用于恢复动态路由菜单
    private List<MenuNodeVO> menus;
}