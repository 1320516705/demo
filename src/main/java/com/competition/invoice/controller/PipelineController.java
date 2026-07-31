package com.competition.invoice.controller;

import com.competition.invoice.common.Result;
import com.competition.invoice.entity.RecallPipelineLog;
import com.competition.invoice.mapper.RecallPipelineLogMapper;
import com.competition.invoice.model.enums.PipelineMode;
import com.competition.invoice.service.pipeline.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 管道控制 API
 */
@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineOrchestrator orchestrator;
    private final RecallPipelineLogMapper logMapper;

    /**
     * 手动触发管道
     *
     * skipDataPull: true=跳过数仓拉取(用已有数据), false=强制拉取, 不传=自动判断
     * skipLlm:     true=跳过LLM策略生成, false=强制生成, 不传=根据API Key自动判断
     */
    @PostMapping("/trigger")
    public Result<Map<String, Object>> trigger(@RequestBody(required = false) TriggerRequest request) {
        LocalDate dataDate = (request != null && request.getDataDate() != null)
                ? request.getDataDate()
                : LocalDate.now().minusDays(1);

        Boolean skipDataPull = request != null ? request.getSkipDataPull() : null;
        Boolean skipLlm = request != null ? request.getSkipLlm() : null;

        // 异步执行
        orchestrator.executeAsync(dataDate, PipelineMode.DAILY, skipDataPull, skipLlm);

        return Result.ok(Map.of(
                "dataDate", dataDate.toString(),
                "status", "RUNNING",
                "skipDataPull", skipDataPull != null ? skipDataPull : "auto",
                "skipLlm", skipLlm != null ? skipLlm : "auto",
                "message", "管道已触发，请通过 /status 接口轮询进度"
        ));
    }

    /**
     * 查询管道状态
     */
    @GetMapping("/status/{runId}")
    public Result<RecallPipelineLog> status(@PathVariable Long runId) {
        RecallPipelineLog log = logMapper.selectById(runId);
        if (log == null) {
            return Result.notFound("找不到该管道运行记录");
        }
        return Result.ok(log);
    }

    /**
     * 查询最近一次运行
     */
    @GetMapping("/last-run")
    public Result<RecallPipelineLog> lastRun() {
        RecallPipelineLog log = logMapper.findLastCompleted();
        return Result.ok(log);
    }

    @lombok.Data
    public static class TriggerRequest {
        private LocalDate dataDate;
        /** true=跳过数仓拉取, false=强制拉取, null=自动判断 */
        private Boolean skipDataPull;
        /** true=跳过LLM, false=强制LLM, null=自动判断 */
        private Boolean skipLlm;
    }
}
