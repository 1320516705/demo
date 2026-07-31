package com.competition.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 司机每日快照（从数仓拉取）
 */
@Data
@TableName("t_driver_daily_snapshot")
public class DriverDailySnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate snapshotDate;
    private String driverId;
    private String driverName;
    private String phone;

    /** 近7天上线次数 */
    @TableField("online_count_7d")
    private Integer onlineCount7d;

    /** 最后完单时间 */
    private LocalDateTime lastOrderTime;

    /** H3网格索引(level 8) */
    private String h3Index;

    /** 所在H3网格供需比 */
    @TableField("supply_demand_ratio")
    private BigDecimal supplyDemandRatio;

    /** 近7天均单金额 */
    @TableField("avg_order_amount")
    private BigDecimal avgOrderAmount;

    /** 近7天均评分 */
    @TableField("avg_rating")
    private BigDecimal avgRating;

    /** 近7天完单量 */
    @TableField("total_orders_7d")
    private Integer totalOrders7d;

    /** 近7天投诉数 */
    @TableField("complaint_count_7d")
    private Integer complaintCount7d;

    /** 近7天取消率 */
    @TableField("cancel_rate_7d")
    private BigDecimal cancelRate7d;

    /** 近7天活跃天数 */
    @TableField("active_days_7d")
    private Integer activeDays7d;

    /** 高峰时段订单占比 */
    @TableField("peak_hour_pct")
    private BigDecimal peakHourPct;

    /** 特征向量（JSON格式，供NN推理） */
    private String featureVector;

    private LocalDateTime createdAt;
}
