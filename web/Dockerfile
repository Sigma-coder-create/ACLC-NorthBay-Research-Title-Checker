# Step 1: Build the application using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY . .

# Run Maven build, skipping tests to speed up the process
RUN mvn clean package -DskipTests

# Step 2: Run the application using Java 21 Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the JAR file from the build stage
# Note: Ensure the filename matches your actual generated JAR name
COPY --from=build /app/web/target/*.jar app.jar

# Expose the port (Render usually uses 8080 or 10000)
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]