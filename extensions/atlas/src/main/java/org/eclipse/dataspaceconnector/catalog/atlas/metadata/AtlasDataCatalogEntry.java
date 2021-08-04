/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
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
