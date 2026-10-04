FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=render -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar app.jar"]