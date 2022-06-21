/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.Collection;

/**
 * Registry where {@link NodeQueryAdapter} instances are stored and maintained.
 */
public interface NodeQueryAdapterRegistry {

    /**
     * Finds a {@link NodeQueryAdapter} that was registered for the given protocol name.
     *
     * @param protocolName An arbitrary String identifying the protocol.
     * @return A list of protocol adapters that can handle that protocol, or an empty list if none was found.
     */
    Collection<NodeQueryAdapter> findForProtocol(String protocolName);

    /**
     * Registers a {@link NodeQueryAdapter} for a given protocol
     */
    void register(String protocolName, NodeQueryAdapter adapter);

    /**
     * Removes a specific {@link NodeQueryAdapter} registration
     */
    void unregister(String protocolName, NodeQueryAdapter adapter);
}
