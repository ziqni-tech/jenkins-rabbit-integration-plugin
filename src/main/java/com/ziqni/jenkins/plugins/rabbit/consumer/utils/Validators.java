package com.ziqni.jenkins.plugins.rabbit.consumer.utils;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public abstract class Validators {

    public static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static final List<String> AMQP_SCHEMES = Arrays.asList("amqp", "amqps");

    public static boolean isValidAMQPUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            String scheme = uri.getScheme();
            return AMQP_SCHEMES.contains(scheme);
        } catch (Exception e) {
            return false;
        }
    }
}
