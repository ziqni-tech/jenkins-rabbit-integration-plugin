package com.ziqni.jenkins.plugins.rabbit.trigger;

import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageProperties;
import hudson.EnvVars;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import hudson.model.EnvironmentContributingAction;

public class EnvironmentVariablesAction extends InvisibleAction implements EnvironmentContributingAction {

    private final RabbitMessageProperties props;

    public EnvironmentVariablesAction(RabbitMessageProperties props) {
        this.props = props;
    }

    @Override
    public void buildEnvironment(Run<?, ?> run, EnvVars env) {
        if (props != null) {
            props.addAll(env); // Inject the environment variables
        }
    }
}

