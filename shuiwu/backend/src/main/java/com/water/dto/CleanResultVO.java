package com.water.dto;

import lombok.Data;

import java.util.Map;

/**
 * 数据清洗结果
 */
@Data
public class CleanResultVO {
    private Long cleaned;
    private Long repaired;
    private Long removed;
    private Map<String, Object> detail;
}