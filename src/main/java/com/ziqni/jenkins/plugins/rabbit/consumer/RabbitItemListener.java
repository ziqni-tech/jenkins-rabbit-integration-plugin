package com.ziqni.jenkins.plugins.rabbit.consumer;

import hudson.Extension;
import hudson.model.listeners.ItemListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements of {@link ItemListener}.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class
RabbitItemListener extends ItemListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitItemListener.class);

    private final RabbitManager rabbitManager;

    /**
     * Creates instance from this class.
     */
    public RabbitItemListener() {
        super();
        this.rabbitManager = RabbitManager.getInstance();
    }

    @Override
    public final void onLoaded() {
        LOGGER.info("Start bootup process.");
        rabbitManager.update();
        super.onLoaded();
    }

    @Override
    public final void onBeforeShutdown() {
        rabbitManager.shutdown();
        super.onBeforeShutdown();
    }

    /**
     * Gets this extension's instance.
     *
     * @return the instance of this extension.
     */
    public static RabbitItemListener get() {
        return ItemListener.all().get(RabbitItemListener.class);
    }
}
