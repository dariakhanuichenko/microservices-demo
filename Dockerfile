# One Dockerfile for all four services - pick which one with --build-arg SERVICE=...
# Stage 1: build inside the image so no local Maven/JDK is required.
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /build

# Copy every pom first: the parent lists all four modules, so Maven needs them
# all present. This layer is cached until a pom actually changes.
COPY pom.xml ./
COPY order-service/pom.xml     order-service/
COPY payment-service/pom.xml   payment-service/
COPY user-service/pom.xml      user-service/
COPY inventory-service/pom.xml inventory-service/
RUN mvn -B -ntp -pl ${SERVICE} -am dependency:go-offline -DskipTests

COPY . .
RUN mvn -B -ntp -pl ${SERVICE} -am clean package -DskipTests \
 && cp ${SERVICE}/target/${SERVICE}-*.jar /build/app.jar

# Stage 2: runtime only - JRE instead of JDK, no Maven, no sources.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 spring
COPY --from=build /build/app.jar app.jar
USER spring
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
