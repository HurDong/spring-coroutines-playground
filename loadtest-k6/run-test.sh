#!/bin/bash

# Create reports directory
mkdir -p reports
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Function to run k6
run_k6() {
    APP_NAME=$1
    PORT=$2
    SLUG=$3
    
    # JSON filename inside the container path (/reports/...)
    JSON_FILENAME="report_${SLUG}_${TIMESTAMP}.json"
    
    echo "=================================================="
    echo "🚀 Starting Load Test for $APP_NAME (Port $PORT)"
    echo "=================================================="
    
    # Run k6 with volume mount for reports and enable summary export
    # We use MSYS_NO_PATHCONV=1 to prevent Git Bash from converting paths
    export MSYS_NO_PATHCONV=1
    
    docker run --rm -i --network host -v "$(pwd)/reports:/reports" grafana/k6 run \
        --summary-export "/reports/$JSON_FILENAME" \
        -e PORT=$PORT - < script.js
        
    echo "✅ Test finished for $APP_NAME"
    echo "💾 JSON Report saved: reports/$JSON_FILENAME"
    echo ""
}

# Check argument
if [ -z "$1" ]; then
    echo "Usage: ./run-test.sh [mvc|java|kotlin|all]"
    exit 1
fi

case "$1" in
    mvc)
        run_k6 "MVC (Blocking)" 8081 "mvc"
        ;;
    java)
        run_k6 "WebFlux (Java)" 8082 "java"
        ;;
    kotlin)
        run_k6 "WebFlux (Kotlin)" 8083 "kotlin"
        ;;
    all)
        run_k6 "MVC (Blocking)" 8081 "mvc"
        sleep 5
        run_k6 "WebFlux (Java)" 8082 "java"
        sleep 5
        run_k6 "WebFlux (Kotlin)" 8083 "kotlin"
        ;;
    *)
        echo "Unknown target: $1"
        echo "Available targets: mvc, java, kotlin, all"
        exit 1
        ;;
esac
