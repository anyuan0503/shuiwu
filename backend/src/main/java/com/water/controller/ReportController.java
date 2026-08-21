package com.water.controller;

import com.water.common.PageResult;
import com.water.common.Result;
import com.water.dto.GenerateReportReq;
import com.water.entity.Report;
import com.water.service.ReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','report:view')")
    public Result<PageResult<Report>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(reportService.page(page, size, keyword));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR')")
    public Result<Map<String, Object>> generate(@RequestBody GenerateReportReq req) {
        return Result.ok(reportService.generate(req));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','report:view')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Report report = reportService.getById(id);
        Path file = Paths.get(report.getFilePath());
        File f = file.toFile();
        if (!f.exists()) {
            File absolute = new File(String.valueOf(Paths.get(report.getFilePath()).toAbsolutePath()));
            f = absolute;
        }
        if (!f.exists()) {
            throw new com.water.common.BizException("报表文件不存在");
        }
        Resource resource = new FileSystemResource(f);
        String name = URLEncoder.encode(report.getReportName() + ".csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(reportService.summary());
    }
}