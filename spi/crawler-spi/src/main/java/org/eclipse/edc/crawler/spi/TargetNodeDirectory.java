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
 * A global list of all {@link TargetNode} entries, that are available to crawl in a data space, much like a "phone book" for catalog endpoints.
 * Each {@link TargetNode} is queryable using a particular protocol.
 */
@ExtensionPoint
public interface TargetNodeDirectory {

    /**
     * Get all nodes.
     */
    List<TargetNode> getAll();

    /**
     * Inserts (="registers") a node into the directory.
     */
    void insert(TargetNode node);

    /**
     * Removes (="unregisters") a node from the directory by its ID.
     *
     * @param id ID of the node to be removed.
     * @return TargetNode containing the removed node if it was found and removed, otherwise null.
     */
    TargetNode remove(String id);
}
