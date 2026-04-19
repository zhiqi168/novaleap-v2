# 第一阶段：Maven 编译构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

# 忽略测试跳过加快打包速度
RUN mvn clean package -Dmaven.test.skip=true

# 第二阶段：最小化 JRE 运行环境
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 拷贝构建出的 jar 
COPY --from=builder /build/target/nova-backend-*.jar app.jar

ENV TZ=Asia/Shanghai

# 核心要求：限制 JVM 内存最高为 384MB
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-XX:+UseG1GC", "-jar", "app.jar"]
