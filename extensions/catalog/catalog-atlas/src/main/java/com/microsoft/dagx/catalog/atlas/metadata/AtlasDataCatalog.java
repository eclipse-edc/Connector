/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataCatalog;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

public class AtlasDataCatalog implements DataCatalog {

    private final DataAddress address;

    public AtlasDataCatalog(DataAddress address) {

        this.address = address;
    }

    @Override
    public DataAddress getPropertiesForEntity(String id) {
        return address;
    }

}
