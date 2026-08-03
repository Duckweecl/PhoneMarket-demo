三页版前端

页面：
1. 当前市场
2. 研发
3. 经营

当前市场显示：
- 游戏和回合状态
- 当前玩家资金、负债额度、累计销售额
- 六类消费者
- 品牌人数 -> 型号人数二级列表
- 六类消费者预算
- 明星信息
- 18项零部件市场
- 上一回合所有公开结果
- 完整 JSON

研发页：
- 型号名称
- 六种部件下拉菜单
- 每个等级显示当前采购价格
- 自动计算综合档次、零部件成本、装配费和单台手机成本

经营页：
- 三个主要人群平均预算
- 生产数量
- 销售价格
- 明星秘密出价
- 三种基础宣传复选框及实时费用
- 自动计算预计生产支出、宣传支出、最大明星支出、支出后资金和剩余可支配额度

后端返回必须增加：
currentPlayer:
{
  "gamePlayerId": 28,
  "username": "张三",
  "status": "ACTIVE",
  "cash": 1000000,
  "debtLimit": 1000000,
  "totalSales": 0
}

backend-required 目录中提供了 CurrentPlayerDTO 和更新后的 GameRoundOverviewResponse。

最终提交接口默认写成：
POST /api/games/{gameId}/rounds/current/actions/{userId}

若你的 Controller 路径不同，修改 static/app.js 顶部：
API.submitAction

重要计算说明：
- 预计明星费用按中标时需要支付计算，因此属于“最大可能支出”。
- 税费、负债利息和销售收入只能在回合结算后确定，没有纳入前端预计支出。
- 当前可支配额度 = cash + debtLimit。
- 预计剩余可支配额度 = cash + debtLimit - 预计总支出。
