package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataEntryPropertyLookup;
import org.apache.atlas.model.instance.AtlasStruct;

import java.util.HashMap;
import java.util.Map;

public class AtlasDataEntryPropertyLookup extends DataEntryPropertyLookup {

    private final AtlasApi atlasApi;

    public AtlasDataEntryPropertyLookup(AtlasApi atlasApi) {
        this.atlasApi = atlasApi;
    }

    @Override
    public Map<String, Object> getPropertiesForEntity(String id) {
        var entity= atlasApi.getEntityById(id);

        if(entity != null){
            return entity.getAttributes();
        }

        return new HashMap<>();
    }
}
