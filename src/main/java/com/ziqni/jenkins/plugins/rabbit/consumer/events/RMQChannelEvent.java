package com.ziqni.jenkins.plugins.rabbit.consumer.events;

/**
 * Events for {@link org.jenkinsci.plugins.roborabbit.listeners.RMQChannelListener}.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public enum RMQChannelEvent {
    /**
     * OnOpen event.
     */
    OPEN,
    /**
     * OnCloseCompleted event.
     */
    CLOSE_COMPLETED;
}
