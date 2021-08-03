/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.spi.types.domain.Polymorphic;
import org.eclipse.edc.spi.types.domain.transfer.DataAddress;

/**
 * Base extension point for data entries.
 */
@JsonTypeName("edc:datacatalogentry")
public interface DataCatalogEntry extends Polymorphic {
    DataAddress getAddress();
}
