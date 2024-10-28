package com.ziqni.jenkins.plugins.rabbit.consumer.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorsTest {

    @Test
    void isValidURL() {
    }

    @Test
    void isValidAMQPUrl() {

        final var result = Validators.isValidAMQPUrl("amqps://b-8fd73ad2-d8d2-479e-a121-f02d3cc74f27.mq.eu-west-1.amazonaws.com:5671");
        assertTrue(result);
    }
}
