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

package org.eclipse.dataspaceconnector.contract.negotiation.store.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Lease {
    @JsonProperty
    private final String leasedBy;
    @JsonProperty
    private final long leasedAt;
    @JsonProperty
    private final long leaseDuration;

    Lease(String leasedBy) {
        this(leasedBy, Instant.now().toEpochMilli(), 1000 * 60);
    }

    public Lease(@JsonProperty("leasedBy") String leasedBy, @JsonProperty("leasedAt") long leasedAt, @JsonProperty("leaseDuration") long leaseDurationSeconds) {
        this.leasedBy = leasedBy;
        this.leasedAt = leasedAt;
        leaseDuration = leaseDurationSeconds;
    }

    public String getLeasedBy() {
        return leasedBy;
    }

    public long getLeasedAt() {
        return leasedAt;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }
}
