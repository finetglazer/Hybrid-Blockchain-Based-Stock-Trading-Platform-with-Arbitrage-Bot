# 1) Use a base image for Java
FROM eclipse-temurin:17-jdk-alpine

# 2) Create app directory
WORKDIR /app

# 3) Copy the jar file from target
COPY target/API-gateway-0.0.1-SNAPSHOT.jar app.jar

# 4) Expose the port (optional for documentation)
EXPOSE 8080

# 5) Run the jar
ENTRYPOINT ["java","-jar","app.jar"]
