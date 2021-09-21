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

package org.eclipse.dataspaceconnector.policy.registry.memory;

import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryPolicyRegistryExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(PolicyRegistry.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(PolicyRegistry.class, new InMemoryPolicyRegistry());
        context.getMonitor().info("Initialized In-Memory Policy Registry extension");
    }

}
