/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.events.azure;

public class EventDto {
    private final String connectorId;

    protected EventDto(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getConnectorId() {
        return connectorId;
    }
}
