package com.competition.invoice.service.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 大模型客户端（Claude API）
 *
 * 功能：根据司机行为 JSON，生成个性化召回策略。
 * 限制：并发 5，间隔 300ms。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${external.llm.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${external.llm.api-key:}")
    private String apiKey;

    @Value("${external.llm.model:claude-sonnet-4-20250514}")
    private String model;

    @Value("${external.llm.max-tokens:1024}")
    private int maxTokens;

    @Value("${external.llm.max-concurrent:5}")
    private int maxConcurrent;

    @Value("${external.llm.rate-limit-ms:300}")
    private long rateLimitMs;

    private final Semaphore semaphore = new Semaphore(5);
    private volatile long lastCallTime = 0;

    private static final String SYSTEM_PROMPT = """
        你是一个网约车平台的运营助手，负责为司机生成召回触达话术。

        ## 重要约束
        话术中绝对不能出现以下内容：
        - 具体金额、奖励、补贴、优惠券、红包、返现、免佣
        - 溢价倍数、高流水、多赚钱、额外收入
        话术只需基于司机的出车习惯，用运力需求侧的理由召回。

        ## 话术要求
        - 30-60个中文字符
        - 包含司机姓名
        - 说明所在城市当前订单需求情况
        - 语气亲切、简洁

        ## 渠道选择
        - PHONE：召回潜力>80分且活跃天数≥5天
        - APP_PUSH：活跃天数≥3天
        - SMS：其他情况

        ## 输出格式（严格JSON，无其他内容）
        {"strategyScript":"王师傅，上海近期晚高峰运力紧张，您所在区域订单等候时间较长，方便时上线看看。","recommendedChannel":"SMS"}
        """;

    /**
     * 生成单个司机的召回策略
     */
    public LLMStrategyResponse generateStrategy(String driverName, String behaviorJson) {
        try {
            // 限流控制
            rateLimit();
            semaphore.acquire();
            try {
                String response = callClaudeApi(driverName, behaviorJson);
                return parseResponse(response);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LLM 调用被中断", e);
        } catch (Exception e) {
            log.error("LLM 策略生成失败, driverName={}", driverName, e);
            // 返回兜底策略
            return fallbackStrategy();
        }
    }

    private void rateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTime;
        if (elapsed < rateLimitMs) {
            try {
                Thread.sleep(rateLimitMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private String callClaudeApi(String driverName, String behaviorJson) {
        String userMessage = String.format("司机姓名：%s\n司机行为数据：\n%s", driverName, behaviorJson);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", SYSTEM_PROMPT,
            "messages", List.of(
                Map.of("role", "user", "content", userMessage)
            )
        );

        String responseBody = webClient.post()
                .uri(baseUrl + "/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        // 从 Anthropic 响应中提取文本内容
        try {
            Map<String, Object> respMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) respMap.get("content");
            if (content != null && !content.isEmpty()) {
                return (String) content.get(0).get("text");
            }
        } catch (Exception e) {
            log.warn("解析 Claude 响应异常", e);
        }
        return responseBody;
    }

    private LLMStrategyResponse parseResponse(String rawText) {
        try {
            // 提取 JSON（可能在 markdown 代码块中）
            String json = rawText;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                int end = json.indexOf("```");
                if (end > 0) json = json.substring(0, end);
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                int end = json.indexOf("```");
                if (end > 0) json = json.substring(0, end);
            }
            json = json.trim();

            return objectMapper.readValue(json, LLMStrategyResponse.class);
        } catch (Exception e) {
            log.warn("LLM JSON 解析失败, 使用原始文本兜底: {}", rawText.substring(0, Math.min(100, rawText.length())));
            return fallbackStrategy();
        }
    }

    private LLMStrategyResponse fallbackStrategy() {
        LLMStrategyResponse r = new LLMStrategyResponse();
        r.setPersonaTag("STABLE_FULL_TIME");
        r.setPersonaConfidence(0.5);
        r.setStrategyScript("平台近日订单增多，现在上线可享额外奖励，快来接单吧！");
        r.setRecommendedChannel("SMS");
        r.setReasoning("LLM 调用失败，使用兜底策略");
        return r;
    }

    // ---------- 响应 DTO ----------

    @Data
    public static class LLMStrategyResponse {
        private String personaTag;
        private Double personaConfidence;
        private String strategyScript;
        private String recommendedChannel;
        private String reasoning;
    }
}
