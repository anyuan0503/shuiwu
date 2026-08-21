package com.water.timeshard;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 时序分表路由工具
 * 将 dataTime 路由到 monitor_data_YYYYMM 物理表
 */
public final class TableRouter {

    private static final String TABLE_PREFIX = "monitor_data_";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMM");
    /** 白名单正则，防 SQL 注入 */
    private static final Pattern SAFE_PATTERN = Pattern.compile("^monitor_data_\\d{6}$");

    private TableRouter() {
    }

    /**
     * 返回指定时间对应的物理表名
     */
    public static String resolve(LocalDateTime time) {
        String ym = time.format(FMT);
        return TABLE_PREFIX + ym;
    }

    /**
     * 返回当前月份的表名
     */
    public static String resolveNow() {
        return resolve(LocalDateTime.now());
    }

    /**
     * 校验表名是否合法（白名单），不合法抛出异常防止注入
     */
    public static String check(String tableName) {
        if (tableName == null || !SAFE_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法分表名: " + tableName);
        }
        return tableName;
    }
}