package com.competition.invoice.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 分布图视图
 */
@Data
public class DistributionVO {

    private String type;
    private List<DistributionItem> items;

    @Data
    public static class DistributionItem {
        private String label;
        private Long value;
        private String color;

        public static DistributionItem of(String label, Long value, String color) {
            DistributionItem item = new DistributionItem();
            item.label = label;
            item.value = value;
            item.color = color;
            return item;
        }
    }
}
