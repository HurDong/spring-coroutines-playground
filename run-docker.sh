#!/bin/bash

echo "Building and starting all services with Docker Compose..."

# Ensure jars are built
echo "Ensuring JARs are built..."
./gradlew bootJar

# Run docker-compose
echo "Starting Docker Compose..."
docker-compose up -d --build

echo "Services are starting:"
echo " - MVC App: http://localhost:8081"
echo " - WebFlux Java: http://localhost:8082"
echo " - WebFlux Kotlin: http://localhost:8083"
echo " - Prometheus: http://localhost:9090"
echo " - Grafana: http://localhost:3000"
