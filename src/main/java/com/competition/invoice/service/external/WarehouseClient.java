package com.competition.invoice.service.external;

import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 数仓数据拉取客户端（JDBC 直连）
 *
 * 从数仓的 driver_profile、driver_online_log、order_history 等表中
 * 拼装每个司机的完整行为快照。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseClient {

    @Qualifier("warehouseDataSource")
    private final DataSource warehouseDataSource;

    /**
     * 拉取 T-1 日司机快照数据
     * @return 司机快照列表
     */
    public List<SnapshotRow> pullDailySnapshot(LocalDate dataDate) {
        List<SnapshotRow> rows = new ArrayList<>();
        String sql = buildQuerySql();

        try (Connection conn = warehouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(dataDate));
            stmt.setDate(2, java.sql.Date.valueOf(dataDate.minusDays(7)));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs, dataDate));
                }
            }
        } catch (SQLException e) {
            log.error("从数仓拉取数据失败, dataDate={}", dataDate, e);
            throw new RuntimeException("数仓数据拉取失败", e);
        }

        log.info("从数仓拉取司机快照 {} 条, dataDate={}", rows.size(), dataDate);
        return rows;
    }

    /**
     * 数仓查询 SQL（示例 — 实际按数仓表结构调整）
     */
    private String buildQuerySql() {
        return """
            SELECT
                d.driver_id,
                d.driver_name,
                d.phone,
                COALESCE(dol.online_count, 0) AS online_count_7d,
                oh.last_order_time,
                d.home_h3_index,
                COALESCE(sd.supply_demand_ratio, 0) AS supply_demand_ratio,
                oh.avg_order_amount,
                oh.avg_rating,
                oh.total_orders_7d,
                oh.complaint_count_7d,
                oh.cancel_rate_7d,
                dol.active_days_7d,
                oh.peak_hour_pct,
                dol.feature_vector
            FROM dw.driver_profile d
            LEFT JOIN (
                SELECT driver_id,
                       COUNT(DISTINCT DATE(online_start_time)) AS active_days_7d,
                       COUNT(*) AS online_count,
                       NULL AS feature_vector
                FROM dw.driver_online_log
                WHERE online_start_time >= DATE_SUB(?, INTERVAL 7 DAY)
                GROUP BY driver_id
            ) dol ON d.driver_id = dol.driver_id
            LEFT JOIN (
                SELECT driver_id,
                       MAX(order_complete_time) AS last_order_time,
                       AVG(order_amount) AS avg_order_amount,
                       AVG(rating) AS avg_rating,
                       COUNT(*) AS total_orders_7d,
                       SUM(CASE WHEN has_complaint = 1 THEN 1 ELSE 0 END) AS complaint_count_7d,
                       SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) / COUNT(*) AS cancel_rate_7d,
                       SUM(CASE WHEN HOUR(order_time) IN (7,8,9,17,18,19) THEN 1 ELSE 0 END) / COUNT(*) AS peak_hour_pct
                FROM dw.order_history
                WHERE order_time >= DATE_SUB(?, INTERVAL 7 DAY)
                GROUP BY driver_id
            ) oh ON d.driver_id = oh.driver_id
            LEFT JOIN (
                SELECT h3_index,
                       demand_count / NULLIF(supply_count, 0) AS supply_demand_ratio
                FROM dw.h3_supply_demand
                WHERE snapshot_date = ?
            ) sd ON d.primary_h3_index = sd.h3_index
            WHERE dol.online_count IS NOT NULL AND dol.online_count > 0
            """;
    }

    private SnapshotRow mapRow(ResultSet rs, LocalDate dataDate) throws SQLException {
        SnapshotRow row = new SnapshotRow();
        row.setSnapshotDate(dataDate);
        row.setDriverId(rs.getString("driver_id"));
        row.setDriverName(rs.getString("driver_name"));
        row.setPhone(rs.getString("phone"));
        row.setOnlineCount7d(rs.getInt("online_count_7d"));

        Timestamp lot = rs.getTimestamp("last_order_time");
        row.setLastOrderTime(lot != null ? lot.toLocalDateTime() : null);

        row.setH3Index(rs.getString("home_h3_index"));

        BigDecimal sdr = rs.getBigDecimal("supply_demand_ratio");
        row.setSupplyDemandRatio(sdr != null ? sdr : BigDecimal.ZERO);

        row.setAvgOrderAmount(rs.getBigDecimal("avg_order_amount"));
        row.setAvgRating(rs.getBigDecimal("avg_rating"));
        row.setTotalOrders7d(rs.getObject("total_orders_7d", Integer.class));
        row.setComplaintCount7d(rs.getObject("complaint_count_7d", Integer.class));
        row.setCancelRate7d(rs.getBigDecimal("cancel_rate_7d"));
        row.setActiveDays7d(rs.getObject("active_days_7d", Integer.class));
        row.setPeakHourPct(rs.getBigDecimal("peak_hour_pct"));
        row.setFeatureVector(rs.getString("feature_vector"));

        return row;
    }

    // ---------- 内部类：快照行数据 ----------

    @lombok.Data
    public static class SnapshotRow {
        private LocalDate snapshotDate;
        private String driverId;
        private String driverName;
        private String phone;
        private Integer onlineCount7d;
        private java.time.LocalDateTime lastOrderTime;
        private String h3Index;
        private BigDecimal supplyDemandRatio;
        private BigDecimal avgOrderAmount;
        private BigDecimal avgRating;
        private Integer totalOrders7d;
        private Integer complaintCount7d;
        private BigDecimal cancelRate7d;
        private Integer activeDays7d;
        private BigDecimal peakHourPct;
        private String featureVector;
    }
}
