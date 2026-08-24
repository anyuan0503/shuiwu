package com.water.dto;

import com.water.vo.MenuNodeVO;
import com.water.vo.UserInfoVO;
import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private UserInfoVO user;
    private List<MenuNodeVO> menus;
}