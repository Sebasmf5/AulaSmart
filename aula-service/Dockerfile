FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copiamos todo el repositorio
COPY . .

# 1. Compilamos e instalamos el security-core primero
WORKDIR /workspace/security-core
RUN mvn clean install -DskipTests

# 2. Compilamos el microservicio
WORKDIR /workspace/aula-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/aula-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
