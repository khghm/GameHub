# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY ..
RUN ./gradlew :server:shadowJar -x test

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/server/build/libs/server-*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
