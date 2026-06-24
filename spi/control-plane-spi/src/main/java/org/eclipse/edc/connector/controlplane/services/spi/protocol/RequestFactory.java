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

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Creates an HTTP request for the DSP HTTP Bindings given the message instance
 *
 * @param <M> the message type.
 * @param <R> the request type.
 */
@FunctionalInterface
public interface RequestFactory<M extends RemoteMessage, R> {

    /**
     * Create the request given the message and a {@link RequestPathProvider}
     *
     * @param message the message.
     * @return the request.
     */
    R createRequest(M message);
}
