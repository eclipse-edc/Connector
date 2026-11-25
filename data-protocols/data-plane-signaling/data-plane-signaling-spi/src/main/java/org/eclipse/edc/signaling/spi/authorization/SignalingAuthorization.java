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

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
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
     * @param headerGetter         a function that retrieves a header value by name
     * @param authorizationProfile the authorization profile assigned to the target Data Plane instance
     * @return a successful {@link Result} containing a token or principal on success, or a failed result with an error message
     */
    Result<String> isAuthorized(Function<String, String> headerGetter, AuthorizationProfile authorizationProfile);

    /**
     * Produces the outgoing authorization header value for a given {@link AuthorizationProfile}.
     *
     * @param authorizationProfile the profile describing the target endpoint's auth requirements
     * @return a successful {@link Result} containing the {@link Header} to attach, or a failed result
     */
    Result<Header> evaluate(AuthorizationProfile authorizationProfile);

    /**
     * Validates the authorization profile of a Data Plane instance that is being registered. A failure rejects the
     * registration. Implementations may log a warning for a profile that is valid but weakens the authorization.
     *
     * @param dataPlaneId          the id of the Data Plane instance
     * @param authorizationProfile the authorization profile of the Data Plane instance
     * @return a successful {@link Result} if the profile can be used, a failed result describing the problem otherwise
     */
    default Result<Void> validate(String dataPlaneId, AuthorizationProfile authorizationProfile) {
        return Result.success();
    }
}
