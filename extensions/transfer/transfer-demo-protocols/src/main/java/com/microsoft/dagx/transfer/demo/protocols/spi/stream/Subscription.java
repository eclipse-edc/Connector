package com.microsoft.dagx.transfer.demo.protocols.spi.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A subscription to a destination topic.
 */
public class Subscription {
    private String topicName;
    private String subscriptionId;

    public Subscription(@JsonProperty("topicName") String topicName, @JsonProperty("subscriptionId") String subscriptionId) {
        this.topicName = topicName;
        this.subscriptionId = subscriptionId;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }


}
