#!/bin/bash
cd "$(dirname "$0")"

echo "======================================"
echo "Running Test Scenario 1: Most Items Available First"
echo "======================================"
./run.sh config_1.json

echo ""
echo "======================================"
echo "Running Test Scenario 2: Price Tie-Breaker"
echo "======================================"
./run.sh config_2.json

echo ""
echo "======================================"
echo "Running Test Scenario 3: Complex Multi-Market Selection"
echo "======================================"
./run.sh config_3.json 