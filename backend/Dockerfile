# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and POM first for Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Prefetch dependencies using BuildKit cache mount with auto-retry
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:resolve -B -Dmaven.wagon.http.retryHandler.count=5 || true

# Copy source code and package application
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -DskipTests -B -Dmaven.wagon.http.retryHandler.count=5

# Minimal runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Production JVM container optimizations
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
