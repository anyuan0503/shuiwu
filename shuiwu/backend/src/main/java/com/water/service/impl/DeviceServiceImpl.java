package com.water.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.water.common.BizException;
import com.water.common.PageResult;
import com.water.dto.DeviceDTO;
import com.water.entity.Device;
import com.water.mapper.DeviceMapper;
import com.water.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    public DeviceServiceImpl(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public PageResult<Device> page(int page, int size, String keyword, String deviceType, Integer status, String area) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w.like(Device::getDeviceName, keyword)
                        .or().like(Device::getDeviceNo, keyword)
                        .or().like(Device::getLocation, keyword))
                .eq(StringUtils.hasText(deviceType), Device::getDeviceType, deviceType)
                .eq(status != null, Device::getStatus, status)
                .eq(StringUtils.hasText(area), Device::getArea, area)
                .orderByAsc(Device::getId);
        Page<Device> p = deviceMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public List<Device> list() {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>().orderByAsc(Device::getId));
    }

    @Override
    public Device getById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BizException("设备不存在: " + id);
        }
        return device;
    }

    @Override
    public Device create(DeviceDTO dto) {
        if (StrUtil.isBlank(dto.getDeviceNo()) || StrUtil.isBlank(dto.getDeviceName())) {
            throw new BizException(400, "设备编号与名称不能为空");
        }
        Device device = new Device();
        copy(dto, device);
        deviceMapper.insert(device);
        return device;
    }

    @Override
    public Device update(DeviceDTO dto) {
        Device exist = getById(dto.getId());
        copy(dto, exist);
        deviceMapper.updateById(exist);
        return exist;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        deviceMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> onlineCount() {
        List<Device> all = deviceMapper.selectList(null);
        // 按类型聚合
        java.util.Map<String, List<Device>> byType = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(d -> d.getDeviceType() == null ? "unknown" : d.getDeviceType()));
        List<Map<String, Object>> result = new ArrayList<>();
        byType.forEach((type, list) -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("deviceType", type);
            row.put("total", list.size());
            row.put("online", list.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1).count());
            row.put("offline", list.stream().filter(d -> d.getStatus() != null && d.getStatus() == 0).count());
            row.put("fault", list.stream().filter(d -> d.getStatus() != null && d.getStatus() == 2).count());
            result.add(row);
        });
        result.sort((a, b) -> ((Number) b.get("total")).intValue() - ((Number) a.get("total")).intValue());
        return result;
    }

    private void copy(DeviceDTO dto, Device device) {
        if (dto.getDeviceNo() != null) device.setDeviceNo(dto.getDeviceNo());
        if (dto.getDeviceName() != null) device.setDeviceName(dto.getDeviceName());
        if (dto.getDeviceType() != null) device.setDeviceType(dto.getDeviceType());
        if (dto.getModel() != null) device.setModel(dto.getModel());
        if (dto.getLocation() != null) device.setLocation(dto.getLocation());
        if (dto.getArea() != null) device.setArea(dto.getArea());
        if (dto.getManufacturer() != null) device.setManufacturer(dto.getManufacturer());
        if (dto.getUnit() != null) device.setUnit(dto.getUnit());
        if (dto.getInstallDate() != null) device.setInstallDate(dto.getInstallDate());
        if (dto.getStatus() != null) device.setStatus(dto.getStatus());
        if (dto.getLon() != null) device.setLon(dto.getLon());
        if (dto.getLat() != null) device.setLat(dto.getLat());
        if (dto.getRemark() != null) device.setRemark(dto.getRemark());
    }
}