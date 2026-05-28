FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

# Crear directorio para uploads con permisos para el usuario spring
RUN mkdir -p /app/uploads/incidencias && chown -R spring:spring /app/uploads

USER spring:spring
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
