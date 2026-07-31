package com.competition.invoice.model.enums;

/**
 * 管道运行模式
 */
public enum PipelineMode {
    DAILY("日常定时召回"),
    EMERGENCY("应急实时召回");

    private final String label;

    PipelineMode(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
