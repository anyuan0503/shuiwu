package com.water.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.water.dto.AnomalyItemVO;
import com.water.dto.ChartDTO;
import com.water.dto.CleanResultVO;
import com.water.dto.NlSqlResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 微服务客户端，调用 http://127.0.0.1:8000
 * 所有方法在调用失败时返回降级结果而非抛出异常
 */
@Slf4j
@Component
public class AiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RuleCenter ruleCenter;

    @Value("${water.ai.base-url:http://127.0.0.1:8000}")
    private String baseUrl;

    @Value("${water.ai.timeout-seconds:5}")
    private long timeoutSeconds;

    public AiClient(ObjectMapper objectMapper, RuleCenter ruleCenter) {
        this.objectMapper = objectMapper;
        this.ruleCenter = ruleCenter;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                // 显式使用 HTTP/1.1：避免 JDK HttpClient 默认发起 h2c upgrade，
                // 导致 uvicorn(h11) 在升级握手时丢弃请求体，FastAPI 因此报 422 缺 body
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 自然语言查数
     */
    public String nlsql(String question, List<Map<String, Object>> tables, Map<String, Object> credentials) {
        try {
            Map<String, Object> body = Map.of(
                    "question", question == null ? "" : question,
                    "tables", tables,
                    "credentials", credentials);
            JsonNode resp = call("/ai/nlsql", body);
            if (resp != null && resp.path("success").asBoolean(false)) {
                NlSqlResultVO vo = new NlSqlResultVO();
                vo.setRawSql(resp.path("rawSql").asText(null));
                vo.setAnswer(resp.path("answer").asText(null));
                vo.setUsedEngine(resp.path("usedEngine").asText("rule"));
                if (resp.has("chartConfig")) {
                    vo.setChartConfig(objectMapper.convertValue(resp.get("chartConfig"), ChartDTO.class));
                }
                if (resp.has("tableData")) {
                    vo.setTableData(convertTableData(resp.get("tableData")));
                }
                return objectMapper.writeValueAsString(vo);
            }
            throw new IllegalStateException("AI 服务返回失败或为空");
        } catch (Exception e) {
            log.warn("AI nlsql 调用失败，降级: {}", e.getMessage());
        }
        return ruleCenter.fallback(question);
    }

    /**
     * 数据清洗，rows 为待清洗的监测数值序列
     */
    public String clean(Long deviceId, List<Object> rows) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("deviceId", deviceId);
            body.put("field", "pressure");
            body.put("rows", rows == null ? new ArrayList<>() : rows);
            JsonNode resp = call("/ai/clean", body);
            // FastAPI /ai/clean 返回 {cleaned,repaired,removed,detail}，无 success 字段
            if (resp != null && resp.has("cleaned")) {
                return objectMapper.writeValueAsString(resp);
            }
            throw new IllegalStateException("AI clean 调用失败");
        } catch (Exception e) {
            log.warn("AI clean 调用失败，降级: {}", e.getMessage());
            CleanResultVO vo = new CleanResultVO();
            vo.setCleaned(0L);
            vo.setRepaired(0L);
            vo.setRemoved(0L);
            try {
                return objectMapper.writeValueAsString(vo);
            } catch (Exception ignore) {
                return "{}";
            }
        }
    }

    /**
     * 异常分析，series 为按设备/指标组织的时间序列 data:[[ts,value],...]
     */
    public String anomaly(Long deviceId, int topN, List<Map<String, Object>> series) {
        try {
            // 用 HashMap 以支持 deviceId 可空（Map.of 不允许 null 值）
            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("deviceId", deviceId);
            body.put("topN", topN);
            body.put("series", series == null ? new ArrayList<>() : series);
            JsonNode resp = call("/ai/anomaly", body);
            // FastAPI /ai/anomaly 返回 {anomalies:[...]}，无 success 字段
            if (resp != null && resp.has("anomalies")) {
                return objectMapper.writeValueAsString(resp);
            }
            throw new IllegalStateException("AI anomaly 调用失败");
        } catch (Exception e) {
            log.warn("AI anomaly 调用失败，降级: {}", e.getMessage());
        }
        return staticAnomaly(deviceId, topN);
    }

    /**
     * 将 AI 返回的 tableData 转成列表。AI 可能返回数组、单个对象或 null，
     * 这里做归一化，避免 Jackson 对 JsonNode+List.class 的转换歧义。
     */
    private List<Map<String, Object>> convertTableData(JsonNode node) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (node == null) {
                return result;
            }
            if (node.isArray()) {
                node.forEach(n -> result.add(objectMapper.convertValue(n, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                })));
            } else if (node.isObject()) {
                result.add(objectMapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                }));
            } else {
                Map<String, Object> single = new java.util.HashMap<>();
                single.put("value", objectMapper.convertValue(node, Object.class));
                result.add(single);
            }
        } catch (Exception e) {
            log.warn("AI tableData 转换失败: {}", e.getMessage());
        }
        return result;
    }

    private String staticAnomaly(Long deviceId, int topN) {
        List<AnomalyItemVO> items = new ArrayList<>();
        AnomalyItemVO a = new AnomalyItemVO();
        a.setDeviceName("AI微服务离线");
        a.setField("pressure");
        a.setStart("--");
        a.setEnd("--");
        a.setScore(0.0);
        a.setDesc("AI 微服务(127.0.0.1:8000)未启动，当前返回静态说明。请启动 AI 服务后进行真实异常分析。");
        items.add(a);
        try {
            return objectMapper.writeValueAsString(Map.of("anomalies", items));
        } catch (Exception e) {
            return "{\"anomalies\":[]}";
        }
    }

    private JsonNode call(String path, Object body) throws Exception {
        String url = baseUrl + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("AI 服务返回 HTTP {}", response.statusCode());
            log.warn("AI 请求体: {}", objectMapper.writeValueAsString(body));
            log.warn("AI 响应体: {}", response.body());
            throw new IllegalStateException("AI 服务错误 " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }
}