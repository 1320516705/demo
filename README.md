# 司机智能召回系统

## 一、系统概述

每天早上定时分析前一天的司机数据，用**神经网络**评分筛选高潜力召回司机，再用**大模型（Claude）**生成个性化召回策略（人设标签 + 话术 + 推荐触达渠道），最终在运营后台展示带策略的召回名单，运营可**一键触达**。同时支持应急场景下地图圈选实时查询。

### 核心流程

```
数仓(T-1数据) → 前置过滤 → 神经网络打分 → 筛选高潜力(>60分) → LLM生成策略 → 运营后台
```

| 步骤 | 做什么 | 说明 |
|------|--------|------|
| ① 拉取 | 从数仓 JDBC 拉取 T-1 司机快照 | 在线记录、订单、评价、H3网格 |
| ② 过滤 | 近7天在线≥1 + 最后完单≤7天 + 供需比≥1.5 | 排除僵尸号、已流失司机 |
| ③ 打分 | 调用 NN REST API 批量推理 | 输入特征向量，输出0~100分 |
| ④ 筛选 | 召回分 > 60 | 标记为高潜力 |
| ⑤ 策略 | 调用 Claude API 生成个性化策略 | 人设标签 + 话术 + 渠道 |
| ⑥ 产出 | 计算KPI、写入趋势表 | 仪表盘直接用 |

### 两种运行模式

| 模式 | 触发方式 | 数据来源 | 场景 |
|------|----------|----------|------|
| 日常定时召回 | 每天 7:00 AM 自动 | 数仓 T-1 离线数据 | 日常运营 |
| 应急实时召回 | 运营圈选地图区域 → 点击查询 | MCP 服务实时查询 | 突发缺车(演唱会散场、恶劣天气) |

## 二、技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.3.5 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM / 分页 |
| MySQL | 8.0+ | 数据存储 |
| ShedLock | 5.2.0 | 分布式定时锁 |
| WebClient | — | HTTP 客户端（NN、LLM、MCP） |
| Caffeine | — | 本地缓存 |
| Lombok | 1.18.38 | 代码简化 |
| Prometheus | — | 监控指标 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4+ | SPA 框架 |
| Vite | 5+ | 构建工具 |
| Element Plus | 2.9+ | UI 组件库 |
| ECharts | 5.6+ | 图表 |
| Pinia | 2.2+ | 状态管理 |
| Axios | — | HTTP 请求 |

### 外部服务
| 服务 | 协议 | 用途 |
|------|------|------|
| 数据仓库 | JDBC | 拉取 T-1 司机数据 |
| 神经网络服务 | REST API | 召回潜力打分 |
| Claude API | Anthropic Messages API | 生成个性化策略 |
| MCP 服务 | MCP JSON-RPC | 实时司机查询（应急） |
| 高德地图 | REST API | 逆地理编码、热力图 |

## 三、数据库设计

系统共 **7 张表**：

```
t_driver_daily_snapshot   — 司机每日快照（数仓拉取，特征向量存JSON列）
t_recall_pipeline_log     — 管道执行日志（每步计数、耗时、状态）
t_recall_list             — 召回名单（核心产出，含分/人设/话术/渠道/触达状态）
t_recall_kpi              — KPI 汇总缓存（仪表盘即时读取）
t_emergency_recall        — 应急召回会话（区域、状态、结果）
t_daily_trend             — 每日趋势（图表缓存）
shedlock                  — 分布式锁表
```

建表脚本：`src/main/resources/db/init.sql`

### 核心表关系

```
t_recall_pipeline_log (1) ──< (N) t_recall_list
t_recall_pipeline_log (1) ──── (1) t_recall_kpi
t_driver_daily_snapshot ────── 被管道读取，不建外键
```

## 四、项目结构

```
demo/
├── pom.xml                                    # Maven 配置
├── README.md
└── src/
    ├── main/
    │   ├── java/com/competition/invoice/
    │   │   ├── DemoApplication.java           # 启动类
    │   │   ├── config/                        # 配置（WebClient、ShedLock、MyBatisPlus、Caffeine、Warehouse）
    │   │   ├── common/                        # 通用（Result、PageResult、BizException、GlobalExceptionHandler）
    │   │   ├── entity/                        # 实体类（6张表）
    │   │   ├── mapper/                        # MyBatis Mapper 接口
    │   │   ├── model/
    │   │   │   ├── dto/                       # 请求/响应 DTO
    │   │   │   ├── vo/                        # 视图对象（RecallListVO、KpiCardVO、TrendChartVO...）
    │   │   │   └── enums/                     # 枚举（PersonaTag、OutreachStatus、PipelineMode...）
    │   │   ├── controller/                    # REST API 控制器
    │   │   │   ├── PipelineController         # 管道触发/状态
    │   │   │   ├── RecallListController       # 召回名单 CRUD + 触达
    │   │   │   ├── KpiController              # KPI 卡片数据
    │   │   │   ├── ChartController            # 图表 + 热力图
    │   │   │   └── EmergencyRecallController  # 应急召回
    │   │   ├── service/
    │   │   │   ├── pipeline/                  # 六步管道（PipelineOrchestrator + Step1~6）
    │   │   │   ├── recall/                    # 召回列表 + 触达服务
    │   │   │   ├── emergency/                 # 应急召回 + MCP 客户端
    │   │   │   ├── kpi/                       # KPI 计算
    │   │   │   ├── chart/                     # 图表数据聚合
    │   │   │   └── external/                  # 外部集成（Warehouse、NeuralNetwork、LLM、AMapGeo）
    │   │   └── scheduler/
    │   │       └── DailyRecallScheduler       # 每天 7:00 定时任务
    │   ├── resources/
    │   │   ├── application.yml                # 应用配置
    │   │   └── db/init.sql                    # 建表脚本
    │   └── frontend/                          # Vue 3 前端
    │       ├── package.json
    │       ├── vite.config.js
    │       └── src/
    │           ├── main.js
    │           ├── App.vue
    │           ├── router/
    │           ├── stores/recall.js           # Pinia 状态管理
    │           ├── api/                       # Axios API 封装
    │           ├── pages/RecallDashboard.vue  # 主页面
    │           └── components/                # 10 个组件
    │               ├── KpiCard.vue / KpiCardRow.vue
    │               ├── DriverHeatMap.vue
    │               ├── DriverListTable.vue / TableFilterBar.vue
    │               ├── OutreachDialog.vue / DriverDetailDrawer.vue
    │               ├── TrendChart.vue / PersonaDistribution.vue / StatusDistribution.vue
    └── test/
```

## 五、快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Node.js 22+（前端开发）
- Maven 3.8+

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/db/init.sql
```

### 2. 配置环境变量

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
export ANTHROPIC_API_KEY=sk-ant-xxx
export NN_API_KEY=your_nn_key      # 可选
export AMAP_API_KEY=your_amap_key  # 可选
```

也可直接修改 `src/main/resources/application.yml` 中的配置。

### 3. 启动后端

```bash
mvn spring-boot:run -DskipTests
```

应用启动在 `http://localhost:8080`，Swagger 文档在 `http://localhost:8080/swagger-ui.html`。

### 4. 启动前端（开发模式）

```bash
cd src/main/frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，API 请求代理到 `localhost:8080`。

### 5. 触发管道

```bash
# 手动触发一次（数据日期为昨天）
curl -X POST http://localhost:8080/api/v1/pipeline/trigger \
  -H "Content-Type: application/json" \
  -d '{"dataDate": "2026-07-29"}'

# 查询管道状态
curl http://localhost:8080/api/v1/pipeline/last-run

# 查看召回名单
curl "http://localhost:8080/api/v1/recall/list?dataDate=2026-07-29&page=1&size=10"
```

### 6. 打包部署

```bash
mvn clean package -DskipTests
java -jar target/demo-1.0-SNAPSHOT.jar
```

## 六、API 接口

所有接口统一返回 `Result<T>` 格式：`{"code": 200, "message": "success", "data": ...}`

### 管道控制

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/pipeline/trigger` | 手动触发管道 |
| GET | `/api/v1/pipeline/status/{runId}` | 查询管道状态 |
| GET | `/api/v1/pipeline/last-run` | 最近一次运行 |

### 召回名单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/recall/list` | 分页查询（支持筛选：personaTag、outreachStatus、scoreMin/Max、keyword） |
| GET | `/api/v1/recall/{id}` | 司机详情（含LLM原始响应） |
| PUT | `/api/v1/recall/{id}/outreach` | 单个触达 |
| POST | `/api/v1/recall/batch-outreach` | 批量触达 |

### 仪表盘

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/kpi/summary` | KPI 卡片数据 |
| GET | `/api/v1/chart/trend` | 7日趋势（可召回/高潜力/已触达/已同意） |
| GET | `/api/v1/chart/distribution` | 人设分布 / 状态分布 |
| GET | `/api/v1/map/heatmap` | 热力图点位数据 |

### 应急召回

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/emergency/recall` | 触发应急召回（传入多边形坐标） |
| GET | `/api/v1/emergency/status/{sessionId}` | 轮询状态 |
| GET | `/api/v1/emergency/result/{sessionId}` | 获取结果 |

## 七、五类司机画像

| 画像标签 | 含义 | 推荐策略 |
|----------|------|----------|
| **PRICE_SENSITIVE** 价格敏感型 | 对优惠券、奖励反应明显 | 推送奖励信息 |
| **TIME_SENSITIVE** 时间敏感型 | 对高峰溢价、高流水机会敏感 | 推送时段溢价信息 |
| **WAY_HOME** 顺路回家型 | 偏好不空驶的顺路单 | 推送顺路单机会 |
| **WEEKEND_PART_TIME** 周末兼职型 | 仅周末活跃 | 周末前定向推送 |
| **STABLE_FULL_TIME** 稳定全职型 | 每天固定出车 | 推送稳定收入信息 |

## 八、关键设计决策

| # | 决策 | 理由 |
|---|------|------|
| 1 | 管道同步顺序执行 | 每步依赖上一步输出，LLM步骤内部 5 线程并发提速 |
| 2 | MyBatis-Plus 非 JPA | 聚合查询 + 批量 upsert 更自然 |
| 3 | 特征向量存 JSON 列 | 适配 NN 模型版本变化，不需 DDL |
| 4 | 预计算 KPI + 趋势表 | 仪表盘即时加载，不做实时聚合 |
| 5 | 应急模式用轮询而非 WebSocket | 10-30秒完成，3秒轮询足够 |
| 6 | LLM 单次调用输出全部 | 一次 API 调用产出（人设+话术+渠道），节约 3 倍成本 |
| 7 | 前端 SPA 嵌入 Spring Boot JAR | 单一部署产物，运营后台标准集成方式 |
| 8 | 触达先做状态记录 | MVP 阶段记录运营意图，短信/电话网关后续接入 |
| 9 | H3 分辨率 level 8 | 0.74km² 城市街区粒度，Uber/Didi 标准 |

## 九、页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  可召回司机 342  │ 预期成功率 38% │ 预算 ¥5,000 │ 提升 +90%  │  ← KPI 卡片
├───────────────────────────┬─────────────────────────────────┤
│  高德地图热力图              │  筛选栏 + 司机名单表格              │  ← 中间主区域
│  [应急：圈选区域按钮]          │  分数条 / 人设标签 / 话术 / 操作按钮  │
├───────────────────────────┴─────────────────────────────────┤
│  近7天趋势  │  人设分布饼图  │  触达状态柱状图                      │  ← 底部图表
└─────────────────────────────────────────────────────────────┘
```

## 十、外部依赖配置

所有外部服务配置在 `application.yml` 的 `external` 段：

```yaml
external:
  warehouse:      # 数仓 JDBC 连接
  neural-network: # NN 推理服务 (base-url, api-key, batch-size, max-retries)
  llm:            # Claude API (provider, model, max-concurrent, rate-limit-ms)
  mcp:            # MCP 实时查询 (base-url, query-timeout)
  amap:           # 高德地图 (api-key, base-url)
```

敏感信息建议通过环境变量注入（如 `${ANTHROPIC_API_KEY}`），不硬编码在配置文件中。

---

*Last updated: 2026-07-30*
