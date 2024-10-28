package com.ziqni.jenkins.plugins.rabbit.consumer.notifiers;


import com.ziqni.jenkins.plugins.rabbit.consumer.events.RMQConnectionEvent;
import com.ziqni.jenkins.plugins.rabbit.consumer.listeners.RMQConnectionListener;

/**
 * Notifier interface for {@link RMQConnectionListener}.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public interface RMQConnectionNotifier {
    /**
     * Add {@link RMQConnectionListener}.
     *
     * @param rmqShutdownListener
     *            the listener.
     */
    void addRMQConnectionListener(RMQConnectionListener rmqShutdownListener);

    /**
     * Notify event for {@link org.jenkins.plugins.rabborabbit.RMQConnection}.
     *
     * @param event
     *            the event.
     */
    void notifyRMQConnectionListeners(RMQConnectionEvent event);

    /**
     * Removes {@link RMQConnectionListener}.
     *
     * @param rmqShutdownListener
     *            the listener.
     */
    void removeRMQConnectionListener(RMQConnectionListener rmqShutdownListener);

    /**
     * Gets whether actual RabbitMQ connection is opened.
     *
     * @return true if connection is already opened.
     */
    boolean isOpenRMQConnection();

}
