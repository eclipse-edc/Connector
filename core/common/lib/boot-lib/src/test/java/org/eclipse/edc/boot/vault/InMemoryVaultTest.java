/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.boot.vault;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InMemoryVaultTest {

    private InMemoryVault vault;

    @BeforeEach
    void setUp() {
        vault = new InMemoryVault(mock(Monitor.class));
    }

    @Test
    void resolveSecret() {
        assertThat(vault.resolveSecret("key")).isNull();
        vault.storeSecret("key", "secret");
        assertThat(vault.resolveSecret("key")).isEqualTo("secret");
    }

    @Test
    void resolveSecret_partitioned() {
        assertThat(vault.resolveSecret("key")).isNull();
        vault.storeSecret("partition", "key", "secret");
        assertThat(vault.resolveSecret("partition", "key")).isEqualTo("secret");
        assertThat(vault.resolveSecret("another", "key")).isNull();
    }

    @Test
    void storeSecret() {
        assertThat(vault.storeSecret("key", "value1").succeeded()).isTrue();
        assertThat(vault.resolveSecret("key")).isEqualTo("value1");
        assertThat(vault.storeSecret("key", "value2").succeeded()).isTrue();
        assertThat(vault.resolveSecret("key")).isEqualTo("value2");
    }

    @Test
    void storeSecret_partitioned() {
        assertThat(vault.storeSecret("partition", "key", "value1").succeeded()).isTrue();
        assertThat(vault.resolveSecret("partition", "key")).isEqualTo("value1");
        assertThat(vault.storeSecret("partition", "key", "value2").succeeded()).isTrue();
        assertThat(vault.resolveSecret("partition", "key")).isEqualTo("value2");

        assertThat(vault.storeSecret("partition", "key", "value2").succeeded()).isTrue();
    }

    @Test
    void deleteSecret() {
        assertThat(vault.deleteSecret("key").succeeded()).isFalse();
        assertThat(vault.storeSecret("key", "value1").succeeded()).isTrue();
        assertThat(vault.deleteSecret("key").succeeded()).isTrue();
        assertThat(vault.resolveSecret("key")).isNull();

    }

    @Test
    void deleteSecret_partitioned() {
        assertThat(vault.deleteSecret("partition", "key").succeeded()).isFalse();
        assertThat(vault.storeSecret("partition", "key", "value1").succeeded()).isTrue();
        assertThat(vault.deleteSecret("partition", "key").succeeded()).isTrue();
        assertThat(vault.resolveSecret("partition", "key")).isNull();

        assertThat(vault.storeSecret("partition", "key", "value1").succeeded()).isTrue();
        assertThat(vault.deleteSecret("another", "key").succeeded()).isFalse();
    }
}