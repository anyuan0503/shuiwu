package com.water.dto;

import lombok.Data;

@Data
public class GenerateReportReq {
    private String reportType;
    private Long deviceId;
    private String startTime;
    private String endTime;
}