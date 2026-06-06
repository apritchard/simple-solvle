#
# Stage 1 — build the React frontend into static assets
#
FROM node:20-alpine AS frontend
WORKDIR /app
COPY solvle-front/package.json solvle-front/package-lock.json ./
RUN npm ci
COPY solvle-front/ ./
# CRA emits a production build to /app/build (relative /solvle API paths, so same-origin)
RUN npm run build

#
# Stage 2 — build the Spring Boot jar, bundling the frontend into static resources
#
FROM maven:3.9.9-eclipse-temurin-21 AS backend
WORKDIR /home/app
COPY pom.xml ./
COPY src ./src
# Spring Boot serves classpath:/static/** at /, so the SPA and API share one origin
COPY --from=frontend /app/build ./src/main/resources/static
# Tests run in the CI workflow; skip here to keep image builds fast
RUN mvn -B -f pom.xml clean package -DskipTests

#
# Stage 3 — minimal runtime
#
FROM eclipse-temurin:21-jre-alpine
COPY --from=backend /home/app/target/*.jar /usr/local/lib/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=80.0", "-jar", "/usr/local/lib/app.jar"]
