package com.ziqni.jenkins.plugins.rabbit.utils;

/**
 * RabbitMessageBuilderAccessors interface to access RabbitMessageBuilder properties
 */
public interface RabbitMessageBuilderAccessors {


    // Setter methods
    void setContentType(String contentType);

    void setContentEncoding(String contentEncoding);

    void setDeliveryMode(Integer deliveryMode);

    void setPriority(Integer priority);

    void setCorrelationId(String correlationId);

    void setReplyTo(String replyTo);

    void setExpiration(String expiration);

    void setMessageId(String messageId);

    void setTimestamp(String timestamp);

    void setType(String type);

    void setUserId(String userId);

    void setAppId(String appId);

    void setClusterId(String clusterId);

    void setHeaders(String headers);

    // Getter methods
    String getContentType();

    String getContentEncoding();

    Integer getDeliveryMode();

    Integer getPriority();

    String getCorrelationId();

    String getReplyTo();

    String getExpiration();

    String getMessageId();

    String getTimestamp();

    String getType();

    String getUserId();

    String getAppId();

    String getClusterId();

    String getHeaders();
}
