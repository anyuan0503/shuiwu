package com.water.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("monitor_latest")
public class MonitorLatest {
    @TableId
    private Long deviceId;
    private String deviceName;
    private String deviceType;
    private BigDecimal pressure;
    private BigDecimal flow;
    private BigDecimal ph;
    private BigDecimal turbidity;
    private BigDecimal residualCl;
    private BigDecimal temperature;
    private BigDecimal level;
    private String qualityStatus;
    private LocalDateTime updateTime;
}