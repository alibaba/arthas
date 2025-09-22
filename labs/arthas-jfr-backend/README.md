# Arthas JFR Backend - Java Flight Recorder 分析后端

基于 Spring Boot 3.5.3 的现代化 JFR (Java Flight Recorder) 文件分析后端服务，提供 RESTful API 支持多维度性能分析和火焰图数据生成。

## 功能特性

### 核心功能
- **JFR 文件解析**: 基于 JMC 8.3.1 的标准 JFR 文件解析引擎
- **多维度分析**: 支持 17+ 种性能维度的深度分析
- **文件管理**: 完整的文件上传、存储、删除和查询功能
- **火焰图数据**: 生成前端火焰图组件所需的数据结构
- **RESTful API**: 提供标准化的 REST API 接口

### 分析维度
- **CPU 性能**: CPU 时间、CPU 采样、原生执行采样
- **内存分析**: 内存分配次数、分配大小统计
- **I/O 操作**: 文件读写时间、网络读写时间
- **线程分析**: 线程同步、线程等待、线程睡眠时间
- **类加载**: 类加载次数、类加载时间统计
- **时钟分析**: 墙钟时间、CPU 时间对比分析

### 技术特性
- **高性能**: 支持大文件处理，优化的内存使用
- **可扩展**: 模块化的提取器架构，易于添加新的分析维度
- **容错性**: 完善的异常处理和错误码体系
- **数据持久化**: 支持 H2 内存数据库和 MySQL 生产数据库

## 技术栈

### 后端技术
- **Spring Boot 3.5.3**: 主框架，支持 Java 17+
- **Java 17**: 开发语言，使用现代 Java 特性
- **Spring Data JPA**: 数据持久化层
- **H2/MySQL**: 数据库支持（开发/生产环境）
- **JMC 8.3.1**: Java Mission Control，JFR 文件解析核心
- **Lombok**: 减少样板代码
- **Maven**: 依赖管理和构建工具

### 核心依赖
- `org.openjdk.jmc:flightrecorder` - JFR 文件解析
- `org.openjdk.jmc:common` - JMC 通用组件
- `org.openjdk.jmc:flightrecorder.rules` - JFR 规则引擎
- `org.springframework.boot:spring-boot-starter-web` - Web 服务
- `org.springframework.boot:spring-boot-starter-data-jpa` - 数据访问

## 项目结构

```
arthas-jfr-backend/
├── src/main/java/org/example/jfranalyzerbackend/
│   ├── config/              # 配置类
│   │   ├── ArthasConfig.java    # Arthas 集成配置
│   │   ├── CorsConfig.java      # 跨域配置
│   │   └── Result.java          # 统一响应结果
│   ├── controller/          # REST 控制器
│   │   ├── FileController.java      # 文件管理 API
│   │   └── JFRAnalysisController.java # JFR 分析 API
│   ├── service/             # 业务服务层
│   │   ├── FileService.java         # 文件服务接口
│   │   ├── JFRAnalysisService.java  # JFR 分析服务接口
│   │   ├── JFRAnalyzer.java        # JFR 分析器接口
│   │   └── impl/                   # 服务实现
│   ├── extractor/           # JFR 数据提取器
│   │   ├── Extractor.java          # 提取器基类
│   │   ├── EventVisitor.java       # 事件访问器
│   │   ├── JFRAnalysisContext.java # 分析上下文
│   │   ├── *Extractor.java         # 各种性能维度提取器
│   │   └── PerfDimensionFactory.java # 性能维度工厂
│   ├── entity/              # 实体类
│   │   ├── shared/              # 共享实体
│   │   ├── FileEntity.java       # 文件实体
│   │   └── ProfileDimension.java # 性能维度实体
│   ├── model/               # 数据模型
│   │   ├── AnalysisResult.java    # 分析结果模型
│   │   ├── FlameGraph.java        # 火焰图数据模型
│   │   ├── jfr/                  # JFR 相关模型
│   │   └── symbol/               # 符号表模型
│   ├── repository/          # 数据访问层
│   ├── enums/              # 枚举类
│   ├── exception/          # 异常处理
│   ├── request/            # 请求对象
│   ├── vo/                 # 视图对象
│   ├── util/               # 工具类
│   └── JfrAnalyzerBackendApplication.java # 启动类
└── src/main/resources/
    └── application.yml      # 应用配置文件
```

##  快速开始

### 环境要求
- **Java 17+**: 必需，项目使用 Java 17 特性
- **Maven 3.6+**: 构建工具
- **内存**: 建议 4GB+，用于处理大型 JFR 文件

### 配置数据库（可选）
默认使用 H2 内存数据库，生产环境可配置 MySQL：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/arthas_jfr
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQLDialect
```

### 启动服务
```bash
# 开发模式
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/arthas-jfr-backend-4.0.5.jar
```

### 验证服务
- 后端服务: `http://localhost:8200`
- H2 控制台: `http://localhost:8200/h2-console`
- API 文档: `http://localhost:8200/api/files` (文件列表)

### 配置说明
```yaml
# 关键配置项
arthas:
  jfr-storage-path: ${user.home}/arthas-jfr-storage  # JFR 文件存储路径

spring:
  servlet:
    multipart:
      max-file-size: 1GB        # 最大文件大小
      max-request-size: 1GB     # 最大请求大小
  server:
    port: 8200                  # 服务端口
```

##  开发指南

### 添加新的分析维度

1. 创建新的提取器类：
```java
@Component
public class CustomExtractor extends Extractor {
    @Override
    public String getDimensionName() {
        return "CUSTOM_DIMENSION";
    }
    
    @Override
    public void extract(RecordedEvent event, JFRAnalysisContext context) {
        // 实现提取逻辑
    }
}
```

2. 在 `PerfDimensionFactory` 中注册：
```java
public static PerfDimension createCustomDimension() {
    return new PerfDimension("CUSTOM_DIMENSION", "自定义维度", "ms");
}
```

### 数据库配置
默认使用 H2 内存数据库，生产环境可配置 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/arthas_jfr
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```


## 参考项目

本项目参考了以下优秀的开源项目：

- **[Java Mission Control (JMC)](https://github.com/openjdk/jmc)** - Oracle 官方的 Java 性能监控工具
- **[JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html)** - 商业 Java 性能分析工具
- **[VisualVM](https://visualvm.github.io/)** - 免费的 Java 性能分析工具
- **[FlameGraph](https://github.com/brendangregg/FlameGraph)** - 火焰图生成工具
- **[Jifa](https://github.com/eclipse-jifa/jifa)** - Java 应用诊断工具

## 相关技术文档

### 后端技术
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA 文档](https://spring.io/projects/spring-data-jpa)
- [Java Mission Control 文档](https://github.com/openjdk/jmc)
- [JFR 文件格式规范](https://openjdk.org/projects/jdk/8/)

### 性能分析
- [Java Flight Recorder 用户指南](https://docs.oracle.com/en/java/javase/11/jfr/)
- [JMC 分析指南](https://www.oracle.com/java/technologies/javase/jmc.html)



