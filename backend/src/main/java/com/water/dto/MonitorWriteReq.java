package com.water.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 监测数据写入请求
 */
@Data
public class MonitorWriteReq {
    private Long deviceId;
    private LocalDateTime dataTime;
    private BigDecimal pressure;
    private BigDecimal flow;
    private BigDecimal ph;
    private BigDecimal turbidity;
    private BigDecimal residualCl;
    private BigDecimal temperature;
    private BigDecimal level;
}