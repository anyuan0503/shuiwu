package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.water.common.BizException;
import com.water.dto.MonitorWriteReq;
import com.water.entity.Device;
import com.water.entity.MonitorLatest;
import com.water.mapper.DeviceMapper;
import com.water.mapper.MonitorLatestMapper;
import com.water.mapper.MonitorMapper;
import com.water.service.MonitorService;
import com.water.timeshard.MonitorRecord;
import com.water.timeshard.TableRouter;
import com.water.vo.RealtimeVO;
import com.water.vo.TrendPointVO;
import com.water.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MonitorServiceImpl implements MonitorService {

    private static final List<String> FIELD_WHITELIST = List.of("pressure", "flow", "turbidity", "ph", "residual_cl", "level");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MonitorMapper monitorMapper;
    private final MonitorLatestMapper latestMapper;
    private final DeviceMapper deviceMapper;
    private final WebSocketSessionManager sessionManager;

    @Value("${water.reports-dir:./reports}")
    private String reportsDir;

    public MonitorServiceImpl(MonitorMapper monitorMapper,
                              MonitorLatestMapper latestMapper,
                              DeviceMapper deviceMapper,
                              WebSocketSessionManager sessionManager) {
        this.monitorMapper = monitorMapper;
        this.latestMapper = latestMapper;
        this.deviceMapper = deviceMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    public List<RealtimeVO> getRealtimeList() {
        return latestMapper.selectList(null).stream()
                .map(RealtimeVO::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrendPointVO> trend(Long deviceId, String type, String startTime, String endTime, Integer pointCount) {
        if (deviceId == null || StrUtil.isBlank(type)) {
            throw new BizException(400, "设备与指标类型不能为空");
        }
        String field = type.trim();
        if (!FIELD_WHITELIST.contains(field)) {
            throw new BizException(400, "不支持的指标类型: " + type);
        }
        LocalDateTime start = StrUtil.isBlank(startTime) ? LocalDateTime.now().minusDays(1) : LocalDateTime.parse(startTime, FMT);
        LocalDateTime end = StrUtil.isBlank(endTime) ? LocalDateTime.now() : LocalDateTime.parse(endTime, FMT);
        if (start.isAfter(end)) {
            throw new BizException(400, "开始时间不能晚于结束时间");
        }
        List<Map<String, Object>> points = new ArrayList<>();
        List<String> tableNames = monthTablesBetween(start, end);
        for (String table : tableNames) {
            if (monitorMapper.existsTable(TableRouter.check(table)) == null || monitorMapper.existsTable(TableRouter.check(table)) == 0) {
                // 表不存在，容错跳过
                continue;
            }
            List<Map<String, Object>> part = monitorMapper.selectTrend(TableRouter.check(table), deviceId, field, start, end);
            points.addAll(part);
        }
        points.sort((a, b) -> String.valueOf(a.get("time")).compareTo(String.valueOf(b.get("time"))));
        // 抽稀
        return downsample(points, pointCount);
    }

    @Override
    @Transactional
    public void writeData(MonitorWriteReq req) {
        if (req.getDeviceId() == null) {
            throw new BizException(400, "设备ID不能为空");
        }
        Device device = deviceMapper.selectById(req.getDeviceId());
        if (device == null) {
            throw new BizException("设备不存在");
        }
        LocalDateTime time = req.getDataTime() == null ? LocalDateTime.now() : req.getDataTime();
        // 写入分表
        MonitorRecord record = new MonitorRecord();
        record.setDeviceId(device.getId());
        record.setDataTime(time);
        record.setPressure(req.getPressure());
        record.setFlow(req.getFlow());
        record.setPh(req.getPh());
        record.setTurbidity(req.getTurbidity());
        record.setResidualCl(req.getResidualCl());
        record.setTemperature(req.getTemperature());
        record.setLevel(req.getLevel());
        record.setIsClean(1);
        String table = TableRouter.check(TableRouter.resolve(time));
        if (monitorMapper.existsTable(table) == null || monitorMapper.existsTable(table) == 0) {
            throw new BizException("数据表不存在: " + table + "，请先建表");
        }
        monitorMapper.insert(table, record);
        // 写入/更新最新值
        saveOrUpdateLatest(req);
        broadcastRealtime();
    }

    @Override
    public void saveOrUpdateLatest(MonitorWriteReq req) {
        MonitorLatest latest = latestMapper.selectById(req.getDeviceId());
        Device device = deviceMapper.selectById(req.getDeviceId());
        String name = device == null ? null : device.getDeviceName();
        String type = device == null ? null : device.getDeviceType();
        if (latest == null) {
            latest = new MonitorLatest();
            latest.setDeviceId(req.getDeviceId());
            latest.setDeviceName(name);
            latest.setDeviceType(type);
            latest.setPressure(req.getPressure());
            latest.setFlow(req.getFlow());
            latest.setPh(req.getPh());
            latest.setTurbidity(req.getTurbidity());
            latest.setResidualCl(req.getResidualCl());
            latest.setTemperature(req.getTemperature());
            latest.setLevel(req.getLevel());
            latest.setUpdateTime(LocalDateTime.now());
            latest.setQualityStatus(calcQuality(req));
            latestMapper.insert(latest);
        } else {
            latest.setPressure(req.getPressure());
            latest.setFlow(req.getFlow());
            latest.setPh(req.getPh());
            latest.setTurbidity(req.getTurbidity());
            latest.setResidualCl(req.getResidualCl());
            latest.setTemperature(req.getTemperature());
            latest.setLevel(req.getLevel());
            latest.setUpdateTime(LocalDateTime.now());
            latest.setQualityStatus(calcQuality(req));
            latestMapper.updateById(latest);
        }
    }

    @Override
    public void simulateData() {
        List<Device> devices = deviceMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Device>()
                .eq(Device::getStatus, 1));
        if (devices.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Device d : devices) {
            MonitorWriteReq req = new MonitorWriteReq();
            req.setDeviceId(d.getId());
            req.setDataTime(now);
            req.setPressure(round(randomIn(0.4, 0.9)));
            req.setFlow(round(randomIn(100, 420)));
            req.setPh(round(randomIn(6.5, 8.6)));
            req.setTurbidity(round(randomIn(0.1, 4.0)));
            req.setResidualCl(round(randomIn(0.3, 3.5)));
            req.setTemperature(round(randomIn(8, 32)));
            req.setLevel(round(randomIn(1.0, 9.5)));
            try {
                saveOrUpdateLatest(req);
                String table = TableRouter.check(TableRouter.resolve(now));
                if (monitorMapper.existsTable(table) == null || monitorMapper.existsTable(table) == 0) {
                    continue;
                }
                MonitorRecord record = new MonitorRecord();
                record.setDeviceId(d.getId());
                record.setDataTime(now);
                record.setPressure(req.getPressure());
                record.setFlow(req.getFlow());
                record.setPh(req.getPh());
                record.setTurbidity(req.getTurbidity());
                record.setResidualCl(req.getResidualCl());
                record.setTemperature(req.getTemperature());
                record.setLevel(req.getLevel());
                record.setIsClean(1);
                monitorMapper.insert(table, record);
            } catch (Exception e) {
                log.warn("模拟采集设备 {} 失败: {}", d.getId(), e.getMessage());
            }
        }
        broadcastRealtime();
    }

    @Override
    public Map<String, Object> stat() {
        List<MonitorLatest> all = latestMapper.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal pressureAvg = avg(all.stream().map(MonitorLatest::getPressure).filter(java.util.Objects::nonNull).toList());
        BigDecimal phAvg = avg(all.stream().map(MonitorLatest::getPh).filter(java.util.Objects::nonNull).toList());
        BigDecimal turbidityAvg = avg(all.stream().map(MonitorLatest::getTurbidity).filter(java.util.Objects::nonNull).toList());
        BigDecimal flowTotal = all.stream().map(MonitorLatest::getFlow).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("pressureAvg", pressureAvg);
        result.put("flowTotal", flowTotal);
        result.put("phAvg", phAvg);
        result.put("turbidityAvg", turbidityAvg);
        long error = all.stream().filter(m -> "error".equals(m.getQualityStatus())).count();
        long warn = all.stream().filter(m -> "warn".equals(m.getQualityStatus())).count();
        result.put("worstQuality", error > 0 ? "error" : warn > 0 ? "warn" : "normal");
        result.put("deviceCount", latestCount());
        return result;
    }

    @Override
    public void broadcastRealtime() {
        sessionManager.broadcast("realtime", getRealtimeList());
    }

    private long latestCount() {
        return latestMapper.selectCount(null);
    }

    private double randomIn(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    private BigDecimal round(double v) {
        return BigDecimal.valueOf(Math.round(v * 1000.0) / 1000.0);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, java.math.RoundingMode.HALF_UP);
    }

    private String calcQuality(MonitorWriteReq req) {
        // 简化水质判定：pH 6.5-8.5，浊度<5，余氯 0.3-4
        boolean error = false;
        boolean warn = false;
        if (req.getPh() != null && (req.getPh().doubleValue() < 6.0 || req.getPh().doubleValue() > 9.0)) error = true;
        else if (req.getPh() != null && (req.getPh().doubleValue() < 6.5 || req.getPh().doubleValue() > 8.5)) warn = true;
        if (req.getTurbidity() != null && req.getTurbidity().doubleValue() > 10) error = true;
        else if (req.getTurbidity() != null && req.getTurbidity().doubleValue() > 5) warn = true;
        if (req.getResidualCl() != null && (req.getResidualCl().doubleValue() < 0.1 || req.getResidualCl().doubleValue() > 5)) error = true;
        else if (req.getResidualCl() != null && (req.getResidualCl().doubleValue() < 0.3 || req.getResidualCl().doubleValue() > 4)) warn = true;
        return error ? "error" : warn ? "warn" : "normal";
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

    private List<TrendPointVO> downsample(List<Map<String, Object>> points, Integer pointCount) {
        List<TrendPointVO> result = new ArrayList<>();
        if (points.isEmpty()) {
            return result;
        }
        int size = points.size();
        int target = pointCount == null || pointCount <= 0 ? size : Math.min(pointCount, size);
        int step = (int) Math.ceil((double) size / target);
        for (int i = 0; i < size && result.size() < target; i += step) {
            Map<String, Object> p = points.get(i);
            TrendPointVO vo = new TrendPointVO();
            vo.setTime(p.get("time") instanceof java.sql.Timestamp ts ? ts.toString() : String.valueOf(p.get("time")));
            vo.setValue(p.get("value"));
            result.add(vo);
        }
        if (result.isEmpty() && !points.isEmpty()) {
            Map<String, Object> last = points.get(points.size() - 1);
            TrendPointVO vo = new TrendPointVO();
            vo.setTime(String.valueOf(last.get("time")));
            vo.setValue(last.get("value"));
            result.add(vo);
        }
        return result;
    }
}