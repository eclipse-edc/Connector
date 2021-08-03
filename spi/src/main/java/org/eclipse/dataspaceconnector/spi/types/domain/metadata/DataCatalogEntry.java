/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.Polymorphic;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

/**
 * Base extension point for data entries.
 */
@JsonTypeName("dataspaceconnector:datacatalogentry")
public interface DataCatalogEntry extends Polymorphic {
    DataAddress getAddress();
}
