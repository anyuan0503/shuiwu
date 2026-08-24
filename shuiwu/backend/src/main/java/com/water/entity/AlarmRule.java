package com.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("alarm_rule")
public class AlarmRule {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
}