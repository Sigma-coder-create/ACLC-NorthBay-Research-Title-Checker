# Step 1: Build the web module
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the web module's pom.xml and source
COPY web/pom.xml ./pom.xml
COPY web/src ./src

# Build (JAR ends up in /app/target/)
RUN mvn clean package -DskipTests

# Step 2: Run the application
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]