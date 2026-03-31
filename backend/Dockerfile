# Stage 1: Build stage
FROM gradle:8.8-jdk21 AS builder

WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY build.gradle .
COPY gradle.properties .
COPY settings.gradle .

# Copiar código fuente
COPY src/ src/

# Compilar la aplicación
RUN gradle build -x test --no-daemon

# Stage 2: Runtime stage
FROM openjdk:21-slim

WORKDIR /app

# Copiar el JAR compilado desde el stage de build
COPY --from=builder /app/build/libs/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.PropertiesLauncher --help || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=docker"]
