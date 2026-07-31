package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.entity.RecallPipelineLog;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import com.competition.invoice.mapper.RecallPipelineLogMapper;
import com.competition.invoice.model.enums.PipelineMode;
import com.competition.invoice.model.enums.PipelineStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管道编排器 — 串联六步召回管道
 *
 * 顺序执行，每步至少提交一次事务。
 * 当外部服务不可用时，自动跳过对应步骤。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final RecallPipelineLogMapper logMapper;
    private final DriverDailySnapshotMapper snapshotMapper;
    private final Step1_DataPuller step1DataPuller;
    private final Step2_PreFilter step2PreFilter;
    private final Step3_NeuralNetwork step3NeuralNetwork;
    private final Step4_HighPotentialFilter step4HighPotentialFilter;
    private final Step5_LLMStrategy step5LLMStrategy;
    private final Step6_FinalProducer step6FinalProducer;

    @Value("${external.llm.api-key:}")
    private String llmApiKey;

    /**
     * 异步执行管道
     */
    @Async
    public void executeAsync(LocalDate dataDate, PipelineMode mode, Boolean skipDataPull, Boolean skipLlm) {
        execute(dataDate, mode, skipDataPull, skipLlm);
    }

    /**
     * 同步执行管道
     *
     * @param dataDate       数据日期
     * @param mode           运行模式
     * @param skipDataPull   true=强制跳过数仓拉取, false=强制执行, null=自动判断
     * @param skipLlm        true=强制跳过LLM, false=强制执行, null=根据API Key自动判断
     */
    public RecallPipelineLog execute(LocalDate dataDate, PipelineMode mode,
                                      Boolean skipDataPull, Boolean skipLlm) {

        // 自动判断：无LLM API Key 则跳过 Step5
        boolean doSkipLlm = skipLlm != null ? skipLlm : (llmApiKey == null || llmApiKey.isEmpty());

        // 自动判断：已有当天数据则跳过 Step1
        boolean doSkipDataPull = skipDataPull != null ? skipDataPull : hasExistingData(dataDate);

        log.info("===== 管道启动 dataDate={}, mode={}, skipDataPull={}, skipLlm={} =====",
                dataDate, mode, doSkipDataPull, doSkipLlm);

        // 创建执行日志
        RecallPipelineLog pipelineLog = new RecallPipelineLog();
        pipelineLog.setRunDate(LocalDate.now());
        pipelineLog.setDataDate(dataDate);
        pipelineLog.setMode(mode.name());
        pipelineLog.setStatus(PipelineStatus.RUNNING.name());
        pipelineLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(pipelineLog);
        Long runId = pipelineLog.getId();

        try {
            // ===== Step 1: 拉取数仓数据 =====
            if (doSkipDataPull) {
                Long existing = snapshotMapper.selectCount(
                        new LambdaQueryWrapper<DriverDailySnapshot>()
                                .eq(DriverDailySnapshot::getSnapshotDate, dataDate));
                pipelineLog.setPreFilterIn(existing != null ? existing.intValue() : 0);
                log.info("[Step1] 跳过(数据已存在), 已有 {} 条", existing);
            } else {
                updateStep(runId, "STEP1_DATA_PULL");
                int pulled = step1DataPuller.execute(dataDate);
                pipelineLog.setPreFilterIn(pulled);
                log.info("[Step1] 完成, 拉取 {} 条", pulled);
            }

            // ===== Step 2: 前置过滤 =====
            updateStep(runId, "STEP2_PRE_FILTER");
            int filtered = step2PreFilter.execute(dataDate);
            pipelineLog.setPreFilterOut(filtered);

            // ===== Step 3: 神经网络推理 =====
            updateStep(runId, "STEP3_NN_INFERENCE");
            int scored = step3NeuralNetwork.execute(dataDate, runId);
            pipelineLog.setNnScored(scored);

            // ===== Step 4: 筛选高潜力 =====
            updateStep(runId, "STEP4_HIGH_POTENTIAL");
            int highPotential = step4HighPotentialFilter.execute(dataDate);
            pipelineLog.setHighPotential(highPotential);

            // ===== Step 5: LLM 策略生成 =====
            if (doSkipLlm) {
                log.info("[Step5] 跳过(未配置 LLM API Key)");
                pipelineLog.setLlmGenerated(0);
            } else {
                updateStep(runId, "STEP5_LLM_STRATEGY");
                int llmGen = step5LLMStrategy.execute(dataDate);
                pipelineLog.setLlmGenerated(llmGen);
            }

            // ===== Step 6: 最终产出 =====
            updateStep(runId, "STEP6_FINAL_PRODUCE");
            int produced = step6FinalProducer.execute(dataDate, runId);
            pipelineLog.setFinalProduced(produced);

            pipelineLog.setStatus(PipelineStatus.COMPLETED.name());
            pipelineLog.setCompletedAt(LocalDateTime.now());
            log.info("===== 管道完成 runId={}, 产出={} =====", runId, produced);

        } catch (Exception e) {
            log.error("===== 管道失败 runId={} =====", runId, e);
            pipelineLog.setStatus(PipelineStatus.FAILED.name());
            pipelineLog.setErrorMsg(e.getMessage());
            pipelineLog.setCompletedAt(LocalDateTime.now());
        }

        logMapper.updateById(pipelineLog);
        return pipelineLog;
    }

    /**
     * 检查当天是否已有快照数据
     */
    private boolean hasExistingData(LocalDate dataDate) {
        Long count = snapshotMapper.selectCount(
                new LambdaQueryWrapper<DriverDailySnapshot>()
                        .eq(DriverDailySnapshot::getSnapshotDate, dataDate));
        return count != null && count > 0;
    }

    private void updateStep(Long runId, String step) {
        RecallPipelineLog log = new RecallPipelineLog();
        log.setId(runId);
        log.setStep(step);
        logMapper.updateById(log);
    }
}
