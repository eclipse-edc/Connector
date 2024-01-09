/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.token.spi;

import java.util.Map;

/**
 * Defines a component that can be used to decorate a JWT token.
 * Both methods return a Map, that will contain keys belonging to the respective standards:
 * <ul>
 *     <li>{@linkplain #claims()} JWT Claims - <a href="https://auth0.com/docs/secure/tokens/json-web-tokens/json-web-token-claims">reference</a></li>
 *     <li>{@linkplain #headers()} JWS Header - <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1">reference</a></li>
 * </ul>
 * -
 * -
 */
public interface JwtDecorator {
    /**
     * Map of claims to be added to a token
     *
     * @return a Map of jwt claims, it should never be null
     */
    default Map<String, Object> claims() {
        return Map.of();
    }

    /**
     * Map of headers to be added to a token
     *
     * @return a Map of jws header parameters, it should never be null
     */
    default Map<String, Object> headers() {
        return Map.of();
    }

}
