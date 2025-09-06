# Multi-stage Docker build for Render deployment
FROM maven:3.8.4-openjdk-8 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src
COPY config ./config

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:8-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/distributed-api-rate-limit-system-1.0-SNAPSHOT.jar app.jar
COPY --from=build /app/config ./config

# Create a startup script that accepts environment variables
RUN echo '#!/bin/bash\n\
java -Xmx256m -cp app.jar com.Motupallisailohith.ratelimit.SimpleMain \
  --algorithm=${ALGORITHM:-tokenbucket} \
  --http.port=${PORT:-8080} \
  --udp.port=${UDP_PORT:-7001} \
  --config=${CONFIG_FILE:-config/demo-nodes.yml}' > start.sh && chmod +x start.sh

# Expose port
EXPOSE $PORT

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:$PORT/api/status || exit 1

# Start the application
CMD ["./start.sh"]