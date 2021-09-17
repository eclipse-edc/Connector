/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataDestination {
    private final String destinationName;
    private final String accessToken;

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
