FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY build/libs/app.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]

EXPOSE 8080
