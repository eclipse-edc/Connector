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

package org.eclipse.dataspaceconnector.core.schema;

import org.eclipse.dataspaceconnector.core.BaseExtension;
import org.eclipse.dataspaceconnector.core.schema.policy.PolicySchema;
import org.eclipse.dataspaceconnector.spi.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@BaseExtension
@Provides(SchemaRegistry.class)
public class SchemaExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Schema Registry";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var sr = new SchemaRegistryImpl();
        sr.register(new PolicySchema());

        context.registerService(SchemaRegistry.class, sr);
    }
}

