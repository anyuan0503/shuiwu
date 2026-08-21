package com.water.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * NL2SQL 结果 VO
 */
@Data
public class NlSqlResultVO {
    private String rawSql;
    private String answer;
    private String usedEngine;
    private ChartDTO chartConfig;
    private List<Map<String, Object>> tableData;
}