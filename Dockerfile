FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENV TZ=UTC
ENTRYPOINT ["java","-jar", "app.jar"]