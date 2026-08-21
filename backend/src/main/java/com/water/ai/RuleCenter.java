package com.water.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 离线规则引擎，NL2SQL 降级兜底
 * 根据自然语言关键词返回可用的模拟 SQL 与表格数据，保证演示离线可用
 */
@Component
public class RuleCenter {

    private final ObjectMapper objectMapper;

    public RuleCenter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String fallback(String question) {
        String q = question == null ? "" : question;
        String sql;
        String answer;
        List<Map<String, Object>> rows = new ArrayList<>();
        String chartType = "line";
        String title = "数据结果";

        if (containsAny(q, "压力", "pressure")) {
            sql = "SELECT device_name, AVG(pressure) AS avg_pressure FROM device d JOIN monitor_data_202608 m ON d.id=m.device_id GROUP BY device_name";
            answer = "各设备平均压力（MPa）统计结果如下：";
            chartType = "bar";
            title = "各设备平均压力";
            rows.add(row("device_name", "泵站1压力计", "avg_pressure", 0.82));
            rows.add(row("device_name", "泵站2压力计", "avg_pressure", 0.67));
            rows.add(row("device_name", "二供压力计", "avg_pressure", 0.45));
        } else if (containsAny(q, "流量", "flow")) {
            sql = "SELECT device_name, SUM(flow) AS total_flow FROM device d JOIN monitor_data_202608 m ON d.id=m.device_id GROUP BY device_name";
            answer = "各设备累计流量（m³/h）：";
            chartType = "bar";
            title = "各设备累计流量";
            rows.add(row("device_name", "出厂流量计", "total_flow", 3820.5));
            rows.add(row("device_name", "泵站1流量计", "total_flow", 2154.2));
        } else if (containsAny(q, "ph", "pH", "水质")) {
            sql = "SELECT device_name, AVG(ph) AS avg_ph FROM device d JOIN monitor_data_202608 m ON d.id=m.device_id GROUP BY device_name";
            answer = "各点位平均 pH 值：";
            title = "各设备平均pH";
            rows.add(row("device_name", "水质监测仪-泵站1", "avg_ph", 7.21));
            rows.add(row("device_name", "水质监测仪-水厂", "avg_ph", 7.45));
        } else if (containsAny(q, "告警", "报警", "alarm")) {
            sql = "SELECT alarm_level, COUNT(*) AS cnt FROM alarm WHERE DATE(alarm_time)=CURDATE() GROUP BY alarm_level";
            answer = "今日各级告警数量统计：";
            chartType = "pie";
            title = "今日告警分布";
            rows.add(row("alarm_level", "1-提示", "cnt", 3));
            rows.add(row("alarm_level", "2-警告", "cnt", 2));
            rows.add(row("alarm_level", "3-严重", "cnt", 1));
        } else if (containsAny(q, "浊度", "turbidity")) {
            sql = "SELECT device_name, AVG(turbidity) AS avg_turbidity FROM device d JOIN monitor_data_202608 m ON d.id=m.device_id GROUP BY device_name";
            answer = "各点位平均浊度（NTU）：";
            title = "各设备平均浊度";
            rows.add(row("device_name", "水质监测仪-水厂", "avg_turbidity", 0.68));
            rows.add(row("device_name", "水质监测仪-泵站2", "avg_turbidity", 1.23));
        } else {
            sql = "SELECT device_id, COUNT(*) AS cnt FROM monitor_data_202608 GROUP BY device_id";
            answer = "当前时间范围监测数据抽样结果如下（规则引擎降级）：";
            title = "监测数据概况";
            rows.add(row("device_id", 1, "cnt", 120));
            rows.add(row("device_id", 2, "cnt", 118));
            rows.add(row("device_id", 3, "cnt", 116));
        }

        Map<String, Object> chartConfig = new LinkedHashMap<>();
        chartConfig.put("chartType", chartType);
        chartConfig.put("title", title);
        chartConfig.put("x", "label");
        chartConfig.put("y", "value");
        chartConfig.put("series", List.of("value"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawSql", sql);
        result.put("answer", answer);
        result.put("usedEngine", "rule");
        result.put("chartConfig", chartConfig);
        result.put("tableData", rows);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean containsAny(String q, String... keys) {
        for (String k : keys) {
            if (q.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}