#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

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
    # MSYS_NO_PATHCONV=1 prevents Git Bash from messing up Docker volume paths like /c/Users/...
    export MSYS_NO_PATHCONV=1
    
    # Use pwd -W to get Windows style path for volume mount if needed, 
    # but normally MSYS_NO_PATHCONV=1 with $(pwd) works or we use absolute windows path.
    # We use the SCRIPT_DIR/reports to store reports.
    
    mkdir -p "$SCRIPT_DIR/reports"
    
    docker run --rm -i --network host -v "$SCRIPT_DIR/reports:/reports" grafana/k6 run \
        --summary-export "/reports/$JSON_FILENAME" \
        -e PORT=$PORT \
        -e VUS=$VUS \
        -e DURATION=$DURATION \
        -e TARGET_VUS=$VUS \
        -e FANOUT=$FANOUT \
        - < "$SCRIPT_DIR/script.js"
        
    echo "✅ Test finished for $APP_NAME"
    echo "💾 JSON Report saved: reports/$JSON_FILENAME"
    echo ""
}

# Create reports directory
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
        
        # Prevent Git Bash path conversion issues for Docker
        export MSYS_NO_PATHCONV=1
        
        mkdir -p "$SCRIPT_DIR/reports"
        
        # Default HOSTNAME to host.docker.internal for Windows compatibility
        # We ignore system HOSTNAME and prefer host.docker.internal for Docker Desktop
        TARGET_HOST=${TARGET_HOST:-host.docker.internal}

        echo "👉 Running Blocking Test (MVC)..."
        docker run --rm -i -v "$SCRIPT_DIR/reports:/reports" grafana/k6 run \
            --summary-export "/reports/report_mvc_${TIMESTAMP}.json" \
            -e TARGET_ENV=blocking \
            -e HOSTNAME=$TARGET_HOST \
            - < "$SCRIPT_DIR/script-comparison.js"
            
        echo "✅ Blocking test done."
        echo ""
        sleep 5
        
        echo "👉 Running Non-Blocking Test (WebFlux)..."
        docker run --rm -i -v "$SCRIPT_DIR/reports:/reports" grafana/k6 run \
            --summary-export "/reports/report_webflux_${TIMESTAMP}.json" \
            -e TARGET_ENV=non-blocking \
            -e HOSTNAME=$TARGET_HOST \
            - < "$SCRIPT_DIR/script-comparison.js"
        
        echo "✅ Non-Blocking test done."
        echo "🎉 All comparison tests finished!"
        ;;
    *)
        echo "Unknown target: $TARGET"
        echo "Available targets: mvc, java, kotlin, all, compare"
        exit 1
        ;;
esac
