/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Acts as exclusive lock on a {@link LeaseableCosmosDocument}
 */
public class Lease {
    @JsonProperty
    private final String leasedBy;
    @JsonProperty
    private final long leasedAt;
    @JsonProperty
    private final long leaseDuration;

    public Lease(@JsonProperty("leasedBy") String leasedBy,
                 @JsonProperty("leasedAt") long leasedAt,
                 @JsonProperty("leaseDuration") long leaseDurationMillis) {
        this.leasedBy = leasedBy;
        this.leasedAt = leasedAt;
        leaseDuration = leaseDurationMillis;
    }

    /**
     * A string identifying the runtime instance that holds the lease
     */
    public String getLeasedBy() {
        return leasedBy;
    }

    /**
     * Time of acquiring the Lease as POSIX timestamp
     */
    public long getLeasedAt() {
        return leasedAt;
    }

    /**
     * Duration of the Lease in Milliseconds
     */
    public long getLeaseDuration() {
        return leaseDuration;
    }
}
