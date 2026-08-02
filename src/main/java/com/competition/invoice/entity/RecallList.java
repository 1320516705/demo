package com.competition.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 召回名单（核心产出表）
 */
@Data
@TableName("t_recall_list")
public class RecallList {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pipelineRunId;
    private LocalDate dataDate;
    private String driverId;
    private String driverName;
    private String phone;

    /** 召回潜力分(0-100) */
    private BigDecimal recallScore;
    private String scoreReason;

    /** 个性化召回话术 */
    private String strategyScript;

    /** 推荐触达渠道 */
    private String recommendedChannel;

    /** LLM原始响应JSON */
    private String llmResponseRaw;

    /** 触达状态: PENDING/CONTACTED/AGREED/DECLINED/NO_RESPONSE */
    private String outreachStatus;

    private String outreachChannel;
    private LocalDateTime outreachTime;
    private String outreachRemark;
    private String operatorId;

    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
