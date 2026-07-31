package com.competition.invoice.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 热力图数据
 */
@Data
public class HeatMapPointVO {

    private List<HeatPoint> points;

    @Data
    public static class HeatPoint {
        private Double lng;
        private Double lat;
        private Integer count;
        private BigDecimal avgScore;
        private String h3Index;
    }
}
