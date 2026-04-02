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

package org.eclipse.edc.signaling.spi.authorization;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.spi.result.Result;

import java.util.function.Function;

/**
 * Represents an authorization strategy for a specific signaling authentication scheme.
 * Implementations are identified by a type string (e.g. {@code "oauth2"}) and registered
 * in a {@link SignalingAuthorizationRegistry}.
 */
public interface SignalingAuthorization {

    /**
     * Returns the type identifier for this authorization strategy (e.g. {@code "oauth2"}).
     */
    String getType();

    /**
     * Verifies that an incoming request is authorized by inspecting its headers.
     *
     * @param headerGetter a function that retrieves a header value by name
     * @return a successful {@link Result} containing a token or principal on success, or a failed result with an error message
     */
    Result<String> isAuthorized(Function<String, String> headerGetter);

    /**
     * Produces the outgoing authorization header value for a given {@link AuthorizationProfile}.
     *
     * @param authorizationProfile the profile describing the target endpoint's auth requirements
     * @return a successful {@link Result} containing the {@link Header} to attach, or a failed result
     */
    Result<Header> evaluate(AuthorizationProfile authorizationProfile);
}
