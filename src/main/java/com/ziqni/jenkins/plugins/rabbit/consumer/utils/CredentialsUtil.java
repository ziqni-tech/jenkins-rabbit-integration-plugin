package com.ziqni.jenkins.plugins.rabbit.consumer.utils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.model.Run;
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException;

public class CredentialsUtil {

    public static StandardUsernamePasswordCredentials getCredentials(String credentialsId, Run<?, ?> run) throws CredentialNotFoundException {
        StandardUsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(
                credentialsId,
                StandardUsernamePasswordCredentials.class,
                run
        );

        if (credentials == null) {
            throw new CredentialNotFoundException("Could not find credentials with ID: " + credentialsId);
        }

        return credentials;
    }
}

