/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.dagx.spi.types.domain.Polymorphic;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

/**
 * Base extension point for data entries.
 */
@JsonTypeName("dagx:datacatalogentry")
public interface DataCatalogEntry extends Polymorphic {
    DataAddress getAddress();
}
