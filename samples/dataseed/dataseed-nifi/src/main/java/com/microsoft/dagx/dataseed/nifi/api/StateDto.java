/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.dataseed.nifi.api;

public class StateDto {
    public String id;
    public String state;
    public boolean disconnectedNodeAcknowledged;

    public StateDto(String processGroup, String state, boolean disconnectedNodeAck) {
        id = processGroup;
        this.state = state;
        disconnectedNodeAcknowledged = disconnectedNodeAck;
    }

    public StateDto() {
    }
}
