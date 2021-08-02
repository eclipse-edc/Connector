package com.microsoft.dagx.transfer.demo.protocols.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class DataDestination {
    private String destinationName;
    private String accessToken;

    public DataDestination(@JsonProperty("destinationName") String destinationName, @JsonProperty("accessToken") String accessToken) {
        this.destinationName = destinationName;
        this.accessToken = accessToken;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
