#!/bin/bash

# Create necessary directories
mkdir -p backups logs

# Build and run the containers
docker-compose up --build -d

# Wait for the application to start
echo "Waiting for the application to start..."
sleep 10

# Initialize the database with sample data
echo "Initializing the database with sample data..."
./init-db.sh

# Check if the application is running
if curl -s http://localhost:8080/api/actuator/health | grep -q "UP"; then
  echo "Application is running successfully!"
  echo "You can access the API at http://localhost:8080/api"
else
  echo "Application failed to start. Check the logs with: docker-compose logs app"
fi 