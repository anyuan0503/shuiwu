package com.water.service;

import com.water.common.PageResult;
import com.water.entity.AiLog;

import java.util.Map;

public interface AiService {

    Map<String, Object> nlsql(String question);

    PageResult<AiLog> logPage(int page, int size, String logType, String keyword);

    Map<String, Object> clean(Long deviceId);

    Map<String, Object> anomaly(Long deviceId, Integer topN);
}