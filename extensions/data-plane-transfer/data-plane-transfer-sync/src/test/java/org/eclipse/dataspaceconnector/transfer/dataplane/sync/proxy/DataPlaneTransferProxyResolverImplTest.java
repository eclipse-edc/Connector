/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferProxyResolverImplTest {

    private static final Faker FAKER = new Faker();

    private DataPlaneSelectorClient selectorClient;
    private DataPlaneTransferProxyResolver resolver;

    @BeforeEach
    public void setUp() {
        selectorClient = mock(DataPlaneSelectorClient.class);
        resolver = new DataPlaneTransferProxyResolverImpl(selectorClient, FAKER.internet().uuid());
    }

    @Test
    void verifyResolveSuccess() {
        var address = DataAddress.Builder.newInstance().type(FAKER.internet().uuid()).build();
        var proxyUrl = FAKER.internet().url();
        var instance = DataPlaneInstanceImpl.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .url("http://" + FAKER.internet().url())
                .property("publicApiUrl", proxyUrl)
                .build();

        var srcAddressCaptor = ArgumentCaptor.forClass(DataAddress.class);
        var destAddressCaptor = ArgumentCaptor.forClass(DataAddress.class);

        when(selectorClient.find(srcAddressCaptor.capture(), destAddressCaptor.capture(), ArgumentCaptor.forClass(String.class).capture())).thenReturn(instance);

        var result = resolver.resolveProxyUrl(address);

        verify(selectorClient).find(any(), any(), any());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(proxyUrl);

        assertThat(srcAddressCaptor.getValue()).isEqualTo(address);
        assertThat(destAddressCaptor.getValue().getType()).isEqualTo("HttpProxy");
    }

    @Test
    void verifyFailedResultReturnedIfDataPlaneResolutionFails() {
        var address = DataAddress.Builder.newInstance().type(FAKER.internet().uuid()).build();

        when(selectorClient.find(any(), any(), any())).thenReturn(null);

        var result = resolver.resolveProxyUrl(address);

        verify(selectorClient).find(any(), any(), any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anySatisfy(s -> assertThat(s).contains(address.getType()));
    }

    @Test
    void verifyFailedResultReturnedIfDataPlaneInstanceDoesNotContainPublicApiUrl() {
        var address = DataAddress.Builder.newInstance().type(FAKER.internet().uuid()).build();
        var instance = DataPlaneInstanceImpl.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .url("http://" + FAKER.internet().url())
                .build();

        when(selectorClient.find(any(), any(), any())).thenReturn(instance);

        var result = resolver.resolveProxyUrl(address);

        verify(selectorClient).find(any(), any(), any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).contains("Missing property `publicApiUrl` in DataPlaneInstance");
    }
}