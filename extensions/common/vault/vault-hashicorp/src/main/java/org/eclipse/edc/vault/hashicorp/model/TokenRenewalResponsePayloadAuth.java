package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TokenRenewalResponsePayloadAuth {

    @JsonProperty("lease_duration")
    private int timeToLive;

    public int getTimeToLive() {
        return timeToLive;
    }
}
