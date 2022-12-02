/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.transfer.sync.proxy;

import org.eclipse.edc.connector.dataplane.spi.DataPlanePublicApiUrl;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddedPlaneTransferProxyResolverTest {


    private DataPlanePublicApiUrl dataPlanePublicApiUrl;
    private DataPlaneTransferProxyResolver resolver;

    @BeforeEach
    public void setUp() {
        dataPlanePublicApiUrl = mock(DataPlanePublicApiUrl.class);
        resolver = new EmbeddedDataPlaneTransferProxyResolve(dataPlanePublicApiUrl);
    }

    @Test
    void verifyResolveSuccess() throws MalformedURLException {
        var address = DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
        var proxyUrl = new URL("http://test.proxy.url");

        when(dataPlanePublicApiUrl.get()).thenReturn(proxyUrl);

        var result = resolver.resolveProxyUrl(address);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(proxyUrl.toString());
    }


}
