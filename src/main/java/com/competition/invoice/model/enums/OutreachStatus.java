package com.competition.invoice.model.enums;

/**
 * 触达状态
 */
public enum OutreachStatus {

    PENDING("待触达"),
    CONTACTED("已触达"),
    AGREED("已同意"),
    DECLINED("已拒绝"),
    NO_RESPONSE("无响应");

    private final String label;

    OutreachStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
