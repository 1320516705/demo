package com.competition.invoice.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 召回名单视图对象
 */
@Data
public class RecallListVO {

    private Long id;
    private String driverId;
    private String driverName;
    private String phone;

    /** 召回潜力分 */
    private BigDecimal recallScore;

    /** 人设标签 */
    private String personaTag;
    private String personaTagLabel;

    /** 个性化召回话术 */
    private String strategyScript;

    /** 推荐触达渠道 */
    private String recommendedChannel;
    private String recommendedChannelLabel;

    /** 触达状态 */
    private String outreachStatus;
    private String outreachStatusLabel;
    private LocalDateTime outreachTime;

    /** LLM 原始响应（仅详情接口返回） */
    private String llmResponseRaw;
}
