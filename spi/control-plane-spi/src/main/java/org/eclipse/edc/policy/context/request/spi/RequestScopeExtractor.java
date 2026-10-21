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

package org.eclipse.edc.policy.context.request.spi;

import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Extracts the set of DCP request scopes for a request, given its {@link Policy} and {@link RequestContext}.
 * <p>
 * An extractor <em>returns</em> the scopes it computes, so the data flow is explicit. A failing extraction is reported
 * through the {@link Result} rather than through a side channel. A blocking failure should fail the whole request;
 * non-blocking extractors should recover to an empty success instead of returning a failure.
 * <p>
 * Extractors are registered per {@link ProtocolRemoteMessage} type in the {@link RequestScopeExtractorRegistry} and
 * invoked for every request whose message is assignable to the registered type.
 */
@FunctionalInterface
public interface RequestScopeExtractor {

    /**
     * Extract the request scopes.
     *
     * @param policy  the policy associated with the request (may be ignored by request-level extractors).
     * @param context the request context, carrying the {@link ProtocolRemoteMessage}, the direction and the
     *                participant context id.
     * @return the set of scopes if succeeded, otherwise a failure.
     */
    Result<Set<String>> extractScopes(Policy policy, RequestContext context);
}
