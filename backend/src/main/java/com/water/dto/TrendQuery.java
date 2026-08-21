package com.water.dto;

import lombok.Data;

/**
 * 趋势查询参数
 */
@Data
public class TrendQuery {
    private Long deviceId;
    private String type;
    private String startTime;
    private String endTime;
    private Integer pointCount;
}