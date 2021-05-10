/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasExtension;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import org.apache.atlas.model.typedef.AtlasTypesDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class AtlasDataSeederExtension implements ServiceExtension {

    private final boolean shouldCleanup;
    List<AtlasTypesDef> entityTypes = new ArrayList<>();
    private Monitor monitor;
    private String[] classificationNames;
    private List<String> entityGuids;
    private AtlasDataSeeder dataSeeder;

    public AtlasDataSeederExtension() {
        shouldCleanup = true;

    }

    public AtlasDataSeederExtension(boolean shouldCleanup) {
        this.shouldCleanup = shouldCleanup;
    }

    @Override
    public Set<String> requires() {
        return Set.of(AtlasExtension.ATLAS_FEATURE, SchemaRegistry.FEATURE);
    }


    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        dataSeeder = new AtlasDataSeeder(context.getService(AtlasApi.class), context.getService(SchemaRegistry.class));

        monitor.info("Initialized Atlas Data Seeder");
    }


    @Override
    public void start() {
        monitor.info("Starting to seed data to Atlas");
//        try {
//            monitor.debug("Create Classifications");
//            classificationNames = dataSeeder.createClassifications();
//        } catch (DagxException e) {
//            monitor.severe("Error creating classifications", e);
//        }

        try {
            monitor.debug("Create TypeDefs");
            entityTypes = dataSeeder.createTypedefs();
        } catch (DagxException e) {
            monitor.severe("Error creating TypeDefs", e);
        }

        try {
            monitor.debug("Create Entities");
            entityGuids = dataSeeder.createEntities();
        } catch (DagxException e) {
            monitor.severe("Error creating Entities", e);
        }

        monitor.info("Done seeding data to Atlas");
    }


    @Override
    public void shutdown() {
        if (shouldCleanup) {
            monitor.info("Cleaning up Entities");
            dataSeeder.deleteEntities(entityGuids);
            monitor.info("Cleaning up Classifications");
            dataSeeder.deleteClassifications(classificationNames);
            monitor.info("Cleaning up Entity Types");
            try {
                dataSeeder.deleteEntityTypes(entityTypes);
            } catch (DagxException ex) {
                monitor.severe("Error cleaning up Atlas entities", ex);
            }
        }
    }

}
