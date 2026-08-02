-- ============================================================
-- 司机智能召回系统 — 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS driver_recall
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE driver_recall;

-- ============================================================
-- 1. 司机每日快照（从数仓拉取）
-- ============================================================
CREATE TABLE IF NOT EXISTS t_driver_daily_snapshot (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date   DATE           NOT NULL COMMENT '数据日期 (T-1)',
    driver_id       VARCHAR(32)    NOT NULL COMMENT '司机ID',
    driver_name     VARCHAR(64)    NULL     COMMENT '司机姓名',
    phone           VARCHAR(20)    NULL     COMMENT '手机号',
    -- 在线状态
    online_count_7d   INT          NOT NULL DEFAULT 0 COMMENT '近7天上线次数',
    last_order_time   DATETIME     NULL     COMMENT '最后完单时间',
    -- 地理位置
    h3_index          VARCHAR(30)  NULL     COMMENT 'H3网格索引(level 8)',
    supply_demand_ratio DECIMAL(10,4) NULL COMMENT '所在H3网格供需比',
    -- 行为特征
    avg_order_amount    DECIMAL(12,2) NULL COMMENT '近7天均单金额',
    avg_rating          DECIMAL(3,2)  NULL COMMENT '近7天均评分',
    total_orders_7d     INT           NULL COMMENT '近7天完单量',
    complaint_count_7d  INT           NULL COMMENT '近7天投诉数',
    cancel_rate_7d       DECIMAL(5,4)  NULL COMMENT '近7天取消率',
    active_days_7d       INT           NULL COMMENT '近7天活跃天数',
    peak_hour_pct        DECIMAL(5,4)  NULL COMMENT '高峰时段订单占比',
    -- 神经网络输入
    feature_vector       JSON          NULL COMMENT '特征向量(供NN推理)',
    -- 元数据
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_driver (snapshot_date, driver_id),
    KEY idx_snapshot_date (snapshot_date),
    KEY idx_driver_id (driver_id),
    KEY idx_h3_index (h3_index),
    KEY idx_last_order (last_order_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='司机每日快照(从数仓拉取)';

-- ============================================================
-- 2. 管道执行日志
-- ============================================================
CREATE TABLE IF NOT EXISTS t_recall_pipeline_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_date        DATE           NOT NULL COMMENT '执行日期',
    data_date       DATE           NOT NULL COMMENT '数据日期(T-1)',
    mode            VARCHAR(16)    NOT NULL DEFAULT 'DAILY' COMMENT 'DAILY / EMERGENCY',
    status          VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
    step            VARCHAR(32)    NULL     COMMENT '当前执行步骤',
    -- 各阶段计数
    pre_filter_in   INT            NULL     COMMENT '预筛选前数量',
    pre_filter_out  INT            NULL     COMMENT '预筛选后数量',
    nn_scored       INT            NULL     COMMENT 'NN打分数量',
    high_potential  INT            NULL     COMMENT '高潜力数量(>60分)',
    llm_generated   INT            NULL     COMMENT 'LLM生成策略数量',
    final_produced  INT            NULL     COMMENT '最终产出数量',
    -- 时间
    started_at      DATETIME       NULL,
    completed_at    DATETIME       NULL,
    error_msg       TEXT           NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_run_date (run_date),
    KEY idx_data_date (data_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='召回流水线执行日志';

-- ============================================================
-- 3. 召回名单（核心产出）
-- ============================================================
CREATE TABLE IF NOT EXISTS t_recall_list (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_run_id BIGINT         NOT NULL COMMENT '关联t_recall_pipeline_log.id',
    data_date       DATE           NOT NULL COMMENT '数据日期',
    driver_id       VARCHAR(32)    NOT NULL COMMENT '司机ID',
    driver_name     VARCHAR(64)    NULL     COMMENT '司机姓名',
    phone           VARCHAR(20)    NULL     COMMENT '手机号',
    -- 评分
    recall_score    DECIMAL(5,2)   NOT NULL COMMENT '召回潜力分(0-100)',
    score_reason    VARCHAR(255)   NULL     COMMENT '打分原因简述',
    -- 人设
    persona_tag     VARCHAR(32)    NULL     COMMENT '人设标签',
    persona_confidence DECIMAL(5,4) NULL   COMMENT '人设置信度',
    -- LLM 策略
    strategy_script  TEXT          NULL     COMMENT '个性化召回话术',
    recommended_channel VARCHAR(16) NULL    COMMENT '推荐触达渠道: SMS/PHONE/APP_PUSH/WECHAT',
    llm_response_raw JSON          NULL     COMMENT 'LLM原始响应(调试用)',
    -- 运营触达
    outreach_status VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONTACTED/AGREED/DECLINED/NO_RESPONSE',
    outreach_channel VARCHAR(16)   NULL     COMMENT '实际触达渠道',
    outreach_time   DATETIME       NULL     COMMENT '触达时间',
    outreach_remark VARCHAR(255)   NULL     COMMENT '触达备注',
    operator_id     VARCHAR(32)    NULL     COMMENT '运营人员ID',
    -- 元数据
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pipeline_run (pipeline_run_id),
    KEY idx_data_date (data_date),
    KEY idx_driver (driver_id),
    KEY idx_recall_score (recall_score),
    KEY idx_persona_tag (persona_tag),
    KEY idx_outreach_status (outreach_status),
    KEY idx_date_score (data_date, recall_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='召回名单(最终产出)';

-- ============================================================
-- 4. KPI 汇总缓存
-- ============================================================
CREATE TABLE IF NOT EXISTS t_recall_kpi (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_run_id BIGINT         NOT NULL COMMENT '关联pipeline_run_id',
    data_date       DATE           NOT NULL COMMENT '数据日期',
    -- KPI 值
    recallable_count      INT      NOT NULL COMMENT '可召回司机数',
    expected_success_rate DECIMAL(5,4) NOT NULL COMMENT '预期成功率',
    today_budget          DECIMAL(12,2)  NULL COMMENT '今日预算',
    rule_based_count      INT      NULL COMMENT '规则引擎产出数(对比基准)',
    improvement_pct       DECIMAL(5,4)  NULL COMMENT '较规则引擎提升百分比',
    -- 分布数据
    persona_distribution  JSON     NULL COMMENT '人设分布',
    status_distribution   JSON     NULL COMMENT '触达状态分布',
    score_distribution    JSON     NULL COMMENT '分数分布直方图',
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date (data_date),
    KEY idx_pipeline_run (pipeline_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='召回KPI汇总(仪表盘缓存)';

-- ============================================================
-- 5. 应急实时召回记录
-- ============================================================
CREATE TABLE IF NOT EXISTS t_emergency_recall (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      VARCHAR(36)    NOT NULL COMMENT '紧急召回会话ID',
    region_polygon  GEOMETRY       NOT NULL COMMENT '圈选区域多边形(SRID=4326)',
    region_desc     VARCHAR(255)   NULL     COMMENT '区域描述',
    operator_id     VARCHAR(32)    NULL     COMMENT '操作人ID',
    -- 结果
    nearby_drivers  INT            NOT NULL DEFAULT 0 COMMENT '区域内司机数',
    scored_drivers  INT            NOT NULL DEFAULT 0 COMMENT '已完成NN打分司机数',
    high_potential  INT            NOT NULL DEFAULT 0 COMMENT '高潜力司机数',
    result_data     JSON           NULL     COMMENT '详细结果数据',
    status          VARCHAR(16)    NOT NULL DEFAULT 'QUERYING' COMMENT 'QUERYING/SCORING/STRATEGIZING/COMPLETED/FAILED',
    started_at      DATETIME       NULL,
    completed_at    DATETIME       NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session (session_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急实时召回记录';

-- ============================================================
-- 6. 每日趋势数据
-- ============================================================
CREATE TABLE IF NOT EXISTS t_daily_trend (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_date       DATE           NOT NULL COMMENT '数据日期',
    recallable_count    INT        NOT NULL DEFAULT 0,
    high_potential_count INT      NOT NULL DEFAULT 0,
    contacted_count     INT        NULL DEFAULT 0,
    agreed_count        INT        NULL DEFAULT 0,
    actual_success_rate  DECIMAL(5,4) NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date (data_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日趋势数据(图表缓存)';

-- ============================================================
-- 7. ShedLock 分布式锁表
-- ============================================================
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ShedLock分布式锁';
