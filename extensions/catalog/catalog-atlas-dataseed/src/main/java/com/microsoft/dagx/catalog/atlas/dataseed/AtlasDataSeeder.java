/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.schema.DataSchema;
import com.microsoft.dagx.schema.RelationshipSchema;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.schema.azure.AzureBlobHasPolicyRelationshipSchema;
import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.schema.policy.PolicySchema;
import com.microsoft.dagx.spi.DagxException;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AtlasDataSeeder {
    private final AtlasApi atlasApi;
    private final SchemaRegistry schemaRegistry;

    public AtlasDataSeeder(AtlasApi atlasApi, SchemaRegistry schemaRegistry) {

        this.atlasApi = atlasApi;
        this.schemaRegistry = schemaRegistry;
    }

    public String[] createClassifications() {
        var mapper = new ObjectMapper();
        try {
            Map<String, List<String>> classifications = mapper.readValue(getClass().getClassLoader().getResourceAsStream("classifications.json"), Map.class);
            String[] classificationNames = classifications.keySet().stream().flatMap(key -> classifications.get(key).stream()).toArray(String[]::new);
            atlasApi.createClassifications(classificationNames);
            return classificationNames;

        } catch (DagxException ignored) {
        } catch (IOException e) {
            throw new DagxException(e);
        }
        return null;
    }

    public List<AtlasTypesDef> createTypedefs() {
        List<AtlasTypesDef> entityTypes = new ArrayList<>();

        // create all entity types
        var schemas = schemaRegistry.getSchemas();
        for (var schema : schemas.stream().filter(s -> s instanceof DataSchema).collect(Collectors.toList())) {
            String sanitizedName = schema.getName();
            try {
                entityTypes.add(atlasApi.createCustomTypes(sanitizedName, Set.of("DataSet"), new ArrayList<>(schema.getAttributes())));
            } catch (DagxException ignored) {
            }
        }

        //create all relations
        for (RelationshipSchema relationshipSchema : schemas.stream().filter(s -> s instanceof RelationshipSchema).map(s -> (RelationshipSchema) s).collect(Collectors.toList())) {
            try {
                AtlasTypesDef td = atlasApi.createRelationshipType(relationshipSchema.getName(), relationshipSchema.getDescription(), relationshipSchema.getRelationshipCategory(), relationshipSchema.getStartDefinition(), relationshipSchema.getEndDefinition());
                entityTypes.add(td);
            } catch (DagxException ignored) {
            }

        }

        return entityTypes;
    }

    public List<String> createEntities() {
        try {
            ArrayList<String> entityGuids = new ArrayList<>();

            String entityId = atlasApi.createEntity(AzureBlobStoreSchema.TYPE, AzureBlobFileEntityBuilder.newInstance()
                    .withAccount("dagxblobstoreitest")
                    .withBlobname("testimage.jpg")
                    .withContainer("testcontainer")
                    .withKeyName("dagxblobstoreitest-key1")
                    .withDescription("this is an entity")
                    .build());
            entityGuids.add(entityId);

            String entity2Id = atlasApi.createEntity(AzureBlobStoreSchema.TYPE, AzureBlobFileEntityBuilder.newInstance()
                    .withAccount("dagxblobstoreitest")
                    .withBlobname("anotherimage.jpg")
                    .withContainer("testcontainer")
                    .withKeyName("dagxblobstoreitest-key1")
                    .withDescription("this is a second entity")
                    .build());
            entityGuids.add(entity2Id);

            var policyId = atlasApi.createEntity(PolicySchema.TYPE, new HashMap<>() {{
                put("name", "RegionalPolicy");
                put("type", PolicySchema.TYPE);
                put("keyName", "foobar");
                put("description", "this is a policy");
                put("qualifiedName", "entity-policy-relation ");
                put("serialized", "foo-bar-baz");
            }});
            entityGuids.add(policyId);

            var relation = atlasApi.createRelation(entityId, policyId, AzureBlobHasPolicyRelationshipSchema.TYPE);
            entityGuids.add(relation.getGuid());

            var relation2 = atlasApi.createRelation(entity2Id, policyId, AzureBlobHasPolicyRelationshipSchema.TYPE);
            entityGuids.add(relation2.getGuid());
            return entityGuids;

        } catch (Exception e) {
            throw new DagxException(e);
        }
    }

    public void deleteEntities(List<String> guids) {
        if (guids != null && !guids.isEmpty()) {
            atlasApi.deleteEntities(guids);
        }
    }

    public void deleteClassifications(String... classificationNames) {
        if (classificationNames != null) {
            atlasApi.deleteClassification(classificationNames);
        }
    }

    public void deleteEntityTypes(List<AtlasTypesDef> entityTypes) {
        if (entityTypes != null && !entityTypes.isEmpty()) {
            atlasApi.deleteType(entityTypes);
        }
    }


}
