#!/bin/bash

# Function to generate a random string of length 20
generate_random_string() {
  cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 20 | head -n 1
}

# Create 1000 JSON files
for i in {1..1000}; do
  # Generate a random integer for "id"
  id=$((RANDOM % 10000 + 1))
  
  # Generate a random string for "value"
  value=$(generate_random_string)
  
  # Create a JSON file with random id and valu
  echo "{\"id\": $id, \"value\": \"$value\", \"general\": {  \"PATIENT_IDENTIFIER\": \"HA$id\"}}" >> "file_$i.json"
  zip "file_$i.json.zip" "file_$i.json"
  rm "file_$i.json"
done

echo "Created 1000 JSON files."

