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

package org.eclipse.edc.sql.lease;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class LeaseContextTest {

    protected static final String LEASE_HOLDER = "test-leaser";
    protected final Instant now = Clock.systemUTC().instant();

    @Test
    void breakLease() {

        insertTestEntity("id1");
        getLeaseContext().acquireLease("id1");
        assertThat(isLeased("id1")).isTrue();

        getLeaseContext().breakLease("entityId");
        assertThat(isLeased("id1")).isTrue();
    }

    @Test
    void breakLease_whenNotExist() {
        getLeaseContext().breakLease("not-exist");
        //should not throw an exception
    }

    @Test
    void breakLease_whenLeaseByOther() {
        var id = "test-id";
        insertTestEntity(id);
        getLeaseContext().acquireLease(id);

        //break lease as someone else
        var leaseContext = createLeaseContext("someone-else");
        assertThatThrownBy(() -> leaseContext.breakLease(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquireLease() {
        var id = "test-id";
        insertTestEntity(id);

        getLeaseContext().acquireLease(id);

        assertThat(isLeased(id)).isTrue();
        var leaseAssert = assertThat(getLeaseContext().getLease(id));
        leaseAssert.extracting(SqlLease::getLeaseId).isNotNull();
        leaseAssert.extracting(SqlLease::getLeasedBy).isEqualTo(LEASE_HOLDER);
        leaseAssert.extracting(SqlLease::getLeasedAt).matches(l -> l <= now.toEpochMilli());
        leaseAssert.extracting(SqlLease::getLeaseDuration).isEqualTo(60_000L);
    }

    @Test
    void acquireLease_leasedBySelf_throwsException() {

        var id = "test-id";
        insertTestEntity(id);

        getLeaseContext().acquireLease(id);
        assertThatThrownBy(() -> getLeaseContext().acquireLease(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquireLease_leasedByOther_throwsException() {

        var id = "test-id";
        insertTestEntity(id);

        getLeaseContext().acquireLease(id);

        var leaseContext = createLeaseContext("someone-else");
        assertThatThrownBy(() -> leaseContext.acquireLease(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getLease() {
        var id = "test-id";
        insertTestEntity(id);

        getLeaseContext().acquireLease(id);
        assertThat(getLeaseContext().getLease(id)).isNotNull();
    }

    @Test
    void getLease_notExist() {
        assertThat(getLeaseContext().getLease("not-exist")).isNull();
    }

    protected abstract SqlLeaseContext createLeaseContext(String holder);

    protected abstract SqlLeaseContext getLeaseContext();

    protected abstract boolean isLeased(String entityId);

    protected abstract void insertTestEntity(String id);

    protected abstract TestEntity getTestEntity(String id);


    protected static class TestEntity {
        private final String id;
        private final String leaseId;

        TestEntity(String id, String leaseId) {
            this.id = id;
            this.leaseId = leaseId;
        }

        public String getId() {
            return id;
        }

        public String getLeaseId() {
            return leaseId;
        }
    }

}