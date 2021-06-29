/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Lease {
    @JsonProperty
    private final String leasedBy;
    @JsonProperty
    private final long leasedAt;
    @JsonProperty
    private final long leasedUntil;

    Lease(String leasedBy) {
        this(leasedBy, Instant.now().toEpochMilli(), Instant.now().plus(60, ChronoUnit.SECONDS).toEpochMilli());
    }

    public Lease(@JsonProperty("leasedBy") String leasedBy, @JsonProperty("leasedAt") long leasedAt, @JsonProperty("leasedUntil") long leasedUntil) {
        this.leasedBy = leasedBy;
        this.leasedAt = leasedAt;
        this.leasedUntil = leasedUntil;
    }

    public String getLeasedBy() {
        return leasedBy;
    }

    public long getLeasedAt() {
        return leasedAt;
    }

    public long getLeasedUntil() {
        return leasedUntil;
    }
}
