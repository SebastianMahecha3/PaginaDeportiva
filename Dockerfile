# ===== Etapa 1: compilar el .jar con Maven y Java 21 =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Los tests se ejecutan en tu computador (./mvnw test); aquí solo se empaqueta para que el despliegue sea rápido.
RUN mvn -B -q clean package -DskipTests

# ===== Etapa 2: imagen liviana solo para ejecutar =====
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Zona horaria de las horas de los partidos
ENV TZ=America/Bogota
ENV SPRING_PROFILES_ACTIVE=prod
# Ajustes para servidores de ~512 MB de RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
