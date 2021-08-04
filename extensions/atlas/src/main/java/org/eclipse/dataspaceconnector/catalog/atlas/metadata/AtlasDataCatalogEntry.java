/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.catalog.atlas.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataCatalogEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

@JsonTypeName("dataspaceconnector:atlascatalogentry")
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
