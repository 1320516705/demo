package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.entity.RecallList;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import com.competition.invoice.mapper.RecallListMapper;
import com.competition.invoice.service.external.NeuralNetworkClient;
import com.competition.invoice.service.external.NeuralNetworkClient.InferenceInput;
import com.competition.invoice.service.external.NeuralNetworkClient.InferenceResult;
import com.competition.invoice.service.pipeline.FeatureEngineeringService.RawFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管道步骤3：神经网络推理
 *
 * 流程：
 *   1. 读取当天司机快照的原始业务字段
 *   2. 特征工程：从原始字段计算 10 维特征向量
 *   3. 批量调用 NN 推理服务，获取 0-100 召回分
 *   4. 写入 t_recall_list
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step3_NeuralNetwork {

    private final DriverDailySnapshotMapper snapshotMapper;
    private final RecallListMapper recallListMapper;
    private final NeuralNetworkClient nnClient;
    private final FeatureEngineeringService featureEngineering;

    private static final int BATCH_SIZE = 100;

    @Value("${recall.supply-demand-ratio-threshold:1.5}")
    private BigDecimal sdThreshold;

    @Value("${recall.recent-active-days:7}")
    private int recentDays;

    @Transactional(rollbackFor = Exception.class)
    public int execute(LocalDate dataDate, Long pipelineRunId) {
        log.info("[Step3] 开始特征工程 + NN推理, dataDate={}", dataDate);

        // 1. 只读取通过 Step2 同等过滤条件的司机
        LocalDateTime orderCutoff = dataDate.atStartOfDay().minusDays(recentDays);
        List<DriverDailySnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<DriverDailySnapshot>()
                        .eq(DriverDailySnapshot::getSnapshotDate, dataDate)
                        .ge(DriverDailySnapshot::getOnlineCount7d, 1)
                        .ge(DriverDailySnapshot::getLastOrderTime, orderCutoff)
                        .ge(DriverDailySnapshot::getSupplyDemandRatio, sdThreshold));

        if (snapshots.isEmpty()) {
            log.warn("[Step3] 无司机数据");
            return 0;
        }

        // 2. 特征工程：从原始字段计算特征向量
        List<InferenceInput> inputs = new ArrayList<>();
        for (DriverDailySnapshot s : snapshots) {
            RawFields f = RawFields.builder()
                    .onlineCount7d(s.getOnlineCount7d() != null ? s.getOnlineCount7d() : 0)
                    .totalOrders7d(s.getTotalOrders7d() != null ? s.getTotalOrders7d() : 0)
                    .activeDays7d(s.getActiveDays7d() != null ? s.getActiveDays7d() : 0)
                    .peakHourPct(toDouble(s.getPeakHourPct()))
                    .cancelRate7d(toDouble(s.getCancelRate7d()))
                    .complaintCount7d(s.getComplaintCount7d() != null ? s.getComplaintCount7d() : 0)
                    .avgOrderAmount(toDouble(s.getAvgOrderAmount()))
                    .avgRating(toDouble(s.getAvgRating()))
                    .build();

            double[] features = featureEngineering.compute(f);

            InferenceInput in = new InferenceInput();
            in.setDriverId(s.getDriverId());
            in.setFeatures(features);
            inputs.add(in);
        }

        log.info("[Step3] 特征工程完成, 构建 {} 个特征向量", inputs.size());

        // 3. 分批调用 NN 推理服务
        Map<String, Double> scoreMap = new java.util.HashMap<>();
        for (int i = 0; i < inputs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, inputs.size());
            List<InferenceInput> batch = inputs.subList(i, end);
            List<InferenceResult> results = nnClient.batchInference(batch, "v2.0-mlp");
            for (InferenceResult r : results) {
                scoreMap.put(r.getDriverId(), r.getRecallScore());
            }
        }

        // 4. 写入召回名单（后续 Step4 标记高潜力、Step5 LLM 补充策略）
        List<RecallList> recallRows = new ArrayList<>();
        for (DriverDailySnapshot s : snapshots) {
            Double score = scoreMap.get(s.getDriverId());
            if (score == null) continue;

            RecallList rl = new RecallList();
            rl.setPipelineRunId(pipelineRunId);
            rl.setDataDate(dataDate);
            rl.setDriverId(s.getDriverId());
            rl.setDriverName(s.getDriverName());
            rl.setPhone(s.getPhone());
            rl.setRecallScore(BigDecimal.valueOf(score));
            recallRows.add(rl);
        }

        // 5. 删除当天已有结果（幂等重跑）
        recallListMapper.delete(new LambdaQueryWrapper<RecallList>()
                .eq(RecallList::getDataDate, dataDate));

        for (RecallList row : recallRows) {
            recallListMapper.insert(row);
        }

        log.info("[Step3] 完成, 特征工程+NN推理共 {} 条", recallRows.size());
        return recallRows.size();
    }

    private double toDouble(BigDecimal v) {
        return v != null ? v.doubleValue() : 0.0;
    }
}
