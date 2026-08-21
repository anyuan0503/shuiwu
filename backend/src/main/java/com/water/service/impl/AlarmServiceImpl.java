package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.water.common.BizException;
import com.water.common.PageResult;
import com.water.common.constants.CommonConstants;
import com.water.dto.AlarmHandleReq;
import com.water.dto.AlarmRuleDTO;
import com.water.entity.Alarm;
import com.water.entity.AlarmRule;
import com.water.mapper.AlarmMapper;
import com.water.mapper.AlarmRuleMapper;
import com.water.service.AlarmService;
import com.water.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlarmServiceImpl implements AlarmService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmMapper alarmMapper;
    private final AlarmRuleMapper ruleMapper;

    public AlarmServiceImpl(AlarmMapper alarmMapper, AlarmRuleMapper ruleMapper) {
        this.alarmMapper = alarmMapper;
        this.ruleMapper = ruleMapper;
    }

    @Override
    public PageResult<Alarm> page(int page, int size, Integer alarmLevel, Integer alarmStatus, Long deviceId,
                                  String startTime, String endTime) {
        LambdaQueryWrapper<Alarm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(alarmLevel != null, Alarm::getAlarmLevel, alarmLevel)
                .eq(alarmStatus != null, Alarm::getAlarmStatus, alarmStatus)
                .eq(deviceId != null, Alarm::getDeviceId, deviceId)
                .ge(StrUtil.isNotBlank(startTime), Alarm::getAlarmTime, startTime)
                .le(StrUtil.isNotBlank(endTime), Alarm::getAlarmTime, endTime)
                .orderByDesc(Alarm::getAlarmTime);
        Page<Alarm> p = alarmMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public List<AlarmRule> ruleList() {
        return ruleMapper.selectList(new LambdaQueryWrapper<AlarmRule>().orderByAsc(AlarmRule::getId));
    }

    @Override
    public AlarmRule createRule(AlarmRuleDTO dto) {
        if (StrUtil.isBlank(dto.getRuleName()) || StrUtil.isBlank(dto.getMonitorField())) {
            throw new BizException(400, "规则名称与监测字段不能为空");
        }
        AlarmRule rule = new AlarmRule();
        copy(dto, rule);
        if (rule.getEnabled() == null) rule.setEnabled(1);
        if (rule.getWindowMinutes() == null) rule.setWindowMinutes(5);
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    public AlarmRule updateRule(AlarmRuleDTO dto) {
        if (dto.getId() == null || ruleMapper.selectById(dto.getId()) == null) {
            throw new BizException("规则不存在");
        }
        AlarmRule rule = new AlarmRule();
        rule.setId(dto.getId());
        copy(dto, rule);
        ruleMapper.updateById(rule);
        return ruleMapper.selectById(dto.getId());
    }

    @Override
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public void handle(AlarmHandleReq req) {
        Alarm alarm = requireAlarm(req.getId());
        alarm.setAlarmStatus(StrUtil.isNotBlank(req.getHandleResult()) ? CommonConstants.ALARM_HANDLED : CommonConstants.ALARM_HANDLING);
        alarm.setHandleUser(SecurityUtil.getUsername());
        alarm.setHandleTime(LocalDateTime.now());
        alarm.setHandleResult(req.getHandleResult());
        alarmMapper.updateById(alarm);
    }

    @Override
    public void ignore(Long id) {
        Alarm alarm = requireAlarm(id);
        alarm.setAlarmStatus(CommonConstants.ALARM_IGNORED);
        alarm.setHandleUser(SecurityUtil.getUsername());
        alarm.setHandleTime(LocalDateTime.now());
        alarmMapper.updateById(alarm);
    }

    @Override
    public Map<String, Object> summary() {
        List<Alarm> all = alarmMapper.selectList(null);
        long total = all.size();
        long level1 = all.stream().filter(a -> a.getAlarmLevel() != null && a.getAlarmLevel() == 1).count();
        long level2 = all.stream().filter(a -> a.getAlarmLevel() != null && a.getAlarmLevel() == 2).count();
        long level3 = all.stream().filter(a -> a.getAlarmLevel() != null && a.getAlarmLevel() == 3).count();
        long unhandled = all.stream().filter(a -> a.getAlarmStatus() == CommonConstants.ALARM_UNHANDLED).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("level1", level1);
        result.put("level2", level2);
        result.put("level3", level3);
        result.put("unhandled", unhandled);
        return result;
    }

    @Override
    public List<Map<String, Object>> trend(Integer days) {
        int n = days == null || days <= 0 ? 7 : days;
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            List<Alarm> alarms = alarmMapper.selectList(new LambdaQueryWrapper<Alarm>()
                    .between(Alarm::getAlarmTime, start, end));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.format(DAY_FMT));
            row.put("count", alarms.size());
            row.put("level3", alarms.stream().filter(a -> a.getAlarmLevel() != null && a.getAlarmLevel() == 3).count());
            result.add(row);
        }
        return result;
    }

    private Alarm requireAlarm(Long id) {
        Alarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BizException("告警不存在: " + id);
        }
        return alarm;
    }

    private void copy(AlarmRuleDTO dto, AlarmRule rule) {
        if (dto.getRuleName() != null) rule.setRuleName(dto.getRuleName());
        if (dto.getDeviceId() != null) rule.setDeviceId(dto.getDeviceId());
        if (dto.getMonitorField() != null) rule.setMonitorField(dto.getMonitorField());
        if (dto.getAlarmType() != null) rule.setAlarmType(dto.getAlarmType());
        if (dto.getAlarmLevel() != null) rule.setAlarmLevel(dto.getAlarmLevel());
        if (dto.getThresholdMin() != null) rule.setThresholdMin(dto.getThresholdMin());
        if (dto.getThresholdMax() != null) rule.setThresholdMax(dto.getThresholdMax());
        if (dto.getWindowMinutes() != null) rule.setWindowMinutes(dto.getWindowMinutes());
        if (dto.getEnabled() != null) rule.setEnabled(dto.getEnabled());
        if (dto.getRemark() != null) rule.setRemark(dto.getRemark());
    }
}