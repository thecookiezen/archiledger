# Use a minimal JRE image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the jar file. The jar is expected to be built by the CI pipeline before this stage.
COPY target/*.jar app.jar

# Expose the port (stdio transport doesn't technically use a port, but Neo4j/HTTP might)
# Exposing 8080 just in case HTTP is enabled later
EXPOSE 8080

# Configure the entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]