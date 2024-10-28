package com.ziqni.jenkins.plugins.rabbit.console;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.*;
import hudson.console.ConsoleLogFilter;

import java.io.IOException;
import java.io.OutputStream;

public class RabbitConsoleLogFilter extends ConsoleLogFilter {

    private final TaskListener listener;
    private final RabbitConsoleCollectorJobProperty property;

    public RabbitConsoleLogFilter(TaskListener listener, RabbitConsoleCollectorJobProperty property) {
        this.listener = listener;
        this.property = property;
    }

    @Override
    public OutputStream decorateLogger(Run run, OutputStream logger) throws IOException, InterruptedException {
        return new RabbitLineLogger(logger, property, run, listener);
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException {
        return super.decorateLogger(build, logger);
    }

    @Override
    public OutputStream decorateLogger(@NonNull Computer computer, OutputStream logger) throws IOException, InterruptedException {
        return super.decorateLogger(computer, logger);
    }

    private void logLine(Run<?, ?> run, String line) {
        // Example: Log to console, or send to external system
        System.out.println("[" + run.getFullDisplayName() + "] " + line); // Log the line with run info
    }
}
