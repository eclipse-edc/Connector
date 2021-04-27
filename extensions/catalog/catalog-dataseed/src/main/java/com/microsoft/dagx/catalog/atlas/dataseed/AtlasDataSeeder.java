package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AtlasDataSeeder {
    private final AtlasApi atlasApi;

    public AtlasDataSeeder(AtlasApi atlasApi) {

        this.atlasApi = atlasApi;
    }

    public AtlasTypesDef createClassifications() {
        var mapper = new ObjectMapper();
        try {
            Map<String, List<String>> classifications = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("classifications.json"), Map.class);
            var c = classifications.keySet().stream().flatMap(key -> classifications.get(key).stream()).toArray(String[]::new);
            return atlasApi.createClassifications(c);

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
                entityTypes.add(atlasApi.createTypesDef(typeDef.getTypeKeyName(), typeDef.getSuperTypeNames(), typeDef.getAttributes()));
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
                entityGuids.add(createEntityOfType(entityTypeName, entity));
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

    public void deleteClassificationTypes(AtlasTypesDef classificationTypes) {

        if (classificationTypes != null) {
            atlasApi.deleteType(Collections.singletonList(classificationTypes));
        }
    }

    public void deleteEntityTypes(List<AtlasTypesDef> entityTypes) {
        if (entityTypes != null && !entityTypes.isEmpty())
            atlasApi.deleteType(entityTypes);
    }


    private String createEntityOfType(String typeName, Map<String, Object> entity) throws Exception {
        AtlasEntity atlasEntity = new AtlasEntity(typeName);

        for (String key : entity.keySet()) {
            if (key.equals("classifications")) {
                continue;
            }
            atlasEntity.setAttribute(key, (String) entity.get(key));
        }


        List<String> classificationNames = (List<String>) entity.get("classifications");

        atlasEntity.setClassifications(toAtlasClassifications(classificationNames));

        var response = atlasApi.createEntity(new AtlasEntity.AtlasEntityWithExtInfo(atlasEntity));

        var guidMap = response.getGuidAssignments();

        if (guidMap.size() != 1) {
            throw new Exception("Try to create one entity but received multiple guid back.");
        } else {
            for (Map.Entry<String, String> entry : guidMap.entrySet()) {
                return entry.getValue();
            }
        }

        return null;
    }

    private List<AtlasClassification> toAtlasClassifications(List<String> classificationNames) {
        if (classificationNames != null) {
            return classificationNames.stream().map(AtlasClassification::new).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
