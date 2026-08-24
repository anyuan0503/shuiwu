package com.water.controller;

import com.water.common.Result;
import com.water.dto.LoginReq;
import com.water.dto.LoginVO;
import com.water.vo.UserInfoVO;
import com.water.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.me());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.getOrDefault("oldPassword", ""), body.getOrDefault("newPassword", ""));
        return Result.ok();
    }
}