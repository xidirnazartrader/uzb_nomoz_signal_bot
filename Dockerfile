# 1-bosqich: Loyihani yig'ish (Build)
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2-bosqich: Yengil JRE konteynerda ishga tushirish (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# SQLite va ma'lumotlar saqlanishi uchun papka
RUN mkdir -p /app/data

# Fat JAR ni nusxalash
COPY --from=builder /app/target/uzb_nomoz_signal_bot-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
