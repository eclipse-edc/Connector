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

package org.eclipse.edc.catalog.spi.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.NodeQueryAdapter;
import org.eclipse.edc.spi.types.domain.asset.Asset;

/**
 * {@link NodeQueryAdapter}s return {@code UpdateResponse} objects after a
 * catalog query returns. Contains information about the {@code source} (i.e. where the response comes from) and the
 * {@code assetNames}.
 * <p>
 * <p>
 * TODO: This must be updated to contain a list of {@link Asset}s after https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/pull/159 has been merged!
 */
public class UpdateResponse {
    private final String source;
    private final Catalog catalog;

    @JsonCreator
    public UpdateResponse(@JsonProperty("source") String source, @JsonProperty("catalog") Catalog assetNames) {
        this.source = source;
        catalog = assetNames;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * The URL from which the catalog update originates, i.e. the catalog's sender
     */
    public String getSource() {
        return source;
    }
}
