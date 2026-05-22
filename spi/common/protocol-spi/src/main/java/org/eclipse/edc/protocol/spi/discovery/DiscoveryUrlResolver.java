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

package org.eclipse.edc.protocol.spi.discovery;

import org.eclipse.edc.spi.result.ServiceResult;

public interface DiscoveryUrlResolver {

    /**
     * Resolves the given discovery request to a URL that can be used to fetch the counterparty's
     * {@code /.well-known/dspace-version} document.
     *
     * @param request the discovery request (DID or address).
     * @return the discovery result; must not be null.
     */
    ServiceResult<String> resolve(DiscoveryRequest request);

    /**
     * Determines whether this resolver can handle the given discovery request.
     *
     * @param request the discovery request (DID or address).
     * @return true if this resolver can handle the request; false otherwise.
     */
    boolean canResolve(DiscoveryRequest request);
}
