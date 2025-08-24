#!/bin/bash

# Build and run PC orchestrator development script

echo "Building Bucika GSR Orchestrator..."

# Build PC application
./gradlew :pc:build

if [ $? -eq 0 ]; then
    echo "Build successful. Starting orchestrator..."
    ./gradlew :pc:run
else
    echo "Build failed. Check errors above."
    exit 1
fi