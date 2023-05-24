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

package org.eclipse.edc.connector.core.vault;

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
    void storeSecret() {
        assertThat(vault.storeSecret("key", "value1").succeeded()).isTrue();
        assertThat(vault.resolveSecret("key")).isEqualTo("value1");
        assertThat(vault.storeSecret("key", "value2").succeeded()).isTrue();
        assertThat(vault.resolveSecret("key")).isEqualTo("value2");
    }

    @Test
    void deleteSecret() {
        assertThat(vault.deleteSecret("key").succeeded()).isFalse();
        assertThat(vault.storeSecret("key", "value1").succeeded()).isTrue();
        assertThat(vault.deleteSecret("key").succeeded()).isTrue();
        assertThat(vault.resolveSecret("key")).isNull();

    }
}