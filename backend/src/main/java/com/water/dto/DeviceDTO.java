package com.water.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备 DTO
 */
@Data
public class DeviceDTO {
    private Long id;
    private String deviceNo;
    private String deviceName;
    private String deviceType;
    private String model;
    private String location;
    private String area;
    private String manufacturer;
    private String unit;
    private LocalDate installDate;
    private Integer status;
    private BigDecimal lon;
    private BigDecimal lat;
    private String remark;
}