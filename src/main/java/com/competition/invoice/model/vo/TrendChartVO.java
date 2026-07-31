package com.competition.invoice.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 趋势图视图
 */
@Data
public class TrendChartVO {

    private List<TrendPoint> points;

    @Data
    public static class TrendPoint {
        private LocalDate date;
        private Integer recallable;
        private Integer highPotential;
        private Integer contacted;
        private Integer agreed;
        private BigDecimal successRate;
    }
}
