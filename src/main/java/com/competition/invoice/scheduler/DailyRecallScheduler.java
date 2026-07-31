package com.competition.invoice.scheduler;

import com.competition.invoice.model.enums.PipelineMode;
import com.competition.invoice.service.pipeline.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日定时召回调度器
 *
 * 每天 7:00 AM 自动运行，处理前一天的司机数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.daily-recall.enabled", havingValue = "true", matchIfMissing = true)
public class DailyRecallScheduler {

    private final PipelineOrchestrator orchestrator;

    /**
     * 每天凌晨 7:00 执行
     * ShedLock 保证多实例下只执行一次
     */
    @Scheduled(cron = "${scheduling.daily-recall.cron:0 0 7 * * ?}")
    @SchedulerLock(name = "DailyRecallTask",
            lockAtLeastFor = "PT5M",
            lockAtMostFor = "PT1H")
    public void runDailyPipeline() {
        LocalDate dataDate = LocalDate.now().minusDays(1); // T-1
        log.info("====== 定时召回任务触发, dataDate={} ======", dataDate);
        orchestrator.executeAsync(dataDate, PipelineMode.DAILY, null, null);
    }
}
