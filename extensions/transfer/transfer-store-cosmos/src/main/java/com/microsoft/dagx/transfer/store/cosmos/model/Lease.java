/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.store.cosmos.model;

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
        this(leasedBy, Instant.now().toEpochMilli(), 60);
    }

    public Lease(@JsonProperty("leasedBy") String leasedBy, @JsonProperty("leasedAt") long leasedAt, @JsonProperty("leaseDurationSeconds") long leaseDurationSeconds) {
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
