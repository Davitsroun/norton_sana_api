# BUILD STAGE
FROM gradle:8.14.3-jdk21-jammy AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./

# Normalize line endings (Windows) and make wrapper executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Cache dependencies
RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon \
    && JAR="$(ls build/libs/*.jar | grep -v plain | head -n 1)" \
    && cp "$JAR" /app/app.jar

# RUN STAGE
FROM eclipse-temurin:21.0.7_6-jre-ubi9-minimal

WORKDIR /app

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
