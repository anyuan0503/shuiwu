package com.water.mapper;

import com.water.timeshard.MonitorRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 监测数据分表 Mapper
 * 表名通过 ${tableName} 动态拼接，方法内必须先经 TableRouter.check 白名单校验
 */
public interface MonitorMapper {

    /** 写入一条监测数据 */
    int insert(@Param("tableName") String tableName, @Param("rec") MonitorRecord rec);

    /** 校验物理表是否存在 */
    Integer existsTable(@Param("tableName") String tableName);

    /** 查询趋势原始数据（按时间升序） */
    List<Map<String, Object>> selectTrend(@Param("tableName") String tableName,
                                          @Param("deviceId") Long deviceId,
                                          @Param("field") String field,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /** 查询监测明细（报表导出用） */
    List<Map<String, Object>> selectRows(@Param("tableName") String tableName,
                                         @Param("deviceId") Long deviceId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

}