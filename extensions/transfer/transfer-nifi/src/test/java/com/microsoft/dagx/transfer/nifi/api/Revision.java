/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Revision {
    @JsonProperty
    public int version;
    @JsonProperty
    private String clientId;

    public Revision(int version) {
        this.version = version;
    }

    public Revision() {
    }

    public String getClientId() {
        return clientId;
    }
}
