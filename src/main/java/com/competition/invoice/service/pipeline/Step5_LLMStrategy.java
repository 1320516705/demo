package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.entity.RecallList;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import com.competition.invoice.mapper.RecallListMapper;
import com.competition.invoice.service.external.LLMClient;
import com.competition.invoice.service.external.LLMClient.LLMStrategyResponse;
import com.competition.invoice.service.external.LLMResponseParser;
import com.competition.invoice.service.external.LLMResponseParser.ParsedStrategy;
import com.competition.invoice.service.external.LocalLlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 管道步骤5：生成个性化召回策略
 *
 * LLM 模式：配置了 API Key → 调用 Claude，更精准的话术
 * 本地模式：未配置     → 规则引擎兜底，基于行为数据做画像匹配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step5_LLMStrategy {

    private final RecallListMapper recallListMapper;
    private final DriverDailySnapshotMapper snapshotMapper;
    private final LLMClient llmClient;
    private final LocalLlmService localLlm;
    private final ObjectMapper objectMapper;

    @Value("${external.llm.api-key:}")
    private String llmApiKey;

    @Value("${recall.high-potential-threshold:60.0}")
    private BigDecimal threshold;

    @Value("${recall.top-n-count:0}")
    private int topN;

    public int execute(LocalDate dataDate) {
        boolean useRealLlm = llmApiKey != null && !llmApiKey.isEmpty();
        log.info("[Step5] 开始策略生成, mode={}, dataDate={}, threshold={}, topN={}",
                useRealLlm ? "CLAUDE" : "LOCAL_RULE_ENGINE", dataDate, threshold, topN);

        // 查询高潜力司机（与 Step4 使用相同阈值），按分数降序
        List<RecallList> highPotential = recallListMapper.selectList(
                new LambdaQueryWrapper<RecallList>()
                        .eq(RecallList::getDataDate, dataDate)
                        .gt(RecallList::getRecallScore, threshold)
                        .isNull(RecallList::getPersonaTag)
                        .orderByDesc(RecallList::getRecallScore)
                        .last(topN > 0 ? "LIMIT " + topN : ""));

        if (highPotential.isEmpty()) {
            log.info("[Step5] 无高潜力司机需要生成策略");
            return 0;
        }

        log.info("[Step5] 高潜力司机={} 人", highPotential.size());

        if (useRealLlm) {
            processWithClaude(highPotential);
        } else {
            processWithLocal(highPotential);
        }

        log.info("[Step5] 完成, 生成策略={}", highPotential.size());
        return highPotential.size();
    }

    /**
     * Claude API 模式：并发调用（5线程 + 限流）
     */
    private void processWithClaude(List<RecallList> list) {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = list.stream()
                .map(rl -> CompletableFuture.runAsync(() -> processOneWithClaude(rl), executor))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("[Step5] Claude 并发异常", e);
        } finally {
            executor.shutdown();
        }
    }

    private void processOneWithClaude(RecallList rl) {
        try {
            DriverDailySnapshot s = snapshotMapper.selectOne(
                    new LambdaQueryWrapper<DriverDailySnapshot>()
                            .eq(DriverDailySnapshot::getSnapshotDate, rl.getDataDate())
                            .eq(DriverDailySnapshot::getDriverId, rl.getDriverId()));
            // 构建全景数据发给 LLM
            String behaviorJson = buildBehaviorJson(s, rl);
            LLMStrategyResponse raw = llmClient.generateStrategy(rl.getDriverName(), behaviorJson);
            ParsedStrategy parsed = LLMResponseParser.parse(raw);
            saveStrategy(rl, parsed, objectMapper.writeValueAsString(raw));
        } catch (Exception e) {
            log.error("[Step5] Claude 处理失败: driverId={}", rl.getDriverId(), e);
        }
    }

    /**
     * 本地规则引擎模式：串行处理（已经很快，不需要并发）
     */
    private void processWithLocal(List<RecallList> list) {
        for (RecallList rl : list) {
            try {
                DriverDailySnapshot s = snapshotMapper.selectOne(
                        new LambdaQueryWrapper<DriverDailySnapshot>()
                                .eq(DriverDailySnapshot::getSnapshotDate, rl.getDataDate())
                                .eq(DriverDailySnapshot::getDriverId, rl.getDriverId()));

                LLMStrategyResponse raw = localLlm.generate(s);
                ParsedStrategy parsed = LLMResponseParser.parse(raw);
                saveStrategy(rl, parsed, objectMapper.writeValueAsString(raw));
            } catch (Exception e) {
                log.error("[Step5] 本地规则引擎处理失败: driverId={}", rl.getDriverId(), e);
            }
        }
    }

    private void saveStrategy(RecallList rl, ParsedStrategy parsed, String rawJson) {
        recallListMapper.update(null,
                new LambdaUpdateWrapper<RecallList>()
                        .eq(RecallList::getId, rl.getId())
                        .set(RecallList::getPersonaTag, parsed.getPersonaTag())
                        .set(RecallList::getPersonaConfidence, parsed.getPersonaConfidence())
                        .set(RecallList::getStrategyScript, parsed.getStrategyScript())
                        .set(RecallList::getRecommendedChannel, parsed.getRecommendedChannel())
                        .set(RecallList::getLlmResponseRaw, rawJson));
    }

    /**
     * 构建发给 LLM 的司机全景数据
     */
    private String buildBehaviorJson(DriverDailySnapshot s, RecallList rl) {
        int onlineCount = s.getOnlineCount7d() != null ? s.getOnlineCount7d() : 0;
        int activeDays = s.getActiveDays7d() != null ? s.getActiveDays7d() : 0;
        int totalOrders = s.getTotalOrders7d() != null ? s.getTotalOrders7d() : 0;
        double peakPct = s.getPeakHourPct() != null ? s.getPeakHourPct().doubleValue() : 0;
        double cancelRate = s.getCancelRate7d() != null ? s.getCancelRate7d().doubleValue() : 0;
        double avgAmount = s.getAvgOrderAmount() != null ? s.getAvgOrderAmount().doubleValue() : 0;
        double avgRating = s.getAvgRating() != null ? s.getAvgRating().doubleValue() : 0;
        int complaints = s.getComplaintCount7d() != null ? s.getComplaintCount7d() : 0;
        double sdRatio = s.getSupplyDemandRatio() != null ? s.getSupplyDemandRatio().doubleValue() : 0;
        double recallScore = rl.getRecallScore() != null ? rl.getRecallScore().doubleValue() : 0;

        return String.format("""
            {
              "司机姓名": "%s",
              "手机号": "%s",
              "召回潜力分": %.1f,
              "活跃数据": {
                "近7天上线次数": %d,
                "近7天活跃天数": %d,
                "近7天完单量": %d,
                "日均完单": %.1f
              },
              "行为数据": {
                "均单金额": %.1f,
                "评分": %.1f,
                "取消率": "%.0f%%",
                "高峰时段订单占比": "%.0f%%",
                "投诉次数": %d
              },
              "区域数据": {
                "所在区域供需比": %.1f,
                "区域解读": "%s"
              },
              "画像特征": {
                "活跃度": "%s",
                "出车规律": "%s",
                "高峰偏好": "%s",
                "忠诚度": "%s"
              },
              "运营建议": {
                "召回潜力等级": "%s",
                "推荐触达方向": "%s"
              }
            }""",
            s.getDriverName(),
            s.getPhone() != null ? s.getPhone() : "未知",
            recallScore,
            onlineCount, activeDays, totalOrders,
            activeDays > 0 ? (double) totalOrders / activeDays : 0,
            avgAmount, avgRating,
            cancelRate * 100,
            peakPct * 100,
            complaints,
            sdRatio,
            sdRatio >= 2.5 ? "运力严重不足，司机议价能力强" :
                sdRatio >= 1.5 ? "运力偏紧，订单等待时间短" : "供需平衡",
            onlineCount >= 12 ? "高（频繁在线，召回概率大）" :
                onlineCount >= 6 ? "中（规律在线，有出车习惯）" : "低（偶尔在线）",
            activeDays >= 6 ? "全职型（几乎每天出车）" :
                activeDays >= 3 ? "半职型（每周3-5天）" : "兼职型（每周1-2天）",
            peakPct >= 0.7 ? "强（主要在高峰时段跑车，追求高流水）" :
                peakPct >= 0.4 ? "中（兼顾高峰和平峰）" :
                    peakPct > 0 ? "弱（偏好平峰时段，顺路接单为主）" : "无数据",
            avgRating >= 4.7 ? "高（评分优秀，优质司机）" :
                avgRating >= 4.3 ? "中（评分合格）" : "低（评分偏低，需关注服务质量）",
            recallScore >= 80 ? "S级（极高召回价值，建议人工外呼）" :
                recallScore >= 60 ? "A级（高召回价值，优先触达）" :
                    recallScore >= 40 ? "B级（中等，批量触达即可）" : "C级（低优先级）",
            recallScore >= 80 ? "外呼+专属优惠" :
                recallScore >= 60 ? "短信/App推送+通用奖励" : "短信批量触达"
        );
    }
}
