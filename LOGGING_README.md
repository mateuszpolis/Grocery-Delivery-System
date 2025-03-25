# Logging System for Grocery Delivery Application

## Overview

This document describes the implementation of a comprehensive logging system for the Grocery Delivery application. The logging system uses Log4j2, an industry-standard logging framework for Java applications, to provide structured and configurable logging for all agents and behaviors.

## Features

- **Organized Directory Structure**: Separate folders for agent and behavior logs
- **Configurable Logging Levels**: Debug, Info, Warn, Error levels
- **Structured Log Format**: Timestamps, agent/behavior names, log levels, and messages
- **File Rotation**: Log files are rotated based on size (10MB) with compression
- **Console Output**: Logs are also displayed in the console for development

## Log File Structure

Log files are stored in the following directory structure:

```
logs/
├── agents/           # Contains all agent logs
│   ├── market1.log
│   ├── market2.log
│   ├── delivery1.log
│   └── client1.log
└── behaviours/       # Contains all behavior logs
    ├── marketcontractnet_market1.log
    ├── deliverycontractnet_boltfood.log
    └── clientorder_alice.log
```

## Log Format

Log entries follow this format:
```
2023-04-25 14:30:45.123 [AgentName] INFO - Message content
```

## Implementation

The logging system is implemented using the following components:

1. **Log4j2 Configuration**: `src/main/resources/log4j2.xml` defines the base configuration.

2. **LoggerUtil Class**: `src/main/java/com/example/grocerydelivery/utils/LoggerUtil.java` provides a centralized way to create and manage loggers.

3. **Logger Integration in Agents and Behaviors**: Each agent and behavior class has been updated to use the logging system.

## Usage

### In Agent Classes

```java
// Initialize in setup() method
logger = LoggerUtil.getLogger(agentName, "Agent");

// Use throughout the agent class
logger.info("Agent starting: {}", agentName);
logger.debug("Processing request: {}", requestId);
logger.error("Failed to process request", exception);
```

### In Behavior Classes

```java
// Initialize in constructor
this.logger = LoggerUtil.getLogger(behaviorName, "Behaviour");

// Use throughout the behavior class
logger.info("Behavior initialized for {}", agentName);
logger.debug("Processing message from {}", sender);
```

## Benefits

1. **Organized Log Files**: Clear separation between agent and behavior logs
2. **Easier Debugging**: Each agent/behavior has its own log file, making it easier to trace issues
3. **No Log Conflicts**: Multi-agent interactions don't create interleaved log entries
4. **Persistent Records**: Logs are stored for future analysis
5. **Configurable Verbosity**: Log levels can be adjusted without code changes

## Dependencies

The logging system uses the following dependencies (added to pom.xml):

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.20.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.20.0</version>
</dependency>
``` 