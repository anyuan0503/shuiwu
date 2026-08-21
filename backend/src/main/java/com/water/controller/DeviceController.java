package com.water.controller;

import com.water.common.PageResult;
import com.water.common.Result;
import com.water.dto.DeviceDTO;
import com.water.entity.Device;
import com.water.service.DeviceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','device:list','device:manage')")
    public Result<PageResult<Device>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String deviceType,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String area) {
        return Result.ok(deviceService.page(page, size, keyword, deviceType, status, area));
    }

    @GetMapping("/list")
    public Result<List<Device>> list() {
        return Result.ok(deviceService.list());
    }

    @GetMapping("/{id}")
    public Result<Device> detail(@PathVariable Long id) {
        return Result.ok(deviceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','device:manage')")
    public Result<Device> create(@RequestBody DeviceDTO dto) {
        return Result.ok(deviceService.create(dto));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','device:manage')")
    public Result<Device> update(@RequestBody DeviceDTO dto) {
        return Result.ok(deviceService.update(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','device:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }

    @GetMapping("/onlineCount")
    @PreAuthorize("hasAnyAuthority('ADMIN','OPERATOR','VIEWER','device:list')")
    public Result<List<Map<String, Object>>> onlineCount() {
        return Result.ok(deviceService.onlineCount());
    }
}