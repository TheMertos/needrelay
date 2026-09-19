# All-in-one NeedRelay app: Vite SPA embedded in Spring Boot JAR.
# Build from repo root: docker build -t needrelay-app .
# Compose: docker compose up -d --build

FROM node:22-alpine AS frontend
WORKDIR /fe
COPY needrelay-frontend/package.json needrelay-frontend/yarn.lock needrelay-frontend/.yarnrc.yml ./
RUN corepack enable && yarn install --immutable
COPY needrelay-frontend/ .
ENV VITE_API_URL=
RUN yarn build

FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /app
COPY needrelay-backend/pom.xml .
COPY needrelay-backend/src ./src
COPY --from=frontend /fe/dist/ ./src/main/resources/static/
RUN mvn -q -DskipTests package \
 && cp target/needrelay-backend-*.jar /app/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
COPY --from=backend /app/app.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
