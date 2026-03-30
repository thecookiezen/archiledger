FROM eclipse-temurin:21-jre

WORKDIR /app

ENV LADYBUGDB_DATA_DIR=/data/archiledger.lbdb
ENV LADYBUGDB_EXTENSION_DIR=/data/ladybugdb-extensions
ENV INITIAL_MEMORY=256m
ENV MAX_MEMORY=512m
ENV MAX_RAM_PERCENTAGE=75.0

RUN groupadd -r spring && useradd -r -g spring -u 1000 spring
RUN mkdir -p /data/ladybugdb-extensions && chown -R spring:spring /data

USER spring:spring

COPY mcp/target/*.jar app.jar

EXPOSE 8080

VOLUME ["/data"]

ENTRYPOINT ["sh", "-c", "java \
    -Xms${INITIAL_MEMORY} \
    -Xmx${MAX_MEMORY} \
    -XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE} \
    -Dladybugdb.data-dir=${LADYBUGDB_DATA_DIR} \
    -Dladybugdb.extension-dir=${LADYBUGDB_EXTENSION_DIR} \
    -jar \
    app.jar"]
