/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core.discovery;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DidDiscoveryUrlResolverTest {

    private static final String DID = "did:web:participant";
    private static final String DATA_SERVICE_URL = "https://counter-party.example";

    private final DidResolverRegistry didResolverRegistry = mock();
    private final DidDiscoveryUrlResolver resolver = new DidDiscoveryUrlResolver(didResolverRegistry);

    @Test
    void canResolve_returnsTrueForDidCounterPartyId() {
        assertThat(resolver.canResolve(new DiscoveryRequest(DID, null))).isTrue();
    }

    @Test
    void canResolve_returnsTrueWhenAddressAlsoSetButIdIsDid() {
        assertThat(resolver.canResolve(new DiscoveryRequest(DID, "https://x.example"))).isTrue();
    }

    @Test
    void canResolve_returnsFalseWhenCounterPartyIdIsNull() {
        assertThat(resolver.canResolve(new DiscoveryRequest(null, "https://x.example"))).isFalse();
    }

    @Test
    void canResolve_returnsFalseWhenCounterPartyIdIsBlank() {
        assertThat(resolver.canResolve(new DiscoveryRequest("   ", null))).isFalse();
    }

    @Test
    void canResolve_returnsFalseWhenCounterPartyIdIsNotDid() {
        assertThat(resolver.canResolve(new DiscoveryRequest("https://counter-party.example", null))).isFalse();
    }

    @Test
    void canResolve_doesNotInvokeRegistry() {
        resolver.canResolve(new DiscoveryRequest(DID, null));

        verifyNoInteractions(didResolverRegistry);
    }

    @Test
    void resolve_returnsDataServiceEndpoint() {
        when(didResolverRegistry.resolve(eq(DID))).thenReturn(success(didDocumentWith(
                new Service("svc-1", "DataService", DATA_SERVICE_URL))));

        var result = resolver.resolve(new DiscoveryRequest(DID, null));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(DATA_SERVICE_URL);
    }

    @Test
    void resolve_picksFirstDataServiceWhenMultipleArePresent() {
        when(didResolverRegistry.resolve(eq(DID))).thenReturn(success(didDocumentWith(
                new Service("svc-1", "OtherService", "https://other.example"),
                new Service("svc-2", "DataService", DATA_SERVICE_URL),
                new Service("svc-3", "DataService", "https://second.example"))));

        var result = resolver.resolve(new DiscoveryRequest(DID, null));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(DATA_SERVICE_URL);
    }

    @Test
    void resolve_failsWithBadRequestWhenDidCannotBeResolved() {
        when(didResolverRegistry.resolve(eq(DID))).thenReturn(failure("DID method not supported"));

        var result = resolver.resolve(new DiscoveryRequest(DID, null));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        assertThat(result.getFailureDetail()).contains(DID).contains("DID method not supported");
    }

    @Test
    void resolve_failsWithNotFoundWhenDidDocumentHasNoServices() {
        when(didResolverRegistry.resolve(eq(DID))).thenReturn(success(didDocumentWith()));

        var result = resolver.resolve(new DiscoveryRequest(DID, null));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
        assertThat(result.getFailureDetail()).contains("DataService").contains(DID);
    }

    @Test
    void resolve_failsWithNotFoundWhenNoDataServiceEntryIsPresent() {
        when(didResolverRegistry.resolve(eq(DID))).thenReturn(success(didDocumentWith(
                new Service("svc-1", "OtherService", "https://other.example"))));

        var result = resolver.resolve(new DiscoveryRequest(DID, null));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
        assertThat(result.getFailureDetail()).contains("DataService");
    }

    private static DidDocument didDocumentWith(Service... services) {
        return DidDocument.Builder.newInstance()
                .id(DID)
                .service(List.of(services))
                .build();
    }
}
