# =====================================================================
# Stage 1: Build
# =====================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Gradle primero para cachear dependencias
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew gradlew
RUN chmod +x gradlew

# Descargar dependencias (se cachea si build.gradle no cambia)
RUN ./gradlew dependencies --no-daemon || true

# Copiar fuentes y compilar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# =====================================================================
# Stage 2: Runtime
# =====================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
