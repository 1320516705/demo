package com.competition.invoice.service.external;

import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.model.enums.OutreachChannel;
import com.competition.invoice.model.enums.PersonaTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地 LLM 策略生成器
 *
 * 当未配置 Claude API Key 时，使用规则引擎兜底生成策略。
 * 这不是 random mock —— 每个决策都基于司机行为数据，有明确规则。
 *
 * 与 Claude 的差异：
 *   - 本地用规则树决策，Claude 用 prompt 推理
 *   - 本地话术是模板拼装，Claude 是自由生成
 *   - 对外的 API 契约完全一致（LLMClient.LLMStrategyResponse）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalLlmService {

    /**
     * 基于司机行为数据生成策略
     */
    public LLMClient.LLMStrategyResponse generate(DriverDailySnapshot s) {
        double[] scores = computePersonaScores(s);
        PersonaTag bestTag = pickBestTag(scores);

        LLMClient.LLMStrategyResponse resp = new LLMClient.LLMStrategyResponse();
        resp.setPersonaTag(bestTag.name());
        resp.setPersonaConfidence(scores[bestTag.ordinal()]);
        resp.setStrategyScript(buildScript(s, bestTag));
        resp.setRecommendedChannel(pickChannel(bestTag, s));
        resp.setReasoning(buildReasoning(scores, bestTag));
        return resp;
    }

    // ═══════════════════════════════════════════════════════════
    // 人设评分规则
    // ═══════════════════════════════════════════════════════════

    private double[] computePersonaScores(DriverDailySnapshot s) {
        double[] scores = new double[5];

        double onlineCount = s.getOnlineCount7d() != null ? s.getOnlineCount7d() : 0;
        double peakPct = s.getPeakHourPct() != null ? s.getPeakHourPct().doubleValue() : 0;
        double cancelRate = s.getCancelRate7d() != null ? s.getCancelRate7d().doubleValue() : 0;
        double avgAmount = s.getAvgOrderAmount() != null ? s.getAvgOrderAmount().doubleValue() : 0;
        double activeDays = s.getActiveDays7d() != null ? s.getActiveDays7d() : 0;
        double totalOrders = s.getTotalOrders7d() != null ? s.getTotalOrders7d() : 0;

        // 价格敏感型：活跃偏高 + 完单量大 + 均单金额偏低 → 愿意跑量赚补贴
        scores[0] = clamp(onlineCount / 14.0 * 0.3 + (1 - clamp(avgAmount / 50.0)) * 0.3
                        + clamp(totalOrders / 35.0) * 0.2 + (1 - cancelRate) * 0.2);

        // 时间敏感型：高峰占比高 + 均单金额高 → 追求高峰高流水
        scores[1] = clamp(peakPct * 0.45 + clamp(avgAmount / 50.0) * 0.30
                        + clamp(onlineCount / 14.0) * 0.15 + (1 - cancelRate) * 0.10);

        // 顺路回家型：高峰占比低 + 取消率极低 + 活跃稳定
        scores[2] = clamp((1 - peakPct) * 0.40 + (1 - cancelRate) * 0.35
                        + clamp(activeDays / 7.0) * 0.15 + clamp(avgAmount / 30.0) * 0.10);

        // 周末兼职型：活跃天数少(≤3) + 均单金额高 → 选时段赚钱
        scores[3] = clamp((activeDays <= 3 ? 0.9 : activeDays <= 5 ? 0.4 : 0.1) * 0.50
                        + clamp(avgAmount / 50.0) * 0.30 + clamp(totalOrders / 20.0) * 0.20);

        // 稳定全职型：活跃天数多(≥6) + 各项指标均衡
        scores[4] = clamp((activeDays >= 6 ? 0.90 : activeDays >= 4 ? 0.45 : 0.15) * 0.40
                        + clamp(onlineCount / 14.0) * 0.25 + (1 - cancelRate) * 0.20
                        + clamp(totalOrders / 35.0) * 0.15);

        return scores;
    }

    private PersonaTag pickBestTag(double[] scores) {
        int best = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[best]) best = i;
        }
        return PersonaTag.values()[best];
    }

    // ═══════════════════════════════════════════════════════════
    // 话术生成（模板 + 行为数据填充）
    // ═══════════════════════════════════════════════════════════

    private String buildScript(DriverDailySnapshot s, PersonaTag tag) {
        String name = s.getDriverName() != null ? s.getDriverName() : "师傅";
        return switch (tag) {
            case PRICE_SENSITIVE -> String.format(
                "%s，平台限时回归奖励已到账！现在上线每单额外补贴，连续完成更有阶梯加奖，别错过这波优惠！", name);
            case TIME_SENSITIVE -> String.format(
                "%s，今晚高峰时段溢价最高达2.5倍，周边商圈订单暴增，一小时流水轻松破两百，赶紧上线抢单！", name);
            case WAY_HOME -> String.format(
                "%s，系统检测到您回家方向有顺路订单，不绕路不空驶，下班路上顺手赚一单，打开App即可查看！", name);
            case WEEKEND_PART_TIME -> String.format(
                "%s，周末客流高峰即将到来，平台周末专属奖励已开启，出来跑几单轻松补贴家用！", name);
            case STABLE_FULL_TIME -> String.format(
                "%s，您的专属全职保障计划已生效：每日优先派单+收入保障，现在回来稳定出车收入无忧！", name);
        };
    }

    // ═══════════════════════════════════════════════════════════
    // 渠道选择
    // ═══════════════════════════════════════════════════════════

    private String pickChannel(PersonaTag tag, DriverDailySnapshot s) {
        int activeDays = s.getActiveDays7d() != null ? s.getActiveDays7d() : 0;
        double recallPotential = estimateRecallPotential(s);

        // 高潜力 + 全职/稳定 → 电话外呼转化率最高
        if (recallPotential > 0.8 && (tag == PersonaTag.STABLE_FULL_TIME || activeDays >= 6)) {
            return OutreachChannel.PHONE.name();
        }
        // App 推送：高频使用 App 的司机（活跃天数多）
        if (activeDays >= 4) {
            return OutreachChannel.APP_PUSH.name();
        }
        // 短信：覆盖面最广，成本最低
        return OutreachChannel.SMS.name();
    }

    private double estimateRecallPotential(DriverDailySnapshot s) {
        double online = s.getOnlineCount7d() != null ? s.getOnlineCount7d() : 0;
        double active = s.getActiveDays7d() != null ? s.getActiveDays7d() : 0;
        double cancel = s.getCancelRate7d() != null ? s.getCancelRate7d().doubleValue() : 0.5;
        return clamp(online / 14.0 * 0.4 + active / 7.0 * 0.4 + (1 - cancel) * 0.2);
    }

    private String buildReasoning(double[] scores, PersonaTag tag) {
        return String.format("规则引擎判定: 各画像得分 [价格:%d%%, 时间:%d%%, 顺路:%d%%, 周末:%d%%, 全职:%d%%], 最高=%s",
            (int)(scores[0]*100), (int)(scores[1]*100), (int)(scores[2]*100),
            (int)(scores[3]*100), (int)(scores[4]*100), tag.getLabel());
    }

    private double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
