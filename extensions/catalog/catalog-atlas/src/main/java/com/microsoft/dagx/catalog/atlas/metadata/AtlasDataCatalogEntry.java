/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataCatalogEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

public class AtlasDataCatalogEntry implements DataCatalogEntry {

    private final DataAddress address;

    public AtlasDataCatalogEntry(DataAddress address) {

        this.address = address;
    }

    @Override
    public DataAddress getAddress() {
        return address;
    }

}
