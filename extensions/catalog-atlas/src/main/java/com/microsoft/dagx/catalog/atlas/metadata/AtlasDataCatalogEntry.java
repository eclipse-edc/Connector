/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.metadata.DataCatalogEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

@JsonTypeName("dagx:atlascatalogentry")
public class AtlasDataCatalogEntry implements DataCatalogEntry {

    @JsonProperty
    private final DataAddress address;

    public AtlasDataCatalogEntry(@JsonProperty("address") DataAddress address) {

        this.address = address;
    }

    @Override
    public DataAddress getAddress() {
        return address;
    }

}
