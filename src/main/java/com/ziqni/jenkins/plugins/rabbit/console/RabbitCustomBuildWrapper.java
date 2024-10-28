package com.ziqni.jenkins.plugins.rabbit.console;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildWrapper;

import java.io.IOException;

public class RabbitCustomBuildWrapper extends SimpleBuildWrapper {

    @Override
    public void setUp(Context context, Run<?, ?> build, TaskListener listener, Launcher launcher) throws IOException, InterruptedException {
        // Log a message indicating that the setup phase has started
        listener.getLogger().println("Custom setup is starting...");

        // Perform setup actions here, such as preparing environment variables or directories

        // You can also add environment variables here like this:
        context.env("MY_ENV_VARIABLE", "value");
    }

    @Override
    public ConsoleLogFilter createLoggerDecorator(Run<?, ?> run) {
        return new RabbitCustomConsoleLogFilter();
    }

    @Extension
    public static final class DescriptorImpl extends SimpleBuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> project) {
            // Define the types of jobs this BuildWrapper is applicable to, e.g., all projects
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Custom Console Log Filter";
        }
    }
}
