package com.ziqni.jenkins.plugins.rabbit.console;

import hudson.model.Job;
import hudson.model.Run;
import hudson.console.ConsoleLogFilter;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.OutputStream;

public class RabbitConsoleLogFilter extends ConsoleLogFilter {

    private final TaskListener listener;

    public RabbitConsoleLogFilter(TaskListener listener) {
        this.listener = listener;
    }

    @Override
    public OutputStream decorateLogger(Run run, OutputStream logger) throws IOException, InterruptedException {
        // Retrieve the job property from the run
        Job<?, ?> job = run.getParent();
        RabbitConsoleCollectorJobProperty property = job.getProperty(RabbitConsoleCollectorJobProperty.class);

        if (property != null && property.isEnableCollector()) {
            // Pass the routing key or any other settings from the property to your logger
            return new RabbitLineLogger(logger, property, run, listener);
        } else {
            // If property is not enabled or not present, return the original logger
            return logger;
        }
    }

    private void logLine(Run<?, ?> run, String line) {
        // Example: Log to console, or send to external system
        System.out.println("[" + run.getFullDisplayName() + "] " + line); // Log the line with run info
    }
}
