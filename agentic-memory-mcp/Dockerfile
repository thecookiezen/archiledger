FROM eclipse-temurin:21-jre

WORKDIR /app

# Default file path for LadybugDB persistence
ENV LADYBUGDB_DATA_PATH=/data/archiledger.lbdb
# Default directory for LadybugDB extension cache
ENV LADYBUGDB_EXTENSION_DIR=/data/ladybugdb-extensions
# Default JVM memory settings
ENV INITIAL_MEMORY=256m
ENV MAX_MEMORY=512m
ENV MAX_RAM_PERCENTAGE=75.0
# LLM Configuration (must be overridden at runtime)
ENV OPENAI_CUSTOM_BASE_URL=""
ENV OPENAI_CUSTOM_MODELS=""
ENV OPENAI_CUSTOM_API_KEY=""
ENV OPENAI_CUSTOM_COMPLETIONS_PATH="/v1/chat/completions"

RUN groupadd -r spring && useradd -r -g spring -u 1000 spring

# Create data and extension directories with proper permissions before switching user
RUN mkdir -p /data/ladybugdb-extensions && chown -R spring:spring /data

USER spring:spring

COPY agentic-memory-mcp/target/*.jar app.jar

# HTTP server port
EXPOSE 8080

# Volume mount point for LadybugDB data
VOLUME ["/data"]

ENTRYPOINT ["sh", "-c", "java \
    -Xms${INITIAL_MEMORY} \
    -Xmx${MAX_MEMORY} \
    -XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE} \
    -Dladybugdb.data-path=${LADYBUGDB_DATA_PATH} \
    -Dladybugdb.extension-dir=${LADYBUGDB_EXTENSION_DIR} \
    -jar \
    app.jar"]
