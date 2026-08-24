# =====================================================================
# 水务监测分析系统 - 后端镜像 (Spring Boot)
# 多阶段构建：Maven 编译 -> JRE 运行
# =====================================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷贝 pom 以利用层缓存
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

# 拷贝源码并打包（跳过测试）
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/water-monitor-1.0.0.jar /app/app.jar

VOLUME ["/app/reports"]

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]