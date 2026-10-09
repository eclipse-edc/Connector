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

package org.eclipse.edc.jwt.validation.jti;

import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;

public abstract class JtiValidationStoreTestBase {
    @Test
    void storeEntry() {
        assertThat(getStore().storeEntry(new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli()))).isSucceeded();
    }

    @Test
    void storeEntry_noExpiresAt() {
        assertThat(getStore().storeEntry(new JtiValidationEntry("test-id"))).isSucceeded();
    }

    @Test
    void storeEntry_alreadyExists() {
        getStore().storeEntry(new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli()));
        assertThat(getStore().storeEntry(new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli())))
                .isFailed()
                .detail().isEqualTo("JTI Validation Entry with ID 'test-id' already exists");
    }

    @Test
    void storeEntry_whenExistingEntryIsExpired_shouldReplaceIt() {
        getStore().storeEntry(new JtiValidationEntry("test-id", Instant.now().minusSeconds(10).toEpochMilli()));
        var entry = new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli());

        assertThat(getStore().storeEntry(entry)).isSucceeded();

        assertThat(getStore().findById("test-id", false)).usingRecursiveComparison().isEqualTo(entry);
    }

    @Test
    void storeEntry_whenExistingEntryHasNoExpiration_shouldFail() {
        getStore().storeEntry(new JtiValidationEntry("test-id"));

        assertThat(getStore().storeEntry(new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli())))
                .isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_EXISTS);
    }

    @Test
    void storeEntry_concurrently_shouldStoreOnce() throws Exception {
        var threads = 16;
        var barrier = new CyclicBarrier(threads);
        var executor = Executors.newFixedThreadPool(threads);
        try {
            var tasks = range(0, threads).mapToObj(i -> (Callable<StoreResult<Void>>) () -> {
                barrier.await(30, TimeUnit.SECONDS);
                return getStore().storeEntry(new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli()));
            }).toList();

            var stored = 0;
            for (var future : executor.invokeAll(tasks, 60, TimeUnit.SECONDS)) {
                // rethrows an exception of the store, e.g. a violated primary key
                var result = future.get();
                if (result.succeeded()) {
                    stored++;
                } else {
                    assertThat(result.reason()).isEqualTo(ALREADY_EXISTS);
                }
            }
            assertThat(stored).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void findById() {
        var entry = new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli());
        getStore().storeEntry(entry);
        assertThat(getStore().findById("test-id")).usingRecursiveComparison().isEqualTo(entry);
        assertThat(getStore().findById("test-id")).isNull();
    }

    @Test
    void findById_noAutoRemove() {
        var entry = new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli());
        getStore().storeEntry(entry);
        assertThat(getStore().findById("test-id", false)).usingRecursiveComparison().isEqualTo(entry);
        assertThat(getStore().findById("test-id", false)).usingRecursiveComparison().isEqualTo(entry);
    }

    @Test
    void findById_noExpiresAt() {
        var entry = new JtiValidationEntry("test-id", null);
        var store = getStore();
        store.storeEntry(entry);

        var found = store.findById("test-id", false);
        assertThat(found).isNotNull();
        assertThat(found.expirationTimestamp()).isNull();
        assertThat(found.expirationTimestampAsInstant()).isNull();
        assertThat(found.isExpired()).isFalse();

    }

    @Test
    void findById_notFound() {
        assertThat(getStore().findById("test-id")).isNull();
    }

    @Test
    void deleteById() {
        var entry = new JtiValidationEntry("test-id", Instant.now().plusSeconds(10).toEpochMilli());
        getStore().storeEntry(entry);
        assertThat(getStore().deleteById("test-id")).isSucceeded();
    }

    @Test
    void deleteById_notFound() {
        assertThat(getStore().deleteById("test-id")).isFailed()
                .detail().isEqualTo("JTI Validation Entry with ID 'test-id' not found");
    }

    @Test
    void deleteExpired_noExpiredEntries() {

        assertThat(getStore().deleteExpired()).isSucceeded().isEqualTo(0);

        range(0, 10).forEach(i -> assertThat(getStore().findById("test-id" + i)).isNull());
    }

    @Test
    void deleteExpired() {
        range(0, 10).forEach(i -> getStore().storeEntry(new JtiValidationEntry("test-id" + i, Instant.now().minusSeconds(100).toEpochMilli())));
        getStore().storeEntry(new JtiValidationEntry("some-other-entry1"));
        getStore().storeEntry(new JtiValidationEntry("some-other-entry2"));

        assertThat(getStore().deleteExpired()).isSucceeded().isEqualTo(10);

        range(0, 10).forEach(i -> assertThat(getStore().findById("test-id" + i)).isNull());
        assertThat(getStore().findById("some-other-entry1")).isNotNull();
        assertThat(getStore().findById("some-other-entry2")).isNotNull();
    }

    protected abstract JtiValidationStore getStore();
}
