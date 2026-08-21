package com.water.util;

import com.water.entity.SysMenu;
import com.water.vo.MenuNodeVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单树构建工具
 */
public final class MenuTreeBuilder {

    private MenuTreeBuilder() {
    }

    /**
     * @param menus       全部菜单
     * @param visibleMenuIds 可见菜单 id 集合（null 表示全量可见）
     */
    public static List<MenuNodeVO> build(List<SysMenu> menus, List<Long> visibleMenuIds) {
        List<SysMenu> filtered = menus.stream()
                .filter(m -> visibleMenuIds == null || visibleMenuIds.contains(m.getId()))
                .sorted(Comparator.comparing(SysMenu::getSortOrder))
                .toList();

        Map<Long, MenuNodeVO> nodeMap = filtered.stream().collect(Collectors.toMap(SysMenu::getId, m -> {
            MenuNodeVO vo = new MenuNodeVO();
            vo.setId(m.getId());
            vo.setName(m.getMenuName());
            vo.setPath(m.getPath());
            vo.setComponent(m.getComponent());
            vo.setIcon(m.getIcon());
            return vo;
        }));

        List<MenuNodeVO> roots = new ArrayList<>();
        for (SysMenu m : filtered) {
            MenuNodeVO vo = nodeMap.get(m.getId());
            Long parent = m.getParentId();
            if (parent != null && parent != 0L && nodeMap.containsKey(parent)) {
                nodeMap.get(parent).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }
}