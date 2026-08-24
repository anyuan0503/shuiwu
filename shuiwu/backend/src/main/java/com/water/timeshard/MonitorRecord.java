package com.water.timeshard;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 监测数据记录（对应 monitor_data_YYYYMM 分表，无固定实体映射）
 */
@Data
public class MonitorRecord {
    private Long id;
    private Long deviceId;
    private LocalDateTime dataTime;
    private BigDecimal pressure;
    private BigDecimal flow;
    private BigDecimal ph;
    private BigDecimal turbidity;
    private BigDecimal residualCl;
    private BigDecimal temperature;
    private BigDecimal level;
    private Integer isClean;
    private LocalDateTime createTime;
}