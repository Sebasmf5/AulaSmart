FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# 1. Copiamos los archivos de configuración (POMs) para cachear dependencias
COPY security-core/pom.xml security-core/pom.xml
COPY incidencia-service/pom.xml incidencia-service/pom.xml

# 2. Instalamos security-core primero
COPY security-core security-core
RUN mvn -f security-core/pom.xml clean install -DskipTests -B

# 3. Descargamos las dependencias de incidencia-service
RUN mvn -f incidencia-service/pom.xml dependency:go-offline -B -Dhttp.keepAlive=false

# 4. Copiamos el código fuente y compilamos
COPY incidencia-service/src incidencia-service/src
WORKDIR /workspace/incidencia-service
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

# Crear directorio para uploads con permisos para el usuario spring
RUN mkdir -p /app/uploads/incidencias && chown -R spring:spring /app/uploads

USER spring:spring
WORKDIR /app
COPY --from=build /workspace/incidencia-service/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
