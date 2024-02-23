/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.dataplane.spi;

import java.util.Map;

/**
 * Representation of a publicly accessible data egress point. This indicates to consumers the type of data (denoted by the {@code endpointType})
 * and an object containing fields describing the endpoint.
 * For HTTP this could be as simple as a {@code "url" -> "http://foo.bar"} entry, where for other protocols there could be
 * formatted and structured data.
 *
 * @param endpoint     An object describing the endpoint
 * @param endpointType A string uniquely identifying the type of endpoint
 */
public record Endpoint(Map<String, Object> endpoint, String endpointType) {

    /**
     * Convenience factory method to create a HTTP endpoint.
     *
     * @param url A URL containing the HTTP endpoint
     * @return the endpoint.
     */
    public static Endpoint url(String url) {
        return new Endpoint(Map.of("url", url), "https://w3id.org/idsa/v4.1/HTTP");
    }

}
