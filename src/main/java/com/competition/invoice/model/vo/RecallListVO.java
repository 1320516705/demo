package com.competition.invoice.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecallListVO {
    private Long id;
    private String driverId;
    private String driverName;
    private String phone;

    // 完单
    private Integer dailyOrders;
    private Integer morningPeakOrders;
    private Integer eveningPeakOrders;

    // 在线
    private BigDecimal dailyOnlineHours;

    // 收入
    private BigDecimal baseIncome;
    private BigDecimal bonusIncome;

    // 召回分
    private BigDecimal recallScore;

    // 话术 + 渠道
    private String strategyScript;
    private String recommendedChannel;
    private String recommendedChannelLabel;

    // 触达状态
    private String outreachStatus;
    private String outreachStatusLabel;
    private LocalDateTime outreachTime;

    // 详情
    private String llmResponseRaw;
}
