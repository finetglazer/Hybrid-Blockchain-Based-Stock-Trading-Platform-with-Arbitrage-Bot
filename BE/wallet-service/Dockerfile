# 1️⃣ Use OpenJDK 17 as the base image
FROM openjdk:17

# 2️⃣ Set the working directory inside the container
WORKDIR /app

# 3️⃣ Copy the built JAR file into the container
COPY target/wallet-service-0.0.1-SNAPSHOT.jar app.jar

# 4️⃣ Expose the application port (8082)
EXPOSE 8082

# 5️⃣ Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
