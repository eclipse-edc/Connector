package com.microsoft.dagx.transfer.demo.protocols.spi.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A subscription to a destination.
 */
public class Subscription {
    private String destinationName;
    private String subscriptionId;

    public Subscription(@JsonProperty("destinationName") String destinationName, @JsonProperty("subscriptionId") String subscriptionId) {
        this.destinationName = destinationName;
        this.subscriptionId = subscriptionId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }


}
