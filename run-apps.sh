#!/bin/bash

echo "Starting apps using Gradle Wrapper..."

# Now that wrapper is generated, we can use it directly.
# We launch wrapper in separate PowerShell windows.

start powershell -NoExit -Command ".\gradlew :app-mvc-java:bootRun"
start powershell -NoExit -Command ".\gradlew :app-webflux-java:bootRun"
start powershell -NoExit -Command ".\gradlew :app-webflux-kotlin:bootRun"

echo "Launching..."
