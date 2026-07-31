package com.competition.invoice.model.enums;

/**
 * 司机画像标签（5大类）
 */
public enum PersonaTag {

    PRICE_SENSITIVE("价格敏感型", "对优惠券、奖励类激励反应明显"),
    TIME_SENSITIVE("时间敏感型", "对时段溢价、高流水机会反应明显"),
    WAY_HOME("顺路回家型", "偏好接顺路订单，不空驶"),
    WEEKEND_PART_TIME("周末兼职型", "周末活跃，工作日不出车"),
    STABLE_FULL_TIME("稳定全职型", "每天固定出车，追求稳定收入");

    private final String label;
    private final String description;

    PersonaTag(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }

    /**
     * 模糊匹配（用于 LLM 响应解析）
     */
    public static PersonaTag fuzzyMatch(String text) {
        if (text == null) return STABLE_FULL_TIME;
        String t = text.toUpperCase().replace(" ", "_");
        for (PersonaTag tag : values()) {
            if (tag.name().equals(t)) return tag;
        }
        // 中文匹配
        if (t.contains("价格") || t.contains("优惠") || t.contains("奖励")) return PRICE_SENSITIVE;
        if (t.contains("时间") || t.contains("时段") || t.contains("溢价")) return TIME_SENSITIVE;
        if (t.contains("顺路") || t.contains("回家") || t.contains("空驶")) return WAY_HOME;
        if (t.contains("周末") || t.contains("兼职")) return WEEKEND_PART_TIME;
        if (t.contains("全职") || t.contains("稳定") || t.contains("固定")) return STABLE_FULL_TIME;
        return STABLE_FULL_TIME;
    }
}
