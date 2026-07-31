package com.competition.invoice.service.emergency;

import com.competition.invoice.entity.EmergencyRecall;
import com.competition.invoice.mapper.EmergencyRecallMapper;
import com.competition.invoice.service.emergency.McpServiceClient.NearbyDriver;
import com.competition.invoice.service.external.LLMClient;
import com.competition.invoice.service.external.NeuralNetworkClient;
import com.competition.invoice.service.external.NeuralNetworkClient.InferenceInput;
import com.competition.invoice.service.external.NeuralNetworkClient.InferenceResult;
import com.competition.invoice.service.external.LLMResponseParser;
import com.competition.invoice.service.external.LLMResponseParser.ParsedStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应急实时召回服务
 *
 * 流程：MCP查询 → NN打分 → 筛选高潜力 → LLM生成策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyRecallService {

    private final EmergencyRecallMapper emergencyRecallMapper;
    private final McpServiceClient mcpServiceClient;
    private final NeuralNetworkClient nnClient;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    /**
     * 异步执行应急召回
     */
    @Async
    public void executeAsync(String sessionId, List<double[]> polygon, String regionDesc, String operatorId) {
        try {
            // Step 1: 查询周边司机
            updateStatus(sessionId, "QUERYING");
            List<NearbyDriver> nearby = mcpServiceClient.queryNearbyDrivers(polygon);

            updateCount(sessionId, nearby.size(), 0, 0);

            if (nearby.isEmpty()) {
                completeSession(sessionId, "COMPLETED", "[]");
                return;
            }

            // Step 2: NN 打分（过滤在线司机）
            updateStatus(sessionId, "SCORING");
            List<NearbyDriver> onlineDrivers = nearby.stream()
                    .filter(d -> "ONLINE".equals(d.getStatus()) || "BUSY".equals(d.getStatus()))
                    .toList();

            if (onlineDrivers.isEmpty()) {
                updateCount(sessionId, nearby.size(), 0, 0);
                completeSession(sessionId, "COMPLETED", "[]");
                return;
            }

            List<InferenceInput> inputs = onlineDrivers.stream().map(d -> {
                InferenceInput in = new InferenceInput();
                in.setDriverId(d.getDriverId());
                in.setFeatures(parseFeature(d.getFeatureVector()));
                return in;
            }).collect(Collectors.toList());

            List<InferenceResult> scores = nnClient.batchInference(inputs, "v1.0-emergency");
            Map<String, Double> scoreMap = scores.stream()
                    .collect(Collectors.toMap(InferenceResult::getDriverId, InferenceResult::getRecallScore));

            updateCount(sessionId, nearby.size(), scores.size(), 0);

            // Step 3: 筛选高潜力 + LLM 生成策略
            updateStatus(sessionId, "STRATEGIZING");

            List<NearbyDriver> highPotential = onlineDrivers.stream()
                    .filter(d -> scoreMap.getOrDefault(d.getDriverId(), 0.0) > 60)
                    .toList();

            List<Map<String, Object>> results = new ArrayList<>();
            for (NearbyDriver d : highPotential) {
                try {
                    String behaviorJson = objectMapper.writeValueAsString(d);
                    var raw = llmClient.generateStrategy(d.getDriverName(), behaviorJson);
                    ParsedStrategy parsed = LLMResponseParser.parse(raw);

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("driverId", d.getDriverId());
                    item.put("driverName", d.getDriverName());
                    item.put("phone", d.getPhone());
                    item.put("lng", d.getLng());
                    item.put("lat", d.getLat());
                    item.put("recallScore", scoreMap.getOrDefault(d.getDriverId(), 0.0));
                    item.put("personaTag", parsed.getPersonaTag());
                    item.put("strategyScript", parsed.getStrategyScript());
                    item.put("recommendedChannel", parsed.getRecommendedChannel());
                    results.add(item);
                } catch (Exception e) {
                    log.warn("应急策略生成失败: driverId={}", d.getDriverId(), e);
                }
            }

            updateCount(sessionId, nearby.size(), scores.size(), highPotential.size());

            // 完成
            completeSession(sessionId, "COMPLETED", objectMapper.writeValueAsString(results));

        } catch (Exception e) {
            log.error("应急召回失败, sessionId={}", sessionId, e);
            completeSession(sessionId, "FAILED", "[]");
        }
    }

    private void updateStatus(String sessionId, String status) {
        EmergencyRecall record = emergencyRecallMapper.findBySessionId(sessionId);
        if (record != null) {
            record.setStatus(status);
            emergencyRecallMapper.updateById(record);
        }
    }

    private void updateCount(String sessionId, int nearby, int scored, int highPotential) {
        EmergencyRecall record = emergencyRecallMapper.findBySessionId(sessionId);
        if (record != null) {
            record.setNearbyDrivers(nearby);
            record.setScoredDrivers(scored);
            record.setHighPotential(highPotential);
            emergencyRecallMapper.updateById(record);
        }
    }

    private void completeSession(String sessionId, String status, String resultJson) {
        EmergencyRecall record = emergencyRecallMapper.findBySessionId(sessionId);
        if (record != null) {
            record.setStatus(status);
            record.setResultData(resultJson);
            record.setCompletedAt(LocalDateTime.now());
            emergencyRecallMapper.updateById(record);
        }
    }

    private double[] parseFeature(String featureVectorJson) {
        if (featureVectorJson == null || featureVectorJson.isEmpty()) return new double[0];
        try {
            List<Double> list = objectMapper.readValue(featureVectorJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
            return list.stream().mapToDouble(Double::doubleValue).toArray();
        } catch (Exception e) {
            return new double[0];
        }
    }
}
