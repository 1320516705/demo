package com.competition.invoice.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * KPI 卡片视图
 */
@Data
public class KpiCardVO {

    /** 可召回司机数 */
    private Integer recallableCount;

    /** 预期成功率 */
    private BigDecimal expectedSuccessRate;

    /** 今日预算 */
    private BigDecimal todayBudget;

    /** 规则引擎产出数 */
    private Integer ruleBasedCount;

    /** 较规则提升百分比 */
    private BigDecimal improvementPct;
}
