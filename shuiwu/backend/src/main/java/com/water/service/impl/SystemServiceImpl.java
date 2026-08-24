package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.water.common.BizException;
import com.water.common.PageResult;
import com.water.common.constants.CommonConstants;
import com.water.dto.UserDTO;
import com.water.entity.*;
import com.water.mapper.*;
import com.water.service.SystemService;
import com.water.util.MenuTreeBuilder;
import com.water.vo.MenuNodeVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemServiceImpl implements SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final DeviceMapper deviceMapper;
    private final AlarmMapper alarmMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemServiceImpl(SysUserMapper userMapper,
                             SysRoleMapper roleMapper,
                             SysUserRoleMapper userRoleMapper,
                             SysMenuMapper menuMapper,
                             DeviceMapper deviceMapper,
                             AlarmMapper alarmMapper,
                             PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuMapper = menuMapper;
        this.deviceMapper = deviceMapper;
        this.alarmMapper = alarmMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUser> userPage(int page, int size, String keyword, Long roleId) {
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .and(StrUtil.isNotBlank(keyword), w -> w.like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getRealName, keyword))
                        .orderByAsc(SysUser::getId));
        p.getRecords().forEach(u -> u.setPassword(null));
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Transactional
    public SysUser createUser(UserDTO dto) {
        validateUser(dto);
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(StrUtil.isBlank(dto.getPassword()) ? "123456" : dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userMapper.insert(user);
        bindRoles(user.getId(), dto.getRoleIds());
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public SysUser updateUser(UserDTO dto) {
        if (dto.getId() == null || userMapper.selectById(dto.getId()) == null) {
            throw new BizException("用户不存在");
        }
        SysUser user = new SysUser();
        user.setId(dto.getId());
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, dto.getId()));
            bindRoles(dto.getId(), dto.getRoleIds());
        }
        SysUser result = userMapper.selectById(dto.getId());
        result.setPassword(null);
        return result;
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Override
    public List<SysRole> roleList() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Override
    public List<MenuNodeVO> menuTree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder));
        return MenuTreeBuilder.build(all, null);
    }

    @Override
    public Map<String, Object> stat() {
        long userCount = userMapper.selectCount(null);
        long deviceCount = deviceMapper.selectCount(null);
        List<Device> devices = deviceMapper.selectList(null);
        long online = devices.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1).count();
        double onlineRatio = deviceCount == 0 ? 0 : Math.round(online * 100.0 / deviceCount * 100) / 100.0;
        long alarmUnhandled = alarmMapper.selectCount(new LambdaQueryWrapper<Alarm>()
                .eq(Alarm::getAlarmStatus, CommonConstants.ALARM_UNHANDLED));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayAlarm = alarmMapper.selectCount(new LambdaQueryWrapper<Alarm>()
                .ge(Alarm::getAlarmTime, todayStart));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userCount);
        result.put("deviceCount", deviceCount);
        result.put("alarmUnhandled", alarmUnhandled);
        result.put("onlineRatio", onlineRatio);
        result.put("todayAlarm", todayAlarm);
        return result;
    }

    private void validateUser(UserDTO dto) {
        if (StrUtil.isBlank(dto.getUsername())) {
            throw new BizException(400, "用户名不能为空");
        }
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}