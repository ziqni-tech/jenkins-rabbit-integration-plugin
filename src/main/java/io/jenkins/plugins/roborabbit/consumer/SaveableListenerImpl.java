package io.jenkins.plugins.roborabbit.consumer;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import io.jenkins.plugins.roborabbit.configuration.RoboRabbitConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement class for {@link SaveableListener}.
 *
 * @author rinrinne a.k.a. rin_ne
 */
@Extension
public class SaveableListenerImpl extends SaveableListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveableListenerImpl.class);

    @Override
    public final void onChange(Saveable o, XmlFile file) {
        if (o instanceof RoboRabbitConfiguration) {
            LOGGER.info("RabbitMQ configuration is updated, so update connection...");
            RMQManager.getInstance().update();
        }
        super.onChange(o, file);
    }

    /**
     * Gets instance of this extension.
     *
     * @return the instance of this extension.
     */
    public static SaveableListenerImpl get() {
        return SaveableListener.all().get(SaveableListenerImpl.class);
    }
}
