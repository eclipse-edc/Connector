package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.spi.DagxException;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtlasDataSeeder {
    private final AtlasApi atlasApi;

    public AtlasDataSeeder(AtlasApi atlasApi) {

        this.atlasApi = atlasApi;
    }

    public String[] createClassifications() {
        var mapper = new ObjectMapper();
        try {
            Map<String, List<String>> classifications = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("classifications.json"), Map.class);
            String[] classificationNames = classifications.keySet().stream().flatMap(key -> classifications.get(key).stream()).toArray(String[]::new);
            atlasApi.createClassifications(classificationNames);
            return classificationNames;

        } catch (IOException e) {
            throw new DagxException(e);
        }

    }

    public List<AtlasTypesDef> createTypedefs() {
        var mapper = new ObjectMapper();

        // convert JSON array to list of entities
        try {
            var typeDefs = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("types.json"), AtlasTypeDefDto[].class);

            List<AtlasTypesDef> entityTypes = new ArrayList<>();
            for (var typeDef : typeDefs) {
                entityTypes.add(atlasApi.createCustomTypes(typeDef.getTypeKeyName(), typeDef.getSuperTypeNames(), typeDef.getAttributes()));
            }
            return entityTypes;
        } catch (IOException e) {
            throw new DagxException(e);
        }
    }

    public List<String> createEntities() {
        var mapper = new ObjectMapper();
        try {
            Map<?, ?> entities = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("entities.json"), Map.class);
            String entityTypeName = (String) entities.getOrDefault("entityTypeName", null);
            List<Map<String, Object>> entityList = (List<Map<String, Object>>) entities.getOrDefault("entities", null);

            ArrayList<String> entityGuids = new ArrayList<>();
            for (Map<String, Object> entity : entityList) {
                entityGuids.add(atlasApi.createEntity(entityTypeName, entity));
            }
            return entityGuids;

        } catch (Exception e) {
            throw new DagxException(e);
        }
    }

    public void deleteEntities(List<String> guids) {
        if (guids != null && !guids.isEmpty())
            atlasApi.deleteEntities(guids);
    }

    public void deleteClassifications(String... classificationNames) {
        if (classificationNames != null) {
            atlasApi.deleteClassification(classificationNames);
        }
    }

    public void deleteEntityTypes(List<AtlasTypesDef> entityTypes) {
        if (entityTypes != null && !entityTypes.isEmpty())
            atlasApi.deleteType(entityTypes);
    }


}
