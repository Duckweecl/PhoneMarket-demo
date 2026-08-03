提交接口说明

1. 旧接口（保留现有逻辑）

POST /{userId}/submit/{gameId}

请求示例：
{
  "actionType": "TEST"
}

返回：RoundAction

说明：旧接口及其原有 PlayService 逻辑没有修改，只整理了 Controller 写法。


2. 新接口（仅完成接口定义）

POST /api/games/{gameId}/rounds/current/actions/{userId}

请求：RoundActionRequest

{
  "modelName": "Nova X",
  "screenLevel": 2,
  "processorLevel": 3,
  "bodyLevel": 1,
  "batteryLevel": 2,
  "storageLevel": 2,
  "cameraLevel": 3,
  "productionQuantity": 100,
  "salePrice": 5000,
  "filmAd": true,
  "onlineAd": false,
  "magazineAd": true,
  "starBid": 100000
}

返回类型：RoundActionResponse

当前行为：HTTP 501 Not Implemented

{
  "status": 501,
  "message": "新提交接口已建立，执行逻辑尚未接入"
}

本次没有增加以下逻辑：
- 参数校验
- 创建 phone_model
- 插入 round_action
- 增加 submitted_count
- 全员提交判断
- 回合结算
- 下一回合创建
