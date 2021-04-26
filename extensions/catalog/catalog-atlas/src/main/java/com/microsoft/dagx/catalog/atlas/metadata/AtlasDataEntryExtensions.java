package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;

public class AtlasDataEntryExtensions extends DataEntryExtensions {

    private final AtlasApiImpl atlasApi;

    public AtlasDataEntryExtensions(AtlasApiImpl atlasApi) {
        this.atlasApi = atlasApi;
    }
}
