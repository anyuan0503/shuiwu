package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.water.common.BizException;
import com.water.common.constants.CommonConstants;
import com.water.dto.LoginReq;
import com.water.dto.LoginVO;
import com.water.vo.MenuNodeVO;
import com.water.vo.UserInfoVO;
import com.water.entity.*;
import com.water.mapper.*;
import com.water.security.JwtUtil;
import com.water.security.LoginUser;
import com.water.service.AuthService;
import com.water.util.MenuTreeBuilder;
import com.water.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder,
                           SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           SysRoleMenuMapper roleMenuMapper,
                           SysMenuMapper menuMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public LoginVO login(LoginReq req) {
        if (StrUtil.isBlank(req.getUsername()) || StrUtil.isBlank(req.getPassword())) {
            throw new BizException(400, "用户名或密码不能为空");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BizException(400, "用户名或密码错误");
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        List<String> roles = loginUser.getRoles();

        String token = jwtUtil.generateToken(loginUser.getUserId(), loginUser.getUsername(), roles);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(buildUserInfo(loginUser.getUser(), roles));
        vo.setMenus(buildMenus(loginUser.getUser().getId(), roles));
        return vo;
    }

    @Override
    public UserInfoVO me() {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        return buildUserInfo(loginUser.getUser(), loginUser.getRoles());
    }

    @Override
    public void logout() {
        // 无状态 JWT，服务端无需处理
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        LoginUser loginUser = SecurityUtil.getLoginUser();
        SysUser user = loginUser.getUser();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(400, "原密码错误");
        }
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BizException(400, "新密码长度不能小于6位");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
    }

    private UserInfoVO buildUserInfo(SysUser user, List<String> roles) {
        UserInfoVO info = new UserInfoVO();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        String roleCode = roles.isEmpty() ? "" : roles.get(0);
        info.setRoleCode(roleCode);
        if (!roles.isEmpty()) {
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
            info.setRoleName(role == null ? "" : role.getRoleName());
        }
        return info;
    }

    private List<MenuNodeVO> buildMenus(Long userId, List<String> roles) {
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder));
        // ADMIN 全量
        if (roles.contains(CommonConstants.ROLE_ADMIN)) {
            return MenuTreeBuilder.build(allMenus, null);
        }
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        return MenuTreeBuilder.build(allMenus, menuIds);
    }
}