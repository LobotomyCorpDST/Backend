# --- BUILD STAGE ---
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod 777 ./gradlew
RUN ./gradlew dependencies || return 0
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# --- FINAL STAGE ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Update the exposed port to match your application's configuration
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
