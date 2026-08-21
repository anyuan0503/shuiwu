package com.water.vo;

import com.water.entity.MonitorLatest;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时监测值 VO
 */
@Data
public class RealtimeVO {
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

    public static RealtimeVO from(MonitorLatest m) {
        if (m == null) {
            return null;
        }
        RealtimeVO v = new RealtimeVO();
        v.setDeviceId(m.getDeviceId());
        v.setDeviceName(m.getDeviceName());
        v.setDeviceType(m.getDeviceType());
        v.setPressure(m.getPressure());
        v.setFlow(m.getFlow());
        v.setPh(m.getPh());
        v.setTurbidity(m.getTurbidity());
        v.setResidualCl(m.getResidualCl());
        v.setTemperature(m.getTemperature());
        v.setLevel(m.getLevel());
        v.setQualityStatus(m.getQualityStatus());
        v.setUpdateTime(m.getUpdateTime());
        return v;
    }
}