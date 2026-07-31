package com.competition.invoice.model.enums;

/**
 * 触达渠道
 */
public enum OutreachChannel {

    SMS("短信"),
    PHONE("外呼"),
    APP_PUSH("App推送"),
    WECHAT("微信");

    private final String label;

    OutreachChannel(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public static OutreachChannel fuzzyMatch(String text) {
        if (text == null) return SMS;
        String t = text.toUpperCase();
        if (t.contains("SMS") || t.contains("短信")) return SMS;
        if (t.contains("PHONE") || t.contains("电话") || t.contains("外呼")) return PHONE;
        if (t.contains("PUSH") || t.contains("推送") || t.contains("APP")) return APP_PUSH;
        if (t.contains("WECHAT") || t.contains("微信")) return WECHAT;
        return SMS;
    }
}
