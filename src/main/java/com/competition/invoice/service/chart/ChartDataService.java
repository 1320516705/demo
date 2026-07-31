package com.competition.invoice.service.chart;

import com.competition.invoice.entity.DailyTrend;
import com.competition.invoice.mapper.DailyTrendMapper;
import com.competition.invoice.mapper.RecallListMapper;
import com.competition.invoice.model.enums.PersonaTag;
import com.competition.invoice.model.vo.DistributionVO;
import com.competition.invoice.model.vo.DistributionVO.DistributionItem;
import com.competition.invoice.model.vo.TrendChartVO;
import com.competition.invoice.model.vo.TrendChartVO.TrendPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图表数据服务
 */
@Service
@RequiredArgsConstructor
public class ChartDataService {

    private final DailyTrendMapper dailyTrendMapper;
    private final RecallListMapper recallListMapper;

    /**
     * 获取趋势数据
     */
    public TrendChartVO getTrend(int days, LocalDate endDate) {
        List<DailyTrend> trends = dailyTrendMapper.findRecentTrend(endDate.toString(), days);

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

    /**
     * 获取分布数据
     */
    public DistributionVO getDistribution(LocalDate dataDate, String type) {
        DistributionVO vo = new DistributionVO();
        vo.setType(type);

        switch (type) {
            case "persona" -> vo.setItems(buildPersonaDist(dataDate));
            case "status" -> vo.setItems(buildStatusDist(dataDate));
            default -> vo.setItems(List.of());
        }
        return vo;
    }

    private List<DistributionItem> buildPersonaDist(LocalDate dataDate) {
        List<DistributionItem> items = new ArrayList<>();

        // 预设颜色
        String[] colors = {"#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de"};

        List<Map<String, Object>> rows = recallListMapper.countByPersonaTag(dataDate.toString());
        for (Map<String, Object> row : rows) {
            String tag = (String) row.get("tag");
            Long cnt = ((Number) row.get("cnt")).longValue();
            String label = tag;
            try {
                label = PersonaTag.valueOf(tag).getLabel();
            } catch (IllegalArgumentException ignored) {}

            items.add(DistributionItem.of(label, cnt,
                    colors[items.size() % colors.length]));
        }
        return items;
    }

    private List<DistributionItem> buildStatusDist(LocalDate dataDate) {
        List<DistributionItem> items = new ArrayList<>();
        String[] colors = {"#91cc75", "#5470c6", "#fac858", "#ee6666", "#999999"};

        List<Map<String, Object>> rows = recallListMapper.countByOutreachStatus(dataDate.toString());
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            Long cnt = ((Number) row.get("cnt")).longValue();
            items.add(DistributionItem.of(status, cnt,
                    colors[items.size() % colors.length]));
        }
        return items;
    }
}
