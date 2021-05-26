/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.catalog.atlas.dto.AtlasEntity;
import com.microsoft.dagx.catalog.atlas.dto.AtlasSearchResult;
import com.microsoft.dagx.catalog.atlas.dto.AtlasTypesDef;
import com.microsoft.dagx.schema.RelationshipSchema;
import com.microsoft.dagx.schema.SchemaAttribute;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AtlasApi {
    AtlasTypesDef createClassifications(String... classificationName);

    void deleteClassification(String classificationName);

    AtlasTypesDef createCustomTypes(String typeName, Set<String> superTypeNames, List<? extends SchemaAttribute> attributes);

    void deleteCustomType(String typeName);

    AtlasRelationship createRelation(String sourceEntityGuid, String targetEntityGuid, String name);

    void deleteType(List<AtlasTypesDef> type);

    AtlasEntity.AtlasEntityWithExtInfo getEntityById(String id);

    String createEntity(String entityTypeName, Map<String, Object> properties);

    void deleteEntities(List<String> entityGuids);

    AtlasTypesDef createRelationshipType(String name, String description, int relationshipCategory, RelationshipSchema.EndpointDefinition startDefinition, RelationshipSchema.EndpointDefinition endDefinition);

    AtlasTypesDef getAllTypes(String name);

    AtlasSearchResult dslSearchWithParams(String from_policy, int i, int i1);
}