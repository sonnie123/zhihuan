# 智换（Zhihuan）- AI 驱动的二手交易微服务平台

> 基于 Spring Boot 3.5 + Spring Cloud 2025 + Spring Cloud Alibaba 2025 + Dubbo 3 + MiniMax-M3 构建的中大型微服务平台

## 项目介绍

智换是一个面向 C2C 二手交易场景的 AI 驱动平台，集成 Spring-AI-Alibaba Agent-Framework + Graph 和 MiniMax-M3 多模态大模型，提供 AI 一键上架、智能议价、智能客服、智能审核等能力。

## 技术栈

| 类别 | 技术 |
|---|---|
| 基础框架 | JDK 17/21 + Spring Boot 3.5.7 + Spring Cloud 2025.0.0 |
| 微服务 | Spring Cloud Alibaba 2025.0.0.0（Nacos 3.0.3 / Sentinel 1.8.9 / Seata 2.5.0） |
| RPC | Dubbo 3.3.4（Triple 协议） |
| ORM | MyBatis-Plus 3.5.9 + Druid 1.2.27 |
| 数据库 | MySQL 8.0（8 库分库）|
| 缓存 | Redis 7.4 + Caffeine + Nginx 三级缓存 |
| 消息队列 | Kafka 3.9.1 + RocketMQ 5.3.1 |
| 搜索 | Elasticsearch 8.15 |
| 向量库 | Milvus 2.4 |
| AI | Spring-AI 1.1.2 + Spring-AI-Alibaba Agent-Framework 1.1.2 + Graph + MiniMax-M3 |
| 任务调度 | XXL-JOB 2.5.0 |
| 可观测性 | Prometheus + Grafana + ELK + SkyWalking |

## 项目结构

```
zhihuan/
├── docs/                       # 设计文档（26 个 .md 文件）
├── pom.xml                     # 父工程 POM
├── zhihuan-common/             # 公共模块
│   ├── zhihuan-common-core     # Result/Exception/PageQuery
│   ├── zhihuan-common-web      # Web 通用配置
│   ├── zhihuan-common-dubbo    # Dubbo 配置
│   ├── zhihuan-common-mybatis  # MyBatis-Plus 配置
│   └── zhihuan-common-redis    # Redis 配置
├── zhihuan-gateway/            # API 网关（9000）
├── zhihuan-user/               # 用户服务（9011）
├── zhihuan-product/            # 商品服务（9012）
├── zhihuan-ai/                 # AI 智能体中心（9013）⭐
├── zhihuan-trade/              # 交易服务（9014）
├── zhihuan-search/             # 搜索服务（9015）
├── zhihuan-recommend/          # 推荐服务（9016）
├── zhihuan-feed/               # Feed 流服务（9017）
├── zhihuan-marketing/          # 营销服务（9018）
├── zhihuan-im/                 # 即时通讯（9019）
├── zhihuan-audit/              # 内容审核（9020）
├── zhihuan-notification/       # 通知服务（9021）
├── zhihuan-job/                # 任务调度（9022）
├── zhihuan-admin/              # 运营后台（9023）
├── deploy/                     # 部署文件
│   ├── docker-compose.yml      # 一键启动中间件
│   └── init-sql/               # 数据库初始化脚本
└── scripts/                    # 工具脚本
```

## 快速开始

### 前置条件
- JDK 17 或 JDK 21
- Maven 3.9+
- Docker + Docker Compose

### 第一步：启动中间件

```powershell
cd F:\file\learn\projects\JAVA\zhihuan\deploy
docker-compose up -d
```

启动后访问：
- Nacos：http://localhost:8848/nacos（nacos/nacos）
- XXL-JOB：http://localhost:8080/xxl-job-admin（admin/123456）
- Kibana（如启用）：http://localhost:5601

### 第二步：编译项目

```powershell
cd F:\file\learn\projects\JAVA\zhihuan

# 设置 JDK 17
$env:JAVA_HOME = ''D:\APPLICATIONS\JAVA\JDK17''
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 编译公共模块
mvn clean install -pl zhihuan-common -am -DskipTests

# 编译所有服务
mvn clean compile -DskipTests
```

### 第三步：启动服务

```powershell
# 启动网关
mvn spring-boot:run -pl zhihuan-gateway

# 启动用户服务（新窗口）
mvn spring-boot:run -pl zhihuan-user

# 启动 AI 服务
mvn spring-boot:run -pl zhihuan-ai
```

启动后访问 Nacos 控制台查看服务注册情况。

## 当前进度

✅ **阶段 1 完成：基础架构 + 14 个服务骨架**
- [x] 父工程 POM（版本管理）
- [x] 公共模块（5 个子模块）
- [x] 14 个微服务骨架
- [x] Docker Compose 中间件
- [x] 数据库初始化脚本

🚧 **待开发：**
- 各服务的业务代码实现
- AI 服务集成 MiniMax-M3
- Dubbo 接口完整定义
- Kafka 事件订阅实现
- 缓存 / 分布式锁等

## 镜像源配置（Maven）

如遇依赖下载问题，配置 `~/.m2/settings.xml` 使用腾讯云镜像：

```xml
<mirrors>
  <mirror>
    <id>tencent</id>
    <name>tencent maven</name>
    <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

## 学习资源

详细设计文档见 `docs/` 目录，共 26 个 Markdown 文件，约 339 KB。

## 许可证

本项目仅供学习使用。
