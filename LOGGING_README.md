# Logging System for Grocery Delivery Application

## Overview

This document describes the implementation of a comprehensive logging system for the Grocery Delivery application. The logging system uses Log4j2, an industry-standard logging framework for Java applications, to provide structured and configurable logging for all agents and behaviors.

## Features

- **Separate Log Files**: Each agent and behavior has its own dedicated log file
- **Configurable Logging Levels**: Debug, Info, Warn, Error levels
- **Structured Log Format**: Timestamps, log levels, and class information
- **File Rotation**: Log files are rotated based on size (10MB) with compression
- **Console Output**: Logs are also displayed in the console for development

## Log File Structure

Log files are stored in the `logs` directory with the following naming convention:

- Agent logs: `agent_[agentname].log`
- Behavior logs: `behaviour_[behaviorname].log`

For example:
- `logs/agent_market1.log`
- `logs/behaviour_deliverycontractnet_boltfood.log`

## Log Format

Log entries follow this format:
```
2023-04-25 14:30:45.123 [JADE-Thread-1] INFO - Message content
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

1. **Easier Debugging**: Each agent/behavior has its own log file, making it easier to trace issues
2. **No Log Conflicts**: Multi-agent interactions don't create interleaved log entries
3. **Persistent Records**: Logs are stored for future analysis
4. **Configurable Verbosity**: Log levels can be adjusted without code changes

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