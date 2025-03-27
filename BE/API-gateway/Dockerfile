# Use a smaller JRE image to run the application
FROM openjdk:17

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file built manually from the host machine into the container
COPY ./target/API-gateway-0.0.1-SNAPSHOT.jar /app/api-gateway.jar

# Expose the port the app will run on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "api-gateway.jar"]
