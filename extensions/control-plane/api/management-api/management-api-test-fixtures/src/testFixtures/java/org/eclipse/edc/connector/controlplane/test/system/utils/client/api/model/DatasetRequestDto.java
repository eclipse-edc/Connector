/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representation of a Dataset Request.
 */
public final class DatasetRequestDto extends Typed {
    @JsonProperty("@id")
    private final String id;
    private final String profile;
    private final String counterPartyAddress;
    private final String counterPartyId;

    public DatasetRequestDto(@JsonProperty("@id") String id,
                             String profile,
                             String counterPartyAddress,
                             String counterPartyId) {
        super("DatasetRequest");
        this.id = id;
        this.profile = profile;
        this.counterPartyAddress = counterPartyAddress;
        this.counterPartyId = counterPartyId;
    }

    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public String getProfile() {
        return profile;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

}
