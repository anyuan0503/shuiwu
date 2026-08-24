package com.water.job;

import com.water.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时模拟采集：每 5 秒生成若干设备监测数据并推送
 */
@Slf4j
@Component
public class MonitorDataSimJob {

    private final MonitorService monitorService;

    public MonitorDataSimJob(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(fixedDelay = 5000)
    public void simulate() {
        try {
            monitorService.simulateData();
        } catch (Exception e) {
            log.warn("模拟采集失败: {}", e.getMessage());
        }
    }
}