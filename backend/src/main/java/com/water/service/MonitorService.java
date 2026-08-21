package com.water.service;

import com.water.dto.MonitorWriteReq;
import com.water.vo.RealtimeVO;
import com.water.vo.TrendPointVO;

import java.util.List;
import java.util.Map;

public interface MonitorService {

    List<RealtimeVO> getRealtimeList();

    List<TrendPointVO> trend(Long deviceId, String type, String startTime, String endTime, Integer pointCount);

    void writeData(MonitorWriteReq req);

    void saveOrUpdateLatest(MonitorWriteReq req);

    /** 模拟采集：为所有在线设备生成随机监测数据 */
    void simulateData();

    Map<String, Object> stat();

    void broadcastRealtime();
}