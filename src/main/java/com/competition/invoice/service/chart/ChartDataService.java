package com.competition.invoice.service.chart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DailyTrend;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.mapper.DailyTrendMapper;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import com.competition.invoice.mapper.RecallListMapper;
import com.competition.invoice.model.vo.DistributionVO;
import com.competition.invoice.model.vo.DistributionVO.DistributionItem;
import com.competition.invoice.model.vo.TrendChartVO;
import com.competition.invoice.model.vo.TrendChartVO.TrendPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChartDataService {

    private final DailyTrendMapper dailyTrendMapper;
    private final RecallListMapper recallListMapper;
    private final DriverDailySnapshotMapper snapshotMapper;

    /** 主要城市坐标 */
    private static final Map<String, double[]> CITY_COORDS = new LinkedHashMap<>();
    static {
        CITY_COORDS.put("上海", new double[]{121.47, 31.23});
        CITY_COORDS.put("北京", new double[]{116.41, 39.90});
        CITY_COORDS.put("深圳", new double[]{114.06, 22.54});
        CITY_COORDS.put("广州", new double[]{113.26, 23.13});
        CITY_COORDS.put("成都", new double[]{104.07, 30.57});
        CITY_COORDS.put("杭州", new double[]{120.15, 30.28});
        CITY_COORDS.put("武汉", new double[]{114.30, 30.60});
        CITY_COORDS.put("南京", new double[]{118.79, 32.06});
        CITY_COORDS.put("重庆", new double[]{106.55, 29.57});
    }

    /** 城市级可召回分布（来自召回名单 + 快照关联） */
    public List<Map<String, Object>> getCitySummary(LocalDate dataDate) {
        // 从 t_recall_list 关联 t_driver_daily_snapshot 取城市
        List<DriverDailySnapshot> allSnapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<DriverDailySnapshot>()
                        .eq(DriverDailySnapshot::getSnapshotDate, dataDate)
                        .isNotNull(DriverDailySnapshot::getCity));
        // 过滤：只保留在 t_recall_list 中有评分的司机
        List<com.competition.invoice.entity.RecallList> recallList = recallListMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.competition.invoice.entity.RecallList>()
                        .eq(com.competition.invoice.entity.RecallList::getDataDate, dataDate));
        java.util.Set<String> scoredDriverIds = recallList.stream()
                .map(com.competition.invoice.entity.RecallList::getDriverId)
                .collect(java.util.stream.Collectors.toSet());

        List<DriverDailySnapshot> list = allSnapshots.stream()
                .filter(s -> scoredDriverIds.contains(s.getDriverId()))
                .toList();

        Map<String, Long> cityCount = new LinkedHashMap<>();
        for (DriverDailySnapshot s : list) {
            String city = s.getCity() != null ? s.getCity() : "其他";
            cityCount.merge(city, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        long max = cityCount.values().stream().mapToLong(v -> v).max().orElse(1);
        for (Map.Entry<String, Long> e : cityCount.entrySet()) {
            double[] coord = CITY_COORDS.getOrDefault(e.getKey(), new double[]{116.4, 39.9});
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            item.put("ratio", Math.round(e.getValue() * 100.0 / max));
            item.put("lng", coord[0]);
            item.put("lat", coord[1]);
            result.add(item);
        }
        return result;
    }

    public TrendChartVO getTrend(int days, LocalDate endDate) {
        List<DailyTrend> trends = dailyTrendMapper.findRecentTrend(endDate.toString(), days);
        // 按日期升序（左旧右新）
        trends.sort((a, b) -> a.getDataDate().compareTo(b.getDataDate()));
        TrendChartVO vo = new TrendChartVO();
        List<TrendPoint> points = new ArrayList<>();
        for (DailyTrend t : trends) {
            TrendPoint p = new TrendPoint();
            p.setDate(t.getDataDate());
            p.setRecallable(t.getRecallableCount());
            p.setHighPotential(t.getHighPotentialCount());
            p.setContacted(t.getContactedCount() != null ? t.getContactedCount() : 0);
            p.setAgreed(t.getAgreedCount() != null ? t.getAgreedCount() : 0);
            p.setSuccessRate(t.getActualSuccessRate());
            points.add(p);
        }
        vo.setPoints(points);
        return vo;
    }

    public DistributionVO getDistribution(LocalDate dataDate, String type) {
        DistributionVO vo = new DistributionVO();
        vo.setType(type);
        if ("status".equals(type)) vo.setItems(buildStatusDist(dataDate));
        else vo.setItems(List.of());
        return vo;
    }

    private List<DistributionItem> buildStatusDist(LocalDate dataDate) {
        List<DistributionItem> items = new ArrayList<>();
        String[] colors = {"#91cc75", "#5470c6", "#fac858", "#ee6666", "#999999"};
        List<Map<String, Object>> rows = recallListMapper.countByOutreachStatus(dataDate.toString());
        for (Map<String, Object> row : rows) {
            items.add(DistributionItem.of((String) row.get("status"),
                    ((Number) row.get("cnt")).longValue(),
                    colors[items.size() % colors.length]));
        }
        return items;
    }
}
