package com.microsoft.dagx.spi.types.domain.metadata;

import com.microsoft.dagx.spi.types.domain.Polymorphic;

import java.util.Map;

/**
 * Base extension point for data entries.
 */
public abstract class DataEntryPropertyLookup implements Polymorphic {
    public abstract Map<String, Object> getPropertiesForEntity(String id);
}
