# Build stage — compiles the jar with the Maven wrapper, so the exact Maven
# version pinned by mvnw is used rather than whatever the base image ships.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Run stage — a slim JRE-only image with just the built jar.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
