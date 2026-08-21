package com.water.service;

import com.water.common.PageResult;
import com.water.dto.GenerateReportReq;
import com.water.entity.Report;

import java.util.Map;

public interface ReportService {

    PageResult<Report> page(int page, int size, String keyword);

    Map<String, Object> generate(GenerateReportReq req);

    Report getById(Long id);

    Map<String, Object> summary();
}