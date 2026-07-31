package com.competition.invoice.service.kpi;

import com.competition.invoice.entity.RecallKpi;
import com.competition.invoice.mapper.RecallKpiMapper;
import com.competition.invoice.model.vo.KpiCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * KPI 服务
 */
@Service
@RequiredArgsConstructor
public class KpiService {

    private final RecallKpiMapper recallKpiMapper;

    /**
     * 获取 KPI 卡片数据
     */
    public KpiCardVO getSummary(LocalDate dataDate) {
        RecallKpi kpi = recallKpiMapper.findByDataDate(dataDate.toString());
        if (kpi == null) {
            return emptyKpi();
        }

        KpiCardVO vo = new KpiCardVO();
        vo.setRecallableCount(kpi.getRecallableCount());
        vo.setExpectedSuccessRate(kpi.getExpectedSuccessRate());
        vo.setTodayBudget(kpi.getTodayBudget());
        vo.setRuleBasedCount(kpi.getRuleBasedCount());
        vo.setImprovementPct(kpi.getImprovementPct());
        return vo;
    }

    private KpiCardVO emptyKpi() {
        KpiCardVO vo = new KpiCardVO();
        vo.setRecallableCount(0);
        vo.setExpectedSuccessRate(java.math.BigDecimal.ZERO);
        vo.setTodayBudget(java.math.BigDecimal.ZERO);
        vo.setRuleBasedCount(0);
        vo.setImprovementPct(java.math.BigDecimal.ZERO);
        return vo;
    }
}
