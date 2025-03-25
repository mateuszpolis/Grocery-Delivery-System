package com.example.grocerydelivery.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating and managing loggers for agents and behaviors.
 */
public class LoggerUtil {
    private static final Map<String, Logger> loggers = new HashMap<>();
    private static final String LOG_DIR = "logs";
    
    /**
     * Creates a logger for an agent or behavior with a dedicated log file.
     * 
     * @param name The name of the agent or behavior
     * @param type The type (agent or behavior)
     * @return A configured Logger
     */
    public static synchronized Logger getLogger(String name, String type) {
        String loggerKey = type + "." + name;
        if (loggers.containsKey(loggerKey)) {
            return loggers.get(loggerKey);
        }
        
        // Create a new logger
        Logger logger = createLogger(name, type);
        loggers.put(loggerKey, logger);
        return logger;
    }
    
    private static Logger createLogger(String name, String type) {
        // Ensure log directory exists
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        // Get Logger Context
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        
        // Create file appender
        String logFileName = String.format("%s/%s_%s.log", LOG_DIR, type.toLowerCase(), name.toLowerCase());
        String appenderName = type + "_" + name + "_Appender";
        
        // Use builder to create appender
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level - %msg%n")
                .build();
        
        SizeBasedTriggeringPolicy triggeringPolicy = SizeBasedTriggeringPolicy.createPolicy("10MB");
        
        DefaultRolloverStrategy rolloverStrategy = DefaultRolloverStrategy.newBuilder()
                .withMax("5")
                .withConfig(config)
                .build();
        
        RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName(appenderName)
                .withFileName(logFileName)
                .withFilePattern(String.format("%s/%s_%s-%%d{yyyy-MM-dd}-%%i.log.gz", 
                        LOG_DIR, type.toLowerCase(), name.toLowerCase()))
                .withLayout(layout)
                .withPolicy(triggeringPolicy)
                .withStrategy(rolloverStrategy)
                .build();
        
        appender.start();
        config.addAppender(appender);
        
        // Create logger config
        String loggerName = "com.example.grocerydelivery." + type.toLowerCase() + "." + name.toLowerCase();
        
        AppenderRef ref = AppenderRef.createAppenderRef(appenderName, Level.DEBUG, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.DEBUG, loggerName, 
                "true", refs, null, config, null);
        
        loggerConfig.addAppender(appender, Level.DEBUG, null);
        config.addLogger(loggerName, loggerConfig);
        
        // Update context
        context.updateLoggers();
        
        return LogManager.getLogger(loggerName);
    }
} 