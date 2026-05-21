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

import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryUrlResolver;
import org.eclipse.edc.spi.result.ServiceResult;

public class DidDiscoveryUrlResolver implements DiscoveryUrlResolver {

    private static final String DSP_SERVICE_TYPE = "DataService";
    private final DidResolverRegistry didResolverRegistry;

    public DidDiscoveryUrlResolver(DidResolverRegistry didResolverRegistry) {
        this.didResolverRegistry = didResolverRegistry;
    }

    @Override
    public ServiceResult<String> resolve(DiscoveryRequest request) {
        return resolveDidToDataServiceEndpoint(request.counterPartyId());
    }

    @Override
    public boolean canResolve(DiscoveryRequest request) {
        return request.counterPartyId() != null && !request.counterPartyId().isBlank() && request.counterPartyId().startsWith("did:");
    }

    private ServiceResult<String> resolveDidToDataServiceEndpoint(String did) {
        var didDocument = didResolverRegistry.resolve(did);
        if (didDocument.failed()) {
            return ServiceResult.badRequest("Failed to resolve DID '%s': %s".formatted(did, didDocument.getFailureDetail()));
        }
        return didDocument.getContent().getService().stream()
                .filter(s -> DSP_SERVICE_TYPE.equals(s.getType()))
                .findFirst()
                .map(s -> ServiceResult.success(s.getServiceEndpoint()))
                .orElseGet(() -> ServiceResult.notFound("No '%s' service endpoint in DID document for '%s'".formatted(DSP_SERVICE_TYPE, did)));
    }

}
