# 论文查重管理系统（后端）

## 项目简介

论文查重管理系统后端，基于 Spring Boot 构建，为高校论文查重和审核流程提供完整的 RESTful API 服务。支持学生论文提交与查重、教师在线审核、管理员系统监控等功能，涵盖论文全生命周期管理。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.7 | 应用框架 |
| Java | 17 | 开发语言 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存与会话管理 |
| MinIO | — | 对象存储（文件管理） |
| Spring Security | — | 安全框架，JWT 无状态认证 |
| Apache Tika | — | 文档内容提取（doc/docx/pdf） |
| IK Analyzer | — | 中文分词（自定义词典） |
| iText7 | — | PDF 报告生成（支持中文 SimHei 字体） |
| Micrometer + Prometheus | — | 应用性能监控 |
| Springfox Swagger 3 | — | API 文档 |
| Lombok | — | 代码简化 |

## 核心功能

### 学生模块
- 论文提交（支持 Word/PDF，MD5 秒传）
- 查重任务监控（WebSocket 实时推送进度）
- 查重报告查看与下载
- 论文版本管理与版本对比
- 论文撤回与重新提交
- 导师互动消息系统

### 教师模块
- 待审核论文列表与优先级排序
- 论文审核（通过/驳回/修改建议）
- 审核统计与趋势分析
- 学生分组管理
- 批量审核操作

### 管理员模块
- 用户管理（学生/教师/管理员）
- 论文分配（手动/自动）
- 查重规则配置
- 系统监控（CPU/内存/磁盘/数据库连接池）
- 数据备份与恢复
- 操作日志审计
- 学校/学院/专业基础数据管理

### 查重引擎
- 本地 SimHash 引擎（Hamming 距离 ≤ 3 判定相似）
- 支持第三方和深度学习引擎扩展（默认禁用）
- IK 分词 + 自定义词典

## 系统架构

```
Vue 前端 (port 3000) → Vite 代理 → Spring Boot (port 8080, /check 上下文)
                                        ↓
                          MySQL (业务数据) + Redis (缓存) + MinIO (文件)
```

## 包结构

```
com.abin.checkrepeatsystem
├── admin/              # 管理员模块（用户/论文/配置/统计/分配）
├── student/            # 学生模块（论文提交/查重/报告/消息）
├── teacher/            # 教师模块（审核/统计/学生管理）
├── user/               # 认证模块（登录/刷新/通知/消息）
├── common/             # 公共模块
│   ├── config/         #   安全配置、WebSocket、CORS
│   ├── filter/         #   JWT 过滤器
│   ├── handler/        #   全局异常处理
│   ├── service/        #   文件服务（MinIO/本地）
│   ├── utils/          #   工具类（JWT、PDF 生成等）
│   └── annotation/     #   自定义注解（操作日志）
├── monitor/            # 监控模块（系统指标采集、数据库监控）
├── notification/       # 通知模块（消息发送、公告发布）
├── detection/          # 查重检测模块（SimHash、引擎管理）
├── pojo/
│   ├── entity/         #   40+ 实体类（MyBatis-Plus，雪花ID，软删除）
│   ├── dto/            #   请求/响应 DTO
│   └── vo/             #   视图对象
├── mapper/             # MyBatis-Plus Mapper 接口
└── schedule/           # 定时任务（数据完整性检查等）
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 配置

编辑 `src/main/resources/application-dev.yml`，配置数据库、Redis、MinIO 连接信息。

敏感信息（邮件密码、OSS 密钥等）建议通过环境变量或 Jasypt 加密配置。

### 构建与运行

```bash
# 编译
mvn clean package

# 运行（默认 dev profile）
java -jar target/check-repeat-system-0.0.1-SNAPSHOT.jar

# 指定 profile
java -jar target/check-repeat-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 仅运行测试
mvn test
```

### API 文档

启动后访问 Swagger UI：
```
http://localhost:8080/check/swagger-ui/
```

### 监控端点

```
http://localhost:8080/check/actuator/prometheus    # Prometheus 指标
http://localhost:8080/check/actuator/health         # 健康检查
```

## 数据库规范

- 主键：雪花 ID（`ASSIGN_ID`）
- 字段映射：下划线转驼峰
- 软删除：`is_deleted` 字段（1 = 已删除）
- 自动填充：`BaseEntity` 提供 `createTime`/`updateTime` 自动填充

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `file.upload.storage-type` | 文件存储类型（minio/local） | minio |
| `jwt.secret` | JWT 签名密钥 | — |
| `jwt.expiration` | JWT 过期时间 | — |
| `spring.datasource.*` | 数据库连接 | — |
| `spring.data.redis.*` | Redis 连接 | — |

## 许可证

MIT License

## 联系方式

- 开发者：MournDev
- 邮箱：3070500838@qq.com
- GitHub：https://github.com/MournDev/check-repeat-system

---

**© 2026 论文查重管理系统 版权所有**
