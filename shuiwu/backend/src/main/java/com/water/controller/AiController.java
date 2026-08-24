package com.water.controller;

import com.water.common.PageResult;
import com.water.common.Result;
import com.water.dto.NlSqlReq;
import com.water.entity.AiLog;
import com.water.service.AiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/nlsql")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','ai:analyze')")
    public Result<Map<String, Object>> nlsql(@RequestBody NlSqlReq req) {
        return Result.ok(aiService.nlsql(req.getQuestion()));
    }

    @GetMapping("/log/page")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','ai:log')")
    public Result<PageResult<AiLog>> logPage(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String logType,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(aiService.logPage(page, size, logType, keyword));
    }

    @PostMapping("/clean")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<Map<String, Object>> clean(@RequestBody(required = false) Map<String, Long> body) {
        return Result.ok(aiService.clean(body == null ? null : body.get("deviceId")));
    }

    @GetMapping("/anomaly")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<Map<String, Object>> anomaly(@RequestParam(required = false) Long deviceId,
                                               @RequestParam(required = false) Integer topN) {
        return Result.ok(aiService.anomaly(deviceId, topN));
    }
}