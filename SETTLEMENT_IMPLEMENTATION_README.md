# 完整结算版本使用说明

## 1. 数据库

先备份数据库，然后执行：

```sql
source database/migration_v2_complete_settlement.sql;
```

脚本会：

- 将玩家资金字段统一为 `DECIMAL(15,2)`，并增加 `cumulative_sales_profit` 和 `total_settlement_profit`。
- 创建 `round_player_result`。
- 更新三类消费者的预算增长和经济敏感度。
- 更新三种初始手机的展示名称。
- 增加同一玩家同一回合只能提交一次的唯一索引。
- 调整经济系数、明星力度和市场结算标记字段类型。

脚本包含存在性检查，可以重复执行；执行前仍应备份数据库。

## 2. 回合事务

提交和结算已经拆成两个事务：

1. `RoundSubmissionService` 保存玩家方案、增加提交人数，并在全部提交后把回合改为 `PROCESSING`。
2. `RoundSettlementTriggerService` 使用 `@Async` 触发后台任务。
3. `RoundSettlementService` 在独立事务中完成全部结算。

最后一名玩家的提交接口会先返回 `PROCESSING`，因此所有玩家前端轮询时能够稳定看到：

`COLLECTING -> PROCESSING -> 新回合 COLLECTING`

## 3. 已实现规则

- 六类消费者按固定顺序购买，同人群内按当前手机 grade 升序、使用回合数降序、批次 ID 升序。
- 性能分、价格分、品牌占比、三种广告各 `+0.20`。
- 明星对全体加成，目标人群加倍；宣传力度范围 `0.00~0.50`。
- 消费者不考虑售价超过人群平均预算 `125%` 的手机。
- 库存不足时按原得分在剩余手机中重新分配。
- 未售手机按生产时零部件单台成本的 `85%` 回收。
- 普通贷款 `5%`，实际价格上涨后的超额缺口 `25%`。
- 收入到账后自动偿还全部可偿还贷款；未还清则现金为 0，剩余负债占用贷款额度。
- 公开销售利润：消费者销售收入减生产成本。
- 胜负结算利润：消费者销售收入 + 未售回收收入 - 生产成本 - 贷款利息，不扣宣传和明星费用。
- 最终排名只按累计结算利润；完全相同则并列。
- 最后一回合完成后不创建新回合，直接返回最终排名与结算结果。

## 4. 前端

- 每秒轮询轻量状态接口：
  `GET /api/games/{gameId}/rounds/status/{userId}`
- 全部提交时显示 `PROCESSING`。
- 新回合创建后自动重新加载完整 overview。
- 当前明星宣传力度在结算前不会由后端返回。
- 市场显示六类消费者的品牌占比饼图。
- 绿色数字用于现金和盈利；红色数字用于亏损、负债、贷款、利息和支出。
- 研发下拉菜单显示零部件方案名称和当前价格，不显示等级字样。

## 5. 接口

- 完整市场概览：
  `GET /api/games/{gameId}/overview/{userId}`
- 轻量回合状态：
  `GET /api/games/{gameId}/rounds/status/{userId}`
- 提交方案：
  `POST /api/games/{gameId}/rounds/current/actions/{userId}`

## 6. 重要说明

`application.yaml` 中数据库用户名和密码仍沿用原项目配置，请按本机 MySQL 修改。

构建时需要能够访问 Maven Central，首次运行 Maven Wrapper 会下载 Maven 和依赖。
