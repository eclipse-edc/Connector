/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataseed.atlas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasTypesDef;
import org.eclipse.dataspaceconnector.catalog.atlas.metadata.AtlasApi;
import org.eclipse.dataspaceconnector.schema.RelationshipSchema;
import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.schema.azure.AzureBlobHasPolicyRelationshipSchema;
import org.eclipse.dataspaceconnector.schema.azure.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.schema.policy.PolicySchema;
import org.eclipse.dataspaceconnector.schema.s3.AmazonS3HasPolicyRelationshipSchema;
import org.eclipse.dataspaceconnector.schema.s3.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;

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

        } catch (EdcException ignored) {
        } catch (IOException e) {
            throw new EdcException(e);
        }
        return null;
    }

    public List<AtlasTypesDef> createTypedefs() {
        List<AtlasTypesDef> entityTypes = new ArrayList<>();

        // create all entity types
        var schemas = schemaRegistry.getSchemas();
        for (var schema : schemas.stream().filter(s -> !(s instanceof RelationshipSchema)).collect(Collectors.toList())) {
            String sanitizedName = schema.getName();
            try {
                entityTypes.add(atlasApi.createCustomTypes(sanitizedName, Set.of("DataSet"), new ArrayList<>(schema.getAttributes())));
            } catch (EdcException ignored) {
            }
        }

        //create all relations
        for (RelationshipSchema relationshipSchema : schemas.stream().filter(s -> s instanceof RelationshipSchema).map(s -> (RelationshipSchema) s).collect(Collectors.toList())) {
            try {
                AtlasTypesDef td = atlasApi.createRelationshipType(relationshipSchema.getName(), relationshipSchema.getDescription(), relationshipSchema.getRelationshipCategory(), relationshipSchema.getStartDefinition(), relationshipSchema.getEndDefinition());
                entityTypes.add(td);
            } catch (EdcException ignored) {
            }

        }

        return entityTypes;
    }

    public List<String> createEntities() {
        try {
            ArrayList<String> entityGuids = new ArrayList<>();
            final String policyName = "use-us-eu";
            var policyId = atlasApi.createEntity(PolicySchema.TYPE, new HashMap<>() {{
                put("name", "RegionalPolicy-US-EU");
                put("type", PolicySchema.TYPE);
                put("keyName", "foobar");
                put("description", "This policy protects assets so they can only be read inside US or EU");
                put("qualifiedName", "entity-policy-relation");
                put("serialized", "not yet available");
                put("policyName", policyName);
            }});
            entityGuids.add(policyId);

            String azureEntity1 = atlasApi.createEntity(AzureBlobStoreSchema.TYPE, AzureBlobFileEntityBuilder.newInstance()
                    .account("edcdemogpstorage")
                    .blobName("testimage.jpg")
                    .container("src-container")
                    .keyName("src-container")
                    .policy(policyName)
                    .description("this is an entity, only for EU")
                    .build());
            entityGuids.add(azureEntity1);

            String azureEntity2 = atlasApi.createEntity(AzureBlobStoreSchema.TYPE, AzureBlobFileEntityBuilder.newInstance()
                    .account("edcdemogpstorage")
                    .blobName("anotherimage.jpg")
                    .container("src-container")
                    .keyName("src-container")
                    .policy(policyName)
                    .description("this is a second entity, for US or EU")
                    .build());
            entityGuids.add(azureEntity2);

            String s3Entity1 = atlasApi.createEntity(S3BucketSchema.TYPE, S3BucketFileEntityBuilder.newInstance()
                    .bucket("dataspaceconnector-src-bucket")
                    .region("us-east-1")
                    .name("s3-testimage.jpg")
                    .description("this is a file hosted in an S3 bucket")
                    .keyname("aws-credentials")
                    .policy(policyName)
                    .build());
            entityGuids.add(s3Entity1);

            String s3Entity2 = atlasApi.createEntity(S3BucketSchema.TYPE, S3BucketFileEntityBuilder.newInstance()
                    .bucket("dataspaceconnector-src-bucket")
                    .region("us-east-1")
                    .name("s3-anotherimage.jpg")
                    .description("this is another file hosted in an S3 bucket")
                    .keyname("aws-credentials")
                    .policy(policyName)
                    .build());
            entityGuids.add(s3Entity2);


            try {
                var relation = atlasApi.createRelationship(azureEntity1, policyId, AzureBlobHasPolicyRelationshipSchema.TYPE);
                entityGuids.add(relation.getGuid());
            } catch (EdcException ignored) {
            }

            try {
                var relation2 = atlasApi.createRelationship(azureEntity2, policyId, AzureBlobHasPolicyRelationshipSchema.TYPE);
                entityGuids.add(relation2.getGuid());
            } catch (EdcException ignored) {
            }

            try {
                var relation3 = atlasApi.createRelationship(s3Entity1, policyId, AmazonS3HasPolicyRelationshipSchema.TYPE);
                entityGuids.add(relation3.getGuid());
            } catch (EdcException ignored) {
            }

            try {
                var relation4 = atlasApi.createRelationship(s3Entity2, policyId, AmazonS3HasPolicyRelationshipSchema.TYPE);
                entityGuids.add(relation4.getGuid());
            } catch (EdcException ignored) {
            }

            return entityGuids;

        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    public void deleteEntities(List<String> guids) {
        if (guids != null && !guids.isEmpty()) {
            atlasApi.deleteEntities(guids);
        }
    }

    public void deleteClassifications(String... classificationNames) {
        if (classificationNames != null) {
            Arrays.stream(classificationNames).forEach(atlasApi::deleteClassification);
        }
    }

    public void deleteEntityTypes(List<AtlasTypesDef> entityTypes) {
        if (entityTypes != null && !entityTypes.isEmpty()) {
            atlasApi.deleteType(entityTypes);
        }
    }


}
