-- 加城市维度 + 分布到全国
USE driver_recall;

ALTER TABLE t_driver_daily_snapshot ADD COLUMN city VARCHAR(32) NULL COMMENT '城市' AFTER phone;

-- 上海 20条
UPDATE t_driver_daily_snapshot SET city='上海' WHERE driver_id BETWEEN 'D0001' AND 'D0020' AND snapshot_date='2026-07-29';
-- 北京 15条
UPDATE t_driver_daily_snapshot SET city='北京' WHERE driver_id BETWEEN 'D0021' AND 'D0035' AND snapshot_date='2026-07-29';
-- 深圳 12条
UPDATE t_driver_daily_snapshot SET city='深圳' WHERE driver_id BETWEEN 'D0036' AND 'D0047' AND snapshot_date='2026-07-29';
-- 广州 12条
UPDATE t_driver_daily_snapshot SET city='广州' WHERE driver_id BETWEEN 'D0048' AND 'D0059' AND snapshot_date='2026-07-29';
-- 成都 10条
UPDATE t_driver_daily_snapshot SET city='成都' WHERE driver_id BETWEEN 'D0060' AND 'D0069' AND snapshot_date='2026-07-29';
-- 杭州 10条
UPDATE t_driver_daily_snapshot SET city='杭州' WHERE driver_id BETWEEN 'D0070' AND 'D0079' AND snapshot_date='2026-07-29';
-- 武汉 8条
UPDATE t_driver_daily_snapshot SET city='武汉' WHERE driver_id BETWEEN 'D0080' AND 'D0087' AND snapshot_date='2026-07-29';
-- 南京 7条
UPDATE t_driver_daily_snapshot SET city='南京' WHERE driver_id BETWEEN 'D0088' AND 'D0094' AND snapshot_date='2026-07-29';
-- 重庆 6条
UPDATE t_driver_daily_snapshot SET city='重庆' WHERE driver_id BETWEEN 'D0095' AND 'D0100' AND snapshot_date='2026-07-29';

SELECT city, COUNT(*) AS cnt FROM t_driver_daily_snapshot WHERE snapshot_date='2026-07-29' GROUP BY city ORDER BY cnt DESC;
