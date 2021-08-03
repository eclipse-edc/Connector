/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.dataseed.atlas;

import org.eclipse.edc.catalog.atlas.dto.AtlasTypesDef;
import org.eclipse.edc.catalog.atlas.metadata.AtlasApi;
import org.eclipse.edc.catalog.atlas.metadata.AtlasExtension;
import org.eclipse.edc.schema.SchemaRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

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
        this(false);

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

        try {
            monitor.debug("Create TypeDefs");
            entityTypes = dataSeeder.createTypedefs();
        } catch (EdcException e) {
            monitor.severe("Error creating TypeDefs", e);
        }

        try {
            monitor.debug("Create Entities");
            entityGuids = dataSeeder.createEntities();
        } catch (EdcException e) {
//            monitor.severe("Error creating Entities", e);
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
            } catch (EdcException ex) {
                monitor.severe("Error cleaning up Atlas entities", ex);
            }
        }
    }

}
