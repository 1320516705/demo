-- ============================================================
-- V2 迁移：新增业务字段
-- ============================================================
USE driver_recall;

ALTER TABLE t_driver_daily_snapshot
    ADD COLUMN daily_orders           INT            NULL COMMENT '日完单量' AFTER last_order_time,
    ADD COLUMN morning_peak_orders    INT            NULL COMMENT '早高峰完单量(7-9点)' AFTER daily_orders,
    ADD COLUMN evening_peak_orders    INT            NULL COMMENT '晚高峰完单量(17-19点)' AFTER morning_peak_orders,
    ADD COLUMN daily_online_hours     DECIMAL(5,2)   NULL COMMENT '日在线时长(小时)' AFTER evening_peak_orders,
    ADD COLUMN morning_peak_online_hours DECIMAL(5,2) NULL COMMENT '早高峰在线时长' AFTER daily_online_hours,
    ADD COLUMN evening_peak_online_hours DECIMAL(5,2) NULL COMMENT '晚高峰在线时长' AFTER morning_peak_online_hours,
    ADD COLUMN base_income            DECIMAL(12,2)  NULL COMMENT '基本收入' AFTER evening_peak_online_hours,
    ADD COLUMN bonus_income           DECIMAL(12,2)  NULL COMMENT '奖励收入' AFTER base_income,
    ADD COLUMN commission_free_income DECIMAL(12,2)  NULL COMMENT '免佣收入' AFTER bonus_income;
