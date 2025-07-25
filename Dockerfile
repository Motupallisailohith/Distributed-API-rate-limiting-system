# 1) Build stage
FROM maven:3.8.7-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# 2) Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/distributed-api-rate-limit-system-1.0-SNAPSHOT.jar app.jar

# Expose both HTTP & UDP ports
EXPOSE 8080 7000/udp

ENTRYPOINT ["java","-jar","/app/app.jar"]
