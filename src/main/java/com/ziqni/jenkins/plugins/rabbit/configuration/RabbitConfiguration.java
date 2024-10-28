package com.ziqni.jenkins.plugins.rabbit.configuration;

import hudson.Extension;
import hudson.util.Secret;
import hudson.ExtensionList;
import hudson.util.ListBoxModel;
import hudson.util.FormValidation;

import com.ziqni.jenkins.plugins.rabbit.consumer.Messages;
import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitManager;
import com.ziqni.jenkins.plugins.rabbit.consumer.RabbitmqConsumeItem;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import com.ziqni.jenkins.plugins.rabbit.consumer.utils.Validators;
import com.ziqni.jenkins.plugins.rabbit.consumer.watchdog.ReconnectTimer;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.PossibleAuthenticationFailureException;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import org.kohsuke.stapler.verb.POST;

import java.util.List;
import java.util.Objects;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import java.net.URISyntaxException;
import javax.servlet.ServletException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeoutException;

/**
 * Global configuration for the Robo Rabbit AMPQ Consumer Plugin.
 */
@Extension
public class RabbitConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(RabbitConfiguration.class.getName());

    /**
     * The string in global configuration that indicates content is empty.
     */
    public static final String CONTENT_NONE = "-";

    private String label;
    private String serviceUri;
    private boolean enableDebug;
    private String credentialsId;
    private boolean enableConsumer;
    private List<RabbitmqConsumeItem> consumeItems;
    private long watchdogPeriod = ReconnectTimer.DEFAULT_RECCURENCE_TIME;

    public RabbitConfiguration() {
        load();
    }

    /**
     * Constructor.
     *
     * @param enableConsumer
     *            the flag to enable consumer.
     * @param serviceUri
     *            the URI of RabbitMQ service.
     * @param credentialsId
     *            the credentials ID.
     * @param watchdogPeriod
     *            the period of watchdog.
     * @param consumeItems
     *            the list of consume items.
     * @param enableDebug
     *            the flag to enable debug.
     */
    @DataBoundConstructor
    public RabbitConfiguration(boolean enableConsumer, String serviceUri, String credentialsId, long watchdogPeriod, List<RabbitmqConsumeItem> consumeItems, boolean enableDebug) {

        this.serviceUri = serviceUri;
        this.enableDebug = enableDebug;
        this.consumeItems = consumeItems;
        this.credentialsId = credentialsId;
        this.enableConsumer = enableConsumer;
        this.watchdogPeriod = watchdogPeriod;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
        if (consumeItems != null) {
            consumeItems.clear();
        }
        req.bindJSON(this, json);

        ReconnectTimer timer = ReconnectTimer.get();
        if (timer != null) {
            timer.setRecurrencePeriod(watchdogPeriod);
        }

        if (Validators.isValidAMQPUrl(serviceUri)) {

            req.bindJSON(this, json);
            save();
            return true;
        }

        return false;
    }

    /** @return the singleton instance */
    public static RabbitConfiguration get() {
        return ExtensionList.lookupSingleton(RabbitConfiguration.class);
    }

    public String getLabel() {
        return label;
    }

    /**
     * Together with {@link #getLabel}, binds to entry in {@code config.jelly}.
     * @param label the new value of this field
     */
    @DataBoundSetter
    public void setLabel(String label) {
        this.label = label;
        save();
    }

    /**
     * Checks given label is valid.
     *
     * @param value
     *            the label.
     * @return FormValidation object that indicates ok or warning.
     */
    @POST
    public FormValidation doCheckLabel(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Ensure proper permission is granted

        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a label.");
        }
        return FormValidation.ok();
    }

    /**
     * Gets the value of enableConsumer.
     *
     * @return the value of enableConsumer.
     */
    public boolean isEnableConsumer() {
        return enableConsumer;
    }

    /**
     * Sets the enableConsumer.
     *
     * @param enableConsumer
     *            the value to set.
     */
    @DataBoundSetter
    public void setEnableConsumer(boolean enableConsumer) {
        this.enableConsumer = enableConsumer;
        save();
    }

    /**
     * Gets the value of serviceUri.
     *
     * @return the value of serviceUri.
     */
    public String getServiceUri() {
        return serviceUri;
    }

    /**
     * Sets the serviceUri.
     *
     * @param serviceUri
     *            the value to set.
     */
    @DataBoundSetter
    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
        save();
    }

    /**
     * Gets the value of credentialsId.
     *
     * @return the value of credentialsId.
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Sets the credentialsId.
     *
     * @param credentialsId
     *            the value to set.
     */
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        save();
    }

    /**
     * Gets the credentials.
     *
     * @return the credentials.
     */
    public StandardUsernamePasswordCredentials getCredentials() {
        if(Objects.isNull(credentialsId)) {
            Logger.getLogger(RabbitConfiguration.class.getName()).warning("Credentials ID is null");
            return null;
        }

        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentialsInItemGroup(
                StandardUsernamePasswordCredentials.class,
                null,
                null,
                Collections.emptyList()
        );

        return CredentialsMatchers.firstOrNull(credentials, CredentialsMatchers.withId(getCredentialsId()));
    }

    /**
     * Gets the credentials.
     *
     * @return the credentials.
     */
    private StandardUsernamePasswordCredentials getCredentials(String tmpCredentialsId) {
        if(Objects.isNull(tmpCredentialsId)) {
            Logger.getLogger(RabbitConfiguration.class.getName()).warning("Credentials ID is null");
            return null;
        }

        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentialsInItemGroup(
                StandardUsernamePasswordCredentials.class,
                null,
                null,
                Collections.emptyList()
        );

        return CredentialsMatchers.firstOrNull(credentials, CredentialsMatchers.withId(tmpCredentialsId));
    }

    /**
     * Fills the credentialsId field with a list of credentials.
     *
     * @return the list of credentials.
     */
    public ListBoxModel doFillCredentialsIdItems() {
        return CredentialsProvider.listCredentialsInItemGroup(
                StandardUsernamePasswordCredentials.class,
                null,
                null,
                Collections.emptyList(),
                CredentialsMatchers.always()
        );
    }

    /**
     * Gets the value of watchdogPeriod.
     *
     * @return the value of watchdogPeriod.
     */
    public long getWatchdogPeriod() {
        return watchdogPeriod;
    }

    /**
     * Sets the watchdogPeriod.
     *
     * @param watchdogPeriod
     *            the value to set.
     */
    @DataBoundSetter
    public void setWatchdogPeriod(long watchdogPeriod) {
        this.watchdogPeriod = watchdogPeriod;
        save();
    }

    /**
     * Gets the value of consumeItems.
     *
     * @return the value of consumeItems.
     */
    public List<RabbitmqConsumeItem> getConsumeItems() {
        return consumeItems;
    }

    /**
     * Sets the consumeItems.
     *
     * @param consumeItems
     *            the value to set.
     */
    @DataBoundSetter
    public void setConsumeItems(List<RabbitmqConsumeItem> consumeItems) {
        this.consumeItems = consumeItems;
        save();
    }

    /**
     * Gets the value of enableDebug.
     *
     * @return the value of enableDebug.
     */
    public boolean isEnableDebug() {
        return enableDebug;
    }

    /**
     * Sets the enableDebug.
     *
     * @param enableDebug
     *            the value to set.
     */
    @DataBoundSetter
    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
        save();
    }

    /**
     * Gets connection to service is established. Note that this is called by
     * Ajax.
     *
     * @return true if connection is established.
     */
    @JavaScriptMethod
    public boolean isOpen() {
        return RabbitManager.getInstance().isOpen();
    }

    /**
     * Gets specified queue is consumed or not. Note that this is called by
     * Ajax.
     *
     * @param queueName
     *            the queue name.
     * @return true if specified queue is already consumed.
     */
    @JavaScriptMethod
    public boolean isConsume(String queueName) {
        RabbitManager manager = RabbitManager.getInstance();
        if (manager.isOpen()) {
            return manager.getChannelStatus(queueName);
        }
        return false;
    }

    /**
     * Checks given URI is valid.
     *
     * @param value
     *            the URI.
     * @return FormValidation object that indicates ok or error.
     */
    @POST
    public FormValidation doCheckServiceUri(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Ensure proper permission is granted

        String val = org.apache.commons.lang3.StringUtils.stripToNull(value);

        if (val == null || Validators.isValidAMQPUrl(val)) {
            return FormValidation.ok();
        } else {
            return FormValidation.error(Messages.InvalidURI());
        }
    }

    /**
     * Tests connection to given URI.
     *
     * @param serviceUri the URI.
     * @return FormValidation object that indicates ok or error.
     * @throws ServletException exception for servlet.
     */
    @POST
    public FormValidation doTestConnection(@QueryParameter("serviceUri") String serviceUri, @QueryParameter("credentialsId") String credentialsId) throws ServletException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Ensure proper permission is granted

        String uri = org.apache.commons.lang3.StringUtils.strip(org.apache.commons.lang3.StringUtils.stripToNull(serviceUri), "/");

        if (uri != null && Validators.isValidAMQPUrl(uri)) {
            try {
                final var credentials = getCredentials(credentialsId);

                if(credentials == null || credentials.getUsername() == null || credentials.getPassword() == null) {
                    LOGGER.warning("Credentials are null");
                    return FormValidation.error(Messages.AuthFailure());
                }

                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(uri);

                if (org.apache.commons.lang3.StringUtils.isNotEmpty(credentials.getUsername())) {
                    factory.setUsername(credentials.getUsername());
                }

                if (org.apache.commons.lang3.StringUtils.isNotEmpty(Secret.toString(credentials.getPassword()))) {
                    factory.setPassword(Secret.toString(credentials.getPassword()));
                }

                try (Connection connection = factory.newConnection()) {
                    return FormValidation.ok(Messages.Success());
                }catch (PossibleAuthenticationFailureException e) {
                    return FormValidation.error(Messages.AuthFailure());
                } catch (IOException e) {
                    return FormValidation.error(Messages.Error() + ": " + e);
                } catch (TimeoutException e) {
                    return FormValidation.error(Messages.Error() + ": " + e);
                }

            } catch (URISyntaxException e) {
                return FormValidation.error(Messages.InvalidURI());
            } catch (GeneralSecurityException e) {
                return FormValidation.error(Messages.Error() + ": " + e);
            }
        }
        return FormValidation.error(Messages.InvalidURI());
    }

    @Override
    public String toString() {
        return "GlobalRabbitmqConfiguration{" +
                "enableConsumer=" + enableConsumer +
                ", serviceUri='" + serviceUri + '\'' +
                ", credentialsId='" + credentialsId + '\'' +
                ", watchdogPeriod=" + watchdogPeriod +
                ", consumeItems=" + consumeItems +
                ", enableDebug=" + enableDebug +
                ", label='" + label + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RabbitConfiguration)) return false;
        RabbitConfiguration that = (RabbitConfiguration) o;
        return isEnableConsumer() == that.isEnableConsumer() && getWatchdogPeriod() == that.getWatchdogPeriod() && isEnableDebug() == that.isEnableDebug() && Objects.equals(getServiceUri(), that.getServiceUri()) && Objects.equals(getCredentialsId(), that.getCredentialsId()) && Objects.equals(getConsumeItems(), that.getConsumeItems()) && Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isEnableConsumer(), getServiceUri(), getCredentialsId(), getWatchdogPeriod(), getConsumeItems(), isEnableDebug(), getLabel());
    }
}
