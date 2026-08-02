package com.competition.invoice.controller;

import com.competition.invoice.common.Result;
import com.competition.invoice.model.vo.DistributionVO;
import com.competition.invoice.model.vo.TrendChartVO;
import com.competition.invoice.service.chart.ChartDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 图表数据 API
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChartController {

    private final ChartDataService chartDataService;

    @GetMapping("/chart/trend")
    public Result<TrendChartVO> trend(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "") String endDate) {
        LocalDate end = endDate.isEmpty() ? LocalDate.now() : LocalDate.parse(endDate);
        return Result.ok(chartDataService.getTrend(days, end));
    }

    /** 城市热力图：各城市可召回司机分布 */
    @GetMapping("/chart/city-heatmap")
    public Result<List<Map<String, Object>>> cityHeatmap(
            @RequestParam(defaultValue = "") String dataDate) {
        LocalDate date = dataDate.isEmpty() ? LocalDate.now().minusDays(1) : LocalDate.parse(dataDate);
        return Result.ok(chartDataService.getCitySummary(date));
    }

    @GetMapping("/chart/distribution")
    public Result<DistributionVO> distribution(
            @RequestParam(defaultValue = "") String dataDate,
            @RequestParam(defaultValue = "status") String type) {
        LocalDate date = dataDate.isEmpty() ? LocalDate.now().minusDays(1) : LocalDate.parse(dataDate);
        return Result.ok(chartDataService.getDistribution(date, type));
    }
}
