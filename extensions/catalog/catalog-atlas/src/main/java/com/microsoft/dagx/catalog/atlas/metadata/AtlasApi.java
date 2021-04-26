package com.microsoft.dagx.catalog.atlas.metadata;

import org.apache.atlas.model.typedef.AtlasTypesDef;

import java.util.List;
import java.util.Set;

public interface AtlasApi {
    AtlasTypesDef createClassifications(String... classificationName);

    void deleteType(List<AtlasTypesDef> classificationTypes);

    AtlasTypesDef createTypesDef(String typeKeyName, Set<String> superTypeNames, List<TypeAttribute> attributes);
}
