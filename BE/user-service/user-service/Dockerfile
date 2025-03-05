FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar

# (No EXPOSE line here because we don't expose it publicly,
#  but you can declare EXPOSE 8081 if you like. It's optional.)
ENTRYPOINT ["java","-jar","app.jar"]
