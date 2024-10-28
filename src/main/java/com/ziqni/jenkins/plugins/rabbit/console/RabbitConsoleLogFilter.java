package com.ziqni.jenkins.plugins.rabbit.console;

import hudson.model.Job;
import hudson.model.Run;
import hudson.console.ConsoleLogFilter;

import java.io.IOException;
import java.io.OutputStream;

public class RabbitConsoleLogFilter extends ConsoleLogFilter {

    @Override
    public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
        // Retrieve the job property from the run
        Job<?, ?> job = build.getParent();
        RabbitConsoleCollectorJobProperty property = job.getProperty(RabbitConsoleCollectorJobProperty.class);

        if (property != null && property.isEnableCollector()) {
            // Pass the routing key or any other settings from the property to your logger
            return new RabbitLineLogger(logger, property, build);
        } else {
            // If property is not enabled or not present, return the original logger
            return logger;
        }
    }

    private void logLine(Run<?, ?> build, String line) {
        // Example: Log to console, or send to external system
        System.out.println("[" + build.getFullDisplayName() + "] " + line); // Log the line with build info
    }
}
