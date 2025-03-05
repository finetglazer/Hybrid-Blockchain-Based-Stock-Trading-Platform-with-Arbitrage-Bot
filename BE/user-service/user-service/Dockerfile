# Use a base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Install MongoDB driver manually
RUN wget -O /usr/share/java/mongo-driver.jar https://repo1.maven.org/maven2/org/mongodb/mongodb-driver/4.6.1/mongodb-driver-4.6.1.jar

# Copy the application JAR
COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-cp", "/usr/share/java/mongo-driver.jar:app.jar", "org.springframework.boot.loader.JarLauncher"]
