#!/bin/bash
set -e

# Build the project
echo "Building project..."
mvn clean package -DskipTests
JAR_CANDIDATES=(load-tests/target/load-tests-*.jar)
JAR_FILE="${JAR_CANDIDATES[0]}"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: Jar file matching load-tests/target/load-tests-*.jar not found!"
    exit 1
fi

run_scenario() {
    local SCENARIO_NAME=$1
    local ENTITIES=$2
    local RELATIONS=$3
    local HEAP=$4
    
    echo "===================================================================================================="
    echo "Running Scenario: $SCENARIO_NAME | Entities: $ENTITIES | Relations: $RELATIONS | Heap: $HEAP"
    echo "===================================================================================================="
    
    java -Xmx$HEAP -jar "$JAR_FILE" \
    --logging.level.root=ERROR \
    --logging.level.com.thecookiezen.archiledger.loadtests=INFO \
    --spring.profiles.active=neo4j \
    --spring.neo4j.uri=embedded \
    --memory.neo4j.data-dir=/tmp/$SCENARIO_NAME \
    --loadtest.scenario.name="$SCENARIO_NAME" \
    --loadtest.entity-count=$ENTITIES \
    --loadtest.relations-per-entity=$RELATIONS \
    --loadtest.batch-size=25 || { status=$?; echo "Scenario $SCENARIO_NAME FAILED with exit code $status"; }
    
    echo ""
}

# Run Scenarios

run_scenario "Functional" 100 5 "512m"

# run_scenario "Small" 1000 10 "512m"

# run_scenario "Medium" 10000 25 "1g"

# run_scenario "Large" 100000 30 "1g"

echo "All tests completed."
