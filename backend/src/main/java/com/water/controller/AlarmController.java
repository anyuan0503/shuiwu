package com.water.controller;

import com.water.common.PageResult;
import com.water.common.Result;
import com.water.dto.AlarmHandleReq;
import com.water.dto.AlarmRuleDTO;
import com.water.entity.Alarm;
import com.water.entity.AlarmRule;
import com.water.service.AlarmService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','alarm:list')")
    public Result<PageResult<Alarm>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer alarmLevel,
                                          @RequestParam(required = false) Integer alarmStatus,
                                          @RequestParam(required = false) Long deviceId,
                                          @RequestParam(required = false) String startTime,
                                          @RequestParam(required = false) String endTime) {
        return Result.ok(alarmService.page(page, size, alarmLevel, alarmStatus, deviceId, startTime, endTime));
    }

    @GetMapping("/rule/list")
    public Result<List<AlarmRule>> ruleList() {
        return Result.ok(alarmService.ruleList());
    }

    @PostMapping("/rule")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<AlarmRule> createRule(@RequestBody AlarmRuleDTO dto) {
        return Result.ok(alarmService.createRule(dto));
    }

    @PutMapping("/rule")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<AlarmRule> updateRule(@RequestBody AlarmRuleDTO dto) {
        return Result.ok(alarmService.updateRule(dto));
    }

    @DeleteMapping("/rule/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public Result<Void> deleteRule(@PathVariable Long id) {
        alarmService.deleteRule(id);
        return Result.ok();
    }

    @PutMapping("/handle")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<Void> handle(@RequestBody AlarmHandleReq req) {
        alarmService.handle(req);
        return Result.ok();
    }

    @PutMapping("/ignore")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<Void> ignore(@RequestBody Map<String, Long> body) {
        alarmService.ignore(body.get("id"));
        return Result.ok();
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(alarmService.summary());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(required = false) Integer days) {
        return Result.ok(alarmService.trend(days));
    }
}