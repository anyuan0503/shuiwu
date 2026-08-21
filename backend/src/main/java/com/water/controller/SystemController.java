package com.water.controller;

import com.water.common.PageResult;
import com.water.common.Result;
import com.water.dto.UserDTO;
import com.water.entity.SysRole;
import com.water.entity.SysUser;
import com.water.service.SystemService;
import com.water.vo.MenuNodeVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/user/page")
    @PreAuthorize("hasAnyAuthority('ADMIN','sys:user:list')")
    public Result<PageResult<SysUser>> userPage(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long roleId) {
        return Result.ok(systemService.userPage(page, size, keyword, roleId));
    }

    @PostMapping("/user")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public Result<SysUser> createUser(@RequestBody UserDTO dto) {
        return Result.ok(systemService.createUser(dto));
    }

    @PutMapping("/user")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public Result<SysUser> updateUser(@RequestBody UserDTO dto) {
        return Result.ok(systemService.updateUser(dto));
    }

    @PutMapping("/user/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        systemService.updateUserStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.ok();
    }

    @GetMapping("/role/list")
    public Result<List<SysRole>> roleList() {
        return Result.ok(systemService.roleList());
    }

    @GetMapping("/menu/tree")
    public Result<List<MenuNodeVO>> menuTree() {
        return Result.ok(systemService.menuTree());
    }

    @GetMapping("/stat")
    public Result<Map<String, Object>> stat() {
        return Result.ok(systemService.stat());
    }
}