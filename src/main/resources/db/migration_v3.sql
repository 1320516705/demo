-- V3 精简：去掉画像标签、H3网格、价格话术相关字段
USE driver_recall;

-- t_recall_list: 去掉 persona 相关
ALTER TABLE t_recall_list
    DROP COLUMN IF EXISTS persona_tag,
    DROP COLUMN IF EXISTS persona_confidence;

-- t_driver_daily_snapshot: 去掉 H3
ALTER TABLE t_driver_daily_snapshot
    DROP COLUMN IF EXISTS h3_index;

-- t_recall_kpi: 去掉 persona_distribution
ALTER TABLE t_recall_kpi
    DROP COLUMN IF EXISTS persona_distribution;

-- 更新已有数据的召回话术（去掉价格/奖励相关内容）
UPDATE t_recall_list SET strategy_script = NULL WHERE data_date = '2026-07-29';

SELECT 'V3 migration complete' AS status;
