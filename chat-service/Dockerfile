FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata
ENV TZ=America/Bogota
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Duser.timezone=America/Bogota", "-jar", "app.jar"]
