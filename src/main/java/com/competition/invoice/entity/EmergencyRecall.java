package com.competition.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应急实时召回记录
 */
@Data
@TableName("t_emergency_recall")
public class EmergencyRecall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    /** GEOMETRY类型，使用WKT字符串映射: POLYGON((...)) */
    private String regionPolygon;

    private String regionDesc;
    private String operatorId;

    private Integer nearbyDrivers;
    private Integer scoredDrivers;
    private Integer highPotential;

    /** JSON: 详细结果数据 */
    private String resultData;

    /** QUERYING/SCORING/STRATEGIZING/COMPLETED/FAILED */
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
