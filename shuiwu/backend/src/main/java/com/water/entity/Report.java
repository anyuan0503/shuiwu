package com.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportName;
    private String reportType;
    private Long deviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String filePath;
    private String summary;
    private String createUser;
    private LocalDateTime createTime;
}