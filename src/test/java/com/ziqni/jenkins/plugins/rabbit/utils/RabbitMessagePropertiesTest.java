package com.ziqni.jenkins.plugins.rabbit.utils;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RabbitMessagePropertiesTest {

    private RabbitMessageProperties messageProperties;
    private AMQP.BasicProperties basicProperties;
    private Envelope envelope;
    private String queueName = "testQueue";

    @Before
    public void setUp() {
        // Set up a test envelope
        envelope = new Envelope(1L, true, "testExchange", "testRoutingKey");

        // Set up test properties with headers
        Map<String, Object> headers = new HashMap<>();
        headers.put("testHeader", "testHeaderValue");

        basicProperties = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .contentEncoding("UTF-8")
                .deliveryMode(2)
                .priority(1)
                .correlationId("12345")
                .replyTo("testReplyTo")
                .expiration("60000")
                .messageId("msg123")
                .timestamp(new Date())
                .type("testType")
                .userId("testUser")
                .appId("testApp")
                .clusterId("testCluster")
                .headers(headers)
                .build();

        // Create the RabbitMessageProperties instance
        messageProperties = new RabbitMessageProperties(queueName, envelope, basicProperties);
    }

    @Test
    public void testPopulateVars() {
        // Verify that all basic properties are correctly populated
        Map<String, String> vars = messageProperties.getVars();

        assertEquals("text/plain", vars.get(RabbitMessageProperties.RABBIT_CONTENT_TYPE));
        assertEquals("UTF-8", vars.get(RabbitMessageProperties.RABBIT_CONTENT_ENCODING));
        assertEquals("2", vars.get(RabbitMessageProperties.RABBIT_DELIVERY_MODE));
        assertEquals("1", vars.get(RabbitMessageProperties.RABBIT_PRIORITY));
        assertEquals("12345", vars.get(RabbitMessageProperties.RABBIT_CORRELATION_ID));
        assertEquals("testReplyTo", vars.get(RabbitMessageProperties.RABBIT_REPLY_TO));
        assertEquals("60000", vars.get(RabbitMessageProperties.RABBIT_EXPIRATION));
        assertEquals("msg123", vars.get(RabbitMessageProperties.RABBIT_MESSAGE_ID));
        assertNotNull(vars.get(RabbitMessageProperties.RABBIT_TIMESTAMP)); // Timestamp is converted to string
        assertEquals("testType", vars.get(RabbitMessageProperties.RABBIT_TYPE));
        assertEquals("testUser", vars.get(RabbitMessageProperties.RABBIT_USER_ID));
        assertEquals("testApp", vars.get(RabbitMessageProperties.RABBIT_APP_ID));
        assertEquals("testCluster", vars.get(RabbitMessageProperties.RABBIT_CLUSTER_ID));

        // Verify envelope properties
        assertEquals(queueName, vars.get(RabbitMessageProperties.RABBIT_QUEUE_NAME));
        assertEquals("testExchange", vars.get(RabbitMessageProperties.RABBIT_EXCHANGE));
        assertEquals("testRoutingKey", vars.get(RabbitMessageProperties.RABBIT_ROUTING_KEY));
        assertEquals("1", vars.get(RabbitMessageProperties.RABBIT_DELIVERY_TAG));
        assertEquals("true", vars.get(RabbitMessageProperties.RABBIT_REDELIVERED));

        // Verify headers
        assertEquals("testHeaderValue", vars.get(RabbitMessageProperties.RABBIT_HEADER_ + "TESTHEADER"));
    }

    @Test
    public void testGetValue() {
        // Test getting specific values using keys
        assertEquals("text/plain", messageProperties.getValue(RabbitMessageProperties.RABBIT_CONTENT_TYPE));
        assertEquals("testExchange", messageProperties.getValue(RabbitMessageProperties.RABBIT_EXCHANGE));
        assertEquals("testHeaderValue", messageProperties.getValue(RabbitMessageProperties.RABBIT_HEADER_ + "TESTHEADER"));
        assertNull(messageProperties.getValue("NON_EXISTENT_KEY"));
    }
}
