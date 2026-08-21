package com.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("alarm")
public class Alarm {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alarmNo;
    private Long deviceId;
    private Long ruleId;
    private String alarmType;
    private Integer alarmLevel;
    private String alarmDesc;
    private BigDecimal currentValue;
    private Integer alarmStatus;
    private LocalDateTime alarmTime;
    private String handleUser;
    private LocalDateTime handleTime;
    private String handleResult;
}