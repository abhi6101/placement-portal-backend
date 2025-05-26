# Stage 1: Build the Spring Boot application
# Uses a Maven image with Temurin JDK 21 for building
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the container for the build process
WORKDIR /build

# Copy the entire backend project source code to the build directory
# This assumes the Dockerfile is in the 'fully backend/' directory
# so '.' refers to the content of 'fully backend/'
COPY . /build

# Execute the Maven clean package command to build the JAR
# -DskipTests is used to skip running tests during the Docker build,
# which speeds up the build process. You should run tests separately.
RUN mvn clean package -DskipTests

# Stage 2: Create the final, smaller runtime image
# Uses a lean JRE-only image (Temurin 21 JRE Alpine for minimal size)
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container for the application
WORKDIR /app

# Copy the built JAR file from the 'builder' stage to the '/app' directory
# The JAR name 'authProject-0.0.1-SNAPSHOT.jar' is specific to your project.
# It's safer to use a wildcard if the name changes, or ensure it's exact.
# If you used 'target/*.jar' in previous examples, stick to that
# or use your specific JAR name:
COPY --from=builder /build/target/authProject-00.1-SNAPSHOT.jar /app/app.jar
# OR, if you prefer a wildcard (more robust if version changes):
# COPY --from=builder /build/target/*.jar /app/app.jar


# Expose the port that the Spring Boot application listens on
# This informs Docker that the container will listen on this port.
EXPOSE 8080

# Define the command to run the application when the container starts
# 'java -jar /app/app.jar' executes your Spring Boot application.
CMD ["java", "-jar", "/app/app.jar"]