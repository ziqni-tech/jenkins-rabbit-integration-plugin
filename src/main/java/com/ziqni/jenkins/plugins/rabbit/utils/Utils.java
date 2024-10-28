package com.ziqni.jenkins.plugins.rabbit.utils;

import hudson.model.AbstractBuild;

import java.util.Map;
import java.util.function.Supplier;

public abstract class Utils {

    /**
     * Prepare response.
     * @param build build
     * @param envVars environment variables
     * @param template template
     * @param orElse Result is the template is null or empty
     * @return response
     */
    public static String prepareResponse(AbstractBuild<?, ?> build, Map<String, String> envVars, String template, Supplier<String> orElse) {
        if(template == null || template.trim().isEmpty()) {
            return orElse.get();
        }
        else {

            var customTemplate = template;

            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                customTemplate = template.replace("${" + entry.getKey() + "}", entry.getValue());
            }

            return customTemplate;
        }
    }
}
