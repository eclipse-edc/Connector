package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasCustomTypeAttribute;
import com.microsoft.dagx.spi.DagxException;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;

public class AtlasDataSeeder {
    private final AtlasApi atlasApi;

    public AtlasDataSeeder(AtlasApi atlasApi) {

        this.atlasApi = atlasApi;
    }

    public String[] createClassifications() {
        var mapper = new ObjectMapper();
        try {
            Map<String, List<String>> classifications = mapper.readValue(getClass().getClassLoader().getResourceAsStream("classifications.json"), Map.class);
            String[] classificationNames = classifications.keySet().stream().flatMap(key -> classifications.get(key).stream()).toArray(String[]::new);
            atlasApi.createClassifications(classificationNames);
            return classificationNames;

        } catch (IOException e) {
            throw new DagxException(e);
        }

    }

    public List<AtlasTypesDef> createTypedefs() {
        List<AtlasTypesDef> entityTypes = new ArrayList<>();
        entityTypes.add(atlasApi.createCustomTypes("AzureBlobFile", Set.of("DataSet"), AtlasCustomTypeAttribute.AZURE_BLOB_ATTRS));
        entityTypes.add(atlasApi.createCustomTypes("S3Bucket", Set.of("DataSet"), AtlasCustomTypeAttribute.AMAZON_S3_BUCKET_ATTRS));
        //todo: add more source file types, e.g. S3, etc.
        return entityTypes;
    }

    public List<String> createEntities() {
        var mapper = new ObjectMapper();
        try {
            Map<?, ?> entities = mapper.readValue(getClass().getClassLoader().getResourceAsStream("entities.json"), Map.class);
            String entityTypeName = (String) entities.getOrDefault("entityTypeName", null);
            List<Map<String, Object>> entityList = (List<Map<String, Object>>) entities.getOrDefault("entities", null);

            // create entities that are stored in the json file
            ArrayList<String> entityGuids = new ArrayList<>();
            for (Map<String, Object> entity : entityList) {
                entityGuids.add(atlasApi.createEntity(entityTypeName, entity));
            }

            // create another entity from code
            entityGuids.add(atlasApi.createEntity(AzureBlobFileEntityBuilder.ENTITY_TYPE_NAME, AzureBlobFileEntityBuilder.newInstance()
                    .withAccount("dagxblobstoreitest")
                    .withBlobname("testimage.jpg")
                    .withContainer("testcontainer")
                    .withKeyName("dagxblobstoreitest-key1")
                    .withDescription("this is a second entity")
                    .build()));
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
