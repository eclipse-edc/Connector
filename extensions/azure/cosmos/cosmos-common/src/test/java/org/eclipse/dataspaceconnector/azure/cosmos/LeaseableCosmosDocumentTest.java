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

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaseableCosmosDocumentTest {

    private final Clock clock = Clock.systemUTC();

    @Test
    void getLease() {
        var doc = new TestDocument("foo", "testpartitionkey");
        assertThat(doc.getLease()).isNull();
    }

    @Test
    void breakLease() {
        var doc = new TestDocument("foo", "testpartitionkey");

        doc.acquireLease("me", clock);

        doc.breakLease("me");
        assertThat(doc.getLease()).isNull();

    }

    @Test
    void breakLease_whenNotLeased() {
        var doc = new TestDocument("foo", "testpartitionkey");

        doc.breakLease("me");
        assertThat(doc.getLease()).isNull();
    }

    @Test
    void breakLease_whenNotLeasedBySelf() {
        var doc = new TestDocument("foo", "testpartitionkey");
        doc.acquireLease("me", clock);
        assertThatThrownBy(() -> doc.breakLease("not-me")).isInstanceOf(IllegalStateException.class);
    }


    @Test
    void acquireLease() {
        var doc = new TestDocument("foo", "testpartitionkey");

        doc.acquireLease("me", clock);
        assertThat(doc.getLease()).isNotNull();
        assertThat(doc.getLease().getLeasedBy()).isEqualTo("me", clock);
        assertThat(doc.getLease().getLeaseDuration()).isEqualTo(60_000L);
        assertThat(doc.getLease().getLeasedAt()).isGreaterThan(0);
    }

    @Test
    void acquireLease_withDuration() {
        var doc = new TestDocument("foo", "testpartitionkey");

        doc.acquireLease("me", clock, Duration.ofSeconds(2));
        assertThat(doc.getLease()).isNotNull();
        assertThat(doc.getLease().getLeasedBy()).isEqualTo("me", clock);
        assertThat(doc.getLease().getLeaseDuration()).isEqualTo(2_000L);
        assertThat(doc.getLease().getLeasedAt()).isGreaterThan(0);
    }

    @Test
    void acquireLease_whenAlreadyAcquired() {
        var doc = new TestDocument("foo", "testpartitionkey");
        doc.acquireLease("me", clock);
        doc.acquireLease("me", clock);
        assertThat(doc.getLease()).isNotNull();
    }

    @Test
    void acquireLease_whenAlreadyAcquired_byOther() {
        var doc = new TestDocument("foo", "testpartitionkey");
        doc.acquireLease("me", clock);
        assertThatThrownBy(() -> doc.acquireLease("not-me", clock)).isInstanceOf(IllegalStateException.class);
    }
}