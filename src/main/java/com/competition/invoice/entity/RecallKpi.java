package com.competition.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 召回KPI汇总（仪表盘缓存）
 */
@Data
@TableName("t_recall_kpi")
public class RecallKpi {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pipelineRunId;
    private LocalDate dataDate;

    private Integer recallableCount;
    private BigDecimal expectedSuccessRate;
    private BigDecimal todayBudget;
    private Integer ruleBasedCount;
    private BigDecimal improvementPct;

    /** JSON: 人设分布 */
    private String personaDistribution;

    /** JSON: 触达状态分布 */
    private String statusDistribution;

    /** JSON: 分数直方图数据 */
    private String scoreDistribution;

    private LocalDateTime createdAt;
}
