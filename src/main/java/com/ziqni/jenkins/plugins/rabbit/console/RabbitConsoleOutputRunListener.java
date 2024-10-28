package com.ziqni.jenkins.plugins.rabbit.console;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

@Extension
public class RabbitConsoleOutputRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        super.onStarted(run, listener);

        try {
            // Attach your custom console log filter to the build
            run.addAction(new RabbitConsoleLogFilterAction(new RabbitConsoleLogFilter(listener)));
            listener.getLogger().println("Custom Console Log Filter has been attached.");
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }
    }
}
