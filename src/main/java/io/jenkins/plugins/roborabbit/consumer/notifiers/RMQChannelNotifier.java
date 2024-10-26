package io.jenkins.plugins.roborabbit.consumer.notifiers;


import io.jenkins.plugins.roborabbit.consumer.events.RMQChannelEvent;
import io.jenkins.plugins.roborabbit.consumer.listeners.RMQChannelListener;

/**
 * Notifier interface for RabbitMQ Channel.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public interface RMQChannelNotifier {

    /**
     * Add {@link RMQChannelListener}.
     *
     * @param rmqChannelListener
     *            the listener.
     */
    void addRMQChannelListener(RMQChannelListener rmqChannelListener);

    /**
     * Notify {@link RMQChannelListener}s.
     *
     * @param event
     *            the event.
     */
    void notifyRMQChannelListeners(RMQChannelEvent event);

    /**
     * Removes {@link RMQChannelListener}.
     *
     * @param rmqChannelListener
     *            the listener.
     */
    void removeRMQChannelListener(RMQChannelListener rmqChannelListener);

    /**
     * Gets whether actual RabbitMQ channel is opened.
     *
     * @return true if channel is already opened.
     */
    boolean isOpenRMQChannel();
}
