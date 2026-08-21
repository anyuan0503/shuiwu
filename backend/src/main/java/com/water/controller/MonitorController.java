package com.water.controller;

import com.water.common.Result;
import com.water.dto.MonitorWriteReq;
import com.water.dto.TrendQuery;
import com.water.service.MonitorService;
import com.water.vo.RealtimeVO;
import com.water.vo.TrendPointVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/realtime")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','monitor:view')")
    public Result<List<RealtimeVO>> realtime() {
        return Result.ok(monitorService.getRealtimeList());
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','monitor:view')")
    public Result<List<TrendPointVO>> trend(TrendQuery query) {
        return Result.ok(monitorService.trend(query.getDeviceId(), query.getType(),
                query.getStartTime(), query.getEndTime(), query.getPointCount()));
    }

    @PostMapping("/data")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','device:manage')")
    public Result<Void> data(@RequestBody MonitorWriteReq req) {
        monitorService.writeData(req);
        return Result.ok();
    }

    @GetMapping("/stat")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','monitor:view')")
    public Result<Map<String, Object>> stat() {
        return Result.ok(monitorService.stat());
    }
}