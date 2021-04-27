package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataEntryPropertyLookup;
import org.apache.commons.lang.NotImplementedException;

import java.util.Map;

public class AtlasDataEntryPropertyLookup extends DataEntryPropertyLookup {

    private final AtlasApi atlasApi;

    public AtlasDataEntryPropertyLookup(AtlasApi atlasApi) {
        this.atlasApi = atlasApi;
    }

    @Override
    public Map<String, Object> getPropertiesForEntity(String id) {
        throw new NotImplementedException();
    }
}
