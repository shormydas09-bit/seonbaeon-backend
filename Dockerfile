FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

# Use the Asia-Pacific Google mirror of Maven Central to reduce dependence on
# repo.maven.apache.org when that endpoint rate-limits Render builds.
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">' \
    '  <mirrors>' \
    '    <mirror>' \
    '      <id>google-maven-central-asia</id>' \
    '      <name>Google Maven Central Mirror (Asia Pacific)</name>' \
    '      <url>https://maven-central-asia.storage-download.googleapis.com/maven2/</url>' \
    '      <mirrorOf>central</mirrorOf>' \
    '    </mirror>' \
    '  </mirrors>' \
    '</settings>' > /root/.m2/settings.xml

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "java -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=2 -Dspring.profiles.active=render -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar app.jar"]