# Build stage: chạy trên GitHub Actions, không phải EC2
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY . .
RUN mvn -B clean package -DskipTests

# Runtime stage: chỉ chứa Java runtime và file WAR
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.war /app/app.war

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "/app/app.war"]