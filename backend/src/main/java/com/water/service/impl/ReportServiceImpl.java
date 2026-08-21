package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.water.common.BizException;
import com.water.common.PageResult;
import com.water.dto.GenerateReportReq;
import com.water.entity.Device;
import com.water.entity.Report;
import com.water.mapper.DeviceMapper;
import com.water.mapper.MonitorMapper;
import com.water.mapper.ReportMapper;
import com.water.service.ReportService;
import com.water.timeshard.TableRouter;
import com.water.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportMapper reportMapper;
    private final MonitorMapper monitorMapper;
    private final DeviceMapper deviceMapper;

    @Value("${water.reports-dir:./reports}")
    private String reportsDir;

    public ReportServiceImpl(ReportMapper reportMapper, MonitorMapper monitorMapper, DeviceMapper deviceMapper) {
        this.reportMapper = reportMapper;
        this.monitorMapper = monitorMapper;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public PageResult<Report> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), Report::getReportName, keyword)
                .orderByDesc(Report::getCreateTime);
        Page<Report> p = reportMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> generate(GenerateReportReq req) {
        if (req.getDeviceId() == null) {
            throw new BizException(400, "设备不能为空");
        }
        Device device = deviceMapper.selectById(req.getDeviceId());
        if (device == null) {
            throw new BizException("设备不存在");
        }
        LocalDateTime start = StrUtil.isBlank(req.getStartTime())
                ? LocalDateTime.now().minusDays(1) : LocalDateTime.parse(req.getStartTime(), DATETIME_FMT);
        LocalDateTime end = StrUtil.isBlank(req.getEndTime())
                ? LocalDateTime.now() : LocalDateTime.parse(req.getEndTime(), DATETIME_FMT);
        String type = StrUtil.isBlank(req.getReportType()) ? "custom" : req.getReportType();

        List<Map<String, Object>> rows = queryRows(device.getId(), start, end);

        Path dir = Paths.get(reportsDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BizException("报表目录创建失败: " + e.getMessage());
        }
        String fileName = "report_" + device.getId() + "_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        Path file = dir.resolve(fileName);
        writeCsv(file, rows);

        Report report = new Report();
        report.setReportName(device.getDeviceName() + "-" + type + "报表");
        report.setReportType(type);
        report.setDeviceId(device.getId());
        report.setStartTime(start);
        report.setEndTime(end);
        report.setFilePath("./reports/" + fileName);
        report.setSummary("共导出 " + rows.size() + " 条监测数据");
        report.setCreateUser(SecurityUtil.getUsername());
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);

        return Map.of("id", report.getId(), "filePath", report.getFilePath());
    }

    @Override
    public Report getById(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException("报表不存在: " + id);
        }
        return report;
    }

    @Override
    public Map<String, Object> summary() {
        long total = reportMapper.selectCount(null);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", total);
        return result;
    }

    private List<Map<String, Object>> queryRows(Long deviceId, LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> tables = monthTablesBetween(start, end);
        for (String table : tables) {
            if (monitorMapper.existsTable(TableRouter.check(table)) == null
                    || monitorMapper.existsTable(TableRouter.check(table)) == 0) {
                continue;
            }
            rows.addAll(monitorMapper.selectRows(TableRouter.check(table), deviceId, start, end));
        }
        rows.sort((a, b) -> String.valueOf(a.get("data_time")).compareTo(String.valueOf(b.get("data_time"))));
        return rows;
    }

    private List<String> monthTablesBetween(LocalDateTime start, LocalDateTime end) {
        List<String> tables = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.of(start.getYear(), start.getMonth(), 1, 0, 0);
        LocalDateTime endMonth = LocalDateTime.of(end.getYear(), end.getMonth(), 1, 0, 0);
        while (!cursor.isAfter(endMonth)) {
            tables.add(TableRouter.resolve(cursor));
            cursor = cursor.plusMonths(1);
        }
        return tables;
    }

    private void writeCsv(Path file, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("时间,压力,流量,pH,浊度,余氯,温度,液位\n");
        for (Map<String, Object> r : rows) {
            sb.append(str(r.get("data_time"))).append(',')
                    .append(str(r.get("pressure"))).append(',')
                    .append(str(r.get("flow"))).append(',')
                    .append(str(r.get("ph"))).append(',')
                    .append(str(r.get("turbidity"))).append(',')
                    .append(str(r.get("residual_cl"))).append(',')
                    .append(str(r.get("temperature"))).append(',')
                    .append(str(r.get("level"))).append('\n');
        }
        try {
            Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BizException("报表写入失败: " + e.getMessage());
        }
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}