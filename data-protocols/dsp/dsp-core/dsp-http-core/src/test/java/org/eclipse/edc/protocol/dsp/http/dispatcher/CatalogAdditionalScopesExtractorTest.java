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

package org.eclipse.edc.protocol.dsp.http.dispatcher;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class CatalogAdditionalScopesExtractorTest {

    private final CatalogAdditionalScopesExtractor extractor = new CatalogAdditionalScopesExtractor();

    @Test
    void shouldReturnAdditionalScopes_whenCatalogRequestMessage() {
        var message = CatalogRequestMessage.Builder.newInstance().protocol("protocol").additionalScopes("scope1", "scope2").build();
        var context = RequestContext.Builder.newInstance().message(message).direction(RequestContext.Direction.Egress).build();

        var result = extractor.extractScopes(Policy.Builder.newInstance().build(), context);

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).containsExactlyInAnyOrder("scope1", "scope2"));
    }

    @Test
    void shouldReturnEmpty_whenNotCatalogRequestMessage() {
        var context = RequestContext.Builder.newInstance().message(mock(ProtocolRemoteMessage.class)).direction(RequestContext.Direction.Egress).build();

        var result = extractor.extractScopes(Policy.Builder.newInstance().build(), context);

        assertThat(result).isSucceeded().satisfies(scopes -> assertThat(scopes).isEmpty());
    }
}
