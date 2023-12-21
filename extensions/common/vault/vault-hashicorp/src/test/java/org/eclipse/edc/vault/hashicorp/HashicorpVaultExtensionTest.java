/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponsePayloadToken;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewalResponsePayloadToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_URL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HashicorpVaultExtensionTest {

    private static final String DEFAULT_POLICY = "default";

    private HashicorpVaultExtension extension;
    private final ExecutorInstrumentation executorInstrumentation = mock();
    private final EdcHttpClient httpClient = mock();

    @BeforeAll
    void beforeAll(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(TypeManager.class, mock(TypeManager.class));
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);
        extension = factory.constructInstance(HashicorpVaultExtension.class);
    }

    @BeforeEach
    void beforeEach(ObjectFactory ignored, ServiceExtensionContext context) {
        when(context.getSetting(VAULT_URL, null)).thenReturn("foo");
        when(context.getSetting(VAULT_TOKEN, null)).thenReturn("foo");
    }

    @Nested
    class Initialize {

        @Test
        void initialize_whenVaultUrlUndefined_shouldThrowEdcException(ServiceExtensionContext context) {
            when(context.getSetting(VAULT_URL, null)).thenReturn(null);

            assertThrows(EdcException.class, () -> extension.initialize(context));
        }

        @Test
        void initialize_whenVaulTokenUndefined_shouldThrowEdcException(ServiceExtensionContext context) {
            when(context.getSetting(VAULT_TOKEN, null)).thenReturn(null);

            assertThrows(EdcException.class, () -> extension.initialize(context));
        }

        @Test
        void initialize_whenTokenLookUpFailed_shouldThrowEdcException(ServiceExtensionContext context) {
            mockClient(
                    (client, mockContext) -> when(client.lookUpToken()).thenReturn(Result.failure("403"))
            ).accept(
                    clientConstruction -> {
                        extension.initialize(context);
                        var thrown = assertThrows(EdcException.class,
                                () -> extension.start(),
                                "Expected token look up to throw");
                        assertThat(thrown.getMessage()).isEqualTo("[Hashicorp Vault Extension] Initial token look up failed: 403");
                        var client = clientConstruction.constructed().get(0);
                        verify(client, never()).scheduleNextTokenRenewal(anyLong());
                    });
        }

        @Test
        void initialize_whenTokenRenewalFailed_shouldThrowEdcException(ServiceExtensionContext context) {
            var tokenLookUpResponse = getTokenLookUpResponse(true);

            mockClient(
                    (client, mockContext) -> {
                        when(client.lookUpToken()).thenReturn(Result.success(tokenLookUpResponse));
                        when(client.renewToken()).thenReturn(Result.failure("403"));
                    }
            ).accept(
                    clientConstruction -> {
                        extension.initialize(context);

                        var thrown = assertThrows(EdcException.class,
                                () -> extension.start(),
                                "Expected token renewal to throw");
                        assertThat(thrown.getMessage()).isEqualTo("[Hashicorp Vault Extension] Initial token renewal failed: 403");
                        var client = clientConstruction.constructed().get(0);
                        verify(client, never()).scheduleNextTokenRenewal(anyLong());
                    });
        }
    }

    @Nested
    class Start {
        @Test
        void start_whenTokenIsValidAndRenewable_shouldScheduleNextTokenRenewal(ServiceExtensionContext context) {
            var tokenLookUpResponse = getTokenLookUpResponse(true);
            var renewTokenResponse = getRenewTokenResponse();

            mockClient(
                    (client, mockContext) -> {
                        when(client.lookUpToken()).thenReturn(Result.success(tokenLookUpResponse));
                        when(client.renewToken()).thenReturn(Result.success(renewTokenResponse));
                    }
            ).accept(
                    clientConstruction -> {
                        extension.initialize(context);
                        extension.start();
                        var client = clientConstruction.constructed().get(0);
                        verify(client).renewToken();
                        verify(client).scheduleNextTokenRenewal(eq(renewTokenResponse.getTimeToLive()));
                    }
            );
        }

        @Test
        void start_whenTokenIsValidAndNotRenewable_shouldNotScheduleNextTokenRenewal(ServiceExtensionContext context) {
            var tokenLookUpResponse = getTokenLookUpResponse(false);

            mockClient(
                    (client, mockContext) -> {
                        when(client.lookUpToken()).thenReturn(Result.success(tokenLookUpResponse));
                    }
            ).accept(
                    clientConstruction -> {
                        extension.initialize(context);
                        extension.start();
                        var client = clientConstruction.constructed().get(0);
                        verify(client, never()).renewToken();
                        verify(client, never()).scheduleNextTokenRenewal(anyLong());
                    }
            );
        }
    }

    @Nested
    class Shutdown {

        @Test
        void shutdown_shouldShutdownScheduledExecutorService(ServiceExtensionContext context) {
            var scheduledExecutorService = mock(ScheduledExecutorService.class);
            when(executorInstrumentation.instrument(any(ScheduledExecutorService.class), eq(extension.name()))).thenReturn(scheduledExecutorService);
            var tokenLookUpResponse = getTokenLookUpResponse(false);
            mockClient(
                    (client, mockContext) -> {
                        when(client.lookUpToken()).thenReturn(Result.success(tokenLookUpResponse));
                    }
            ).accept(
                    clientConstruction -> {
                        extension.initialize(context);
                        extension.shutdown();
                        verify(scheduledExecutorService).shutdownNow();
                    }
            );
        }
    }

    private static TokenLookUpResponsePayloadToken getTokenLookUpResponse(boolean isRenewable) {
        return TokenLookUpResponsePayloadToken.Builder.newInstance()
                .isRenewable(isRenewable)
                .policies(List.of(DEFAULT_POLICY))
                .build();
    }

    private static TokenRenewalResponsePayloadToken getRenewTokenResponse() {
        return TokenRenewalResponsePayloadToken.Builder.newInstance()
                .timeToLive(100L)
                .build();
    }

    private static MockedClient mockClient(MockedConstruction.MockInitializer<HashicorpVaultClient> mockInitializer) {
        return new MockedClient(mockInitializer);
    }

    /**
     * Helper class which allows to capture the construction of {@link HashicorpVaultClient} inside private method
     * {@link HashicorpVaultExtension#initHashicorpVaultClient(HashicorpVaultConfig, Monitor)}.
     *
     * @param mockInitializer defines how the mocked client should behave
     */
    private record MockedClient(MockedConstruction.MockInitializer<HashicorpVaultClient> mockInitializer) implements Consumer<Consumer<MockedConstruction<HashicorpVaultClient>>> {

        @Override
        public void accept(Consumer<MockedConstruction<HashicorpVaultClient>> consumer) {
            try (var clientConstruction = mockConstruction(HashicorpVaultClient.class, mockInitializer)) {
                consumer.accept(clientConstruction);
            }
        }
    }

}
