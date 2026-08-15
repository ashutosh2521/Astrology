# syntax=docker/dockerfile:1
#
# Monitoring image: the Spring Boot Admin server (dashboard + email alerts). Self-contained
# Maven module, so this builds with the monitoring/ directory as its context.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B clean package -DskipTests
RUN cp target/kundli-monitoring-*.jar /app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
RUN groupadd --system kundli \
    && useradd --system --gid kundli --home-dir /opt/kundli --shell /usr/sbin/nologin kundli
USER kundli
WORKDIR /opt/kundli
COPY --from=build --chown=kundli:kundli /app.jar /opt/kundli/kundli-monitoring.jar
# Dashboard port, published on loopback by compose; reached over an SSH tunnel.
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/opt/kundli/kundli-monitoring.jar"]
