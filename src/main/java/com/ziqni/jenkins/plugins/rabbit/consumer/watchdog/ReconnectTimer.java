package com.ziqni.jenkins.plugins.rabbit.consumer.watchdog;

import com.ziqni.jenkins.plugins.rabbit.configuration.RabbitConfiguration;
import hudson.Extension;
import hudson.model.AperiodicWork;
import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitManager;

/**
 * Reconnect timer class.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class ReconnectTimer extends AperiodicWork {

    /* Default reccurence time */
    public static final long DEFAULT_RECCURENCE_TIME = 60000;

    private static final long INITIAL_DELAY_TIME = 15000;

    private volatile boolean stopRequested = false;
    private volatile boolean updateChannelRequested = false;
    private long reccurencePeriod = DEFAULT_RECCURENCE_TIME;

    /**
     * Creates instance.
     */
    public ReconnectTimer() {
        this(DEFAULT_RECCURENCE_TIME, false, false);
    }

    /**
     * Creates instance with specified parameters.
     *
     * @param reccurencePeriod
     *            the reccurence period in millis.
     * @param stopRequested
     *            true if stop timer is requested.
     * @param updateChannelRequested
     *            true if update channel is required.
     */
    public ReconnectTimer(long reccurencePeriod, boolean stopRequested, boolean updateChannelRequested) {
        this.reccurencePeriod = reccurencePeriod;
        this.stopRequested = stopRequested;
        this.updateChannelRequested = updateChannelRequested;
    }

    @Override
    public long getRecurrencePeriod() {
        return reccurencePeriod;
    }

    /**
     * Sets recurrence period.
     *
     * @param reccurencePeriod
     *            the recurrnce period in millis.
     */
    public void setRecurrencePeriod(long reccurencePeriod) {
        this.reccurencePeriod = reccurencePeriod;
    }

    /**
     * Request updating channel.
     */
    public void updateChannel() {
        updateChannelRequested = true;
    }

    @Override
    public long getInitialDelay() {
        return INITIAL_DELAY_TIME;
    }

    @Override
    public AperiodicWork getNewInstance() {
        return new ReconnectTimer(reccurencePeriod, stopRequested, updateChannelRequested);
    }

    @Override
    protected void doAperiodicRun() {
        if (!stopRequested) {
            RabbitManager manager = RabbitManager.getInstance();
            RabbitConfiguration config = RabbitConfiguration.get();
            ConnectionMonitor monitor = ConnectionMonitor.get();

            if (config.isEnableConsumer()) {
                if (!manager.isOpen()) {
                    logger.info("watchdog: Reconnect requesting..");
                    monitor.setActivate(true);
                    RabbitManager.getInstance().update();
                    updateChannelRequested = false;
                } else {
                    if (updateChannelRequested) {
                        logger.info("watchdog: channel update requesting..");
                        RabbitManager.getInstance().update();
                        updateChannelRequested = false;
                    }
                    monitor.setActivate(false);
                    monitor.setLastMeanTime(System.currentTimeMillis());
                }
            } else {
                monitor.setActivate(false);
            }
        }
    }

    /**
     * Stops periodic run.
     */
    public void stop() {
        stopRequested = true;
    }

    /**
     * Starts periodic run.
     */
    public void start() {
        stopRequested = false;
    }

    /**
     * Gets this extension from extension list.
     *
     * @return the instance of this plugin.
     */
    public static ReconnectTimer get() {
        return AperiodicWork.all().get(ReconnectTimer.class);
    }
}
