package com.competition.invoice.model.enums;

/**
 * 管道执行状态
 */
public enum PipelineStatus {

    PENDING("待执行"),
    RUNNING("运行中"),
    COMPLETED("已完成"),
    FAILED("失败");

    private final String label;

    PipelineStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
