/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.util.collection;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Wraps a value for usage in a {@link Cache}, adding a time-of-last-update and a validity period.
 *
 * @param value          The raw value
 * @param lastUpdatedAt  The {@link Instant} when the value was last updated.
 * @param validityMillis The time in milliseconds how long the entry is valid. Defaults to {@link TimestampedValue#DEFAULT_VALIDITY_MILLIS} (= 5 minutes)
 */
public record TimestampedValue<V>(V value, Instant lastUpdatedAt, long validityMillis) {
    public static final long DEFAULT_VALIDITY_MILLIS = 5 * 60 * 1000L; // 5 minutes

    public TimestampedValue(V value) {
        this(value, Instant.now(), DEFAULT_VALIDITY_MILLIS);
    }

    public boolean isExpired(Clock clock) {
        return lastUpdatedAt == null || lastUpdatedAt.plus(validityMillis, ChronoUnit.MILLIS).isBefore(Instant.now(clock));
    }
}
