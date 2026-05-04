# Step 1: Build the application using Maven and JDK 17
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the application using Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /target/web-app-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]