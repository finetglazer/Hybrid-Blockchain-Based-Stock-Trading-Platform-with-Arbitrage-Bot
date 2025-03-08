# 1️⃣ Use OpenJDK 17 as the base image
FROM openjdk:17

# 2️⃣ Set the working directory inside the container
WORKDIR /app

# 3️⃣ Copy the pom.xml and src folder to the container (to build the project)
COPY pom.xml .
COPY src ./src

# 4️⃣ Run Maven to build the project (this will generate the JAR file in the target directory)
RUN mvn clean package -DskipTests

# 5️⃣ Copy the built JAR file into the container (from the target directory)
COPY target/API-gateway-0.0.1-SNAPSHOT.jar app.jar

# 6️⃣ Expose the application port (8080)
EXPOSE 8080

# 7️⃣ Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
