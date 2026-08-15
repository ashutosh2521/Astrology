# syntax=docker/dockerfile:1
#
# Backend image: Spring Boot API + Ashtakoot engine + the EMBEDDED Angular web client.
# The Maven build reaches into ../web (frontend-maven-plugin builds the SPA and copies it
# under the jar's static/), so this image MUST be built with the repo ROOT as context:
#
#   docker build -f backend/Dockerfile -t kundli-backend:latest .
#   # or via infra/docker-compose.yml, which sets context: .. for this service.

# ---- Build stage: compiles the jar and the Angular app in one reproducible step ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Bring in the two modules the backend build needs. frontend-maven-plugin downloads its
# own pinned Node under target/, so no Node install is needed here.
COPY backend/ backend/
COPY web/ web/
# BuildKit cache mount keeps the local Maven repo warm across builds (does not end up in
# the final image).
RUN --mount=type=cache,target=/root/.m2 \
    cd backend && mvn -q -B clean package -DskipTests
RUN cp backend/target/kundli-backend-*.jar /app.jar

# ---- Runtime stage: JRE + jar only, no build toolchain ----
FROM eclipse-temurin:21-jre AS runtime
# Unprivileged user; also owns the SQLite state dir so the named volume mounted there
# initializes writable (Docker seeds a fresh named volume with the mountpoint's ownership).
RUN groupadd --system kundli \
    && useradd --system --gid kundli --home-dir /opt/kundli --shell /usr/sbin/nologin kundli \
    && mkdir -p /var/lib/kundli /opt/kundli \
    && chown -R kundli:kundli /var/lib/kundli /opt/kundli
USER kundli
WORKDIR /opt/kundli
COPY --from=build --chown=kundli:kundli /app.jar /opt/kundli/kundli-backend.jar
# 8080 = API/SPA (published on loopback by compose); 8081 = Actuator (internal network only).
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/opt/kundli/kundli-backend.jar"]
