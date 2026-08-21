package com.water.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点 VO
 */
@Data
public class MenuNodeVO {
    private Long id;
    private String name;
    private String path;
    private String component;
    private String icon;
    private List<MenuNodeVO> children = new ArrayList<>();
}