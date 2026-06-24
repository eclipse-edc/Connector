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
 * Provide the http request base path given the outgoing message.
 */
@FunctionalInterface
public interface RequestBasePathProvider {
    /**
     * Return the path
     *
     * @param message the message.
     * @return the path.
     */
    String provideBasePath(RemoteMessage message);
}
