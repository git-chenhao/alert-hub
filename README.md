# 告警聚合平台 (Alert Hub)

## 项目介绍

告警聚合平台是一个基于 Spring Boot 的智能告警处理系统，支持多种告警源的接收、去重、聚合，并通过 A2A 协议调用 AI Agent 进行根因分析。

## 核心功能

### 1. 告警接收模块
- 支持 Prometheus Alertmanager 格式
- 支持 Grafana Webhook 格式
- 支持 Zabbix Webhook 格式
- 通用告警创建接口

### 2. 去重模块
- 基于指纹去重
- 指纹生成规则：MD5(alertname + severity + instance + labels)
- 可配置去重窗口时间

### 3. 聚合模块
- 可配置聚合策略
- 支持按标签分组聚合
- 批次状态管理：PENDING → AGGREGATING → DISPATCHED → COMPLETED

### 4. A2A 调度模块
- 实现 A2A 协议调用 Sub-Agent
- 异步调度 + 结果回调
- 支持配置 Agent 端点和超时

### 5. 飞书通知模块
- 分析完成后发送飞书卡片
- 卡片内容：告警摘要、根因分析结果、处理建议

### 6. 后台可视化
- 告警列表查询
- 批次列表查询
- 手动触发分析
- 统计信息展示

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (开发) / MySQL (生产)
- WebFlux (HTTP Client)
- Lombok

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+

### 启动步骤

1. 克隆项目
```bash
git clone <repository-url>
cd alert-hub
```

2. 构建项目
```bash
mvn clean package
```

3. 启动应用
```bash
java -jar target/alert-hub-1.0.0.jar
```

或者使用 Maven 直接启动：
```bash
mvn spring-boot:run
```

4. 访问应用
- 主页: http://localhost:8080
- 管理后台: http://localhost:8080/index.html
- H2 控制台: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:alertdb`
  - 用户名: `sa`
  - 密码: (留空)

## API 接口

### Webhook 接口

#### 接收 Prometheus 告警
```bash
POST /api/webhook/alerts
Content-Type: application/json

{
  "status": "firing",
  "alerts": [...]
}
```

#### 接收 Grafana 告警
```bash
POST /api/webhook/grafana
Content-Type: application/json

{
  "title": "告警标题",
  "state": "alerting",
  "message": "告警内容",
  "labels": {...}
}
```

#### 接收 Zabbix 告警
```bash
POST /api/webhook/zabbix
Content-Type: application/json

{
  "host": "服务器名称",
  "trigger_name": "触发器名称",
  "severity": "high",
  "message": "告警内容"
}
```

#### 通用告警创建
```bash
POST /api/webhook/create
Content-Type: application/json

{
  "alertname": "告警名称",
  "severity": "critical",
  "instance": "实例标识",
  "message": "告警内容",
  "labels": {...},
  "source": "generic"
}
```

### 管理接口

#### 获取告警列表
```bash
GET /api/admin/alerts?page=0&size=20&status=PENDING
```

#### 获取批次列表
```bash
GET /api/admin/batches?status=COMPLETED
```

#### 触发批次分析
```bash
POST /api/admin/batches/{id}/dispatch
```

#### 获取统计信息
```bash
GET /api/admin/stats
```

## 配置说明

配置文件位于 `src/main/resources/application.yml`

### 去重配置
```yaml
alert:
  deduplication:
    enabled: true
    window-minutes: 10
```

### 聚合配置
```yaml
alert:
  aggregation:
    group-wait-seconds: 30
    max-count: 50
    max-wait-seconds: 300
    group-by-labels: "severity,alertname"
```

### Agent 配置
```yaml
alert:
  agent:
    endpoint: http://localhost:9001/api/agent/analyze
    timeout-seconds: 60
    max-retries: 3
```

### 飞书通知配置
```yaml
alert:
  feishu:
    enabled: true
    webhook-url: https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxxxxx
    send-detail: true
```

## 项目结构

```
alert-hub/
├── src/main/java/com/alert/platform/
│   ├── AlertHubApplication.java      # 主应用类
│   ├── config/                       # 配置类
│   ├── controller/                   # 控制器
│   ├── dto/                         # 数据传输对象
│   ├── entity/                      # 实体类
│   ├── exception/                   # 异常处理
│   ├── repository/                  # 数据访问层
│   └── service/                     # 服务层
├── src/main/resources/
│   ├── application.yml              # 应用配置
│   ├── static/                      # 静态资源
│   └── db/migration/                # 数据库迁移脚本
└── .github/workflows/               # CI 配置
```

## 开发指南

### 数据库
开发环境使用内存数据库 H2，生产环境建议使用 MySQL。

### 添加新的告警源
1. 在 `WebhookController` 中添加新的接收接口
2. 解析告警源格式为 `WebhookAlertRequest`
3. 调用 `processAlert()` 处理

### 扩展 Agent 接口
修改 `AgentSchedulerService` 中的 `buildAgentRequest()` 和 `handleAgentResponse()` 方法。

## License

MIT License

<!-- Review test at 2026年 3月 2日 星期一 00时09分13秒 CST -->
