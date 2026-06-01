## build stage ##
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app
COPY . .
RUN mvn install -DskipTests=true

## run stage ##
FROM alpine:3.19

RUN apk add openjdk21

WORKDIR /run
COPY --from=build /app/target/be-project-web-0.0.1-SNAPSHOT.war /run/be-project-web-0.0.1-SNAPSHOT.war

EXPOSE 5001

ENTRYPOINT ["java", "-jar", "be-project-web-0.0.1-SNAPSHOT.war"]