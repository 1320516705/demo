package com.competition.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 召回流水线执行日志
 */
@Data
@TableName("t_recall_pipeline_log")
public class RecallPipelineLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate runDate;
    private LocalDate dataDate;
    private String mode;    // DAILY / EMERGENCY
    private String status;  // PENDING/RUNNING/COMPLETED/FAILED
    private String step;

    private Integer preFilterIn;
    private Integer preFilterOut;
    private Integer nnScored;
    private Integer highPotential;
    private Integer llmGenerated;
    private Integer finalProduced;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMsg;

    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
