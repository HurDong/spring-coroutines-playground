#!/bin/bash

# Definition of the script
# This script manages the Docker infrastructure for the project.
# It supports starting, stopping, and restarting the environment.
# It includes health checks to ensure the services are ready.

# Function to check if a port is open (Generic TCP)
wait_for_port() {
  local HOST=$1
  local PORT=$2
  local TIMEOUT=$3
  local START_TIME=$(date +%s)

  echo "⏳ Waiting for $HOST:$PORT to be ready..."
  
  while ! timeout 1 bash -c "cat < /dev/null > /dev/tcp/$HOST/$PORT" >/dev/null 2>&1; do
      current_time=$(date +%s)
      elapsed=$((current_time - START_TIME))
      
      if [ $elapsed -ge $TIMEOUT ]; then
          echo "❌ Timeout waiting for $HOST:$PORT"
          return 1
      fi
      
      sleep 1
  done
  
  echo "✅ $HOST:$PORT is ready!"
  return 0
}

# Function to check HTTP Health Endpoint (Better for Web Apps)
wait_for_http() {
  local URL=$1
  local TIMEOUT=$2
  local START_TIME=$(date +%s)

  echo "⏳ Checking Health: $URL"
  
  while ! curl -s -f "$URL" >/dev/null 2>&1; do
      current_time=$(date +%s)
      elapsed=$((current_time - START_TIME))
      
      if [ $elapsed -ge $TIMEOUT ]; then
          echo "❌ Timeout waiting for $URL"
          return 1
      fi
      
      sleep 1
  done
  
  echo "✅ Service at $URL is UP!"
  return 0
}

# Function to start infrastructure
start_infra() {
    echo "🚀 Starting Infrastructure..."
    docker-compose up -d --build
    
    # Wait for PostgreSQL (TCP Check)
    # Using specific host ip 127.0.0.1 to avoid localhost resolution issues
    if ! wait_for_port 127.0.0.1 5432 30; then
        echo "⚠️  Postgres check failed, but continuing..."
    fi
    
    # Wait for Apps (HTTP Health Check)
    if ! wait_for_http "http://localhost:8081/actuator/health" 60; then
        echo "❌ App MVC failed to start!"
        exit 1
    fi
    
    if ! wait_for_http "http://localhost:8082/actuator/health" 60; then
        echo "❌ App WebFlux Java failed to start!"
        exit 1
    fi
    
    if ! wait_for_http "http://localhost:8083/actuator/health" 60; then
        echo "❌ App WebFlux Kotlin failed to start!"
        exit 1
    fi
    
    echo "🎉 All services are up and running!"
}

# Function to stop infrastructure
stop_infra() {
    echo "🛑 Stopping Infrastructure..."
    docker-compose down -v
    echo "✅ Infrastructure stopped and volumes cleaned."
}

# command switch
case "$1" in
    start)
        start_infra
        ;;
    stop)
        stop_infra
        ;;
    restart)
        stop_infra
        sleep 2
        start_infra
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac
