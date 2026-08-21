package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.water.ai.AiClient;
import com.water.common.BizException;
import com.water.common.PageResult;
import com.water.entity.AiLog;
import com.water.entity.Device;
import com.water.mapper.AiLogMapper;
import com.water.mapper.DeviceMapper;
import com.water.service.AiService;
import com.water.service.MonitorService;
import com.water.util.SecurityUtil;
import com.water.vo.TrendPointVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final AiClient aiClient;
    private final AiLogMapper aiLogMapper;
    private final DeviceMapper deviceMapper;
    private final ObjectMapper objectMapper;
    private final MonitorService monitorService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AiServiceImpl(AiClient aiClient, AiLogMapper aiLogMapper, DeviceMapper deviceMapper,
                         ObjectMapper objectMapper, MonitorService monitorService) {
        this.aiClient = aiClient;
        this.aiLogMapper = aiLogMapper;
        this.deviceMapper = deviceMapper;
        this.objectMapper = objectMapper;
        this.monitorService = monitorService;
    }

    @Override
    public Map<String, Object> nlsql(String question) {
        if (StrUtil.isBlank(question)) {
            throw new BizException(400, "问题不能为空");
        }
        long start = System.currentTimeMillis();
        List<Map<String, Object>> tables = buildTables();
        Map<String, Object> credentials = Map.of(
                "host", "127.0.0.1", "port", 3306,
                "user", "root", "password", "root", "database", "shuiwu");
        String json = aiClient.nlsql(question, tables, credentials);
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            result = Map.of("rawSql", "", "answer", "解析失败", "usedEngine", "rule", "tableData", new ArrayList<>());
        }
        // 写 AI 日志
        AiLog aiLog = new AiLog();
        aiLog.setUserId(SecurityUtil.getUserId());
        aiLog.setUserName(SecurityUtil.getUsername());
        aiLog.setLogType("nlsql");
        aiLog.setQuestion(question);
        aiLog.setSqlText(String.valueOf(result.getOrDefault("rawSql", "")));
        aiLog.setAnswer(String.valueOf(result.getOrDefault("answer", "")));
        aiLog.setAiEngine(String.valueOf(result.getOrDefault("usedEngine", "")));
        aiLog.setCostMs((int) (System.currentTimeMillis() - start));
        aiLog.setSuccess(1);
        aiLog.setCreateTime(LocalDateTime.now());
        aiLogMapper.insert(aiLog);
        return result;
    }

    @Override
    public PageResult<AiLog> logPage(int page, int size, String logType, String keyword) {
        LambdaQueryWrapper<AiLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(logType), AiLog::getLogType, logType)
                .and(StrUtil.isNotBlank(keyword), w -> w.like(AiLog::getQuestion, keyword).or().like(AiLog::getAiEngine, keyword))
                .orderByDesc(AiLog::getCreateTime);
        Page<AiLog> p = aiLogMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> clean(Long deviceId) {
        List<Object> rows = buildCleanRows(deviceId);
        String json = aiClient.clean(deviceId, rows);
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            result = Map.of("cleaned", 0, "repaired", 0, "removed", 0);
        }
        writeLog("clean", "数据清洗", result);
        return result;
    }

    @Override
    public Map<String, Object> anomaly(Long deviceId, Integer topN) {
        int n = topN == null ? 5 : topN;
        List<Map<String, Object>> series = buildSeries(deviceId);
        String json = aiClient.anomaly(deviceId, n, series);
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            result = Map.of("anomalies", new ArrayList<>());
        }
        writeLog("anomaly", "异常分析", result);
        return result;
    }

    /** 取近24小时压力序列作为清洗样本 */
    private List<Object> buildCleanRows(Long deviceId) {
        List<Object> rows = new ArrayList<>();
        if (deviceId != null) {
            collectFieldValues(deviceId, "pressure", rows);
        } else {
            for (Device d : deviceMapper.selectList(null)) {
                collectFieldValues(d.getId(), "pressure", rows);
                if (rows.size() >= 200) break;
            }
        }
        if (rows.isEmpty()) {
            // 无真实数据时的演示样本
            rows.add(0.82); rows.add(0.85); rows.add(9.99); rows.add(0.16);
            rows.add(0.81); rows.add(0.84); rows.add(0.005); rows.add(0.83);
        }
        return rows;
    }

    private void collectFieldValues(Long deviceId, String type, List<Object> rows) {
        try {
            String start = LocalDateTime.now().minusDays(1).format(FORMATTER);
            String end = LocalDateTime.now().format(FORMATTER);
            List<TrendPointVO> pts = monitorService.trend(deviceId, type, start, end, 80);
            for (TrendPointVO p : pts) {
                rows.add(p.getValue());
                if (rows.size() >= 200) return;
            }
        } catch (Exception ignored) {
        }
    }

    /** 为候选设备构建 pressure/flow/ph 时间序列供异常分析 */
    private List<Map<String, Object>> buildSeries(Long deviceId) {
        List<Map<String, Object>> series = new ArrayList<>();
        List<Device> devices;
        if (deviceId != null) {
            Device d = deviceMapper.selectById(deviceId);
            devices = d == null ? new ArrayList<>() : List.of(d);
        } else {
            devices = deviceMapper.selectList(null);
        }
        String start = LocalDateTime.now().minusDays(1).format(FORMATTER);
        String end = LocalDateTime.now().format(FORMATTER);
        for (Device d : devices) {
            for (String field : new String[]{"pressure", "flow", "ph"}) {
                List<Map<String, Object>> data = new ArrayList<>();
                try {
                    for (TrendPointVO p : monitorService.trend(d.getId(), field, start, end, 60)) {
                        if (p.getValue() == null) continue;
                        Map<String, Object> pt = new HashMap<>();
                        pt.put("ts", p.getTime());
                        pt.put("value", Double.parseDouble(String.valueOf(p.getValue())));
                        data.add(pt);
                    }
                } catch (Exception ignored) {
                }
                if (data.isEmpty()) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("deviceId", d.getId());
                item.put("deviceName", d.getDeviceName());
                item.put("field", field);
                item.put("data", data.stream()
                        .map(pt -> List.of(pt.get("ts"), pt.get("value"))).toList());
                series.add(item);
            }
        }
        return series;
    }

    private void writeLog(String type, String question, Map<String, Object> result) {
        try {
            AiLog aiLog = new AiLog();
            aiLog.setUserId(SecurityUtil.getUserId());
            aiLog.setUserName(SecurityUtil.getUsername());
            aiLog.setLogType(type);
            aiLog.setQuestion(question);
            aiLog.setAnswer(objectMapper.writeValueAsString(result));
            aiLog.setAiEngine("rule");
            aiLog.setSuccess(1);
            aiLog.setCreateTime(LocalDateTime.now());
            aiLogMapper.insert(aiLog);
        } catch (Exception e) {
            log.warn("写 AI 日志失败: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> buildTables() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(Map.of("name", "id", "desc", "主键"));
        fields.add(Map.of("name", "device_id", "desc", "设备ID"));
        fields.add(Map.of("name", "data_time", "desc", "时间"));
        fields.add(Map.of("name", "pressure", "desc", "压力"));
        fields.add(Map.of("name", "flow", "desc", "流量"));
        fields.add(Map.of("name", "ph", "desc", "pH"));
        fields.add(Map.of("name", "turbidity", "desc", "浊度"));
        fields.add(Map.of("name", "residual_cl", "desc", "余氯"));
        fields.add(Map.of("name", "temperature", "desc", "温度"));
        fields.add(Map.of("name", "level", "desc", "液位"));
        return List.of(Map.of("name", "monitor_data", "desc", "监测数据月分表", "fields", fields));
    }
}