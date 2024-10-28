package com.ziqni.jenkins.plugins.rabbit.consumer.extensions;

import com.rabbitmq.client.Channel;
import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitConnection;
import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.apache.tools.ant.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * RabbitMQ server operation class.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public abstract class ServerOperator extends ExtensionPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOperator.class);

    /**
     * Calls when channel is opened.
     * You must not hold given values from control channel.
     *
     * @param controlChannel
     *            the control channel.
     * @param serviceUri
     *            the URL to endpoint of service.
     * @throws IOException if ControlRMQChannel has somthing wrong.
     */
    public abstract void onOpen(Channel controlChannel, String serviceUri) throws IOException;

    /**
     * Calls when channel is closed.
     * You must not hold given values from control channel.
     *
     * @param seviceUri
     *            the service URI.
     */
    public abstract void onCloseCompleted(String seviceUri);

    /**
     * Fires OnOpen event.
     *
     * @param rabbitConnection
     *            the RabbitMQ connection.
     */
    public static void fireOnOpen(RabbitConnection rabbitConnection) {
        LOGGER.trace("ServerOperator", "fireOnOpen");
        if (rabbitConnection.getConnection() != null) {
            for (ServerOperator l : all()) {
                try {
                    Channel ch = rabbitConnection.getConnection().createChannel();
                    l.onOpen(ch, rabbitConnection.getServiceUri());
                    ch.close();
                } catch (Exception ex) {
                    LOGGER.warn("Caught exception from {}#OnOpen().", l.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Fires OnCloseCompleted event.
     *
     * @param rabbitConnection
     *            the RabbitMQ connection.
     */
    public static void fireOnCloseCompleted(RabbitConnection rabbitConnection) {
        LOGGER.trace("ServerOperator", "fireOnCloseCompleted");
        for (ServerOperator l : all()) {
            try {
                l.onCloseCompleted(rabbitConnection.getServiceUri());
            } catch (Exception ex) {
                LOGGER.warn("Caught exception during OnCloseCompleted()", ex);
            }
        }
    }

    /**
     * Gets all listeners.
     *
     * @return the extension list.
     */
    public static ExtensionList<ServerOperator> all() {
        return Jenkins.getInstance().getExtensionList(ServerOperator.class);
    }
}
