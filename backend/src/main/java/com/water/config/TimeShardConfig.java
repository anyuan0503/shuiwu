package com.water.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 时间分表配置（可扩展，占位）
 */
@Configuration
public class TimeShardConfig {

    @Value("${water.shard.table-prefix:monitor_data_}")
    private String tablePrefix = "monitor_data_";

    public String getTablePrefix() {
        return tablePrefix;
    }
}