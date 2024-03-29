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

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

/**
 * Keeps track of all the dataspace protocol versions.
 */
public interface ProtocolVersionRegistry {

    /**
     * Register a protocol version
     *
     * @param protocolVersion the protocol version.
     */
    void register(ProtocolVersion protocolVersion);

    /**
     * get all the protocol versions.
     *
     * @return the protocol versions.
     */
    ProtocolVersions getAll();
}
