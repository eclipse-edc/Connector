/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.schema;

import org.eclipse.dataspaceconnector.schema.policy.PolicySchema;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class SchemaExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(SchemaRegistry.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sr = new SchemaRegistryImpl();
        sr.register(new PolicySchema());

        context.registerService(SchemaRegistry.class, sr);
        monitor.info("Initialized Schema Registry");

    }
}

