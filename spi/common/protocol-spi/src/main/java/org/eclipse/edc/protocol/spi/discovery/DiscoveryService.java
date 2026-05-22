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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Discovers the dataspace profiles that a participant can use to communicate with a counter * party. The implementation fetches the counterparty's {@code /.well-known/dspace-version}
 * document (either via direct address or via DID resolution) and intersects the advertised DSP
 * protocol versions with the profiles associated with the participant context.
 */
@ExtensionPoint
public interface DiscoveryService {

    /**
     * Returns one {@link DiscoveryResponse} per (local profile, counterparty version) pair that
     * shares the same {@code version} and {@code binding}.
     *
     * @param participantContextId the participant context performing the discovery.
     * @param request              the discovery request (DID or address).
     * @return the list of matches; empty when nothing matches.
     */
    ServiceResult<List<DiscoveryResponse>> discover(String participantContextId, DiscoveryRequest request);

    /**
     * Registers a {@link DiscoveryUrlResolver} to be used for resolving discovery requests.
     *
     * @param resolver the resolver to register.
     */
    void registerResolver(DiscoveryUrlResolver resolver);
}
