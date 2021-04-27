package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasExtension;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class AtlasDataSeederExtension implements ServiceExtension {

    private final boolean shouldCleanup;
    private AtlasApi atlasApi;
    private Monitor monitor;
    private AtlasTypesDef classificationTypes;
    List<AtlasTypesDef> entityTypes = new ArrayList<>();
    private List<String> entityGuids;

    public AtlasDataSeederExtension() {
        shouldCleanup = true;

    }

    public AtlasDataSeederExtension(boolean shouldCleanup) {
        this.shouldCleanup = shouldCleanup;
    }

    @Override
    public Set<String> requires() {
        return Set.of(AtlasExtension.ATLAS_FEATURE);
    }


    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        atlasApi = context.getService(AtlasApi.class);

        monitor = context.getMonitor();
        monitor.info("Initialized Atlas Data Seeder");
    }


    @Override
    public void start() {
        monitor.info("Starting to seed data to Atlas");
        try {
            createClassifications();
            createTypedefs();
            createEntities();
        } catch (DagxException e) {
            monitor.severe("Error seeding Atlas data", e);
        }
        monitor.info("Done seeding data to Atlas");
    }


    @Override
    public void shutdown() {
        if (shouldCleanup) {
            monitor.info("delete entities");
            atlasApi.deleteEntities(entityGuids);
            monitor.info("Cleaning up Classifications");
            atlasApi.deleteType(Collections.singletonList(classificationTypes));
            monitor.info("Cleaning up Entity Types");
            atlasApi.deleteType(entityTypes);
        }
    }

    private void createClassifications() {
        var mapper = new ObjectMapper();
        try {
            Map<String, List<String>> classifications = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("classifications.json"), Map.class);
            var c = classifications.keySet().stream().flatMap(key -> classifications.get(key).stream()).toArray(String[]::new);
            classificationTypes = atlasApi.createClassifications(c);

        } catch (IOException e) {
            throw new DagxException(e);
        }
    }

    private void createTypedefs() {
        var mapper = new ObjectMapper();

        // convert JSON array to list of entities
        try {
            var typeDefs = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("types.json"), AtlasTypeDefDto[].class);

            monitor.debug("Create " + typeDefs.length + " Atlas type def(s):");

            for (var typeDef : typeDefs) {
                monitor.debug("   Creating '" + typeDef.getTypeKeyName() + "'...");
                entityTypes.add(atlasApi.createTypesDef(typeDef.getTypeKeyName(), typeDef.getSuperTypeNames(), typeDef.getAttributes()));
                monitor.debug("   done");
            }
            monitor.info(typeDefs[0].toString());
        } catch (IOException e) {
            throw new DagxException(e);
        }
    }


    private void createEntities() {
        var mapper = new ObjectMapper();
        try {
            Map<?, ?> entities = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("entities.json"), Map.class);
            String entityTypeName = (String) entities.getOrDefault("entityTypeName", null);
            List<Map<String, Object>> entityList = (List<Map<String, Object>>) entities.getOrDefault("entities", null);

            entityGuids = new ArrayList<>();
            monitor.debug("Create entities");
            for (Map<String, Object> entity : entityList) {
                monitor.debug("   creating " + entity.get("name"));
                entityGuids.add(createEntityOfType(entityTypeName, entity));
            }

        } catch (Exception e) {
            throw new DagxException(e);
        }
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
            monitor.severe("    Contains more than 1 guid.");
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
