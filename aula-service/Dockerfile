FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache dos2unix
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN dos2unix mvnw && chmod +x mvnw
COPY security-core-1.0-SNAPSHOT.jar /tmp/
RUN ./mvnw install:install-file -q \
    -Dfile=/tmp/security-core-1.0-SNAPSHOT.jar \
    -DgroupId=co.edu.uceva \
    -DartifactId=security-core \
    -Dversion=1.0-SNAPSHOT \
    -Dpackaging=jar
RUN ./mvnw dependency:go-offline -q 2>/dev/null || true
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
