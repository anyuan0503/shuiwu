package com.water.service;

import com.water.dto.LoginReq;
import com.water.dto.LoginVO;
import com.water.vo.UserInfoVO;

/**
 * 认证服务
 */
public interface AuthService {

    LoginVO login(LoginReq req);

    UserInfoVO me();

    void logout();

    void changePassword(String oldPassword, String newPassword);
}