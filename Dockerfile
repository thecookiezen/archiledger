FROM eclipse-temurin:21-jre-alpine-3.23

WORKDIR /app

# Default data directory for Neo4j persistence
ENV NEO4J_DATA_DIR=/data/neo4j

# Default Bolt port for Neo4j connections (0 = dynamic port)
ENV NEO4J_BOLT_PORT=7687

RUN addgroup -S spring && adduser -S spring -G spring

# Create data directory with proper permissions before switching user
RUN mkdir -p /data/neo4j && chown -R spring:spring /data

USER spring:spring

COPY mcp/target/*.jar app.jar

# HTTP server port and Neo4j Bolt port
EXPOSE 8080 7687

# Volume mount point for Neo4j data
VOLUME ["/data/neo4j"]

ENTRYPOINT ["java", \
           "-Dspring.profiles.active=neo4j", \
           "-Dspring.neo4j.uri=embedded", \
           "-Dmemory.neo4j.data-dir=${NEO4J_DATA_DIR}", \
           "-Dmemory.neo4j.bolt-port=${NEO4J_BOLT_PORT}", \
           "-jar", \
           "app.jar"]