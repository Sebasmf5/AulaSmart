FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# 1. Copiamos los archivos de configuración (POMs) para cachear dependencias
COPY security-core/pom.xml security-core/pom.xml
COPY chat-service/pom.xml chat-service/pom.xml

# 2. Instalamos security-core primero (dependencia local de chat-service)
# Copiamos solo la carpeta de security-core para este paso
COPY security-core security-core
RUN mvn -f security-core/pom.xml clean install -DskipTests -B

# 3. Descargamos las dependencias de chat-service para cachearlas en una capa
# Usamos dependency:go-offline para bajar todo lo necesario sin compilar aún
RUN mvn -f chat-service/pom.xml dependency:go-offline -B -Dhttp.keepAlive=false

# 4. Copiamos el código fuente de chat-service y compilamos el microservicio
COPY chat-service/src chat-service/src
WORKDIR /workspace/chat-service
RUN mvn clean package -DskipTests -B

# Etapa de ejecución
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=build /workspace/chat-service/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
