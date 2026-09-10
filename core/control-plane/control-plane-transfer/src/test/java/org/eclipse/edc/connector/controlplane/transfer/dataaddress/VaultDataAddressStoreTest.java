/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.dataaddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VaultDataAddressStoreTest {

    private final Vault vault = mock();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VaultDataAddressStore store = new VaultDataAddressStore(vault, () -> objectMapper);

    @Nested
    class Store {

        @Test
        void shouldStoreDataAddressInTheVault() throws JsonProcessingException {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .build();
            when(vault.storeSecret(any(), any(), any())).thenReturn(Result.success());

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isSucceeded();
            var alias = "transfer-process-tp-id-data-address";
            assertThat(transferProcess.getDataAddressAlias()).isEqualTo(alias);
            verify(vault).storeSecret("participant-context-id", alias, objectMapper.writeValueAsString(dataAddress));
        }

        @Test
        void shouldFailWhenStorageFails() {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .build();
            when(vault.storeSecret(any(), any(), any())).thenReturn(Result.failure("error"));

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class Resolve {

        @Test
        void shouldResolveDataAddressFromTheVault() throws JsonProcessingException {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias("data-address-alias")
                    .build();
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();

            when(vault.resolveSecret(any(), any())).thenReturn(objectMapper.writeValueAsString(dataAddress));

            var result = store.resolve(transferProcess);

            assertThat(result).isSucceeded().usingRecursiveComparison().isEqualTo(dataAddress);
        }

        @Test
        void shouldFailIfStoredStringIsNotValidJson() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias("data-address-alias")
                    .build();
            when(vault.resolveSecret(any(), any())).thenReturn("not a json");

            var result = store.resolve(transferProcess);

            assertThat(result).isFailed();
        }

        @Test
        void shouldFailIfNoAddressAvailable() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias("data-address-alias")
                    .build();
            when(vault.resolveSecret(any(), any())).thenReturn(null);

            var result = store.resolve(transferProcess);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class Remove {
        @Test
        void shouldRemoveDataAddressFromVault() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias("data-address-alias")
                    .build();
            when(vault.deleteSecret(any(), any())).thenReturn(Result.success());

            var result = store.remove(transferProcess);

            assertThat(result).isSucceeded();
            assertThat(transferProcess.getDataAddressAlias()).isNull();
            verify(vault).deleteSecret("participant-context-id", "data-address-alias");
        }

        @Test
        void shouldFail_whenDeleteSecretFails() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias("data-address-alias")
                    .build();
            when(vault.deleteSecret(any(), any())).thenReturn(Result.failure("error"));

            var result = store.remove(transferProcess);

            assertThat(result).isFailed();
            assertThat(transferProcess.getDataAddressAlias()).isNotNull();
        }

        @Test
        void shouldSucceedWithoutEffect_whenAliasIsNull() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias(null)
                    .build();
            when(vault.deleteSecret(any(), any())).thenReturn(Result.failure("error"));

            var result = store.remove(transferProcess);

            assertThat(result).isSucceeded();
            verifyNoInteractions(vault);
        }
    }
}
