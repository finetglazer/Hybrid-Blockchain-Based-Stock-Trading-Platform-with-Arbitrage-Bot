# 1️⃣ Use OpenJDK 17 as the base image
FROM openjdk:17

# 2️⃣ Set the working directory inside the container
WORKDIR /app

# 5️⃣ Copy the built JAR file into the container (from the target directory)
COPY target/trading-service-0.0.1-SNAPSHOT.jar /app/app.jar

# 6️⃣ Expose the application port (8084)
EXPOSE 8084

# 7️⃣ Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
