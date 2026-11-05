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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.List;

/**
 * A list of all {@link TargetNode} entries, that are available to crawl in a data space, much like a "phone book" for catalog endpoints.
 * Each {@link TargetNode} is queryable using a particular protocol and is scoped to the local participant context on whose behalf it is crawled.
 */
@ExtensionPoint
public interface TargetNodeDirectory {

    /**
     * Get all nodes registered for the given participant context.
     *
     * @param participantContextId the local participant context whose nodes should be returned
     */
    List<TargetNode> getAll(String participantContextId);

    /**
     * Inserts (="registers") a node into the directory. The node carries the owning participant context.
     */
    void insert(TargetNode node);

    /**
     * Removes (="unregisters") a node from the directory by its participant context and ID.
     *
     * @param participantContextId the local participant context that owns the node.
     * @param id                   ID of the node to be removed.
     * @return TargetNode containing the removed node if it was found and removed, otherwise null.
     */
    TargetNode remove(String participantContextId, String id);
}
