package com.water.job;

import com.water.timeshard.TableRouter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 时间分表初始化任务：启动时及每月 1 号创建当月/下月分表
 */
@Slf4j
@Component
public class TimeShardInitJob {

    private final JdbcTemplate jdbcTemplate;

    public TimeShardInitJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            LocalDateTime now = LocalDateTime.now();
            createTableIfAbsent(TableRouter.resolve(now));
            createTableIfAbsent(TableRouter.resolve(now.plusMonths(1)));
            log.info("时序分表初始化完成");
        } catch (Exception e) {
            log.warn("时序分表初始化失败(请确认数据库已建表): {}", e.getMessage());
        }
    }

    /** 每月 1 号凌晨创建下月分表 */
    @Scheduled(cron = "0 10 2 1 * ?")
    public void createNextMonth() {
        try {
            LocalDateTime next = LocalDateTime.now().plusMonths(1);
            createTableIfAbsent(TableRouter.resolve(next));
            log.info("已创建下月分表 {}", TableRouter.resolve(next));
        } catch (Exception e) {
            log.warn("创建下月分表失败: {}", e.getMessage());
        }
    }

    private void createTableIfAbsent(String tableName) {
        String table = TableRouter.check(tableName);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class, table);
        if (count != null && count > 0) {
            return;
        }
        String ddl = "CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                " id BIGINT NOT NULL AUTO_INCREMENT," +
                " device_id BIGINT NOT NULL," +
                " data_time DATETIME NOT NULL," +
                " pressure DECIMAL(12,4) DEFAULT NULL," +
                " flow DECIMAL(12,4) DEFAULT NULL," +
                " ph DECIMAL(12,4) DEFAULT NULL," +
                " turbidity DECIMAL(12,4) DEFAULT NULL," +
                " residual_cl DECIMAL(12,4) DEFAULT NULL," +
                " temperature DECIMAL(12,4) DEFAULT NULL," +
                " level DECIMAL(12,4) DEFAULT NULL," +
                " is_clean TINYINT NOT NULL DEFAULT 1," +
                " create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                " PRIMARY KEY (id)," +
                " KEY idx_device_time (device_id, data_time)" +
                ") ENGINE=InnoDB COMMENT='监测数据月分表'";
        jdbcTemplate.execute(ddl);
    }
}