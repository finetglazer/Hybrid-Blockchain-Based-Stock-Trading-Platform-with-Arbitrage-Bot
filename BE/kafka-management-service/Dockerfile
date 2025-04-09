FROM openjdk:11-jdk-slim

WORKDIR /app

# Copy maven executable and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Build dependencies separately (for better caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Extract the jar file
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Create an entrypoint
ENTRYPOINT ["java", "-cp", "target/dependency:", "-jar", "target/*.jar"]

# Expose the port
EXPOSE 8085