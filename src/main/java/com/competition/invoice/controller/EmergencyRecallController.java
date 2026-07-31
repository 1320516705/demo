package com.competition.invoice.controller;

import com.competition.invoice.common.Result;
import com.competition.invoice.entity.EmergencyRecall;
import com.competition.invoice.mapper.EmergencyRecallMapper;
import com.competition.invoice.model.dto.EmergencyRegionDTO;
import com.competition.invoice.service.emergency.EmergencyRecallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 应急实时召回 API
 */
@RestController
@RequestMapping("/api/v1/emergency")
@RequiredArgsConstructor
public class EmergencyRecallController {

    private final EmergencyRecallService emergencyRecallService;
    private final EmergencyRecallMapper emergencyRecallMapper;

    /**
     * 触发应急召回
     */
    @PostMapping("/recall")
    public Result<Map<String, Object>> recall(@Valid @RequestBody EmergencyRegionDTO request) {
        String sessionId = UUID.randomUUID().toString();

        // 构建多边形 WKT
        String polygonWkt = "POLYGON((" +
                request.getPolygon().stream()
                        .map(p -> p[0] + " " + p[1])
                        .collect(Collectors.joining(", ")) +
                "))";

        // 创建应急召回记录
        EmergencyRecall record = new EmergencyRecall();
        record.setSessionId(sessionId);
        record.setRegionPolygon(polygonWkt);
        record.setRegionDesc(request.getRegionDesc());
        record.setOperatorId(request.getOperatorId());
        record.setStatus("QUERYING");
        record.setNearbyDrivers(0);
        record.setScoredDrivers(0);
        record.setHighPotential(0);
        record.setStartedAt(LocalDateTime.now());
        emergencyRecallMapper.insert(record);

        // 异步执行
        emergencyRecallService.executeAsync(
                sessionId,
                request.getPolygon(),
                request.getRegionDesc(),
                request.getOperatorId());

        return Result.ok(Map.of(
                "sessionId", sessionId,
                "status", "QUERYING"
        ));
    }

    /**
     * 轮询应急召回状态
     */
    @GetMapping("/status/{sessionId}")
    public Result<Map<String, Object>> status(@PathVariable String sessionId) {
        EmergencyRecall record = emergencyRecallMapper.findBySessionId(sessionId);
        if (record == null) {
            return Result.notFound("找不到该会话");
        }
        return Result.ok(Map.of(
                "sessionId", record.getSessionId(),
                "status", record.getStatus(),
                "nearbyDrivers", record.getNearbyDrivers(),
                "scoredDrivers", record.getScoredDrivers(),
                "highPotential", record.getHighPotential()
        ));
    }

    /**
     * 获取应急召回结果
     */
    @GetMapping("/result/{sessionId}")
    public Result<Map<String, Object>> result(@PathVariable String sessionId) {
        EmergencyRecall record = emergencyRecallMapper.findBySessionId(sessionId);
        if (record == null) {
            return Result.notFound("找不到该会话");
        }
        return Result.ok(Map.of(
                "sessionId", record.getSessionId(),
                "status", record.getStatus(),
                "nearbyDrivers", record.getNearbyDrivers(),
                "scoredDrivers", record.getScoredDrivers(),
                "highPotential", record.getHighPotential(),
                "resultData", record.getResultData()
        ));
    }
}
