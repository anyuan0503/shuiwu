package com.water.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.water.entity.Alarm;
import com.water.entity.AlarmRule;
import com.water.entity.Device;
import com.water.entity.MonitorLatest;
import com.water.mapper.AlarmMapper;
import com.water.mapper.AlarmRuleMapper;
import com.water.mapper.DeviceMapper;
import com.water.mapper.MonitorLatestMapper;
import com.water.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警扫描任务：每 10 秒扫描 monitor_latest 各字段，按告警规则判定阈值
 */
@Slf4j
@Component
public class AlarmCheckJob {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 字段名 -> monitor_latest 取值函数 */
    private static final Map<String, java.util.function.Function<MonitorLatest, BigDecimal>> FIELD_GETTER = fieldGetters();

    private final MonitorLatestMapper latestMapper;
    private final AlarmRuleMapper ruleMapper;
    private final AlarmMapper alarmMapper;
    private final DeviceMapper deviceMapper;
    private final WebSocketSessionManager sessionManager;

    public AlarmCheckJob(MonitorLatestMapper latestMapper,
                         AlarmRuleMapper ruleMapper,
                         AlarmMapper alarmMapper,
                         DeviceMapper deviceMapper,
                         WebSocketSessionManager sessionManager) {
        this.latestMapper = latestMapper;
        this.ruleMapper = ruleMapper;
        this.alarmMapper = alarmMapper;
        this.deviceMapper = deviceMapper;
        this.sessionManager = sessionManager;
    }

    private static Map<String, java.util.function.Function<MonitorLatest, BigDecimal>> fieldGetters() {
        Map<String, java.util.function.Function<MonitorLatest, BigDecimal>> m = new HashMap<>();
        m.put("pressure", MonitorLatest::getPressure);
        m.put("flow", MonitorLatest::getFlow);
        m.put("ph", MonitorLatest::getPh);
        m.put("turbidity", MonitorLatest::getTurbidity);
        m.put("residual_cl", MonitorLatest::getResidualCl);
        m.put("temperature", MonitorLatest::getTemperature);
        m.put("level", MonitorLatest::getLevel);
        return m;
    }

    @Scheduled(fixedDelay = 10000)
    public void scan() {
        try {
            List<AlarmRule> rules = ruleMapper.selectList(
                    new LambdaQueryWrapper<AlarmRule>().eq(AlarmRule::getEnabled, 1));
            if (rules.isEmpty()) {
                return;
            }
            List<MonitorLatest> latestList = latestMapper.selectList(null);
            for (AlarmRule rule : rules) {
                java.util.function.Function<MonitorLatest, BigDecimal> getter = FIELD_GETTER.get(rule.getMonitorField());
                if (getter == null) {
                    continue;
                }
                for (MonitorLatest m : latestList) {
                    // 全局规则(deviceId=null)对所有设备生效，否则匹配指定设备
                    if (rule.getDeviceId() != null && !rule.getDeviceId().equals(m.getDeviceId())) {
                        continue;
                    }
                    BigDecimal value = getter.apply(m);
                    if (value == null) {
                        continue;
                    }
                    checkRule(rule, m, value);
                }
            }
        } catch (Exception e) {
            log.warn("告警扫描失败: {}", e.getMessage());
        }
    }

    private void checkRule(AlarmRule rule, MonitorLatest latest, BigDecimal value) {
        boolean trigger = trigger(rule, value);
        if (!trigger) {
            return;
        }
        // 去重：同一规则+设备存在未完结告警则跳过
        Long existing = alarmMapper.selectCount(new LambdaQueryWrapper<Alarm>()
                .eq(Alarm::getRuleId, rule.getId())
                .eq(Alarm::getDeviceId, latest.getDeviceId())
                .in(Alarm::getAlarmStatus, List.of(0, 1))
                .ge(Alarm::getAlarmTime, LocalDateTime.now().minusMinutes(rule.getWindowMinutes() == null ? 5 : rule.getWindowMinutes())));
        if (existing != null && existing > 0) {
            return;
        }
        Device device = deviceMapper.selectById(latest.getDeviceId());
        Alarm alarm = new Alarm();
        alarm.setAlarmNo(genAlarmNo());
        alarm.setDeviceId(latest.getDeviceId());
        alarm.setRuleId(rule.getId());
        alarm.setAlarmType(rule.getAlarmType() == null ? "threshold" : rule.getAlarmType());
        alarm.setAlarmLevel(rule.getAlarmLevel() == null ? 1 : rule.getAlarmLevel());
        String desc = buildDesc(rule, value);
        alarm.setAlarmDesc(desc);
        alarm.setCurrentValue(value);
        alarm.setAlarmStatus(0);
        alarm.setAlarmTime(LocalDateTime.now());
        alarmMapper.insert(alarm);

        // 推送 WebSocket 告警
        Map<String, Object> data = new HashMap<>();
        data.put("id", alarm.getId());
        data.put("deviceName", device == null ? "未知设备" : device.getDeviceName());
        data.put("alarmLevel", alarm.getAlarmLevel());
        data.put("alarmDesc", desc);
        data.put("alarmTime", alarm.getAlarmTime().format(FMT));
        sessionManager.broadcast("alarm", data);
    }

    private boolean trigger(AlarmRule rule, BigDecimal value) {
        boolean trigger = false;
        if (rule.getThresholdMax() != null && value.compareTo(rule.getThresholdMax()) > 0) {
            trigger = true;
        }
        if (rule.getThresholdMin() != null && value.compareTo(rule.getThresholdMin()) < 0) {
            trigger = true;
        }
        return trigger;
    }

    private String buildDesc(AlarmRule rule, BigDecimal value) {
        if (rule.getThresholdMax() != null && value.compareTo(rule.getThresholdMax()) > 0) {
            return rule.getRuleName() + " 超上限(>" + rule.getThresholdMax() + ")，当前值 " + value;
        }
        if (rule.getThresholdMin() != null && value.compareTo(rule.getThresholdMin()) < 0) {
            return rule.getRuleName() + " 低于下限(<" + rule.getThresholdMin() + ")，当前值 " + value;
        }
        return rule.getRuleName() + " 阈值触发，当前值 " + value;
    }

    private String genAlarmNo() {
        return "ALM" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }
}