package com.ziqni.jenkins.plugins.rabbit.consumer;

import com.ziqni.jenkins.plugins.rabbit.configuration.RabbitConfiguration;
import hudson.util.Secret;
import com.ziqni.jenkins.plugins.rabbit.consumer.channels.PublishRMQChannel;
import com.ziqni.jenkins.plugins.rabbit.consumer.extensions.ServerOperator;
import com.ziqni.jenkins.plugins.rabbit.consumer.listeners.RMQConnectionListener;
import com.ziqni.jenkins.plugins.rabbit.consumer.watchdog.ConnectionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.rabbitmq.client.Channel;

/**
 * Manager class for RabbitMQ connection.
 *
 * @author rinrinne a.k.a. rin_ne
 */
public final class RabbitManager implements RMQConnectionListener {

    /**
     * Intance holder class for {@link RabbitManager}.
     *
     * @author rinrinne a.k.a. rin_ne
     */
    private static class InstanceHolder {
        private static final RabbitManager INSTANCE = new RabbitManager();
    }

    private static final long TIMEOUT_CLOSE = 300000;
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitManager.class);

    private RabbitConnection rabbitConnection;
    private volatile boolean statusOpen = false;
    private CountDownLatch closeLatch = null;

    /**
     * Gets instance.
     *
     * @return the instance.
     */
    public static RabbitManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Updates RabbitMQ connection.
     */
    public void update() {
        LOGGER.info("Start to update connections...");
        RabbitConfiguration conf = RabbitConfiguration.get();

        if(Objects.isNull(conf.getCredentials())) {
            LOGGER.warn("Credentials is not set, exiting.");
            return;
        }

        String uri = conf.getServiceUri();
        String user = conf.getCredentials().getUsername();
        Secret pass = conf.getCredentials().getPassword();
        long watchdog = conf.getWatchdogPeriod();

        boolean enableConsumer = conf.isEnableConsumer();

        try {
            if (!enableConsumer || uri == null) {
                if (rabbitConnection != null) {
                    shutdownWithWait();
                    rabbitConnection = null;
                }
            }
            if (rabbitConnection != null &&
                    !uri.equals(rabbitConnection.getServiceUri()) &&
                    !user.equals(rabbitConnection.getUserName()) &&
                    !pass.equals(rabbitConnection.getUserPassword())) {
                shutdownWithWait();
                rabbitConnection = null;
            }

            if (enableConsumer) {
                if (rabbitConnection == null) {
                    rabbitConnection = new RabbitConnection(uri, user, pass, watchdog);
                    rabbitConnection.addRMQConnectionListener(this);
                    try {
                        rabbitConnection.open();
                    } catch (IOException e) {
                        if (e.getCause() instanceof ConnectException) {
                            LOGGER.warn("Cannot open connection: {}", e.getCause().getMessage());
                        } else {
                            LOGGER.warn("Cannot open connection!", e);
                        }
                        rabbitConnection.removeRMQConnectionListener(this);
                        rabbitConnection = null;
                    }
                } else {
                    rabbitConnection.updateChannels(RabbitConfiguration.get().getConsumeItems());
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted when waiting to close connection.");
        }
    }

    /**
     * Shutdown connection.
     */
    public void shutdown() {
        if (rabbitConnection != null && rabbitConnection.isOpen()) {
            try {
                statusOpen = false;
                rabbitConnection.close();
            } catch(Exception ex) {
                onCloseCompleted(rabbitConnection);
            }
        }
    }

    /**
     * Shutdown connection then wait to close connection.
     *
     * @throws InterruptedException
     *             throw if wait process is interrupted.
     */
    public synchronized void shutdownWithWait() throws InterruptedException {
        if (rabbitConnection != null && rabbitConnection.isOpen()) {
            try {
                setCloseLatch(new CountDownLatch(1));
                shutdown();
                if (!getCloseLatch().await(TIMEOUT_CLOSE, TimeUnit.MILLISECONDS)) {
                    onCloseCompleted(rabbitConnection);
                    throw new InterruptedException("Wait timeout");
                }
            } finally {
                setCloseLatch(null);
            }
        }
    }

    private synchronized void setCloseLatch(CountDownLatch latch) {
        this.closeLatch = latch;
    }

    private synchronized CountDownLatch getCloseLatch() {
        return this.closeLatch;
    }

    /**
     * Gets whether connection is established or not.
     *
     * @return true if connection is already established.
     */
    public boolean isOpen() {
        return statusOpen;
    }

    /**
     * Gets status of channel for specified queue.
     *
     * @param queueName
     *            the queue name.
     * @return true if channel for specified queue is already established.
     */
    public boolean getChannelStatus(String queueName) {
        if (statusOpen) {
            if (rabbitConnection != null && rabbitConnection.isOpen()) {
                return rabbitConnection.getConsumeChannelStatus(queueName);
            }
        }
        return false;
    }

    /**
     * Gets channel.
     * Note that returned channel is not managed in any own classes.
     *
     * @return the channel.
     */
    public Channel getChannel() {
        Channel ch = null;
        if (statusOpen) {
            if (rabbitConnection != null) {
                ch = rabbitConnection.createPureChannel();
            }
        }
        return ch;
    }

    /**
     * Gets instance of {@link PublishRMQChannel}.
     *
     * @return instance.
     */
    public PublishRMQChannel getPublishChannel() {
        if (statusOpen) {
            if (rabbitConnection != null) {
                Collection<PublishRMQChannel> channels = rabbitConnection.getPublishRMQChannels();
                if (!channels.isEmpty()) {
                    return (PublishRMQChannel)(channels.toArray()[0]);
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param rabbitConnection
     *            the connection.
     */
    public void onOpen(RabbitConnection rabbitConnection) {
        if (this.rabbitConnection.equals(rabbitConnection)) {
            LOGGER.info("Open RabbitMQ connection: {}", rabbitConnection.getServiceUri());
            ConnectionMonitor.get().setActivate(false);
            ConnectionMonitor.get().setLastMeanTime(System.currentTimeMillis());
            ServerOperator.fireOnOpen(rabbitConnection);
            rabbitConnection.updateChannels(RabbitConfiguration.get().getConsumeItems());
            statusOpen = true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param rabbitConnection
     *            the connection.
     */
    public void onCloseCompleted(RabbitConnection rabbitConnection) {
        synchronized (this) {
            if (this.rabbitConnection != null && this.rabbitConnection.equals(rabbitConnection)) {
                this.rabbitConnection = null;
                LOGGER.info("Closed RabbitMQ connection: {}", rabbitConnection.getServiceUri());
                rabbitConnection.removeRMQConnectionListener(this);
                ServerOperator.fireOnCloseCompleted(rabbitConnection);
                statusOpen = false;
                if (closeLatch != null) {
                    closeLatch.countDown();
                }
            }
        }
    }

    /**
     * Creates instance.
     */
    private RabbitManager() {
    }
}
