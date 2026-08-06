# PhoneMarket 单机 Redis 缓存升级包

本升级包用于当前 PhoneMarket 项目。目标是保留单服务器架构，不引入分布式锁，只增加清晰、可学习的 Redis 缓存机制。

## 1. 本次调整

- 增加 Spring Cache 与 Spring Data Redis。
- 房间详情 `roomDetails` 缓存 10 秒。
- 用户参与中比赛 `activeGames` 缓存 30 秒。
- 创建、加入、离开、解散、开始游戏和成功提交后主动删除相关缓存。
- 缓存操作与数据库事务联动，事务提交后再执行缓存失效。
- MySQL 继续保存全部永久业务数据，Redis 只保存可重新生成的查询结果。
- 单机并发依靠 Spring 事务、带状态条件的 SQL 和数据库唯一索引处理。
- 保留登录、主页、房间、开始游戏、重新进入和 20 分钟无提交自动结束功能。
- 删除快速登录和早期过时入口。

## 2. 两种代码包

- `PhoneMarket-complete-redis-cache.zip`：整合后的完整项目目录，可直接用 IDE 打开。
- `PhoneMarket-redis-cache-patch.zip`：只包含新增和替换文件，适合覆盖自己的最新仓库。

使用补丁包时：

```bash
python apply-update.py /path/to/PhoneMarket-demo
```

脚本会先备份被覆盖文件，再复制新代码并删除 `DELETE_FILES.txt` 中列出的早期文件。

## 3. 启动 Redis

项目附带：

```text
docker-compose.redis.yml
```

启动：

```bash
docker compose -f docker-compose.redis.yml up -d
```

检查：

```bash
docker compose -f docker-compose.redis.yml exec redis redis-cli PING
```

正常返回：

```text
PONG
```

本机已经安装 Redis 时，也可以直接启动本机的 `6379` 端口。

## 4. 环境变量

MySQL：

```text
MYSQL_HOST
MYSQL_PORT
MYSQL_DATABASE
MYSQL_USERNAME
MYSQL_PASSWORD
```

Redis：

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
REDIS_DATABASE
```

未设置 Redis 环境变量时，默认连接：

```text
localhost:6379
```

## 5. 数据库

已有数据库先备份，再运行一次：

```text
database/upgrade-20260805-room-session.sql
```

新建数据库使用：

```text
database/phonemarket-full-schema.sql
```

Redis 缓存不需要增加数据库表。

## 6. 启动项目

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Windows：

```bat
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

## 7. 检查缓存

登录并访问主页、房间后：

```bash
docker compose -f docker-compose.redis.yml exec redis redis-cli
```

开发环境中查看 Key：

```redis
SCAN 0 MATCH phonemarket:* COUNT 100
```

主要 Key 前缀：

```text
phonemarket:roomDetails::
phonemarket:activeGames::
```

## 8. 缓存规则

查询房间：

```text
先查询 Redis
未命中 -> 查询 MySQL -> 写入 Redis
```

修改房间或比赛：

```text
事务修改 MySQL
事务提交
删除相关 Redis 缓存
下一次查询重新加载
```

这种方式避免把 Redis 当成永久数据库，也减少手工同步两份数据的复杂度。

完整说明：

```text
docs/BUSINESS_LOGIC.md
docs/REDIS_CACHE.md
```
