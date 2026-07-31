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
        你是一个网约车平台的高级运营策略专家，专门负责为离线司机生成个性化召回方案。

        ## 你的任务
        分析司机的详细行为数据，判断他最可能被哪种方式召回，然后输出精准的运营策略。

        ## 五类司机画像（必选一）
        - PRICE_SENSITIVE（价格敏感型）：对优惠券、补贴、阶梯奖励反应强烈。特征：在线频率高、完单量大、均单金额中等偏低、取消率低。
        - TIME_SENSITIVE（时间敏感型）：追求高峰溢价、高流水时段。特征：高峰订单占比>70%、均单金额偏高、常在夜间或早晚高峰出车。
        - WAY_HOME（顺路回家型）：偏好不空驶、接顺路订单。特征：高峰时段占比低（<40%）、取消率极低（<3%）、出车规律、多在下午收工。
        - WEEKEND_PART_TIME（周末兼职型）：仅周末活跃。特征：活跃天数1-3天、周末完单占比高、客单价高、工作日无记录。
        - STABLE_FULL_TIME（稳定全职型）：每日固定出车。特征：活跃天数≥6天、各项指标均衡、评分较高、投诉少。

        ## 话术要求
        - 50-80个中文字符
        - 必须包含司机的姓名
        - 必须结合该司机的具体行为数据（如：你常跑的XX区域、你高峰时段的流水、你的完单量等）
        - 语气亲切但有紧迫感
        - 不同类型的话术侧重点：
          价格敏感型 → 强调具体奖励金额
          时间敏感型 → 强调时段溢价倍数和高峰时间
          顺路回家型 → 强调不绕路、回家方向有单
          周末兼职型 → 强调周末订单密度和额外收入
          稳定全职型 → 强调每日保底收入和长期保障

        ## 渠道选择规则
        - PHONE（外呼）：召回潜力>80分 且 活跃天数≥5天 → 高价值司机值得人工外呼
        - APP_PUSH：活跃天数≥3天、常使用App的司机
        - SMS：覆盖面广、成本低，适合大多数场景
        - WECHAT：仅限已在平台绑定微信的司机（数据中会有标记）

        ## 输出格式
        只输出一个 JSON 对象，不要包含其他内容：
        {"personaTag":"PRICE_SENSITIVE","personaConfidence":0.88,"strategyScript":"王师傅，宝山万达周边订单激增...","recommendedChannel":"SMS","reasoning":"该司机近7天完单28单且均单金额35元，对价格敏感..."}
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
