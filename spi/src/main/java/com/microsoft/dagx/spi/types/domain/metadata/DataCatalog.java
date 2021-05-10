/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.metadata;

import com.microsoft.dagx.spi.types.domain.Polymorphic;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

/**
 * Base extension point for data entries.
 */
public interface DataCatalog extends Polymorphic {
    DataAddress getPropertiesForEntity(String id);
}
