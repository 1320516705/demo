package com.competition.invoice.controller;

import com.competition.invoice.common.Result;
import com.competition.invoice.model.vo.KpiCardVO;
import com.competition.invoice.service.kpi.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * KPI 卡片 API
 */
@RestController
@RequestMapping("/api/v1/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @GetMapping("/summary")
    public Result<KpiCardVO> summary(@RequestParam(defaultValue = "") String dataDate) {
        LocalDate date = dataDate.isEmpty()
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(dataDate);
        return Result.ok(kpiService.getSummary(date));
    }
}
