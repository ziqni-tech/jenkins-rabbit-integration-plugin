package com.ziqni.jenkins.plugins.rabbit.utils;

import hudson.model.Run;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    private static final SimpleDateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Prepare response.
     * @param envVars environment variables
     * @param template template
     * @return response
     */
    public static String injectEnvVars(Run<?,?> run, Map<String, String> envVars, String template) {
        try {
            return injectEnvVars(run, envVars, template, () -> template);
        }
        catch (Exception e) {
            LOGGER.warning("Error while injecting environment variables into the template: " + e.getMessage());
            return template;
        }
    }

    /**
     * Prepare response.
     *
     * @param envVars environment variables
     * @param template template
     * @param orElse Result is the template is null or empty
     * @return response
     */
    public static String injectEnvVars(Run<?,?> run, Map<String, String> envVars, final String template, Supplier<String> orElse) {

        injectBuildStartDate(run, envVars);

        final var decorateTemplate = StringUtils.isBlank(template) ? orElse.get() : template;

        if(StringUtils.isBlank(decorateTemplate)) {
            return decorateTemplate;
        }

        String out = new String(decorateTemplate);

        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            out = out.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return out.trim();
    }

    private static void injectBuildStartDate(Run<?,?> run, Map<String, String> envVars){
        if (envVars.containsKey("BUILD_START_DATE")) {
            return;
        }

        // Get the start time in milliseconds and convert it to an Instant
        long startTimeInMillis = run.getStartTimeInMillis();
        Instant startInstant = Instant.ofEpochMilli(startTimeInMillis);

        // Format the instant as an ISO 8601 string in UTC
        String startDate = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(startInstant);

        envVars.put("BUILD_START_DATE", startDate);
    }
}
