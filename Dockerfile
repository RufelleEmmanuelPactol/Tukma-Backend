# Stage 1: Build the Spring Boot application
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Set the working directory for the build
WORKDIR /app

# Copy the Maven project files
COPY pom.xml ./
COPY src ./src

# Run the Maven build to create the JAR file
RUN mvn clean package

# Debug: Check that the JAR file exists and its name is correct
RUN ls /app/target && jar tf /app/target/*.jar

# Stage 2: Use the OpenJDK slim image for the runtime
FROM openjdk:17

# Set the working directory for the runtime
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/tukma-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app will run on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]