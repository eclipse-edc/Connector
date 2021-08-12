package org.eclipse.dataspaceconnector.events.azure;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class AzureEventGridConfig {
    public static final String DEFAULT_SYSTEM_TOPIC_NAME = "connector-events";
    public static final String DEFAULT_ENDPOINT_NAME_TEMPLATE = "https://%s.westeurope-1.eventgrid.azure.net/api/events";

    @EdcSetting
    public static final String TOPIC_NAME_SETTING = "edc.events.topic.name";
    @EdcSetting
    public static final String TOPIC_ENDPOINT_SETTING = "edc.events.topic.endpoint";
    private final ServiceExtensionContext context;

    public AzureEventGridConfig(ServiceExtensionContext context) {
        this.context = context;
    }

    /**
     * Returns a default {@code AzureEventGridConfig} that uses only default values
     */
    public static AzureEventGridConfig getDefault() {
        return new AzureEventGridConfig(null);
    }

    public String getTopic() {
        if (context == null) {
            return DEFAULT_SYSTEM_TOPIC_NAME;
        }

        return context.getSetting(TOPIC_NAME_SETTING, DEFAULT_SYSTEM_TOPIC_NAME);
    }

    public String getEndpoint(String topicName) {
        if (context == null) {
            return String.format(DEFAULT_ENDPOINT_NAME_TEMPLATE, topicName);
        }

        var ep = context.getSetting(TOPIC_ENDPOINT_SETTING, null);
        if (ep == null) {
            ep = String.format(DEFAULT_ENDPOINT_NAME_TEMPLATE, topicName);
        }
        return ep;
    }
}
