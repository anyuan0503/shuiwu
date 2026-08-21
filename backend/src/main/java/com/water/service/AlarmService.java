package com.water.service;

import com.water.common.PageResult;
import com.water.dto.AlarmHandleReq;
import com.water.dto.AlarmRuleDTO;
import com.water.entity.Alarm;
import com.water.entity.AlarmRule;

import java.util.List;
import java.util.Map;

public interface AlarmService {

    PageResult<Alarm> page(int page, int size, Integer alarmLevel, Integer alarmStatus, Long deviceId,
                           String startTime, String endTime);

    List<AlarmRule> ruleList();

    AlarmRule createRule(AlarmRuleDTO dto);

    AlarmRule updateRule(AlarmRuleDTO dto);

    void deleteRule(Long id);

    void handle(AlarmHandleReq req);

    void ignore(Long id);

    Map<String, Object> summary();

    List<Map<String, Object>> trend(Integer days);
}