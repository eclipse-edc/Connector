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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataPlaneProtocolInUse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VaultDataAddressStoreTest {

    private final Vault vault = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final JsonLd jsonLd = mock();
    private final DataPlaneProtocolInUse dataPlaneProtocolInUse = mock();
    private final VaultDataAddressStore store = new VaultDataAddressStore(vault, typeTransformerRegistry, jsonLd, dataPlaneProtocolInUse);

    @Nested
    class Store {
        @Test
        void shouldStoreDataAddressInTheVault() {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .build();
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
            var expandedJson = Json.createObjectBuilder().add("type", "any").add("this is", "the json data address").build();
            when(jsonLd.expand(any())).thenReturn(Result.success(expandedJson));
            when(vault.storeSecret(any(), any(), any())).thenReturn(Result.success());

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isSucceeded();
            var alias = "transfer-process-tp-id-data-address";
            assertThat(transferProcess.getDataAddressAlias()).isEqualTo(alias);
            verify(vault).storeSecret("participant-context-id", alias, expandedJson.toString());
        }

        @Test
        void shouldEventuallyRemoveDataDestinationFromTransferProcess() {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataDestination(DataAddress.Builder.newInstance().type("test").property("previous-data-address", "stored-in-database").build())
                    .build();
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
            var expandedJson = Json.createObjectBuilder().add("type", "any").add("this is", "the json data address").build();
            when(jsonLd.expand(any())).thenReturn(Result.success(expandedJson));
            when(vault.storeSecret(any(), any(), any())).thenReturn(Result.success());

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isSucceeded();
            assertThat(transferProcess.getDataDestination()).isNull();
            verify(vault).storeSecret("participant-context-id", "transfer-process-tp-id-data-address", expandedJson.toString());
        }

        @Test
        void shouldNotRemoveDataDestinationFromTransferProcess_whenDataPlaneProtocolInUseIsLegacy() {
            when(dataPlaneProtocolInUse.isLegacy()).thenReturn(true);
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataDestination(DataAddress.Builder.newInstance().type("test").property("previous-data-address", "stored-in-database").build())
                    .build();
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
            var expandedJson = Json.createObjectBuilder().add("type", "any").add("this is", "the json data address").build();
            when(jsonLd.expand(any())).thenReturn(Result.success(expandedJson));
            when(vault.storeSecret(any(), any(), any())).thenReturn(Result.success());

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isSucceeded();
            assertThat(transferProcess.getDataDestination()).isNotNull();
            verify(vault).storeSecret("participant-context-id", "transfer-process-tp-id-data-address", expandedJson.toString());
        }

        @Test
        void shouldFailWhenTransformationFails() {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .build();
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isFailed();
        }

        @Test
        void shouldFailWhenJsonLdExpansionFails() {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .build();
            var jsonDataAddress = Json.createObjectBuilder().add("type", "any").add("this is", "the json data address").build();
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(jsonDataAddress));
            when(jsonLd.expand(any())).thenReturn(Result.failure("error"));

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isFailed();
            verifyNoInteractions(vault);
        }

        @Test
        void shouldFailWhenStorageFails() {
            var dataAddress = DataAddress.Builder.newInstance().type("test").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .build();
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
            var expandedJson = Json.createObjectBuilder().add("type", "any").add("this is", "the json data address").build();
            when(jsonLd.expand(any())).thenReturn(Result.success(expandedJson));
            when(vault.storeSecret(any(), any(), any())).thenReturn(Result.failure("error"));

            var result = store.store(dataAddress, transferProcess);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class Resolve {
        @Test
        void shouldResolveDataAddressFromTheVault() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias("data-address-alias")
                    .build();
            when(vault.resolveSecret(any(), any())).thenReturn("{ \"type\": \"data-address-json\"}");
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            when(typeTransformerRegistry.transform(any(), eq(DataAddress.class))).thenReturn(Result.success(dataAddress));

            var result = store.resolve(transferProcess);

            assertThat(result).isSucceeded().isEqualTo(dataAddress);
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
        void shouldReturnDataDestinationIfNoAliasSet() {
            var dataDestination = DataAddress.Builder.newInstance().type("dataDestination").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias(null)
                    .dataDestination(dataDestination)
                    .build();

            var result = store.resolve(transferProcess);

            assertThat(result).isSucceeded().isEqualTo(dataDestination);
            verifyNoInteractions(vault);
        }

        @Test
        void shouldResolveDataDestinationSecretIfExisting() {
            var dataDestination = DataAddress.Builder.newInstance().type("dataDestination").keyName("key-name").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias(null)
                    .dataDestination(dataDestination)
                    .build();
            when(vault.resolveSecret(any(), any())).thenReturn("data-address-secret");

            var result = store.resolve(transferProcess);

            assertThat(result).isSucceeded().satisfies(dataAddress -> {
                assertThat(dataAddress.getType()).isEqualTo("dataDestination");
                assertThat(dataAddress.getStringProperty(DataAddress.EDC_DATA_ADDRESS_SECRET)).isEqualTo("data-address-secret");
            });
        }

        @Test
        void shouldReturnNotFoundIfNoAddressAvailable() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("tp-id")
                    .participantContextId("participant-context-id")
                    .dataAddressAlias(null)
                    .dataDestination(null)
                    .build();
            when(vault.resolveSecret(any(), any())).thenReturn(null);

            var result = store.resolve(transferProcess);

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
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
