package com.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_log")
public class AiLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userName;
    private String logType;
    private String question;
    private String sqlText;
    private String answer;
    private String aiEngine;
    private Integer costMs;
    private Integer success;
    private LocalDateTime createTime;
}