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

import org.eclipse.dataspaceconnector.spi.system.Feature;

import java.util.List;

/**
 * A global list of all FederatedCacheNodes that are available in a data space, much like a "phone book" for catalog endpoints.
 */
@Feature(FederatedCacheNodeDirectory.FEATURE)
public interface FederatedCacheNodeDirectory {
    String FEATURE = "edc:catalog:node-directory";

    /**
     * Get all nodes.
     */
    List<FederatedCacheNode> getAll();

    /**
     * Inserts (="registers") a node into the directory.
     */
    void insert(FederatedCacheNode node);
}
