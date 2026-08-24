package com.water.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlarmRuleDTO {
    private Long id;
    private String ruleName;
    private Long deviceId;
    private String monitorField;
    private String alarmType;
    private Integer alarmLevel;
    private BigDecimal thresholdMin;
    private BigDecimal thresholdMax;
    private Integer windowMinutes;
    private Integer enabled;
    private String remark;
}