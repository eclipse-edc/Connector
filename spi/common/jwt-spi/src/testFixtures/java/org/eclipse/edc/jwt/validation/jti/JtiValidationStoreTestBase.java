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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

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
