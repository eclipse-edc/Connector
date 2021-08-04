/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataseed.nifi.api;

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
