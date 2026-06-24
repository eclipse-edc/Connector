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

package org.eclipse.edc.crawler.spi;

import java.util.Collection;

/**
 * Registry where {@link CrawlerAction} instances are stored and maintained.
 */
public interface CrawlerActionRegistry {

    /**
     * Finds a {@link CrawlerAction} that was registered for the given protocol name.
     *
     * @param protocolName An arbitrary String identifying the protocol.
     * @return A list of protocol adapters that can handle that protocol, or an empty list if none was found.
     */
    Collection<CrawlerAction> findForProtocol(String protocolName);

    /**
     * Registers a {@link CrawlerAction} for a given protocol
     */
    void register(String protocolName, CrawlerAction adapter);

    /**
     * Removes a specific {@link CrawlerAction} registration
     */
    void unregister(String protocolName, CrawlerAction adapter);
}
