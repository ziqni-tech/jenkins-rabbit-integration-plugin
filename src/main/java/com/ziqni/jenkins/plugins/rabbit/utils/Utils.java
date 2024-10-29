package com.ziqni.jenkins.plugins.rabbit.utils;

import hudson.model.Run;

import java.util.Map;
import java.util.function.Supplier;

public abstract class Utils {

    /**
     * Prepare response.
     * @param run run
     * @param envVars environment variables
     * @param template template
     * @return response
     */
    public static String injectEnvVars(Map<String, String> envVars, String template) {
        return injectEnvVars(envVars, template, () -> template);
    }

    /**
     * Prepare response.
     * @param run run
     * @param envVars environment variables
     * @param template template
     * @param orElse Result is the template is null or empty
     * @return response
     */
    public static String injectEnvVars(Map<String, String> envVars, final String template, Supplier<String> orElse) {

        if(template == null || template.trim().isEmpty() || !template.contains("$")) {
            return orElse.get();
        }
        else {

            var customTemplate = template;

            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                customTemplate = customTemplate.replace("${" + entry.getKey() + "}", entry.getValue());
            }

            return customTemplate.trim();
        }
    }
}
