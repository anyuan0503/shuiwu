package com.water.dto;

import lombok.Data;

import java.util.List;

/**
 * 新建/更新用户 DTO
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private List<Long> roleIds;
    private Integer status;
}