package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataEntryPropertyLookup;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

import java.util.Map;
import java.util.stream.Collectors;

public class AtlasDataEntryPropertyLookup implements DataEntryPropertyLookup {

    private final AtlasApi atlasApi;
    private static final String ATLAS_PROPERTY_KEYNAME = "keyName";
    private static final String ATLAS_PROPERTY_TYPE = "type";

    public AtlasDataEntryPropertyLookup(AtlasApi atlasApi) {
        this.atlasApi = atlasApi;
    }

    @Override
    public DataAddress getPropertiesForEntity(String id) {
        var entity = atlasApi.getEntityById(id);

        if (entity != null) {
            return DataAddress.Builder.newInstance()
                    .keyName(entity.getAttribute(ATLAS_PROPERTY_KEYNAME).toString())
                    .type(entity.getAttribute(ATLAS_PROPERTY_TYPE).toString())
                    .properties(convert(entity.getAttributes()))
                    .build();
        }

        return null;
    }

    private Map<String, String> convert(Map<String, Object> attributes) {
        return attributes.entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().toString()));
    }
}
