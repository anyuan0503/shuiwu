package com.water.dto;

import lombok.Data;

/**
 * 异常分析结果项
 */
@Data
public class AnomalyItemVO {
    private String deviceName;
    private String field;
    private String start;
    private String end;
    private Double score;
    private String desc;
}