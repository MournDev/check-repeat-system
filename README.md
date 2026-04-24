# 论文查重管理系统

## 项目简介

论文查重管理系统是一个基于 Spring Boot + Vue 3 的全栈应用，专为高校论文查重和管理设计。系统支持学生论文提交、自动查重、教师审核、实时消息通知等核心功能，提供了完整的论文管理工作流程。

## 技术栈

### 后端
- **框架**：Spring Boot 3.2.4
- **数据库**：MySQL 8.0+
- **缓存**：Redis
- **认证**：JWT
- **文件处理**：Apache Commons FileUpload
- **WebSocket**：原生 WebSocket（实时消息通知）
- **构建工具**：Maven

### 前端
- **框架**：Vue 3 + Composition API
- **UI 库**：Element Plus
- **状态管理**：Pinia
- **路由**：Vue Router
- **HTTP 客户端**：Axios
- **动画**：GSAP（登录页角色动画）
- **图表**：ECharts
- **样式**：SCSS

## 核心功能

### 1. 学生端
- **论文提交**：支持Word、PDF等格式文件上传，自动计算MD5实现秒传
- **查重监控**：实时查看查重进度和结果
- **历史记录**：查看历史查重记录和趋势分析
- **导师互动**：实时消息聊天和文件共享
- **论文管理**：查看和管理已提交的论文

### 2. 教师端
- **论文审核**：审核学生提交的论文
- **待审核列表**：查看待审核的论文
- **审核记录**：查看历史审核记录
- **学生管理**：管理学生信息
- **审核工作流**：配置审核流程

### 3. 管理员端
- **用户管理**：管理系统用户
- **系统配置**：配置系统参数
- **数据统计**：查看系统数据统计
- **日志管理**：查看系统操作日志

## 系统架构

系统采用前后端分离架构：

1. **前端**：Vue 3 单页应用，部署在 Nginx 或类似Web服务器
2. **后端**：Spring Boot 应用，提供 RESTful API
3. **数据库**：MySQL 存储业务数据
4. **缓存**：Redis 缓存热点数据和管理 Session
5. **文件存储**：本地文件系统或对象存储
6. **实时通信**：WebSocket 实现实时消息通知

## 目录结构

```
check-repeat-system/
├── .github/                # GitHub 配置
│   └── workflows/          # CI/CD 工作流
├── frontend/               # 前端项目
│   ├── public/             # 静态资源
│   ├── src/                # 源代码
│   │   ├── api/            # API 接口
│   │   ├── components/     # 组件
│   │   ├── views/          # 页面
│   │   ├── composables/    # 组合式函数
│   │   ├── router/         # 路由
│   │   └── store/          # 状态管理
│   └── package.json        # 前端依赖
├── src/                    # 后端源代码
│   ├── main/java/com/abin/checkrepeatsystem/
│   │   ├── admin/          # 管理员模块
│   │   ├── common/         # 公共模块
│   │   ├── config/         # 配置
│   │   ├── mapper/         # MyBatis 映射
│   │   ├── model/          # 数据模型
│   │   ├── service/        # 业务逻辑
│   │   ├── student/        # 学生模块
│   │   ├── teacher/        # 教师模块
│   │   └── websocket/      # WebSocket 处理
│   └── main/resources/     # 资源文件
├── pom.xml                 # Maven 配置
└── README.md               # 项目说明
```

## 安装部署

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+
- Maven 3.8+

### 后端部署
1. **配置数据库**：
   - 创建数据库 `check_repeat_system`
   - 执行数据库脚本

2. **修改配置**：
   - 修改 `src/main/resources/application.yml` 中的数据库连接信息
   - 修改 Redis 连接信息

3. **构建项目**：
   ```bash
   mvn clean package
   ```

4. **运行项目**：
   ```bash
   java -jar target/check-repeat-system-0.0.1-SNAPSHOT.jar
   ```

### 前端部署
1. **安装依赖**：
   ```bash
   cd frontend
   npm install
   ```

2. **构建项目**：
   ```bash
   npm run build
   ```

3. **部署到服务器**：
   - 将 `dist` 目录部署到 Nginx 或其他 Web 服务器

## CI/CD 配置

项目使用 GitHub Actions 进行持续集成，配置文件位于 `.github/workflows/maven.yml`，主要包括：

- **代码检出**：从 GitHub 仓库检出代码
- **环境配置**：设置 Java 17 环境
- **依赖缓存**：缓存 Maven 依赖
- **编译构建**：编译 Java 源代码
- **运行测试**：执行单元测试
- **打包项目**：生成可执行的 jar 文件

## 贡献指南

1. **Fork 项目**
2. **创建分支**：`git checkout -b feature/your-feature`
3. **提交修改**：`git commit -m "Add your feature"`
4. **推送分支**：`git push origin feature/your-feature`
5. **创建 Pull Request**

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 联系方式

- **开发者**：MournDev
- **邮箱**：3070500838@example.com
- **GitHub**：https://github.com/MournDev/check-repeat-system


**© 2026 论文查重管理系统 版权所有**
