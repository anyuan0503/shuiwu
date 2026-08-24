package com.water.service;

import com.water.common.PageResult;
import com.water.dto.UserDTO;
import com.water.entity.SysRole;
import com.water.entity.SysUser;
import com.water.vo.MenuNodeVO;

import java.util.List;
import java.util.Map;

public interface SystemService {

    PageResult<SysUser> userPage(int page, int size, String keyword, Long roleId);

    SysUser createUser(UserDTO dto);

    SysUser updateUser(UserDTO dto);

    void updateUserStatus(Long id, Integer status);

    void deleteUser(Long id);

    List<SysRole> roleList();

    List<MenuNodeVO> menuTree();

    Map<String, Object> stat();
}