# ============================================
# Dockerfile for auth-service
# Supports: dev, staging, prod environments
# Compatible with ARM64 (Apple Silicon) and AMD64
# ============================================
# Build JAR locally first: mvn clean package -DskipTests
# Then: docker build -t auth-service:latest .
# ============================================

FROM eclipse-temurin:17-jre

# Labels for container metadata
LABEL maintainer="LMS Team"
LABEL service="auth-service"
LABEL version="1.0"

# Create non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash appuser

WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/logs /app/config /tmp && \
    chown -R appuser:appgroup /app /tmp

# Create a volume for temporary files and logs
VOLUME ["/tmp", "/app/logs"]

# Copy the pre-built jar file from target directory
COPY target/user-auth-service-*.jar app.jar

# Change ownership of the jar
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
# Pass DB_URL, DB_USERNAME, DB_PASSWORD, INTERNAL_API_KEY at runtime via -e flags
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -Dserver.port=${SERVER_PORT} -jar app.jar"]
