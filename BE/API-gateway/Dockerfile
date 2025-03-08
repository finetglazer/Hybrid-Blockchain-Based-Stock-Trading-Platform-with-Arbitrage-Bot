# 1️⃣ Use OpenJDK 17 as the base image
FROM openjdk:17

# 2️⃣ Set the working directory inside the container
WORKDIR /app

# 3️⃣ Copy the built JAR file into the container
COPY target/API-gateway-0.0.1-SNAPSHOT.jar app.jar

# 4️⃣ Expose the application port (8080)
EXPOSE 8080

# 5️⃣ Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
