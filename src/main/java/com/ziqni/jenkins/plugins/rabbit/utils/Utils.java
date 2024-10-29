package com.ziqni.jenkins.plugins.rabbit.utils;

import org.apache.commons.lang3.StringUtils;

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
        return injectEnvVars(envVars, template, () -> "");
    }

    /**
     * Prepare response.
     *
     * @param envVars environment variables
     * @param template template
     * @param orElse Result is the template is null or empty
     * @return response
     */
    public static String injectEnvVars(Map<String, String> envVars, final String template, Supplier<String> orElse) {

        final var decorateTemplate = StringUtils.isBlank(template) ? orElse.get() : template;

        String out = new String(decorateTemplate);

        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            out = out.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return out.trim();
    }
}
