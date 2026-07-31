package com.competition.invoice.service.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 特征工程 —— 从司机原始行为数据计算 10 维特征向量
 *
 * 这是连接"原始数据"和"模型推理"的关键环节。
 * 输入：t_driver_daily_snapshot 中除 feature_vector 外的所有业务字段
 * 输出：归一化到 [0, 1] 的 10 维特征向量
 *
 * 特征维度及业务依据：
 *
 *  [0] 活跃度        — 近7天在线次数归一化。依据：在线次数是"司机是否愿意继续接单"的最强信号，
 *                      类比用户 DAU 指标，频率越高 = 召回响应概率越大。
 *  [1] 轨迹得分      — 完单量/在线天数。依据：反映了"上线后实际产出效率"，
 *                      区别于"开了 App 但不接单"的假活跃。
 *  [2] 时间偏好度    — 高峰时段订单占比。依据：高峰时段接单的司机对"溢价/高流水"更敏感，
 *                      这类司机召回时推时段溢价效果更好。
 *  [3] 响应率        — 1 - 取消率。依据：取消率低 = 司机对订单不挑拣，召回意愿强。
 *  [4] 收入敏感度    — 均单金额归一化(以 50 元为上限)。依据：高客单价司机对价格信号敏感，
 *                      推优惠券/奖励文案转化率高。
 *  [5] 投诉惩罚      — 投诉次数/总完单。依据：高投诉率司机召回后体验差，可能引发二次流失。
 *  [6] 取消率惩罚    — 直接取 cancel_rate 的补数。依据：取消率高 = 司机存在挑单习惯，
 *                      召回后也可能随意取消，浪费运力。
 *  [7] 活跃天数      — active_days / 7。依据：稳定出车习惯是"全职/半全职"信号，
 *                      这类司机 LTV 更高，值得优先召回。
 *  [8] 高峰时段得分  — peak_hour_pct 直接作为特征。依据：时段匹配度影响召回话术选择，
 *                      也是 NN 模型区分"时间敏感型"vs"价格敏感型"的关键维度。
 *  [9] 忠诚度        — 综合评分：评分高 + 完单多 + 在线天数多。依据：平台粘性指标，
 *                      高忠诚度司机对平台有感情，召回成功率高。
 */
@Slf4j
@Component
public class FeatureEngineeringService {

    /**
     * 最大在线次数（用于归一化，超过此值截断）
     */
    private static final double MAX_ONLINE_COUNT = 21.0;

    /**
     * 最大均单金额（用于归一化）
     */
    private static final double MAX_AVG_ORDER = 50.0;

    /**
     * 从原始字段计算特征向量
     *
     * @param onlineCount7d    近7天上线次数
     * @param totalOrders7d    近7天完单量
     * @param activeDays7d     近7天活跃天数
     * @param peakHourPct      高峰时段订单占比 (0-1)
     * @param cancelRate7d     取消率 (0-1)
     * @param complaintCount7d 投诉次数
     * @param avgOrderAmount   均单金额
     * @param avgRating        均评分 (1-5)
     * @return 10 维特征向量，每维 [0, 1]
     */
    public double[] compute(RawFields f) {
        double[] vec = new double[10];

        // [0] 活跃度：在线次数 / 21（假设一天最多上线 3 次 × 7 天）
        vec[0] = clamp(f.onlineCount7d / MAX_ONLINE_COUNT);

        // [1] 轨迹得分：完单量 / 在线天数，归一化（假设一天最多 8 单）
        double dailyOrders = f.activeDays7d > 0
                ? (double) f.totalOrders7d / f.activeDays7d
                : 0;
        vec[1] = clamp(dailyOrders / 8.0);

        // [2] 时间偏好度：高峰订单占比，越高 = 越偏好高峰时段
        vec[2] = clamp(f.peakHourPct);

        // [3] 响应率：1 - 取消率
        vec[3] = clamp(1.0 - f.cancelRate7d);

        // [4] 收入敏感度：均单金额越高 → 对价格越敏感
        vec[4] = clamp(f.avgOrderAmount / MAX_AVG_ORDER);

        // [5] 投诉惩罚：投诉率（投诉/完单），取值越高分越低
        double complaintRate = f.totalOrders7d > 0
                ? (double) f.complaintCount7d / f.totalOrders7d
                : 0;
        vec[5] = clamp(complaintRate);  // NN 里权重为负

        // [6] 取消率惩罚：直接取取消率做负向特征
        vec[6] = clamp(f.cancelRate7d);

        // [7] 活跃天数：活跃天数 / 7
        vec[7] = clamp(f.activeDays7d / 7.0);

        // [8] 高峰时段得分
        vec[8] = clamp(f.peakHourPct);

        // [9] 忠诚度：评分归一化 × 0.5 + 完单量归一化 × 0.3 + 活跃天数归一化 × 0.2
        double ratingNorm = (f.avgRating - 1.0) / 4.0;  // 1-5 映射到 0-1
        double ordersNorm = clamp(f.totalOrders7d / 56.0);  // 7天×8单=56
        double activeNorm = clamp(f.activeDays7d / 7.0);
        vec[9] = clamp(ratingNorm * 0.5 + ordersNorm * 0.3 + activeNorm * 0.2);

        return vec;
    }

    private double clamp(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return Math.max(0, Math.min(1, v));
    }

    // ---------- 原始字段容器 ----------

    @lombok.Data
    @lombok.Builder
    public static class RawFields {
        private int onlineCount7d;
        private int totalOrders7d;
        private int activeDays7d;
        private double peakHourPct;
        private double cancelRate7d;
        private int complaintCount7d;
        private double avgOrderAmount;
        private double avgRating;
    }
}
