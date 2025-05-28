FROM maven:3.8.4-openjdk-17 AS build

# Cambiar al subdirectorio donde está el pom.xml
WORKDIR /app
COPY backend /app
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
