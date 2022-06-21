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

package org.eclipse.dataspaceconnector.catalog.spi.model;


import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;

/**
 * {@link NodeQueryAdapter}s accept {@code UpdateRequests} to send out catalog queries
 */
public class UpdateRequest {
    private final String nodeUrl;

    public UpdateRequest(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    public String getNodeUrl() {
        return nodeUrl;
    }
}
