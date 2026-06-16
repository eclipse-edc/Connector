/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class HashicorpVaultTokenProviderFactoryImplTest {

    private HashicorpVaultTokenProviderFactoryImpl.Builder factoryBuilder() {
        return HashicorpVaultTokenProviderFactoryImpl.Builder.newInstance();
    }

    private HashicorpVaultTokenProviderFactoryImpl.Builder exchangeBuilder() {
        return factoryBuilder()
                .tokenExchangeUrl("http://jwtlet:8080")
                .subjectTokenPath("/var/run/secrets/token")
                .scope("read")
                .audience("edcv")
                .role("participant")
                .vaultUrl("http://vault:8200")
                .httpClient(mock())
                .objectMapper(new ObjectMapper());
    }

    @Test
    void staticTokenOnly_returnsStaticProviderForAllPartitions() {
        var factory = factoryBuilder().staticToken("root").build();

        assertThat(factory.create(null)).isInstanceOf(HashicorpVaultTokenProviderImpl.class)
                .extracting(provider -> provider.vaultToken()).isEqualTo("root");
        assertThat(factory.create("participant-1")).isInstanceOf(HashicorpVaultTokenProviderImpl.class);
    }

    @Test
    void tokenExchange_returnsExchangeProviderForNamedPartition() {
        var factory = exchangeBuilder().build();

        assertThat(factory.create("participant-1")).isInstanceOf(HashicorpTokenExchangeProvider.class);
    }

    @Test
    void tokenExchangeWithStaticToken_usesStaticForDefaultPartition() {
        var factory = exchangeBuilder().staticToken("root").build();

        assertThat(factory.create(null)).isInstanceOf(HashicorpVaultTokenProviderImpl.class);
        assertThat(factory.create("participant-1")).isInstanceOf(HashicorpTokenExchangeProvider.class);
    }

    @Test
    void create_cachesProviderPerPartition() {
        var factory = exchangeBuilder().build();

        assertThat(factory.create("participant-1")).isSameAs(factory.create("participant-1"));
        assertThat(factory.create("participant-1")).isNotSameAs(factory.create("participant-2"));
    }

    @Test
    void build_whenNothingConfigured_throws() {
        assertThatThrownBy(() -> factoryBuilder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either a static vault token or token-exchange configuration must be provided");
    }

    @Test
    void create_whenNoStaticAndNoExchange_isPreventedByBuild() {
        // a factory with only a static token cannot produce exchange providers, but never throws for named partitions
        var factory = factoryBuilder().staticToken("root").build();
        assertThat(factory.create("participant-1")).isNotNull();
    }

    @Test
    void create_defaultPartitionWithExchangeOnly_usesDefaultResource() {
        var factory = exchangeBuilder().defaultResource("default-participant").build();

        assertThat(factory.create(null)).isInstanceOf(HashicorpTokenExchangeProvider.class);
    }
}
