package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DailyTrend;
import com.competition.invoice.entity.RecallKpi;
import com.competition.invoice.entity.RecallList;
import com.competition.invoice.mapper.DailyTrendMapper;
import com.competition.invoice.mapper.RecallKpiMapper;
import com.competition.invoice.mapper.RecallListMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管道步骤6：最终产出
 *
 * 计算 KPI 汇总、合并趋势数据、产出最终信息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step6_FinalProducer {

    private final RecallListMapper recallListMapper;
    private final RecallKpiMapper recallKpiMapper;
    private final DailyTrendMapper dailyTrendMapper;
    private final ObjectMapper objectMapper;

    /**
     * 产出最终名单 + KPI
     * @return 最终产出数量
     */
    public int execute(LocalDate dataDate, Long pipelineRunId) {
        log.info("[Step6] 开始最终产出, dataDate={}", dataDate);

        // 1. 查询当日所有召回记录
        List<RecallList> allRecalls = recallListMapper.selectList(
                new LambdaQueryWrapper<RecallList>()
                        .eq(RecallList::getDataDate, dataDate));

        if (allRecalls.isEmpty()) {
            log.warn("[Step6] 无召回记录");
            return 0;
        }

        int totalCount = allRecalls.size();
        long highPotentialCount = allRecalls.stream()
                .filter(r -> r.getRecallScore() != null
                        && r.getRecallScore().compareTo(BigDecimal.valueOf(60)) > 0)
                .count();

        // 2. 计算 KPI
        RecallKpi kpi = new RecallKpi();
        kpi.setPipelineRunId(pipelineRunId);
        kpi.setDataDate(dataDate);
        kpi.setRecallableCount(totalCount);

        // 预期成功率（高潜力司机中已生成策略的占比，初始默认可按 38% 估算）
        long hasStrategy = allRecalls.stream()
                .filter(r -> r.getStrategyScript() != null)
                .count();
        BigDecimal expectedRate = totalCount > 0
                ? BigDecimal.valueOf(hasStrategy).divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        kpi.setExpectedSuccessRate(expectedRate);

        // 预算估算（假设每条策略成本 5 元）
        kpi.setTodayBudget(BigDecimal.valueOf(highPotentialCount * 5L));

        // 规则基准（简单按在线天数 > 3 估算）
        long ruleBased = allRecalls.stream()
                .filter(r -> r.getRecallScore() != null && r.getRecallScore().compareTo(BigDecimal.valueOf(40)) > 0)
                .count();
        kpi.setRuleBasedCount((int) ruleBased);

        BigDecimal improvement = ruleBased > 0
                ? BigDecimal.valueOf(totalCount - ruleBased)
                    .divide(BigDecimal.valueOf(ruleBased), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        kpi.setImprovementPct(improvement);

        // 状态分布
        try {
            kpi.setStatusDistribution(objectMapper.writeValueAsString(buildStatusDist(allRecalls)));
        } catch (Exception e) {
            log.warn("分布数据序列化失败", e);
        }

        // Upsert KPI
        RecallKpi existing = recallKpiMapper.findByDataDate(dataDate.toString());
        if (existing != null) {
            kpi.setId(existing.getId());
            recallKpiMapper.updateById(kpi);
        } else {
            recallKpiMapper.insert(kpi);
        }

        // 3. 合并趋势数据
        DailyTrend trend = new DailyTrend();
        trend.setDataDate(dataDate);
        trend.setRecallableCount(totalCount);
        trend.setHighPotentialCount((int) highPotentialCount);

        recallKpiMapper.delete(new LambdaQueryWrapper<RecallKpi>()
                .eq(RecallKpi::getDataDate, dataDate).ne(RecallKpi::getId, kpi.getId()));

        DailyTrend existingTrend = dailyTrendMapper.selectOne(
                new LambdaQueryWrapper<DailyTrend>().eq(DailyTrend::getDataDate, dataDate));
        if (existingTrend != null) {
            trend.setId(existingTrend.getId());
            trend.setContactedCount(existingTrend.getContactedCount());
            trend.setAgreedCount(existingTrend.getAgreedCount());
            trend.setActualSuccessRate(existingTrend.getActualSuccessRate());
            dailyTrendMapper.updateById(trend);
        } else {
            dailyTrendMapper.insert(trend);
        }

        log.info("[Step6] 完成, 总数={}, 高潜力={}", totalCount, highPotentialCount);
        return totalCount;
    }

    private Map<String, Long> buildStatusDist(List<RecallList> recalls) {
        Map<String, Long> dist = new HashMap<>();
        for (RecallList r : recalls) {
            String status = r.getOutreachStatus() != null ? r.getOutreachStatus() : "PENDING";
            dist.merge(status, 1L, Long::sum);
        }
        return dist;
    }
}
