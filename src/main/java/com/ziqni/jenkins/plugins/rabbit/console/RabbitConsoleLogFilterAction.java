package com.ziqni.jenkins.plugins.rabbit.console;

import hudson.console.ConsoleLogFilter;
import hudson.model.InvisibleAction;

public class RabbitConsoleLogFilterAction extends InvisibleAction {
    private final ConsoleLogFilter logFilter;

    public RabbitConsoleLogFilterAction(ConsoleLogFilter logFilter) {
        this.logFilter = logFilter;
    }

    public ConsoleLogFilter getLogFilter() {
        return logFilter;
    }
}
