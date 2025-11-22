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

package org.eclipse.edc.iam.decentralizedclaims.service;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DidCredentialServiceUrlResolverTest {

    public static final String CREDENTIAL_SERVICE_URL = "https://foo.bar/credentialservice";
    private final DidResolverRegistry registryMock = mock();
    private final DidCredentialServiceUrlResolver resolver = new DidCredentialServiceUrlResolver(registryMock);

    @BeforeEach
    void setup() {
        when(registryMock.resolve(any())).thenReturn(success(createDid().build()));
    }

    @Test
    void resolve() {
        assertThat(resolver.resolve("did:web:participant")).isSucceeded().isEqualTo(CREDENTIAL_SERVICE_URL);
    }

    @Test
    void resolve_didNotSupported() {
        when(registryMock.resolve(any())).thenReturn(failure("DID method not supported"));
        assertThat(resolver.resolve("did:web:participant"))
                .isFailed()
                .detail().isEqualTo("DID method not supported");
    }

    @Test
    void resolve_didContainsNoServices() {
        var did = createDid().build();
        did.getService().clear();
        when(registryMock.resolve(any())).thenReturn(success(did));
        assertThat(resolver.resolve("did:web:participant"))
                .isFailed()
                .detail().isEqualTo("No Service endpoint 'CredentialService' found on DID Document.");
    }

    @Test
    void resolve_serviceNotFound() {
        var did = createDid().build();
        did.getService().clear();
        did.getService().add(new Service("foo", "bar", "https://foo.bar"));
        when(registryMock.resolve(any())).thenReturn(success(did));
        assertThat(resolver.resolve("did:web:participant"))
                .isFailed()
                .detail().isEqualTo("No Service endpoint 'CredentialService' found on DID Document.");
    }

    private DidDocument.Builder createDid() {
        return DidDocument.Builder.newInstance()
                .id("test-did")
                .service(List.of(new Service("test-service", "CredentialService", CREDENTIAL_SERVICE_URL)));
    }
}