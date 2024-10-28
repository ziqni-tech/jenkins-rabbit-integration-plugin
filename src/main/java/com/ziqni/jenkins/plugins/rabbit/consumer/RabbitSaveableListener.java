package com.ziqni.jenkins.plugins.rabbit.consumer;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import com.ziqni.jenkins.plugins.rabbit.configuration.RabbitConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement class for {@link SaveableListener}.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class RabbitSaveableListener extends SaveableListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitSaveableListener.class);

    @Override
    public final void onChange(Saveable o, XmlFile file) {
        if (o instanceof RabbitConfiguration) {
            LOGGER.info("RabbitMQ configuration is updated, so update connection...");
            RabbitManager.getInstance().update();
        }
        super.onChange(o, file);
    }

    /**
     * Gets instance of this extension.
     *
     * @return the instance of this extension.
     */
    public static RabbitSaveableListener get() {
        return SaveableListener.all().get(RabbitSaveableListener.class);
    }
}
