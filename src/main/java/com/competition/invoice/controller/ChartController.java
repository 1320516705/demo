package com.competition.invoice.controller;

import com.competition.invoice.common.Result;
import com.competition.invoice.model.vo.DistributionVO;
import com.competition.invoice.model.vo.HeatMapPointVO;
import com.competition.invoice.model.vo.HeatMapPointVO.HeatPoint;
import com.competition.invoice.model.vo.TrendChartVO;
import com.competition.invoice.mapper.RecallListMapper;
import com.competition.invoice.service.chart.ChartDataService;
import com.competition.invoice.service.external.H3CoordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图表数据 API
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChartController {

    private final ChartDataService chartDataService;
    private final RecallListMapper recallListMapper;
    private final H3CoordMapper h3CoordMapper;

    /**
     * 趋势图
     */
    @GetMapping("/chart/trend")
    public Result<TrendChartVO> trend(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "") String endDate) {

        LocalDate end = endDate.isEmpty()
                ? LocalDate.now()
                : LocalDate.parse(endDate);

        return Result.ok(chartDataService.getTrend(days, end));
    }

    /**
     * 分布图
     */
    @GetMapping("/chart/distribution")
    public Result<DistributionVO> distribution(
            @RequestParam(defaultValue = "") String dataDate,
            @RequestParam(defaultValue = "persona") String type) {

        LocalDate date = dataDate.isEmpty()
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(dataDate);

        return Result.ok(chartDataService.getDistribution(date, type));
    }

    /**
     * 热力图数据
     */
    @GetMapping("/map/heatmap")
    public Result<HeatMapPointVO> heatmap(
            @RequestParam(defaultValue = "") String dataDate) {

        LocalDate date = dataDate.isEmpty()
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(dataDate);

        List<Map<String, Object>> rows = recallListMapper.getHeatmapData(date.toString());

        HeatMapPointVO vo = new HeatMapPointVO();
        List<HeatPoint> points = rows.stream().map(row -> {
            HeatPoint p = new HeatPoint();
            String h3Index = (String) row.get("h3Index");
            int driverCount = ((Number) row.get("driverCount")).intValue();
            p.setH3Index(h3Index);
            // 100 为种子，按司机数分散点位避免完全重叠
            double[] coord = h3CoordMapper.getCoord(h3Index, driverCount * 7 + h3Index.hashCode() % 100);
            p.setLng(coord[0]);
            p.setLat(coord[1]);
            p.setCount(driverCount);
            Object avgScore = row.get("avgScore");
            p.setAvgScore(avgScore != null ? BigDecimal.valueOf(((Number) avgScore).doubleValue()) : BigDecimal.ZERO);
            return p;
        }).collect(Collectors.toList());

        vo.setPoints(points);
        return Result.ok(vo);
    }
}
