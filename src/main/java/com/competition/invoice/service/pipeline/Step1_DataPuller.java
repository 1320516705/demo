package com.competition.invoice.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import com.competition.invoice.service.external.WarehouseClient;
import com.competition.invoice.service.external.WarehouseClient.SnapshotRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 管道步骤1：从数仓拉取 T-1 司机快照数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step1_DataPuller {

    private final WarehouseClient warehouseClient;
    private final DriverDailySnapshotMapper snapshotMapper;

    /**
     * 拉取并持久化司机快照
     * @param dataDate 数据日期 (T-1)
     * @return 拉取的司机数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int execute(LocalDate dataDate) {
        log.info("[Step1] 开始拉取数仓数据, dataDate={}", dataDate);

        // 1. 删除当天已有数据（幂等重跑）
        snapshotMapper.delete(new LambdaQueryWrapper<DriverDailySnapshot>()
                .eq(DriverDailySnapshot::getSnapshotDate, dataDate));

        // 2. 从数仓拉取
        List<SnapshotRow> rows = warehouseClient.pullDailySnapshot(dataDate);

        if (rows.isEmpty()) {
            log.warn("[Step1] 数仓无数据, dataDate={}", dataDate);
            return 0;
        }

        // 3. 批量插入（分批，每批500条）
        List<DriverDailySnapshot> batch = new ArrayList<>(500);
        int totalInserted = 0;

        for (SnapshotRow row : rows) {
            batch.add(toEntity(row));
            if (batch.size() >= 500) {
                saveBatch(batch);
                totalInserted += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            saveBatch(batch);
            totalInserted += batch.size();
        }

        log.info("[Step1] 完成, 拉取={} 插入={}", rows.size(), totalInserted);
        return totalInserted;
    }

    private void saveBatch(List<DriverDailySnapshot> batch) {
        for (DriverDailySnapshot entity : batch) {
            snapshotMapper.insert(entity);
        }
    }

    private DriverDailySnapshot toEntity(SnapshotRow row) {
        DriverDailySnapshot e = new DriverDailySnapshot();
        e.setSnapshotDate(row.getSnapshotDate());
        e.setDriverId(row.getDriverId());
        e.setDriverName(row.getDriverName());
        e.setPhone(row.getPhone());
        e.setOnlineCount7d(row.getOnlineCount7d());
        e.setLastOrderTime(row.getLastOrderTime());
        e.setSupplyDemandRatio(row.getSupplyDemandRatio());
        e.setAvgOrderAmount(row.getAvgOrderAmount());
        e.setAvgRating(row.getAvgRating());
        e.setTotalOrders7d(row.getTotalOrders7d());
        e.setComplaintCount7d(row.getComplaintCount7d());
        e.setCancelRate7d(row.getCancelRate7d());
        e.setActiveDays7d(row.getActiveDays7d());
        e.setPeakHourPct(row.getPeakHourPct());
        e.setFeatureVector(row.getFeatureVector());
        return e;
    }
}
