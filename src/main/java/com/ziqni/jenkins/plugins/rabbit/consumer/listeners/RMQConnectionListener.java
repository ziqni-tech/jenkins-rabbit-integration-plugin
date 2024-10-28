package com.ziqni.jenkins.plugins.rabbit.consumer.listeners;

import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitConnection;

/**
 * Listener interface for {@link RabbitConnection}.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public interface RMQConnectionListener {
    /**
     * Calls when closing process for connection is completed.
     *
     * @param rabbitConnection
     *            the closed connection.
     */
    void onCloseCompleted(RabbitConnection rabbitConnection);

    /**
     * Calls when connection is opend.
     *
     * @param rabbitConnection
     *            the connection.
     */
    void onOpen(RabbitConnection rabbitConnection);
}
