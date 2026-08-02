package com.competition.invoice.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class KpiCardVO {
    /** 可召回司机总数 */
    private Integer recallableCount;
    /** 近7天在线但今日不在线的司机数 */
    private Integer onlineYesterdayNotToday;
    /** 预期召回成功率 */
    private BigDecimal expectedSuccessRate;
}
