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

package org.eclipse.edc.connector.core.discovery;

import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryUrlResolver;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Objects;

public class DefaultUrlResolver implements DiscoveryUrlResolver {

    static final String WELL_KNOWN_PATH = "/.well-known/dspace-version";

    @Override
    public ServiceResult<String> resolve(DiscoveryRequest request) {
        return ServiceResult.success(toWellKnownUrl(Objects.requireNonNull(request.counterPartyAddress())));
    }

    @Override
    public boolean canResolve(DiscoveryRequest request) {
        return request.counterPartyAddress() != null && !request.counterPartyAddress().isBlank() && request.counterPartyId() == null;
    }

    private String toWellKnownUrl(String baseUrl) {
        var trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed.endsWith(WELL_KNOWN_PATH) ? trimmed : trimmed + WELL_KNOWN_PATH;
    }
}
