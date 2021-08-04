/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
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

