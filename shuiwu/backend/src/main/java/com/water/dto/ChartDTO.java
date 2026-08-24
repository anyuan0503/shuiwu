package com.water.dto;

import lombok.Data;

import java.util.List;

/**
 * 图表配置（chartConfig）供前端选择图表渲染
 */
@Data
public class ChartDTO {
    private String chartType;
    private String title;
    private String x;
    private String y;
    private List<Object> series;
}