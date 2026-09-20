# ==============================================================================
# STAGE 1: Build JAR artifact using JDK 25 on Alpine Linux
# ==============================================================================
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper configuration and dependencies descriptor
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Normalize line endings, grant execute permission, and pre-fetch dependencies
RUN sed -i 's/\r$//' mvnw && \
    chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

# Copy project source code
COPY src ./src

# Compile and package production artifact without executing test suites
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# STAGE 2: Lightweight Production JRE 25 Runtime
# ==============================================================================
FROM eclipse-temurin:25-jre-alpine

# Create unprivileged non-root system group and user
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup -s /sbin/nologin

WORKDIR /app

# Copy built application JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Enforce secure file ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose HTTP application port
EXPOSE 8400

# High-performance JVM garbage collection and container memory tuning
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0"

# Application startup entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
