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

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

/**
 * Extract the body from a http response body into a concrete type
 *
 * @param <RB> the type of the response body.
 * @param <B> the type of the extracted body.
 */
@FunctionalInterface
public interface ProtocolResponseBodyExtractor<RB, B> {

    ProtocolResponseBodyExtractor<Object, Object> NOOP = (r, p) -> null;

    /**
     * Extract the body from the Response
     *
     * @param responseBody the Response.
     * @return the body.
     */
    B extractBody(RB responseBody, String protocol);
}
