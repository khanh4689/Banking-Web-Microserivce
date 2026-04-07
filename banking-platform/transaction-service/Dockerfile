FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY transaction-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
