package com.ziqni.jenkins.plugins.rabbit.console;
import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RabbitCustomConsoleLogFilter extends ConsoleLogFilter {

    @Override
    public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
        return new LineTransformationOutputStream() {
            @Override
            protected void eol(byte[] b, int len) throws IOException {
                String line = new String(b, 0, len, StandardCharsets.UTF_8);
                // Process each line of log here as it is written
                processLogLine(build, line);
                // Pass the log line to the original logger
                logger.write(b, 0, len);
            }
        };
    }

    private void processLogLine(Run<?, ?> build, String line) {
        // Here you can handle each line, for example, publish it or store it
        System.out.println("Log Line: " + line);  // Example: Just printing to the console
    }
}
