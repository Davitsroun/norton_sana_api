# BUILD STAGE
FROM gradle:jdk21-ubi-minimal AS build

WORKDIR /app

# Copy only gradle wrapper and config files to cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Pre-download dependencies (without running full build)
RUN ./gradlew dependencies --no-daemon || true

# Now copy the full source
COPY . .

# Build the application (skip tests if desired)
RUN ./gradlew clean build -x test --no-daemon

# RUN STAGE
FROM eclipse-temurin:21.0.7_6-jre-ubi9-minimal

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]