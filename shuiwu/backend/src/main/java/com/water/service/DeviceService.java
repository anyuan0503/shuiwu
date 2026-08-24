package com.water.service;

import com.water.common.PageResult;
import com.water.dto.DeviceDTO;
import com.water.entity.Device;

import java.util.List;
import java.util.Map;

public interface DeviceService {

    PageResult<Device> page(int page, int size, String keyword, String deviceType, Integer status, String area);

    List<Device> list();

    Device getById(Long id);

    Device create(DeviceDTO dto);

    Device update(DeviceDTO dto);

    void delete(Long id);

    List<Map<String, Object>> onlineCount();
}