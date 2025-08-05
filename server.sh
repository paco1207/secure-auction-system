#!/bin/bash

# Set the directory where the .java files are located
SERVER_DIR="./server"

# Set the output directory for .class files
OUTPUT_DIR="./bin"

# The main class that contains the server code (adjust the package path if necessary)
SERVER_CLASS="Server"

# Create the output directory if it doesn't exist
mkdir -p $OUTPUT_DIR

# Step 1: Find and compile all .java files in the server directory and subdirectories
echo "Compiling Java files..."
find $SERVER_DIR -name "*.java" -print | xargs javac -d $OUTPUT_DIR

# Check if the compilation was successful
if [ $? -eq 0 ]; then
  echo "Compilation successful. Class files are located in $OUTPUT_DIR"
else
  echo "Compilation failed. Please check for errors."
  exit 1
fi

# Step 2: Start the RMI registry in the background
echo "Starting rmiregistry..."
cd $OUTPUT_DIR
rmiregistry &
RMI_PID=$!
sleep 2  # Give rmiregistry time to start

# Step 3: Run the RMI server
echo "Running the RMI server..."
java $SERVER_CLASS