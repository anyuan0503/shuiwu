package com.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}