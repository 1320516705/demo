package com.competition.invoice.service.external;

import com.competition.invoice.model.enums.OutreachChannel;
import com.competition.invoice.model.enums.PersonaTag;
import com.competition.invoice.service.external.LLMClient.LLMStrategyResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * LLM 响应解析器 — 对 Claude 的输出做二次校验和兜底
 */
@Slf4j
public final class LLMResponseParser {

    private LLMResponseParser() {}

    /**
     * 校验并规范化 LLM 响应
     */
    public static ParsedStrategy parse(LLMStrategyResponse raw) {
        ParsedStrategy parsed = new ParsedStrategy();

        // 1. 人设标签 — 模糊匹配 + 兜底
        PersonaTag personaTag = PersonaTag.fuzzyMatch(raw.getPersonaTag());
        parsed.setPersonaTag(personaTag.name());

        // 2. 置信度 — 校验范围 + 兜底
        Double confidence = raw.getPersonaConfidence();
        if (confidence == null || confidence < 0 || confidence > 1) {
            parsed.setPersonaConfidence(0.5);
        } else {
            parsed.setPersonaConfidence(confidence);
        }

        // 3. 召回话术 — 长度校验 + 兜底
        String script = raw.getStrategyScript();
        if (script == null || script.trim().isEmpty() || script.length() < 10) {
            script = "平台近日订单增多，现在上线可享限时奖励，快来接单吧！";
        }
        // 限制长度
        if (script.length() > 200) {
            script = script.substring(0, 200);
        }
        parsed.setStrategyScript(script.trim());

        // 4. 推荐渠道 — 模糊匹配 + 兜底
        OutreachChannel channel = OutreachChannel.fuzzyMatch(raw.getRecommendedChannel());
        parsed.setRecommendedChannel(channel.name());

        // 5. 保留原始响应（调试用）和 LLM 理由
        parsed.setReasoning(raw.getReasoning());

        return parsed;
    }

    /**
     * 解析后的策略（经过校验）
     */
    @lombok.Data
    public static class ParsedStrategy {
        private String personaTag;
        private Double personaConfidence;
        private String strategyScript;
        private String recommendedChannel;
        private String reasoning;
    }
}
