
FROM eclipse-temurin:17-jdk AS build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM tomcat:10.1-jdk17

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build ./target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
