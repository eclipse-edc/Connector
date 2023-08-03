/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.spi.dispatcher;


import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Provide http request path given the outgoing message.
 *
 * @param <M> the message type
 */
@FunctionalInterface
public interface RequestPathProvider<M extends RemoteMessage> {
    /**
     * Return the path
     *
     * @param message the message.
     * @return the path.
     */
    String providePath(M message);
}
