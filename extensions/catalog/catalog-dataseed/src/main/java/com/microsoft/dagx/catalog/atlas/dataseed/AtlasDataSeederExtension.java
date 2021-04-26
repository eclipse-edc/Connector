package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasExtension;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;


public class AtlasDataSeederExtension implements ServiceExtension {

    private final boolean shouldCleanup;
    private AtlasApi atlasApi;
    private Monitor monitor;
    private AtlasTypesDef classificationTypes;
    List<AtlasTypesDef> entityTypes = new ArrayList<>();

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

        createClassifications();
        createTypedefs();
        monitor.info("Done seeding data to Atlas");
    }

    private void createTypedefs() {
        var mapper = new ObjectMapper();

        // convert JSON array to list of entities
        try {
            var typeDefs = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream("types.json"), AtlasTypeDefDto[].class);

            monitor.debug("Create "+typeDefs.length+" Atlas type def(s):");

            for (var typeDef : typeDefs) {
                monitor.debug("   Creating '"+typeDef.getTypeKeyName()+"'...");
                entityTypes.add(atlasApi.createTypesDef(typeDef.getTypeKeyName(), typeDef.getSuperTypeNames(), typeDef.getAttributes()));
                monitor.debug("   done");
            }
            monitor.info(typeDefs[0].toString());
        } catch (IOException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public void shutdown() {
        if (shouldCleanup) {
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

        }
    }
}
