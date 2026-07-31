package com.competition.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日趋势数据（图表缓存）
 */
@Data
@TableName("t_daily_trend")
public class DailyTrend {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate dataDate;

    private Integer recallableCount;
    private Integer highPotentialCount;
    private Integer contactedCount;
    private Integer agreedCount;
    private BigDecimal actualSuccessRate;

    private LocalDateTime createdAt;
}
