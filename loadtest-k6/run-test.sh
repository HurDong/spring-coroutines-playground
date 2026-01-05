#!/bin/bash

# Default values
VUS=${VUS:-50}
DURATION=${DURATION:-10s}
SCENARIO=${SCENARIO:-default} # default or comparison

# Function to run k6
run_k6() {
    APP_NAME=$1
    PORT=$2
    SLUG=$3
    
    # JSON filename inside the container path (/reports/...)
    JSON_FILENAME="report_${SLUG}_${TIMESTAMP}.json"
    
    echo "=================================================="
    echo "🚀 Starting Load Test for $APP_NAME (Port $PORT)"
    echo "   VUs: $VUS, Duration: $DURATION"
    echo "=================================================="
    
    # Run k6 with volume mount for reports and enable summary export
    # We use MSYS_NO_PATHCONV=1 to prevent Git Bash from converting paths
    export MSYS_NO_PATHCONV=1
    
    docker run --rm -i --network host -v "$(pwd)/reports:/reports" grafana/k6 run \
        --summary-export "/reports/$JSON_FILENAME" \
        -e PORT=$PORT \
        -e VUS=$VUS \
        -e DURATION=$DURATION \
        -e TARGET_VUS=$VUS \
        -e FANOUT=$FANOUT \
        - < script.js
        
    echo "✅ Test finished for $APP_NAME"
    echo "💾 JSON Report saved: reports/$JSON_FILENAME"
    echo ""
}

# Create reports directory
cd "$(dirname "$0")"
mkdir -p reports
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Check argument
if [ -z "$1" ]; then
    echo "Usage: ./run-test.sh [target] [vus] [duration] [fanout]"
    echo "Targets: mvc, java, kotlin, all, compare"
    echo "Example: ./run-test.sh java 100 30s 50"
    exit 1
fi

TARGET=$1
VUS=${2:-50}        # Default 50 VUs
DURATION=${3:-10s}  # Default 10s
FANOUT=${4:-3}      # Default Fanout 3

case "$TARGET" in
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
    compare)
        echo "=================================================="
        echo "🚀 Starting Comparison Test (Blocking vs Non-Blocking)"
        echo "   Duration: 15s"
        echo "=================================================="
        
        export MSYS_NO_PATHCONV=1
        
        # Default HOSTNAME to host.docker.internal for Windows compatibility
        # Users can override it: HOSTNAME=localhost ./run-test.sh compare
        TARGET_HOST=${HOSTNAME:-host.docker.internal}

        echo "👉 Running Blocking Test (MVC)..."
        docker run --rm -i --network host -v "$(pwd)/reports:/reports" grafana/k6 run \
            -e TARGET_ENV=blocking \
            -e HOSTNAME=$TARGET_HOST \
            - < script-comparison.js
            
        echo ""
        echo "👉 Running Non-Blocking Test (WebFlux)..."
        docker run --rm -i --network host -v "$(pwd)/reports:/reports" grafana/k6 run \
            -e TARGET_ENV=non-blocking \
            -e HOSTNAME=$TARGET_HOST \
            - < script-comparison.js
        ;;
    *)
        echo "Unknown target: $TARGET"
        echo "Available targets: mvc, java, kotlin, all, compare"
        exit 1
        ;;
esac
