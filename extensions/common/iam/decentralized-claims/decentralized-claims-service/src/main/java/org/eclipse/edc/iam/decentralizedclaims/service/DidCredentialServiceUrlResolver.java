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

import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Resolves the URL of the credential service based on the issuer's DID document.
 */
public class DidCredentialServiceUrlResolver implements CredentialServiceUrlResolver {
    private static final String CREDENTIAL_SERVICE_TYPE = "CredentialService";
    private final DidResolverRegistry didResolverRegistry;

    public DidCredentialServiceUrlResolver(DidResolverRegistry didResolverRegistry) {
        this.didResolverRegistry = didResolverRegistry;
    }

    /**
     * Resolves the DCP credential service URL from the DID document based on the issuer. The issuer is interpreted as DID
     * identifier, and the resolved DID is expected to contain a "CredentialServiceUrl" service endpoint.
     *
     * @param issuer The issuer of the DID document.
     * @return The result containing the service URL if found, or a failure if the DID was not resolvable, or if the required service endpoint wasn't found.
     */
    @Override
    public Result<String> resolve(String issuer) {
        var didDocument = didResolverRegistry.resolve(issuer);
        if (didDocument.failed()) {
            return didDocument.mapFailure();
        }
        return didDocument.getContent().getService().stream()
                .filter(s -> s.getType().equals(CREDENTIAL_SERVICE_TYPE))
                .findFirst()
                .map(service -> success(service.getServiceEndpoint()))
                .orElseGet(() -> failure("No Service endpoint '%s' found on DID Document.".formatted(CREDENTIAL_SERVICE_TYPE)));
    }
}
