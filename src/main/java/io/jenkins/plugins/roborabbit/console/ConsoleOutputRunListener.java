package io.jenkins.plugins.roborabbit.console;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ConsoleOutputRunListener extends RunListener<Run<?,?>> {

    private static final Logger LOGGER = Logger.getLogger(ConsoleOutputRunListener.class.getName());
    private ConsoleCollectorJobProperty property;

    @Override
    public void onStarted(Run run, TaskListener listener) {
        this.property = getJobProperty(run);

        if (property != null && property.isEnabled()) {
            listener.getLogger().println("[ConsoleOutputRunListener] Build started: " + run.getFullDisplayName());
            listener.getLogger().println("Broker Name: " + property.getBrokerName());
            listener.getLogger().println("Routing Key: " + property.getRoutingKey());
        }
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {
        listener.getLogger().println("[ConsoleOutputRunListener] Build completed: " + run.getFullDisplayName());

        // Access console output
        try {
            String consoleOutput = run.getLog();
            LOGGER.log(Level.INFO, "Console output of build {0}:\n{1}", new Object[]{run.getFullDisplayName(), consoleOutput});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading console output for build " + run.getFullDisplayName(), e);
        }
    }

    @Override
    public void onFinalized(Run run) {
        LOGGER.log(Level.INFO, "[ConsoleOutputRunListener] Build finalized: {0}", run.getFullDisplayName());
    }

    private ConsoleCollectorJobProperty getJobProperty(Run<?, ?> run) {
        if (run.getParent() instanceof Job) {
            Job<?, ?> job = (Job<?, ?>) run.getParent();
            return job.getProperty(ConsoleCollectorJobProperty.class);
        }
        return null;
    }
}
