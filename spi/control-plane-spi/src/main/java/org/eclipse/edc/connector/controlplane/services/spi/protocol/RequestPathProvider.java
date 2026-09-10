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


import org.eclipse.edc.controlplane.ProtocolRemoteMessage;

/**
 * Provide http request path given the outgoing message.
 *
 * @param <M> the message type
 */
@FunctionalInterface
public interface RequestPathProvider<M extends ProtocolRemoteMessage> {
    /**
     * Return the path
     *
     * @param message the message.
     * @return the path.
     */
    String providePath(M message);
}
