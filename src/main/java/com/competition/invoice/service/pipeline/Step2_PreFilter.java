package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管道步骤2：前置过滤
 *
 * 满足全部三个条件的司机进入NN推理：
 * 1. 近7天至少有一次在线记录
 * 2. 最近一次完单时间在7天以内
 * 3. 所在H3网格的供需比 ≥ 1.5
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step2_PreFilter {

    private final DriverDailySnapshotMapper snapshotMapper;

    @Value("${recall.supply-demand-ratio-threshold:1.5}")
    private BigDecimal supplyDemandRatioThreshold;

    @Value("${recall.recent-active-days:7}")
    private int recentActiveDays;

    @Value("${recall.max-last-order-days:7}")
    private int maxLastOrderDays;

    /**
     * 执行前置过滤
     * @param dataDate 数据日期
     * @return 过滤后的司机数量
     */
    public int execute(LocalDate dataDate) {
        log.info("[Step2] 开始前置过滤, dataDate={}", dataDate);

        // 查询满足三个过滤条件的司机快照
        LocalDateTime orderCutoff = dataDate.atStartOfDay().minusDays(maxLastOrderDays);

        List<DriverDailySnapshot> filtered = snapshotMapper.selectList(
                new LambdaQueryWrapper<DriverDailySnapshot>()
                        .eq(DriverDailySnapshot::getSnapshotDate, dataDate)
                        // 条件1: 近7天至少有一次在线记录
                        .ge(DriverDailySnapshot::getOnlineCount7d, 1)
                        // 条件2: 最后完单时间在7天以内
                        .ge(DriverDailySnapshot::getLastOrderTime, orderCutoff)
                        // 条件3: 所在H3网格供需比 >= 1.5
                        .ge(DriverDailySnapshot::getSupplyDemandRatio, supplyDemandRatioThreshold)
        );

        log.info("[Step2] 完成, 过滤后={}", filtered.size());
        return filtered.size();
    }
}
