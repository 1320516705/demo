package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.RecallList;
import com.competition.invoice.mapper.RecallListMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 管道步骤4：筛选高潜力司机
 *
 * 支持两种模式（通过 application.yml 配置）：
 *   - 阈值模式（默认）：recall_score > high-potential-threshold
 *   - Top-N 模式：取分数排名前 top-n-count 的司机
 *
 * 同时生效时，Top-N 的范围内仍需满足阈值条件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step4_HighPotentialFilter {

    private final RecallListMapper recallListMapper;

    /** 绝对阈值：分数 > 此值才算高潜力（默认 60） */
    @Value("${recall.high-potential-threshold:60.0}")
    private BigDecimal threshold;

    /** Top-N 模式：取前 N 名（0 = 不启用） */
    @Value("${recall.top-n-count:0}")
    private int topN;

    /** Top-N 模式下仍需满足的最低分数线 */
    @Value("${recall.top-n-min-score:40.0}")
    private BigDecimal topNMinScore;

    public int execute(LocalDate dataDate) {
        if (topN > 0) {
            return executeTopN(dataDate);
        }
        return executeThreshold(dataDate);
    }

    /**
     * 阈值模式
     */
    private int executeThreshold(LocalDate dataDate) {
        log.info("[Step4] 阈值模式, threshold={}", threshold);
        Long count = recallListMapper.selectCount(
                new LambdaQueryWrapper<RecallList>()
                        .eq(RecallList::getDataDate, dataDate)
                        .gt(RecallList::getRecallScore, threshold));
        log.info("[Step4] 高潜力={}", count);
        return count != null ? count.intValue() : 0;
    }

    /**
     * Top-N 模式：取分数最高的前 N 名（且分数 > topNMinScore）
     */
    private int executeTopN(LocalDate dataDate) {
        log.info("[Step4] Top-N 模式, N={}, minScore={}", topN, topNMinScore);

        // 按分数降序取前 N 条
        List<RecallList> topDrivers = recallListMapper.selectList(
                new LambdaQueryWrapper<RecallList>()
                        .eq(RecallList::getDataDate, dataDate)
                        .gt(RecallList::getRecallScore, topNMinScore)
                        .orderByDesc(RecallList::getRecallScore)
                        .last("LIMIT " + topN));

        int count = topDrivers.size();
        if (count > 0) {
            BigDecimal lowest = topDrivers.get(count - 1).getRecallScore();
            log.info("[Step4] Top-{} 筛选: 最高={} 最低={} 有效阈值={}",
                    count,
                    topDrivers.get(0).getRecallScore(),
                    lowest,
                    lowest);
        }
        return count;
    }
}
