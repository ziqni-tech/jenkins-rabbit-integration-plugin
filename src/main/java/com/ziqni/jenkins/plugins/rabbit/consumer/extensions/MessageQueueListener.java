package com.ziqni.jenkins.plugins.rabbit.consumer.extensions;

import com.ziqni.jenkins.plugins.rabbit.utils.RabbitMessageProperties;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Listener for message queue.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public abstract class MessageQueueListener implements ExtensionPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueListener.class);

    /**
     * Gets name.
     *
     * @return the name.
     */
    public abstract String getName();

    /**
     * Gets application id.
     *
     * @return the application id.
     */
    public abstract String getAppId();

    /**
     * Calls when binds to queue.
     *
     * @param queueName
     *            the queue name.
     */
    public abstract void onBind(String queueName);

    /**
     * Calls when unbinds from queue.
     *
     * @param queueName
     *            the queue name.
     */
    public abstract void onUnbind(String queueName);

    /**
     * Calls when message arrives.
     *
     * @param rabbitMessageProperties the message properties.
     * @param body
     *            the content body.
     */
    public abstract void onReceive(RabbitMessageProperties rabbitMessageProperties, byte[] body);

    /**
     * Fires OnReceive event.
     *
     * @param rabbitMessageProperties the message properties.
     * @param body
     *            the message body.
     */
    public static void fireOnReceive(RabbitMessageProperties rabbitMessageProperties, byte[] body) {
        LOGGER.trace("MessageQueueListener", "fireOnReceive");
        SecurityContext old = ACL.impersonate(ACL.SYSTEM);
        try {
            for (MessageQueueListener l : all()) {
                if (rabbitMessageProperties.getAppId().equals(l.getAppId())) {
                    try {
                        l.onReceive(rabbitMessageProperties, body);
                    } catch (Exception ex) {
                        LOGGER.warn("Caught exception during calling onReceive()", ex);
                    }
                }
            }
        }
        finally {
            SecurityContextHolder.setContext(old);
        }
    }

    /**
     * Fires OnBind event.
     *
     * @param appIds
     *            the hashset of application ids.
     * @param queueName
     *            the queue name.
     */
    public static void fireOnBind(Collection<String> appIds, String queueName) {
        LOGGER.trace("MessageQueueListener", "fireOnBind");
        SecurityContext old = ACL.impersonate(ACL.SYSTEM);
        try {
            for (MessageQueueListener l : all()) {
                if (appIds.contains(l.getAppId())) {
                    try {
                        l.onBind(queueName);
                    } catch (Exception ex) {
                        LOGGER.warn("Caught exception during calling onBind()", ex);
                    }
                }
            }
        }
        finally {
            SecurityContextHolder.setContext(old);
        }
    }

    /**
     * Fires OnUnbind event.
     *
     * @param appIds
     *            the hashset of application ids.
     * @param queueName
     *            the queue name.
     */
    public static void fireOnUnbind(Collection<String> appIds, String queueName) {
        LOGGER.trace("MessageQueueListener", "fireOnUnbind");
        SecurityContext old = ACL.impersonate(ACL.SYSTEM);
        try {
            for (MessageQueueListener l : all()) {
                if (appIds.contains(l.getAppId())) {
                    try {
                        l.onUnbind(queueName);
                    } catch (Exception ex) {
                        LOGGER.warn("Caught exception during calling onUnbind()", ex);
                    }
                }
            }
        }
        finally {
            SecurityContextHolder.setContext(old);
        }
    }

    /**
     * Gets all listeners.
     *
     * @return the extension list.
     */
    public static ExtensionList<MessageQueueListener> all() {
        return Jenkins.getInstance().getExtensionList(MessageQueueListener.class);
    }
}
